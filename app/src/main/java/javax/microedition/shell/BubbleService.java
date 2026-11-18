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
import android.os.IBinder;
import android.os.Process;

import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;

public class BubbleService extends Service {
	private static final String CHANNEL = "che779_bubble";
	private static final int ID = 779;
	static final String ACTION_MODE = "com.che779.emulator.action.MODE";
	static final String ACTION_STOP = "com.che779.emulator.action.STOP";

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
		String text = "Game đang chạy nền · Chế độ pin: " + name;
		b.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(c.getString(R.string.app_name))
				.setContentText(text)
				.setStyle(new Notification.BigTextStyle().bigText(text))
				.setOngoing(true)
				.addAction(R.mipmap.ic_launcher, "Chế độ", action(c, ACTION_MODE, 1))
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
		return START_NOT_STICKY;
	}

	private void stopApp() {
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
