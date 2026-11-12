/*
 *  Che779 Emulator - per-tab network settings (proxy + IP redirect).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 *  List-import idea: J2ME_NST (NST Tien); IP redirect idea: J2ME Loader v10 (Hung Hero).
 */
package javax.microedition.shell;

/** Proxy and redirect rules of one tab. */
public final class NetConfig {
	public static final int NONE = 0, HTTP = 1, SOCKS5 = 2;

	public volatile int type;
	public volatile String host;
	public volatile int port;
	public volatile String user;
	public volatile String pass;
	/** Lines "from[:port]=to[:port]". */
	public volatile String redirects = "";

	public boolean hasProxy() {
		return type != NONE && host != null && !host.isEmpty() && port > 0;
	}

	public String label() {
		if (!hasProxy()) {
			return "Không dùng proxy";
		}
		return (type == SOCKS5 ? "socks5://" : "http://") + host + ":" + port
				+ (user != null && !user.isEmpty() ? " (" + user + ")" : "");
	}

	/** Same string format that {@link #parse} reads. */
	public String serialize() {
		if (!hasProxy()) {
			return "";
		}
		return (type == SOCKS5 ? "socks5://" : "http://")
				+ (user != null && !user.isEmpty() ? user + ":" + (pass == null ? "" : pass) + "@" : "")
				+ host + ":" + port;
	}

	/**
	 * Accepts: [scheme://][user:pass@]host:port, or host:port:user:pass. Scheme is http or
	 * socks5 (socks, socks5h); default http. Returns null for an unreadable line.
	 */
	public static NetConfig parse(String line) {
		if (line == null) {
			return null;
		}
		line = line.trim();
		if (line.isEmpty() || line.startsWith("#")) {
			return null;
		}
		NetConfig c = new NetConfig();
		c.type = HTTP;
		int i = line.indexOf("://");
		if (i > 0) {
			String scheme = line.substring(0, i).toLowerCase(java.util.Locale.ROOT);
			if ("socks".equals(scheme) || "socks5".equals(scheme) || "socks5h".equals(scheme)) {
				c.type = SOCKS5;
			} else if ("http".equals(scheme) || "https".equals(scheme)) {
				c.type = HTTP;
			} else {
				return null;
			}
			line = line.substring(i + 3);
		}
		try {
			int at = line.lastIndexOf('@');
			if (at >= 0) {
				String cred = line.substring(0, at);
				int colon = cred.indexOf(':');
				c.user = colon >= 0 ? cred.substring(0, colon) : cred;
				c.pass = colon >= 0 ? cred.substring(colon + 1) : "";
				line = line.substring(at + 1);
			}
			String[] p = line.split(":");
			if (p.length == 2) {
				c.host = p[0];
				c.port = Integer.parseInt(p[1].trim());
			} else if (p.length == 4 && c.user == null) {
				c.host = p[0];
				c.port = Integer.parseInt(p[1].trim());
				c.user = p[2];
				c.pass = p[3];
			} else {
				return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
		// IPv6 literals are not accepted by this legacy colon-delimited format.
		// Reject invalid hosts/ports here instead of deferring failure to socket creation.
		if (c.host == null || c.host.trim().isEmpty() || c.host.indexOf('[') >= 0
				|| c.host.indexOf(']') >= 0 || c.port < 1 || c.port > 65535) {
			return null;
		}
		return c.hasProxy() ? c : null;
	}
}
