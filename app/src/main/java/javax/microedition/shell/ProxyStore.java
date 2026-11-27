/*
 *  Che779 Emulator - kho proxy dùng chung (một nguồn dữ liệu duy nhất cho hộp thoại danh sách và bảng lưới/cửa sổ nổi).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class ProxyStore {
	static final String LIST = "che779_proxy_list";
	static final String FORMAT_HELP =
			"Định dạng nhập được (mỗi dòng một proxy, giao thức mặc định: http):\n"
			+ "• host:port\n"
			+ "• host:port:user:pass\n"
			+ "• host:port:giao_thức\n"
			+ "• host:port:user:pass:giao_thức\n"
			+ "• user:pass@host:port\n"
			+ "• giao_thức://host:port\n"
			+ "• giao_thức://user:pass@host:port\n"
			+ "• CSV/TXT cột: host,port,user,pass,giao_thức (ngăn bằng , ; | tab hoặc khoảng trắng; dòng tiêu đề tự bỏ qua)\n"
			+ "• JSON: [\"host:port\", ...] hoặc [{\"host\":..,\"port\":..,\"user\":..,\"pass\":..,\"type\":..}] "
			+ "(khoá tương đương: ip/address, username/login, password, protocol/scheme)\n"
			+ "giao_thức: http, socks4, socks5";

	private ProxyStore() {
	}

	static List<NetConfig> list(SharedPreferences sp) {
		ArrayList<NetConfig> out = new ArrayList<>();
		for (String line : sp.getString(LIST, "").split("\n")) {
			NetConfig c = NetConfig.parse(line);
			if (c != null) {
				out.add(c);
			}
		}
		return out;
	}

	static void saveList(SharedPreferences sp, List<NetConfig> l) {
		StringBuilder sb = new StringBuilder();
		for (NetConfig c : l) {
			sb.append(c.serialize()).append('\n');
		}
		sp.edit().putString(LIST, sb.toString()).apply();
	}

	/** Thêm nếu là mới (cùng host/cổng/user/pass/loại tính là trùng). Trả về true nếu đã thêm. */
	static boolean add(SharedPreferences sp, NetConfig c) {
		List<NetConfig> l = list(sp);
		String k = c.serialize();
		for (NetConfig o : l) {
			if (o.serialize().equals(k)) {
				return false;
			}
		}
		l.add(c);
		saveList(sp, l);
		return true;
	}

	/** Thay mục có dạng chuỗi là {@code oldKey}; thêm mới nếu chưa có. */
	static void replace(SharedPreferences sp, String oldKey, NetConfig c) {
		List<NetConfig> l = list(sp);
		boolean done = false;
		for (int i = 0; i < l.size(); i++) {
			if (oldKey != null && l.get(i).serialize().equals(oldKey)) {
				l.set(i, c);
				done = true;
				break;
			}
		}
		if (!done) {
			l.add(c);
		}
		saveList(sp, l);
	}

	/** Thêm hàng loạt, bỏ trùng; trả về số proxy mới được thêm. */
	static int addAll(SharedPreferences sp, List<NetConfig> add) {
		List<NetConfig> l = list(sp);
		java.util.HashSet<String> have = new java.util.HashSet<>();
		for (NetConfig o : l) {
			have.add(o.serialize());
		}
		int n = 0;
		for (NetConfig c : add) {
			if (have.add(c.serialize())) {
				l.add(c);
				n++;
			}
		}
		if (n > 0) {
			saveList(sp, l);
		}
		return n;
	}

	static void remove(SharedPreferences sp, String key) {
		List<NetConfig> l = list(sp);
		for (int i = l.size() - 1; i >= 0; i--) {
			if (l.get(i).serialize().equals(key)) {
				l.remove(i);
			}
		}
		saveList(sp, l);
	}

	// ---- theo từng tab

	static NetConfig tabProxy(SharedPreferences sp, int no) {
		return NetConfig.parse(sp.getString("che779_tab_proxy_" + no, ""));
	}

	static boolean tabEnabled(SharedPreferences sp, int no) {
		return sp.getBoolean("che779_tab_proxy_on_" + no, true);
	}

	static void setTab(SharedPreferences sp, int no, NetConfig c, boolean enabled) {
		SharedPreferences.Editor e = sp.edit();
		if (c == null) {
			e.remove("che779_tab_proxy_" + no).remove("che779_tab_proxy_on_" + no);
		} else {
			e.putString("che779_tab_proxy_" + no, c.serialize()).putBoolean("che779_tab_proxy_on_" + no, enabled);
		}
		e.apply();
	}

	/** Chép lựa chọn đã lưu của tab {@code no} vào phiên đang chạy (áp dụng cho kết nối mới). */
	static void applyTo(SharedPreferences sp, int no, Session s) {
		NetConfig c = tabProxy(sp, no);
		if (c != null && tabEnabled(sp, no)) {
			s.net.type = c.type;
			s.net.host = c.host;
			s.net.port = c.port;
			s.net.user = c.user;
			s.net.pass = c.pass;
		} else {
			s.net.type = NetConfig.NONE;
		}
	}

	// ---- import

	static final class Parsed {
		final List<NetConfig> ok = new ArrayList<>();
		int bad;
	}

	static Parsed parseImport(String text) {
		Parsed r = new Parsed();
		if (text == null) {
			return r;
		}
		if (text.startsWith("\uFEFF")) {
			text = text.substring(1);
		}
		String t = text.trim();
		if (t.startsWith("[") || t.startsWith("{")) {
			try {
				parseJson(t, r);
				return r;
			} catch (Exception ignored) {
				r.ok.clear();
				r.bad = 0;
			}
		}
		for (String raw : text.split("\\r?\\n")) {
			String line = raw.trim();
			if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
				continue;
			}
			addLine(line, r);
		}
		return r;
	}

	private static void addLine(String line, Parsed r) {
		NetConfig c = NetConfig.parse(line);
		if (c == null && !line.contains("://") && !line.contains("@")) {
			String[] tok = line.split("[,;|\\t ]+");
			if (tok.length >= 2) {
				String first = tok[0].trim().toLowerCase(java.util.Locale.ROOT);
				if (first.equals("host") || first.equals("ip") || first.equals("address") || first.equals("proxy")) {
					return; // header row
				}
				c = NetConfig.parse(String.join(":", tok));
			}
		}
		if (c == null) {
			r.bad++;
		} else {
			r.ok.add(c);
		}
	}

	private static void parseJson(String t, Parsed r) throws Exception {
		JSONArray arr;
		if (t.startsWith("[")) {
			arr = new JSONArray(t);
		} else {
			JSONObject o = new JSONObject(t);
			arr = o.optJSONArray("proxies");
			if (arr == null) {
				arr = o.optJSONArray("list");
			}
			if (arr == null) {
				arr = new JSONArray();
				arr.put(o);
			}
		}
		for (int i = 0; i < arr.length(); i++) {
			Object v = arr.get(i);
			NetConfig c = null;
			if (v instanceof String) {
				c = NetConfig.parse((String) v);
			} else if (v instanceof JSONObject) {
				JSONObject o = (JSONObject) v;
				String host = first(o, "host", "ip", "address", "server");
				String port = first(o, "port");
				if (host != null && port != null) {
					String user = first(o, "user", "username", "login");
					String pass = first(o, "pass", "password", "pwd");
					String type = first(o, "type", "protocol", "scheme", "proto");
					StringBuilder sb = new StringBuilder();
					if (type != null && NetConfig.typeOf(type) >= 0) {
						sb.append(type).append("://");
					}
					if (user != null && !user.isEmpty()) {
						sb.append(user).append(':').append(pass == null ? "" : pass).append('@');
					}
					sb.append(host).append(':').append(port);
					c = NetConfig.parse(sb.toString());
				}
			}
			if (c == null) {
				r.bad++;
			} else {
				r.ok.add(c);
			}
		}
	}

	private static String first(JSONObject o, String... keys) {
		for (String k : keys) {
			if (o.has(k) && !o.isNull(k)) {
				String s = String.valueOf(o.opt(k)).trim();
				if (!s.isEmpty()) {
					return s;
				}
			}
		}
		return null;
	}
}
