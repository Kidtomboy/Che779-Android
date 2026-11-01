/*
 *  Che779 Emulator - keeps the game process alive while the bubble is shown.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import ru.playsoftware.j2meloader.R;

public class BubbleService extends Service {
	private static final String CHANNEL = "che779_bubble";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Che779",
					NotificationManager.IMPORTANCE_LOW));
		}
		Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
				? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
		b.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(getString(R.string.app_name))
				.setContentText("Game đang chạy nền");
		startForeground(779, b.build());
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
