/*
 *  Che779 Emulator - sao lưu cài đặt, proxy, macro, phím tắt, ảnh nền bàn phím vào thư mục Che779Data
 *  cạnh thư mục game, để gỡ app cài lại vẫn khôi phục được.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import ru.playsoftware.j2meloader.config.Config;

public final class Che779Backup {
	private static final String MARK = "che779_restored_v1";
	private static final String[] SKIP = {MARK, "che779_tabs", "update_last_check", "che779_guide_seen"};
	private static final String BG = "keypad_bg.png";
	private static final Object LOCK = new Object();

	private Che779Backup() {
	}

	private static File dir() {
		String d = Config.getEmulatorDir();
		return d == null || d.isEmpty() ? null : new File(d, "Che779Data");
	}

	private static SharedPreferences main(Context c) {
		return c.getSharedPreferences(c.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
	}

	private static SharedPreferences macro(Context c) {
		return c.getSharedPreferences("che779_macro", Context.MODE_MULTI_PROCESS);
	}

	private static boolean skipped(String k) {
		for (String s : SKIP) {
			if (s.equals(k)) {
				return true;
			}
		}
		return false;
	}

	private static JSONObject dump(SharedPreferences sp, boolean filter) throws Exception {
		JSONObject o = new JSONObject();
		for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
			if (filter && skipped(e.getKey())) {
				continue;
			}
			Object v = e.getValue();
			JSONArray a = new JSONArray();
			if (v instanceof Boolean) {
				a.put("b").put(v);
			} else if (v instanceof Integer) {
				a.put("i").put(v);
			} else if (v instanceof Long) {
				a.put("l").put(v);
			} else if (v instanceof Float) {
				a.put("f").put((double) (Float) v);
			} else if (v instanceof String) {
				a.put("s").put(v);
			} else if (v instanceof Set) {
				a.put("t").put(new JSONArray((Set<?>) v));
			} else {
				continue;
			}
			o.put(e.getKey(), a);
		}
		return o;
	}

	private static void load(SharedPreferences sp, JSONObject o) throws Exception {
		SharedPreferences.Editor ed = sp.edit();
		java.util.Iterator<String> it = o.keys();
		while (it.hasNext()) {
			String k = it.next();
			if (skipped(k)) {
				continue;
			}
			JSONArray a = o.getJSONArray(k);
			switch (a.getString(0)) {
				case "b":
					ed.putBoolean(k, a.getBoolean(1));
					break;
				case "i":
					ed.putInt(k, a.getInt(1));
					break;
				case "l":
					ed.putLong(k, a.getLong(1));
					break;
				case "f":
					ed.putFloat(k, (float) a.getDouble(1));
					break;
				case "s":
					ed.putString(k, a.getString(1));
					break;
				case "t":
					java.util.HashSet<String> set = new java.util.HashSet<>();
					JSONArray arr = a.getJSONArray(1);
					for (int i = 0; i < arr.length(); i++) {
						set.add(arr.getString(i));
					}
					ed.putStringSet(k, set);
					break;
				default:
					break;
			}
		}
		ed.commit();
	}

	/** Ghi bản sao lưu ở luồng nền (an toàn khi gọi nhiều lần; lần ghi sau đè lần trước). */
	public static void saveAsync(final Context ctx) {
		final Context c = ctx.getApplicationContext();
		new Thread(() -> save(c), "che779-backup").start();
	}

	static void save(Context c) {
		File d = dir();
		if (d == null) {
			return;
		}
		synchronized (LOCK) {
			try {
				if (!d.isDirectory() && !d.mkdirs()) {
					return;
				}
				JSONObject root = new JSONObject();
				root.put("v", 1);
				root.put("prefs", dump(main(c), true));
				root.put("macro", dump(macro(c), false));
				File tmp = new File(d, "backup.json.tmp");
				try (FileOutputStream out = new FileOutputStream(tmp)) {
					out.write(root.toString().getBytes("UTF-8"));
				}
				File dst = new File(d, "backup.json");
				if (!tmp.renameTo(dst)) {
					dst.delete();
					tmp.renameTo(dst);
				}
				File bg = new File(c.getFilesDir(), KeypadStyle.FILE);
				File bgOut = new File(d, BG);
				if (bg.isFile()) {
					copy(bg, bgOut);
				} else {
					bgOut.delete();
				}
			} catch (Throwable ignored) {
			}
		}
	}

	/** Lần chạy đầu sau khi cài mới: nạp lại bản sao lưu nếu thư mục dữ liệu đọc được. */
	public static void restoreIfFresh(Context ctx) {
		Context c = ctx.getApplicationContext();
		SharedPreferences sp = main(c);
		if (sp.getBoolean(MARK, false)) {
			return;
		}
		File d = dir();
		String base = Config.getEmulatorDir();
		if (d == null || base == null || !new File(base).isDirectory()) {
			return; // chưa đọc được bộ nhớ: thử lại lần mở sau
		}
		synchronized (LOCK) {
			try {
				File f = new File(d, "backup.json");
				if (f.isFile()) {
					JSONObject root = new JSONObject(new String(readAll(f), "UTF-8"));
					load(sp, root.optJSONObject("prefs") == null ? new JSONObject() : root.getJSONObject("prefs"));
					if (root.optJSONObject("macro") != null) {
						load(macro(c), root.getJSONObject("macro"));
					}
					File bg = new File(d, BG);
					if (bg.isFile()) {
						copy(bg, new File(c.getFilesDir(), KeypadStyle.FILE));
					}
				}
			} catch (Throwable ignored) {
			}
			sp.edit().putBoolean(MARK, true).commit();
		}
	}

	private static byte[] readAll(File f) throws Exception {
		try (InputStream in = new FileInputStream(f)) {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			byte[] b = new byte[65536];
			int n;
			while ((n = in.read(b)) > 0) {
				bo.write(b, 0, n);
			}
			return bo.toByteArray();
		}
	}

	private static void copy(File a, File b) throws Exception {
		try (InputStream in = new FileInputStream(a); FileOutputStream out = new FileOutputStream(b)) {
			byte[] buf = new byte[65536];
			int n;
			while ((n = in.read(buf)) > 0) {
				out.write(buf, 0, n);
			}
		}
	}
}
