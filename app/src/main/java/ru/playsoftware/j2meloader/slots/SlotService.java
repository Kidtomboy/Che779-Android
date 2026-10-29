package ru.playsoftware.j2meloader.slots;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import ru.playsoftware.j2meloader.R;

/** Giu tien trinh cua tab song khi ung dung bi day xuong nen. */
public abstract class SlotService extends Service {
	private static final String CHANNEL = "che779_tab";

	public static void start(Context c) {
		int slot = Slots.current();
		if (slot < 0) {
			return;
		}
		try {
			ContextCompat.startForegroundService(c, new Intent(c, Slots.serviceClass(slot)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stop(Context c) {
		int slot = Slots.current();
		if (slot < 0) {
			return;
		}
		try {
			c.stopService(new Intent(c, Slots.serviceClass(slot)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int slot = Slots.current();
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			if (nm.getNotificationChannel(CHANNEL) == null) {
				NotificationChannel ch = new NotificationChannel(CHANNEL, "Che779 - tab dang chay",
						NotificationManager.IMPORTANCE_MIN);
				nm.createNotificationChannel(ch);
			}
		}
		Intent open = new Intent(this, Slots.activityClass(Math.max(slot, 0)));
		open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		int piFlags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
		PendingIntent pi = PendingIntent.getActivity(this, slot, open, piFlags);
		Notification n = new NotificationCompat.Builder(this, CHANNEL)
				.setSmallIcon(R.drawable.ic_che779_notif)
				.setContentTitle("Che779 - Tab " + (slot + 1) + " đang chạy")
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setContentIntent(pi)
				.build();
		startForeground(1000 + Math.max(slot, 0), n);
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
