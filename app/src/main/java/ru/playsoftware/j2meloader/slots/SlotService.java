package ru.playsoftware.j2meloader.slots;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import ru.playsoftware.j2meloader.R;

/**
 * Giu tien trinh cua tab song khi ung dung bi day xuong nen (treo).
 * Giu CPU/Wi-Fi, don RAM khi day bo nho, xu ly lenh Treo va hen gio tat.
 */
public abstract class SlotService extends Service {
	private static final String CHANNEL = "che779_tab";
	private static final long TICK = 30_000L;
	private static final long STOP_WINDOW = 120_000L;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private boolean inited;
	private Treo.Receiver receiver;
	private PowerManager.WakeLock wakeLock;
	private WifiManager.WifiLock wifiLock;

	private final Runnable tick = new Runnable() {
		@Override
		public void run() {
			long at = Treo.autoStopAt(SlotService.this);
			if (at > 0) {
				long now = System.currentTimeMillis();
				if (now >= at && now - at < STOP_WINDOW) {
					shutdown();
					return;
				}
				if (now - at >= STOP_WINDOW) {
					Treo.clearAutoStop(SlotService.this);
				}
			}
			cleanRamIfNeeded();
			handler.postDelayed(this, TICK);
		}
	};

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
		initOnce();
		return START_NOT_STICKY;
	}

	private void initOnce() {
		if (inited) {
			return;
		}
		inited = true;
		Treo.load(this);
		receiver = new Treo.Receiver();
		IntentFilter f = new IntentFilter();
		f.addAction(Treo.ACTION);
		f.addAction(Treo.ACTION_GC);
		registerReceiver(receiver, f);
		try {
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "che779:tab");
			wakeLock.setReferenceCounted(false);
			wakeLock.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
			int mode = Build.VERSION.SDK_INT >= 29 ? WifiManager.WIFI_MODE_FULL_LOW_LATENCY
					: WifiManager.WIFI_MODE_FULL_HIGH_PERF;
			wifiLock = wm.createWifiLock(mode, "che779:tab");
			wifiLock.setReferenceCounted(false);
			wifiLock.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
		handler.postDelayed(tick, TICK);
	}

	private void cleanRamIfNeeded() {
		try {
			Runtime rt = Runtime.getRuntime();
			long used = rt.totalMemory() - rt.freeMemory();
			boolean heapHigh = used > rt.maxMemory() * 0.75;
			ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
			am.getMemoryInfo(mi);
			if (heapHigh || mi.lowMemory) {
				System.gc();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void shutdown() {
		Slots.markStopped(this);
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	@Override
	public void onDestroy() {
		handler.removeCallbacks(tick);
		if (receiver != null) {
			try {
				unregisterReceiver(receiver);
			} catch (Exception ignored) {
			}
		}
		try {
			if (wakeLock != null && wakeLock.isHeld()) {
				wakeLock.release();
			}
			if (wifiLock != null && wifiLock.isHeld()) {
				wifiLock.release();
			}
		} catch (Exception ignored) {
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
