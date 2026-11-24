/*
 *  Che779 Emulator - cấu hình mạng theo từng tab (proxy + chuyển hướng IP).
 *  Giấy phép Apache License 2.0 (cùng giấy phép J2ME Loader).
 */
package javax.microedition.shell;

/** Quy tắc proxy và chuyển hướng của một tab. */
public final class NetConfig {
	public static final int NONE = 0, HTTP = 1, SOCKS5 = 2, SOCKS4 = 3;

	public static String scheme(int type) {
		return type == SOCKS5 ? "socks5" : type == SOCKS4 ? "socks4" : "http";
	}

	/** Từ giao thức -> loại, hoặc -1 nếu không phải giao thức. */
	public static int typeOf(String w) {
		if (w == null) {
			return -1;
		}
		switch (w.trim().toLowerCase(java.util.Locale.ROOT)) {
			case "http": case "https":
				return HTTP;
			case "socks4": case "socks4a":
				return SOCKS4;
			case "socks": case "socks5": case "socks5h":
				return SOCKS5;
			default:
				return -1;
		}
	}

	public volatile int type;
	public volatile String host;
	public volatile int port;
	public volatile String user;
	public volatile String pass;
	/** Các dòng "từ[:cổng]=đến[:cổng]". */
	public volatile String redirects = "";

	public boolean hasProxy() {
		return type != NONE && host != null && !host.isEmpty() && port > 0;
	}

	public String label() {
		if (!hasProxy()) {
			return "Không dùng proxy";
		}
		return scheme(type) + "://" + host + ":" + port
				+ (user != null && !user.isEmpty() ? " (" + user + ")" : "");
	}

	/** Cùng định dạng chuỗi mà {@link #parse} đọc. */
	public String serialize() {
		if (!hasProxy()) {
			return "";
		}
		return scheme(type) + "://"
				+ (user != null && !user.isEmpty() ? user + ":" + (pass == null ? "" : pass) + "@" : "")
				+ host + ":" + port;
	}

	/**
	 * Chấp nhận: [scheme://][user:pass@]host:port, hoặc host:port:user:pass. Scheme là http hoặc
	 * socks5 (socks, socks5h); mặc định http. Trả về null nếu dòng không đọc được.
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
			int t = typeOf(scheme);
			if (t < 0) {
				return null;
			}
			c.type = t;
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
			if ((p.length == 3 || p.length == 5) && typeOf(p[p.length - 1]) >= 0) {
				// host:port:giao_thức  /  host:port:user:pass:giao_thức
				c.type = typeOf(p[p.length - 1]);
				p = java.util.Arrays.copyOf(p, p.length - 1);
			}
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
		// Định dạng cũ phân tách bằng dấu hai chấm này không nhận địa chỉ IPv6 dạng chữ.
		// Từ chối host/cổng sai ngay tại đây thay vì để lỗi xảy ra lúc tạo socket.
		if (c.host == null || c.host.trim().isEmpty() || c.host.indexOf('[') >= 0
				|| c.host.indexOf(']') >= 0 || c.port < 1 || c.port > 65535) {
			return null;
		}
		return c.hasProxy() ? c : null;
	}
}
