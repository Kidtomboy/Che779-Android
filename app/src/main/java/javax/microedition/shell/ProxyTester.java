/*
 *  Che779 Emulator - kiểm tra proxy (HTTP CONNECT, SOCKS4, SOCKS5) bằng cách xin proxy nối tới 1.1.1.1:443.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class ProxyTester {
	private static final int[] TARGET = {1, 1, 1, 1};
	private static final int TARGET_PORT = 443;

	private ProxyTester() {
	}

	/** Trả về độ trễ (ms) nếu proxy chạy được với loại {@code type}, -1 nếu không. */
	static int test(NetConfig c, int type, int timeoutMs) {
		long t0 = System.nanoTime();
		Socket s = new Socket();
		try {
			s.connect(new InetSocketAddress(c.host, c.port), timeoutMs);
			s.setSoTimeout(timeoutMs);
			InputStream in = s.getInputStream();
			OutputStream out = s.getOutputStream();
			boolean ok;
			if (type == NetConfig.SOCKS5) {
				ok = socks5(c, in, out);
			} else if (type == NetConfig.SOCKS4) {
				ok = socks4(c, in, out);
			} else {
				ok = http(c, in, out);
			}
			return ok ? (int) Math.max(1, (System.nanoTime() - t0) / 1000000L) : -1;
		} catch (Exception e) {
			return -1;
		} finally {
			try {
				s.close();
			} catch (IOException ignored) {
			}
		}
	}

	private static boolean hasUser(NetConfig c) {
		return c.user != null && !c.user.isEmpty();
	}

	private static boolean http(NetConfig c, InputStream in, OutputStream out) throws IOException {
		StringBuilder sb = new StringBuilder("CONNECT 1.1.1.1:443 HTTP/1.1\r\nHost: 1.1.1.1:443\r\n");
		if (hasUser(c)) {
			String cred = c.user + ":" + (c.pass == null ? "" : c.pass);
			sb.append("Proxy-Authorization: Basic ")
					.append(android.util.Base64.encodeToString(cred.getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP))
					.append("\r\n");
		}
		sb.append("\r\n");
		out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
		out.flush();
		StringBuilder line = new StringBuilder();
		int b;
		while (line.length() < 200 && (b = in.read()) >= 0 && b != '\n') {
			line.append((char) b);
		}
		String l = line.toString();
		return l.startsWith("HTTP/") && l.contains(" 200");
	}

	private static boolean socks5(NetConfig c, InputStream in, OutputStream out) throws IOException {
		if (hasUser(c)) {
			out.write(new byte[]{5, 2, 0, 2});
		} else {
			out.write(new byte[]{5, 1, 0});
		}
		out.flush();
		byte[] r = new byte[2];
		readFully(in, r);
		if (r[0] != 5 || (r[1] & 0xFF) == 0xFF) {
			return false;
		}
		if (r[1] == 2) {
			byte[] u = c.user.getBytes(StandardCharsets.UTF_8);
			byte[] p = (c.pass == null ? "" : c.pass).getBytes(StandardCharsets.UTF_8);
			if (u.length > 255 || p.length > 255) {
				return false;
			}
			byte[] a = new byte[3 + u.length + p.length];
			a[0] = 1;
			a[1] = (byte) u.length;
			System.arraycopy(u, 0, a, 2, u.length);
			a[2 + u.length] = (byte) p.length;
			System.arraycopy(p, 0, a, 3 + u.length, p.length);
			out.write(a);
			out.flush();
			readFully(in, r);
			if (r[1] != 0) {
				return false;
			}
		} else if (r[1] != 0) {
			return false;
		}
		out.write(new byte[]{5, 1, 0, 1, (byte) TARGET[0], (byte) TARGET[1], (byte) TARGET[2], (byte) TARGET[3],
				(byte) (TARGET_PORT >> 8), (byte) TARGET_PORT});
		out.flush();
		byte[] h = new byte[4];
		readFully(in, h);
		return h[0] == 5 && h[1] == 0;
	}

	private static boolean socks4(NetConfig c, InputStream in, OutputStream out) throws IOException {
		byte[] u = (hasUser(c) ? c.user : "").getBytes(StandardCharsets.UTF_8);
		byte[] a = new byte[9 + u.length];
		a[0] = 4;
		a[1] = 1;
		a[2] = (byte) (TARGET_PORT >> 8);
		a[3] = (byte) TARGET_PORT;
		for (int i = 0; i < 4; i++) {
			a[4 + i] = (byte) TARGET[i];
		}
		System.arraycopy(u, 0, a, 8, u.length);
		out.write(a);
		out.flush();
		byte[] r = new byte[8];
		readFully(in, r);
		return r[1] == 0x5A;
	}

	private static void readFully(InputStream in, byte[] b) throws IOException {
		int off = 0;
		while (off < b.length) {
			int n = in.read(b, off, b.length - off);
			if (n < 0) {
				throw new IOException("eof");
			}
			off += n;
		}
	}
}
