/*
 *  Che779 Emulator - keeps the game process alive while games run (bubble and notification).
 *  The notification has two buttons: "Chế độ" (cycles the battery mode) and "Dừng" (stops the app),
 *  and shows the current battery mode in its text.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.preference.PreferenceManager;
import android.os.IBinder;
import android.os.Process;

import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;

public class BubbleService extends Service {
	private static final String CHANNEL = "che779_bubble";
	private static final int ID = 779;
	static final String ACTION_MODE = "com.che779.emulator.action.MODE";
	static final String ACTION_STOP = "com.che779.emulator.action.STOP";
	static final String ACTION_TIME = "com.che779.emulator.action.TIME";

	/** Moment the first game was opened (0 = no game yet). */
	private static volatile long startedAt;
	private final Handler tick = new Handler(Looper.getMainLooper());
	private final Runnable tickTask = new Runnable() {
		@Override
		public void run() {
			refresh(BubbleService.this);
			tick.postDelayed(this, intervalMs(BubbleService.this));
		}
	};

	/** Notification update period: pref_notif_interval minutes (default 5). */
	private static long intervalMs(Context c) {
		int min = 5;
		try {
			min = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(c)
					.getString("pref_notif_interval", "5"));
		} catch (Exception ignored) {
		}
		return Math.max(1, Math.min(min, 180)) * 60000L;
	}

	/** Elapsed time as "45 phút", "2 giờ 5 phút" or "1 ngày 3 giờ". */
	static String formatElapsed(long ms) {
		long min = Math.max(0, ms) / 60000;
		if (min < 60) {
			return min + " phút";
		}
		long h = min / 60;
		if (h < 24) {
			return h + " giờ " + (min % 60) + " phút";
		}
		return (h / 24) + " ngày " + (h % 24) + " giờ";
	}

	private static String elapsedText() {
		if (startedAt == 0) {
			startedAt = SystemClock.elapsedRealtime();
		}
		return formatElapsed(SystemClock.elapsedRealtime() - startedAt);
	}

	/** Asks the game process to stop completely (used by the main menu "Thoát"). */
	public static void quitGames(Context c) {
		try {
			c.startService(new Intent(c, BubbleService.class).setAction(ACTION_STOP));
		} catch (Exception ignored) {
		}
	}

	/** Redraws the notification (battery mode changed). */
	static void refresh(Context c) {
		try {
			NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
			if (nm != null && Session.count() > 0) {
				nm.notify(ID, build(c, ContextHolder.getActivity() != null ? ContextHolder.getActivity().powerMode() : 0));
			}
		} catch (Exception ignored) {
		}
	}

	private static PendingIntent action(Context c, String action, int code) {
		Intent i = new Intent(c, BubbleService.class).setAction(action);
		int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
		return PendingIntent.getService(c, code, i, flags);
	}

	@SuppressWarnings("deprecation")
	private static Notification build(Context c, int mode) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
			nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Che779",
					NotificationManager.IMPORTANCE_LOW));
		}
		Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
				? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
		String name = mode >= 0 && mode < MicroActivity.POWER_NAMES.length ? MicroActivity.POWER_NAMES[mode] : "?";
		String time = elapsedText();
		String text = "Game đang chạy nền · Chế độ pin: " + name + " · Đã chạy: " + time;
		b.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(c.getString(R.string.app_name))
				.setContentText(text)
				.setStyle(new Notification.BigTextStyle().bigText(text))
				.setOngoing(true)
				.addAction(R.mipmap.ic_launcher, "Chế độ", action(c, ACTION_MODE, 1))
				.addAction(R.mipmap.ic_launcher, "\u23F1 " + time, action(c, ACTION_TIME, 3))
				.addAction(R.mipmap.ic_launcher, "Dừng", action(c, ACTION_STOP, 2));
		return b.build();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String act = intent == null ? null : intent.getAction();
		if (ACTION_STOP.equals(act)) {
			stopApp();
			return START_NOT_STICKY;
		}
		MicroActivity a = ContextHolder.getActivity();
		int mode = a == null ? 0 : a.powerMode();
		if (ACTION_MODE.equals(act) && a != null) {
			// show the new mode at once; the activity applies it on its UI thread
			mode = (mode + 1) % MicroActivity.POWER_NAMES.length;
			a.cyclePowerMode();
		}
		startForeground(ID, build(this, mode));
		tick.removeCallbacks(tickTask);
		tick.postDelayed(tickTask, intervalMs(this));
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		tick.removeCallbacks(tickTask);
		startedAt = 0;
		super.onDestroy();
	}

	private void stopApp() {
		tick.removeCallbacks(tickTask);
		startedAt = 0;
		MicroActivity a = ContextHolder.getActivity();
		try {
			stopForeground(true);
		} catch (Exception ignored) {
		}
		if (a != null) {
			a.exitAll();
		} else {
			stopSelf();
			Process.killProcess(Process.myPid());
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
