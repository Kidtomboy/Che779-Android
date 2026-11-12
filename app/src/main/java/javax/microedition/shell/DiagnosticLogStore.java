/*
 * Che779 Emulator - bounded, two-generation diagnostic log storage.
 * Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Keeps the current diagnostic log and one previous generation without unbounded growth. */
final class DiagnosticLogStore {
    static final long DEFAULT_MAX_BYTES = 3L * 1024L * 1024L;

    private final File current;
    private final File previous;
    private final long maxBytes;

    DiagnosticLogStore(File current, long maxBytes) {
        if (current == null) throw new IllegalArgumentException("current log file is required");
        if (maxBytes < 128) throw new IllegalArgumentException("maxBytes must be at least 128");
        this.current = current;
        this.previous = new File(current.getParentFile(), current.getName() + ".old");
        this.maxBytes = maxBytes;
    }

    synchronized void append(String entry) throws IOException {
        byte[] bytes = truncateUtf8(entry == null ? "" : entry, (int) Math.min(maxBytes, Integer.MAX_VALUE));
        File parent = current.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create diagnostic directory: " + parent);
        }
        long existing = current.isFile() ? current.length() : 0L;
        if (existing + bytes.length > maxBytes) rotate();
        try (FileOutputStream out = new FileOutputStream(current, true)) {
            out.write(bytes);
        }
    }

    private void rotate() throws IOException {
        if (!current.isFile()) return;
        if (previous.exists() && !previous.delete()) {
            throw new IOException("Cannot remove previous diagnostic log: " + previous);
        }
        if (current.renameTo(previous)) return;

        // Some Android filesystems do not support an atomic rename; copy before truncating.
        try (InputStream in = new FileInputStream(current);
             OutputStream out = new FileOutputStream(previous, false)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            out.flush();
        } catch (IOException copyFailure) {
            previous.delete();
            throw copyFailure;
        }
        try (FileOutputStream ignored = new FileOutputStream(current, false)) {
            // Truncate only after the previous generation has been copied successfully.
        }
    }

    private static byte[] truncateUtf8(String value, int maxBytes) {
        byte[] all = value.getBytes(StandardCharsets.UTF_8);
        if (all.length <= maxBytes) return all;
        // Preserve valid UTF-8 by shortening at code-point boundaries.
        int end = value.length();
        while (end > 0) {
            end = value.offsetByCodePoints(end, -1);
            byte[] candidate = value.substring(end).getBytes(StandardCharsets.UTF_8);
            if (candidate.length <= maxBytes) return candidate;
        }
        return new byte[0];
    }
}
