/*
 *  Che779 Emulator - routes game connections through the proxy / redirect of their tab.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package org.microemu.cldc;

import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.microedition.shell.NetConfig;
import javax.microedition.shell.Session;

public final class NetHooks {
	private static final int CONNECT_TIMEOUT = 20000;

	static {
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				NetConfig c = cfg();
				if (c != null && c.hasProxy() && c.user != null && !c.user.isEmpty()
						&& getRequestorType() == RequestorType.PROXY) {
					return new PasswordAuthentication(c.user, (c.pass == null ? "" : c.pass).toCharArray());
				}
				return null;
			}
		});
	}

	private NetHooks() {
	}

	private static NetConfig cfg() {
		Session s = Session.current();
		return s == null ? null : s.net;
	}

	/** Applies the redirect rules; returns {host, port}. */
	private static String[] redirect(NetConfig c, String host, int port) {
		if (c != null && c.redirects != null) {
			for (String line : c.redirects.split("\n")) {
				int eq = line.indexOf('=');
				if (eq <= 0) {
					continue;
				}
				String from = line.substring(0, eq).trim();
				String to = line.substring(eq + 1).trim();
				String fromHost = from;
				int fromPort = -1;
				int fc = from.lastIndexOf(':');
				if (fc > 0) {
					try {
						fromPort = Integer.parseInt(from.substring(fc + 1));
						fromHost = from.substring(0, fc);
					} catch (NumberFormatException ignored) {
					}
				}
				if (!fromHost.equalsIgnoreCase(host) || (fromPort != -1 && fromPort != port)) {
					continue;
				}
				String toHost = to;
				int toPort = port;
				int tc = to.lastIndexOf(':');
				if (tc > 0) {
					try {
						toPort = Integer.parseInt(to.substring(tc + 1));
						toHost = to.substring(0, tc);
					} catch (NumberFormatException ignored) {
					}
				}
				return new String[]{toHost, String.valueOf(toPort)};
			}
		}
		return new String[]{host, String.valueOf(port)};
	}

	/** Opens a TCP connection for a game (direct, through the proxy, with redirects applied). */
	public static Socket connect(String host, int port) throws IOException {
		NetConfig c = cfg();
		String[] r = redirect(c, host, port);
		host = r[0];
		port = Integer.parseInt(r[1]);
		if (c == null || !c.hasProxy()) {
			return new Socket(host, port);
		}
		Socket s = new Socket();
		try {
			s.connect(new InetSocketAddress(c.host, c.port), CONNECT_TIMEOUT);
			s.setSoTimeout(CONNECT_TIMEOUT);
			if (c.type == NetConfig.SOCKS5) {
				socks5(s, host, port, c);
			} else {
				httpConnect(s, host, port, c);
			}
			s.setSoTimeout(0);
		} catch (IOException e) {
			try {
				s.close();
			} catch (IOException ignored) {
			}
			throw e;
		}
		return s;
	}

	private static void readFully(InputStream in, byte[] b) throws IOException {
		int n = 0;
		while (n < b.length) {
			int k = in.read(b, n, b.length - n);
			if (k < 0) {
				throw new IOException("Proxy closed the connection");
			}
			n += k;
		}
	}

	private static void socks5(Socket s, String host, int port, NetConfig c) throws IOException {
		OutputStream out = s.getOutputStream();
		InputStream in = s.getInputStream();
		boolean auth = c.user != null && !c.user.isEmpty();
		out.write(auth ? new byte[]{5, 2, 0, 2} : new byte[]{5, 1, 0});
		out.flush();
		byte[] resp = new byte[2];
		readFully(in, resp);
		if (resp[0] != 5) {
			throw new IOException("Not a SOCKS5 proxy");
		}
		if (resp[1] == 2) {
			byte[] u = c.user.getBytes(StandardCharsets.UTF_8);
			byte[] p = (c.pass == null ? "" : c.pass).getBytes(StandardCharsets.UTF_8);
			byte[] msg = new byte[3 + u.length + p.length];
			msg[0] = 1;
			msg[1] = (byte) u.length;
			System.arraycopy(u, 0, msg, 2, u.length);
			msg[2 + u.length] = (byte) p.length;
			System.arraycopy(p, 0, msg, 3 + u.length, p.length);
			out.write(msg);
			out.flush();
			readFully(in, resp);
			if (resp[1] != 0) {
				throw new IOException("SOCKS5 login refused");
			}
		} else if (resp[1] != 0) {
			throw new IOException("SOCKS5 method refused");
		}
		byte[] h = host.getBytes(StandardCharsets.UTF_8);
		byte[] req = new byte[7 + h.length];
		req[0] = 5;
		req[1] = 1;
		req[3] = 3;
		req[4] = (byte) h.length;
		System.arraycopy(h, 0, req, 5, h.length);
		req[5 + h.length] = (byte) (port >> 8);
		req[6 + h.length] = (byte) port;
		out.write(req);
		out.flush();
		byte[] head = new byte[4];
		readFully(in, head);
		if (head[1] != 0) {
			throw new IOException("SOCKS5 connect failed (code " + head[1] + ")");
		}
		int skip = head[3] == 1 ? 6 : head[3] == 4 ? 18 : 0;
		if (head[3] == 3) {
			byte[] l = new byte[1];
			readFully(in, l);
			skip = (l[0] & 0xFF) + 2;
		}
		readFully(in, new byte[skip]);
	}

	private static void httpConnect(Socket s, String host, int port, NetConfig c) throws IOException {
		StringBuilder sb = new StringBuilder("CONNECT ").append(host).append(':').append(port)
				.append(" HTTP/1.1\r\nHost: ").append(host).append(':').append(port).append("\r\n");
		if (c.user != null && !c.user.isEmpty()) {
			String cred = c.user + ":" + (c.pass == null ? "" : c.pass);
			sb.append("Proxy-Authorization: Basic ")
					.append(Base64.encodeToString(cred.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP))
					.append("\r\n");
		}
		sb.append("\r\n");
		OutputStream out = s.getOutputStream();
		out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
		out.flush();
		InputStream in = s.getInputStream();
		StringBuilder head = new StringBuilder();
		int b;
		while ((b = in.read()) >= 0) {
			head.append((char) b);
			int n = head.length();
			if (n >= 4 && head.charAt(n - 4) == '\r' && head.charAt(n - 3) == '\n'
					&& head.charAt(n - 2) == '\r' && head.charAt(n - 1) == '\n') {
				break;
			}
			if (n > 8192) {
				throw new IOException("Proxy answer too long");
			}
		}
		String status = head.toString().split("\r\n")[0];
		if (!status.contains(" 200")) {
			throw new IOException("HTTP proxy refused: " + status);
		}
	}

	/** HTTP(S) connection of a game: redirect applied to the URL, proxy set on the connection. */
	public static URLConnection open(URL url) throws IOException {
		NetConfig c = cfg();
		if (c == null) {
			return url.openConnection();
		}
		int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
		String[] r = redirect(c, url.getHost(), port);
		if (!r[0].equalsIgnoreCase(url.getHost()) || Integer.parseInt(r[1]) != port) {
			url = new URL(url.getProtocol(), r[0], Integer.parseInt(r[1]), url.getFile());
		}
		if (!c.hasProxy()) {
			return url.openConnection();
		}
		Proxy proxy = new Proxy(c.type == NetConfig.SOCKS5 ? Proxy.Type.SOCKS : Proxy.Type.HTTP,
				InetSocketAddress.createUnresolved(c.host, c.port));
		URLConnection cn = url.openConnection(proxy);
		if (c.type == NetConfig.HTTP && c.user != null && !c.user.isEmpty()) {
			String cred = c.user + ":" + (c.pass == null ? "" : c.pass);
			cn.setRequestProperty("Proxy-Authorization",
					"Basic " + Base64.encodeToString(cred.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
		}
		return cn;
	}

	/** Raw TCP socket for ssl:// connections (TLS is layered on top by the caller). */
	public static Socket rawSocket(String host, int port) throws IOException {
		return connect(host, port);
	}
}
