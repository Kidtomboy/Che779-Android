/*
 *  Che779 Emulator - parallel multi-dex converter.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 *  Idea: J2ME Loader v10 (Hung Hero) splits big jars into converted.dex, converted2.dex, ...
 */
package ru.woesss.j2me.installer;

import com.android.dx.command.dexer.Main;
import com.android.dx.command.DxContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ru.playsoftware.j2meloader.config.Config;

/**
 * Converts a jar into one or several dex files.
 * <p>
 * Small jars become a single converted.dex exactly as before. Big jars (merged games such as
 * x3 / x6 / xN) are split by class count into converted.dex, converted2.dex, ... and the chunks are
 * converted in parallel. This also avoids the 65,536 method limit of a single dex.
 */
final class MultiDex {
	/** Jars with at most this many classes use the old single-dex path. */
	private static final int SINGLE_DEX_MAX_CLASSES = 800;
	/** Classes per dex file when splitting. */
	private static final int CLASSES_PER_DEX = 500;

	private MultiDex() {
	}

	static void convert(File jar, File outDir) throws IOException {
		List<String> names = new ArrayList<>();
		try (ZipFile zip = new ZipFile(jar)) {
			Enumeration<? extends ZipEntry> en = zip.entries();
			while (en.hasMoreElements()) {
				ZipEntry e = en.nextElement();
				if (!e.isDirectory() && e.getName().endsWith(".class")) {
					names.add(e.getName());
				}
			}
		}
		if (names.size() <= SINGLE_DEX_MAX_CLASSES) {
			run(new String[]{"--no-optimize", "--core-library",
					"--output=" + outDir + Config.MIDLET_DEX_FILE, jar.getAbsolutePath()});
			return;
		}
		File classDir = new File(outDir, ".classes");
		if (!classDir.isDirectory() && !classDir.mkdirs()) {
			throw new IOException("Can't create directory: " + classDir);
		}
		try {
			List<List<String>> chunks = new ArrayList<>();
			List<String> all = new ArrayList<>();
			try (ZipFile zip = new ZipFile(jar)) {
				int i = 0;
				List<String> cur = new ArrayList<>();
				for (String name : names) {
					File f = new File(classDir, "c" + (i++) + ".class");
					try (InputStream in = zip.getInputStream(zip.getEntry(name));
						 FileOutputStream out = new FileOutputStream(f)) {
						byte[] buf = new byte[16384];
						int n;
						while ((n = in.read(buf)) > 0) {
							out.write(buf, 0, n);
						}
					}
					cur.add(f.getAbsolutePath());
					if (cur.size() == CLASSES_PER_DEX) {
						chunks.add(cur);
						cur = new ArrayList<>();
					}
				}
				if (!cur.isEmpty()) {
					chunks.add(cur);
				}
			}
			int threads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 4));
			ExecutorService pool = Executors.newFixedThreadPool(threads);
			try {
				List<Future<?>> jobs = new ArrayList<>();
				for (int c = 0; c < chunks.size(); c++) {
					final String out = outDir + (c == 0 ? Config.MIDLET_DEX_FILE : "/converted" + (c + 1) + ".dex");
					final List<String> files = chunks.get(c);
					jobs.add(pool.submit(() -> {
						List<String> args = new ArrayList<>();
						args.add("--no-optimize");
						args.add("--no-strict");
						args.add("--core-library");
						args.add("--output=" + out);
						args.addAll(files);
						run(args.toArray(new String[0]));
						return null;
					}));
				}
				for (Future<?> job : jobs) {
					job.get();
				}
			} catch (Exception e) {
				throw new IOException("Multi-dex conversion failed", e);
			} finally {
				pool.shutdownNow();
			}
		} finally {
			deleteRecursive(classDir);
		}
	}

	private static void run(String[] args) throws IOException {
		Main.Arguments arguments = new Main.Arguments(new DxContext());
		arguments.parse(args);
		int code = new Main(new DxContext()).runDx(arguments);
		if (code != 0) {
			throw new IOException("dx failed with code " + code);
		}
	}

	private static void deleteRecursive(File f) {
		File[] children = f.listFiles();
		if (children != null) {
			for (File c : children) {
				deleteRecursive(c);
			}
		}
		//noinspection ResultOfMethodCallIgnored
		f.delete();
	}
}
