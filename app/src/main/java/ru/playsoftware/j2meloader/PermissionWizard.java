package ru.playsoftware.j2meloader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

/**
 * First-run permission flow, one step at a time: floating bubble, notifications, battery.
 * Every step is asked at most once; refusing never blocks the app.
 */
public final class PermissionWizard {
	private static final String KEY = "perm_wizard_step";
	private static final int STEP_OVERLAY = 0, STEP_NOTIF = 1, STEP_BATTERY = 2, DONE = 3;
	private static boolean showing;

	private PermissionWizard() {
	}

	public static void next(final Activity a) {
		if (showing || a.isFinishing()) {
			return;
		}
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(a);
		final int step = sp.getInt(KEY, STEP_OVERLAY);
		switch (step) {
			case STEP_OVERLAY:
				if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(a)) {
					advance(sp, a);
					return;
				}
				ask(a, sp, "Bong bóng chat",
						"Cho phép hiển thị trên ứng dụng khác để game chạy trong cửa sổ nổi và bong bóng khi bạn thoát ra màn hình chính.",
						() -> a.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
								Uri.parse("package:" + a.getPackageName()))));
				break;
			case STEP_NOTIF:
				if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(a,
						Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
					advance(sp, a);
					return;
				}
				ask(a, sp, "Thông báo",
						"Cho phép thông báo để hiện nút \"Chế độ\" và \"Dừng\" và giữ game chạy nền ổn định.",
						() -> ActivityCompat.requestPermissions(a,
								new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4711));
				break;
			case STEP_BATTERY:
				PowerManager pm = (PowerManager) a.getSystemService(Context.POWER_SERVICE);
				if (Build.VERSION.SDK_INT < 23 || pm == null || pm.isIgnoringBatteryOptimizations(a.getPackageName())) {
					advance(sp, a);
					return;
				}
				ask(a, sp, "Pin",
						"Chọn \"Không hạn chế\" (không tối ưu pin) cho ứng dụng để hệ thống không tắt game khi chạy nền.",
						() -> {
							try {
								a.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
										Uri.parse("package:" + a.getPackageName())));
							} catch (Exception e) {
								try {
									a.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
								} catch (Exception ignored) {
								}
							}
						});
				break;
			default:
				break;
		}
	}

	private static void advance(SharedPreferences sp, Activity a) {
		sp.edit().putInt(KEY, sp.getInt(KEY, STEP_OVERLAY) + 1).apply();
		next(a);
	}

	private static void ask(final Activity a, final SharedPreferences sp, String title, String msg,
							final Runnable action) {
		showing = true;
		new AlertDialog.Builder(a)
				.setTitle("Cấp quyền: " + title)
				.setMessage(msg)
				.setCancelable(false)
				.setPositiveButton("Cho phép", (d, w) -> {
					showing = false;
					sp.edit().putInt(KEY, sp.getInt(KEY, STEP_OVERLAY) + 1).apply(); // asked once
					action.run();
				})
				.setNegativeButton("Để sau", (d, w) -> {
					showing = false;
					advance(sp, a);
				})
				.show();
	}
}
