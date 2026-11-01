/*
 *  Che779 Emulator - periodic memory cleaning (settings idea: J2ME Loader v10 by Hung Hero).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.PreferenceManager;

/** Every N seconds, when free RAM drops under a threshold, asks the VM to collect garbage. */
final class MemoryGuard {
	private final Context ctx;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private boolean running;

	private final Runnable tick = new Runnable() {
		@Override
		public void run() {
			if (!running) {
				return;
			}
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
			if (sp.getBoolean("pref_mem_auto", true)) {
				ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
				ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
				am.getMemoryInfo(mi);
				long availMb = mi.availMem / (1024 * 1024);
				if (mi.lowMemory || availMb < readInt(sp, "pref_mem_threshold_mb", 100)) {
					// never on the UI thread: a slow finalizer must not freeze the screen
					Thread t = new Thread(System::gc, "Che779-MemGuard");
					t.setDaemon(true);
					t.setPriority(Thread.MIN_PRIORITY);
					t.start();
				}
			}
			handler.postDelayed(this, Math.max(5, readInt(sp, "pref_mem_interval_s", 30)) * 1000L);
		}
	};

	MemoryGuard(Context c) {
		ctx = c.getApplicationContext();
	}

	private static int readInt(SharedPreferences sp, String key, int def) {
		try {
			return Integer.parseInt(sp.getString(key, String.valueOf(def)).trim());
		} catch (Exception e) {
			return def;
		}
	}

	void start() {
		if (!running) {
			running = true;
			handler.postDelayed(tick, 10_000);
		}
	}

	void stop() {
		running = false;
		handler.removeCallbacks(tick);
	}
}
