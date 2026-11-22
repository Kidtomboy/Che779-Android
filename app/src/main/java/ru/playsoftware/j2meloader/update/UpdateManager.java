package ru.playsoftware.j2meloader.update;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

import ru.playsoftware.j2meloader.R;

/**
 * Kiểm tra và cài bản cập nhật từ trang Releases của GitHub.
 * <p>
 * Kênh được chọn qua tài nguyên chuỗi: <code>update_tag_prefix</code> ("v" cho bản chính, "dev-" cho V2),
 * <code>update_asset_prefix</code> (tên tệp APK trước phần số phiên bản) và
 * <code>update_allow_prerelease</code>. Chỉ gói chính và V2 tự cài; các gói clone chỉ mở trang Releases.
 */
public final class UpdateManager {
	public static final String PREF_AUTO = "pref_update_auto";
	private static final String PREF_LAST_CHECK = "update_last_check";
	private static final String API = "https://api.github.com/repos/Kidtomboy/Che779-Android/releases?per_page=40";
	private static final String PAGE = "https://github.com/Kidtomboy/Che779-Android/releases";
	private static final long DAY = 24L * 60 * 60 * 1000;
	private static volatile boolean busy;

	private UpdateManager() {
	}

	private static final class Info {
		String tag;
		String name;
		String notes;
		String apkUrl;
		String sumsUrl;
		String apkName;
	}

	/** Mở lúc khởi động: chỉ kiểm tra khi người dùng bật công tắc, tối đa một lần mỗi ngày, im lặng nếu không có gì mới. */
	public static void autoCheck(Activity a) {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(a);
		if (!p.getBoolean(PREF_AUTO, false)) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - p.getLong(PREF_LAST_CHECK, 0) < DAY) {
			return;
		}
		check(a, false);
	}

	/** @param manual true khi người dùng bấm "Kiểm tra cập nhật": luôn báo kết quả. */
	public static void check(final Activity a, final boolean manual) {
		if (busy) {
			return;
		}
		busy = true;
		if (manual) {
			Toast.makeText(a, R.string.update_checking, Toast.LENGTH_SHORT).show();
		}
		new Thread(() -> {
			String err = null;
			Info info = null;
			try {
				info = latest(a);
				PreferenceManager.getDefaultSharedPreferences(a).edit()
						.putLong(PREF_LAST_CHECK, System.currentTimeMillis()).apply();
			} catch (Exception e) {
				err = String.valueOf(e.getMessage());
			}
			final Info found = info;
			final String error = err;
			a.runOnUiThread(() -> {
				busy = false;
				if (a.isFinishing() || a.isDestroyed()) {
					return;
				}
				if (error != null) {
					if (manual) {
						Toast.makeText(a, a.getString(R.string.update_error, error), Toast.LENGTH_LONG).show();
					}
				} else if (found == null) {
					if (manual) {
						Toast.makeText(a, a.getString(R.string.update_latest, currentVersion(a)), Toast.LENGTH_LONG).show();
					}
				} else {
					offer(a, found);
				}
			});
		}, "che779-update").start();
	}

	/** Số phiên bản đang chạy, bỏ hậu tố "-dev-N" của bản dựng thử. */
	public static String currentVersion(Activity a) {
		try {
			String v = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
			int i = v.indexOf('-');
			return i > 0 ? v.substring(0, i) : v;
		} catch (Exception e) {
			return "0";
		}
	}

	private static boolean canInstall(Activity a) {
		String pkg = a.getPackageName();
		return "com.che779.emulator".equals(pkg) || "com.che779.emulatorv2".equals(pkg);
	}

	private static int[] parse(String s) {
		String[] p = s.replaceAll("[^0-9.]", "").split("\\.");
		int[] r = new int[3];
		for (int i = 0; i < 3 && i < p.length; i++) {
			try {
				r[i] = Integer.parseInt(p[i]);
			} catch (NumberFormatException ignored) {
			}
		}
		return r;
	}

	private static int compare(String x, String y) {
		int[] a = parse(x);
		int[] b = parse(y);
		for (int i = 0; i < 3; i++) {
			if (a[i] != b[i]) {
				return Integer.compare(a[i], b[i]);
			}
		}
		return 0;
	}

	private static String http(String url) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
		c.setConnectTimeout(15000);
		c.setReadTimeout(20000);
		c.setRequestProperty("Accept", "application/vnd.github+json");
		c.setRequestProperty("User-Agent", "Che779-Updater");
		if (c.getResponseCode() != 200) {
			throw new Exception("HTTP " + c.getResponseCode());
		}
		try (InputStream in = c.getInputStream()) {
			java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) {
				o.write(buf, 0, n);
			}
			return o.toString("UTF-8");
		}
	}

	/** @return bản mới nhất của kênh nếu mới hơn bản đang chạy, ngược lại null. */
	private static Info latest(Activity a) throws Exception {
		String prefix = a.getString(R.string.update_tag_prefix);
		String assetPrefix = a.getString(R.string.update_asset_prefix);
		boolean allowPre = a.getResources().getBoolean(R.bool.update_allow_prerelease);
		JSONArray arr = new JSONArray(http(API));
		Info best = null;
		String bestVer = null;
		for (int i = 0; i < arr.length(); i++) {
			JSONObject r = arr.getJSONObject(i);
			String tag = r.optString("tag_name");
			if (r.optBoolean("draft") || !tag.startsWith(prefix) || (r.optBoolean("prerelease") && !allowPre)) {
				continue;
			}
			String ver = tag.substring(prefix.length());
			if (!ver.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
				continue; // bỏ qua "buildlog" và tag lạ
			}
			String apkName = assetPrefix + ver + ".apk";
			String apk = null;
			String sums = null;
			JSONArray assets = r.optJSONArray("assets");
			for (int j = 0; assets != null && j < assets.length(); j++) {
				JSONObject s = assets.getJSONObject(j);
				String n = s.optString("name");
				if (n.equals(apkName)) {
					apk = s.optString("browser_download_url");
				} else if (n.equals("SHA256SUMS.txt")) {
					sums = s.optString("browser_download_url");
				}
			}
			if (apk == null || (bestVer != null && compare(ver, bestVer) <= 0)) {
				continue; // Release chưa có APK (đang build) thì bỏ qua
			}
			best = new Info();
			best.tag = tag;
			best.name = ver;
			best.notes = r.optString("body");
			best.apkUrl = apk;
			best.sumsUrl = sums;
			best.apkName = apkName;
			bestVer = ver;
		}
		if (best == null || compare(best.name, currentVersion(a)) <= 0) {
			return null;
		}
		return best;
	}

	private static void offer(final Activity a, final Info info) {
		String notes = info.notes == null ? "" : info.notes.trim();
		if (notes.length() > 700) {
			notes = notes.substring(0, 700) + "…";
		}
		String msg = a.getString(R.string.update_available_msg, info.tag, currentVersion(a)) + "\n\n" + notes;
		AlertDialog.Builder b = new AlertDialog.Builder(a)
				.setTitle(R.string.update_available_title)
				.setMessage(msg)
				.setNegativeButton(R.string.update_later, null);
		if (canInstall(a)) {
			b.setPositiveButton(R.string.update_download, (d, w) -> download(a, info));
		} else {
			// gói clone: chỉ mở trang Releases để tải đúng tệp
			b.setPositiveButton(R.string.update_open_page, (d, w) -> {
				try {
					a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PAGE + "/tag/" + info.tag)));
				} catch (ActivityNotFoundException ignored) {
				}
			});
		}
		b.show();
	}

	private static void download(final Activity a, final Info info) {
		LinearLayout box = new LinearLayout(a);
		box.setOrientation(LinearLayout.VERTICAL);
		int pad = (int) (20 * a.getResources().getDisplayMetrics().density);
		box.setPadding(pad, pad, pad, pad);
		final TextView label = new TextView(a);
		label.setGravity(Gravity.START);
		label.setText(R.string.update_downloading);
		final ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
		bar.setMax(100);
		box.addView(label);
		box.addView(bar);
		final AlertDialog dlg = new AlertDialog.Builder(a).setTitle(info.apkName).setView(box)
				.setCancelable(false).create();
		final boolean[] cancel = {false};
		dlg.setButton(AlertDialog.BUTTON_NEGATIVE, a.getString(android.R.string.cancel), (d, w) -> cancel[0] = true);
		dlg.show();
		new Thread(() -> {
			File out = null;
			String err = null;
			try {
				File dir = new File(a.getCacheDir(), "updates");
				if (!dir.isDirectory() && !dir.mkdirs()) {
					throw new Exception("cache");
				}
				File[] old = dir.listFiles();
				for (int i = 0; old != null && i < old.length; i++) {
					old[i].delete(); // dọn bản tải cũ
				}
				String expected = null;
				if (info.sumsUrl != null) {
					for (String line : http(info.sumsUrl).split("\n")) {
						String[] p = line.trim().split("\\s+");
						if (p.length >= 2 && p[1].replace("*", "").equals(info.apkName)) {
							expected = p[0].toLowerCase(Locale.ROOT);
						}
					}
				}
				if (expected == null) {
					throw new Exception(a.getString(R.string.update_no_sum));
				}
				out = new File(dir, info.apkName);
				HttpURLConnection c = (HttpURLConnection) new URL(info.apkUrl).openConnection();
				c.setConnectTimeout(15000);
				c.setReadTimeout(30000);
				c.setRequestProperty("User-Agent", "Che779-Updater");
				if (c.getResponseCode() != 200) {
					throw new Exception("HTTP " + c.getResponseCode());
				}
				long total = c.getContentLengthLong();
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				try (InputStream in = c.getInputStream(); OutputStream os = new FileOutputStream(out)) {
					byte[] buf = new byte[16384];
					long got = 0;
					int n;
					int last = -1;
					while ((n = in.read(buf)) > 0) {
						if (cancel[0]) {
							throw new Exception("cancel");
						}
						os.write(buf, 0, n);
						md.update(buf, 0, n);
						got += n;
						final int pct = total > 0 ? (int) (got * 100 / total) : 0;
						if (pct != last) {
							last = pct;
							a.runOnUiThread(() -> {
								bar.setProgress(pct);
								label.setText(a.getString(R.string.update_downloading_pct, pct));
							});
						}
					}
				}
				StringBuilder hex = new StringBuilder();
				for (byte x : md.digest()) {
					hex.append(String.format(Locale.ROOT, "%02x", x));
				}
				if (!hex.toString().equals(expected)) {
					out.delete();
					throw new Exception(a.getString(R.string.update_bad_sum));
				}
			} catch (Exception e) {
				err = String.valueOf(e.getMessage());
				if (out != null) {
					out.delete();
				}
			}
			final File apk = out;
			final String error = err;
			a.runOnUiThread(() -> {
				dlg.dismiss();
				if (error != null) {
					if (!"cancel".equals(error) && !a.isFinishing()) {
						Toast.makeText(a, a.getString(R.string.update_error, error), Toast.LENGTH_LONG).show();
					}
				} else if (!a.isFinishing()) {
					install(a, apk);
				}
			});
		}, "che779-update-dl").start();
	}

	private static void install(Activity a, File apk) {
		if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
			Toast.makeText(a, R.string.update_allow_install, Toast.LENGTH_LONG).show();
			try {
				a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
						Uri.parse("package:" + a.getPackageName())));
			} catch (ActivityNotFoundException ignored) {
			}
			return; // người dùng cấp quyền rồi bấm "Kiểm tra cập nhật" lần nữa (tệp đã tải vẫn xác thực lại khi tải)
		}
		try {
			Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".updates", apk);
			Intent i = new Intent(Intent.ACTION_VIEW)
					.setDataAndType(uri, "application/vnd.android.package-archive")
					.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
			a.startActivity(i);
		} catch (Exception e) {
			Toast.makeText(a, a.getString(R.string.update_error, String.valueOf(e.getMessage())), Toast.LENGTH_LONG).show();
		}
	}
}
