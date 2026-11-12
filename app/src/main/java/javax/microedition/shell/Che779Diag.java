/*
 *  Che779 Emulator - diagnostics written to a file (logcat tail is too short to catch game errors).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.config.Config;

/** Writes events and exceptions to J2ME-Che779/che779-diag.txt and shows errors as a toast. */
public final class Che779Diag {
	private static final String TAG = "Che779";
	private static final Handler UI = new Handler(Looper.getMainLooper());
	private static int toasts;
	private static boolean installed;

	private Che779Diag() {
	}

	public static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;
		final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			error("Uncaught in thread " + t.getName(), e);
			if (prev != null) {
				prev.uncaughtException(t, e);
			}
		});
		log("diag installed, process " + android.os.Process.myPid());
	}

	private static File file() {
		try {
			File dir = new File(Config.getEmulatorDir());
			if (!dir.isDirectory() && !dir.mkdirs()) {
				return null;
			}
			return new File(dir, "che779-diag.txt");
		} catch (Throwable t) {
			return null;
		}
	}

	public static void log(String msg) {
		Session s = Session.current();
		logForSession(msg, s == null ? -1 : s.id);
	}

	private static synchronized void logForSession(String msg, int sessionId) {
		Log.i(TAG, msg);
		try {
			File f = file();
			if (f == null) {
				return;
			}
			String entry = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date())
					+ " [" + Thread.currentThread().getName() + "] "
					+ (sessionId < 0 ? "" : "S" + sessionId + " ") + msg + "\n";
			new DiagnosticLogStore(f, DiagnosticLogStore.DEFAULT_MAX_BYTES).append(entry);
		} catch (Throwable ignored) {
		}
	}

	private static final AtomicInteger totalDumps = new AtomicInteger();
	private static final AtomicInteger noSessionDumps = new AtomicInteger();

	/** Writes stacks after a delay; limits are per tab (3) and per process (20). */
	public static void dumpThreadsLater(final String why, final long delayMs) {
		final long safeDelayMs = Math.max(0L, Math.min(delayMs, 60_000L));
		Session owner = Session.current();
		final int sessionId = owner == null ? -1 : owner.id;
		AtomicInteger perSession = owner == null ? noSessionDumps : owner.diagnosticDumps;
		if (perSession.incrementAndGet() > 3) {
			perSession.decrementAndGet();
			return;
		}
		if (totalDumps.incrementAndGet() > 20) {
			totalDumps.decrementAndGet();
			perSession.decrementAndGet();
			return;
		}
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(safeDelayMs);
			} catch (InterruptedException ignored) {
			}
			StringBuilder sb = new StringBuilder("THREAD DUMP (" + why + ")\n");
			for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
				Thread th = e.getKey();
				StackTraceElement[] st = e.getValue();
				if (st.length == 0) {
					continue;
				}
				sb.append("--- ").append(th.getName()).append(" state=").append(th.getState()).append('\n');
				for (int i = 0; i < Math.min(st.length, 22); i++) {
					sb.append("    at ").append(st[i]).append('\n');
				}
			}
			logForSession(sb.toString(), sessionId);
		}, "Che779-Dump");
		t.setDaemon(true);
		t.start();
	}

	public static void error(String what, Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		log("ERROR " + what + "\n" + sw);
		if (toasts++ < 3) {
			final String text = what + ": " + e;
			UI.post(() -> {
				try {
					Toast.makeText(ContextHolder.getActivity(), text, Toast.LENGTH_LONG).show();
				} catch (Throwable ignored) {
				}
			});
		}
	}
}
