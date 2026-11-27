/*
 *  Che779 Emulator - cập nhật thủ công: mở trang tải bản mới nhất trong trình duyệt để người dùng tự tải và cài đè.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package ru.playsoftware.j2meloader.update;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import ru.playsoftware.j2meloader.R;

public final class UpdateManager {
	private static final String LATEST = "https://github.com/Kidtomboy/Che779-Android/releases/latest";
	private static final String ALL = "https://github.com/Kidtomboy/Che779-Android/releases";

	private UpdateManager() {
	}

	/** Bản V2 (tag "dev-") không nằm ở "latest" nên mở danh sách Releases; bản chính mở thẳng bản mới nhất. */
	private static String pageUrl(Activity a) {
		return "dev-".equals(a.getString(R.string.update_tag_prefix)) ? ALL : LATEST;
	}

	public static void check(final Activity a, boolean manual) {
		final String url = pageUrl(a);
		new AlertDialog.Builder(a)
				.setTitle("Cập nhật thủ công")
				.setMessage("Bản đang dùng: " + currentVersion(a)
						+ "\n\n1. Bấm \"Mở trình duyệt\" để vào trang tải bản mới nhất.\n"
						+ "2. Tải file APK về máy.\n"
						+ "3. Mở file vừa tải và cài đè lên bản cũ (dữ liệu được giữ nguyên).\n\n"
						+ "Đường dẫn:\n" + url)
				.setPositiveButton("Mở trình duyệt", (d, w) -> {
					try {
						a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					} catch (Exception e) {
						Toast.makeText(a, "Không mở được trình duyệt, hãy sao chép đường dẫn", Toast.LENGTH_LONG).show();
					}
				})
				.setNeutralButton("Sao chép đường dẫn", (d, w) -> {
					ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
					if (cm != null) {
						cm.setPrimaryClip(ClipData.newPlainText("Che779", url));
						Toast.makeText(a, "Đã sao chép", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("Đóng", null)
				.show();
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
}
