package ru.playsoftware.j2meloader.slots;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

/** Bat/tat bong bong noi. */
public final class Bubble {
	private static final String PREF = "che779_bubble_enabled";

	private Bubble() {
	}

	private static SharedPreferences prefs(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c);
	}

	public static boolean isEnabled(Context c) {
		return prefs(c).getBoolean(PREF, false);
	}

	public static boolean canDraw(Context c) {
		return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
	}

	public static void toggle(Activity a) {
		setEnabled(a, !isEnabled(a));
	}

	public static void setEnabled(Activity a, boolean on) {
		prefs(a).edit().putBoolean(PREF, on).apply();
		if (!on) {
			a.stopService(new Intent(a, BubbleService.class));
			Toast.makeText(a, "Đã tắt bong bóng", Toast.LENGTH_SHORT).show();
			return;
		}
		if (!canDraw(a)) {
			Toast.makeText(a, "Hãy bật \"Hiển thị trên các ứng dụng khác\" cho Che779 rồi quay lại", Toast.LENGTH_LONG).show();
			a.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:" + a.getPackageName())));
			return;
		}
		start(a);
		Toast.makeText(a, "Đã bật bong bóng", Toast.LENGTH_SHORT).show();
	}

	/** Tat bong bong tu chinh service (nut "Tat" tren bang dieu khien). */
	public static void setEnabledFromService(android.app.Service s) {
		prefs(s).edit().putBoolean(PREF, false).apply();
		s.stopSelf();
	}

	/** Goi khi man hinh chinh hien lai: bat bong bong neu da duoc cap quyen. */
	public static void ensure(Context c) {
		if (isEnabled(c) && canDraw(c)) {
			start(c);
		}
	}

	private static void start(Context c) {
		try {
			ContextCompat.startForegroundService(c, new Intent(c, BubbleService.class));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
