/*
 * Che779 Emulator - che do TREO: game van chay logic nhung khong (hoac it) day hinh len man hinh.
 * Y tuong tham khao: J2ME NST (NST Tien) - che do treo, hen gio tat, o Cai dat nhanh;
 * J2ME Loader v10 cua Hung Hero - Sleep Tab. Ma nguon duoc viet lai doc lap.
 */
package ru.playsoftware.j2meloader.slots;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class Treo {
	public static final String ACTION = "com.che779.emulator.action.TREO";
	public static final String ACTION_GC = "com.che779.emulator.action.GC";
	public static final int DRAW = 0;
	public static final int ECO = 1;
	public static final int OFF = 2;

	private static final long ECO_INTERVAL = 50;   // 20 khung/giay
	private static final long OFF_INTERVAL = 3000; // tab ngu: 1 khung moi 3 giay

	private static volatile int globalMode = DRAW;
	private static volatile int slotMode = -1;
	private static volatile long lastFlush;
	private static Runnable pending;
	private static boolean trailing;
	private static final Handler MAIN = new Handler(Looper.getMainLooper());
	private static final Runnable TRAILING = () -> {
		Runnable r;
		synchronized (Treo.class) {
			r = pending;
			pending = null;
			trailing = false;
		}
		lastFlush = SystemClock.uptimeMillis();
		if (r != null) {
			r.run();
		}
	};

	private Treo() {
	}

	// ---------- Phia tab (tien trinh game) ----------

	public static int effective() {
		return slotMode >= 0 ? slotMode : globalMode;
	}

	/** Canvas goi truoc khi day khung hinh len man hinh. false = bo qua (se ve bu khung cuoi sau). */
	public static boolean allowFlush(Runnable later) {
		int m = effective();
		if (m == DRAW) {
			return true;
		}
		long interval = m == ECO ? ECO_INTERVAL : OFF_INTERVAL;
		long now = SystemClock.uptimeMillis();
		long wait = interval - (now - lastFlush);
		if (wait <= 0) {
			lastFlush = now;
			return true;
		}
		synchronized (Treo.class) {
			pending = later;
			if (!trailing) {
				trailing = true;
				MAIN.postDelayed(TRAILING, wait);
			}
		}
		return false;
	}

	/** Doc cau hinh da luu khi tab khoi dong. */
	public static void load(Context c) {
		globalMode = readInt(new File(Slots.stateDir(c), "treo.global"), DRAW);
		int s = Slots.current();
		slotMode = s >= 0 ? readInt(new File(Slots.stateDir(c), "slot" + s + ".mode"), -1) : -1;
	}

	/** Xu ly lenh phat tu tien trinh chinh. */
	static void onCommand(Context c, Intent i) {
		if (ACTION_GC.equals(i.getAction())) {
			System.gc();
			return;
		}
		if (!ACTION.equals(i.getAction())) {
			return;
		}
		int slot = i.getIntExtra("slot", -1);
		int mode = i.getIntExtra("mode", DRAW);
		if (slot < 0) {
			globalMode = mode;
		} else if (slot == Slots.current()) {
			slotMode = mode;
		}
	}

	static final class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			onCommand(context, intent);
		}
	}

	// ---------- Phia giao dien (tien trinh chinh) ----------

	public static int getGlobal(Context c) {
		return readInt(new File(Slots.stateDir(c), "treo.global"), DRAW);
	}

	public static int getSlot(Context c, int slot) {
		return readInt(new File(Slots.stateDir(c), "slot" + slot + ".mode"), -1);
	}

	public static void setGlobal(Context c, int mode) {
		writeInt(new File(Slots.stateDir(c), "treo.global"), mode);
		send(c, -1, mode);
	}

	/** mode = -1: bo ghi de, theo che do chung. */
	public static void setSlot(Context c, int slot, int mode) {
		File f = new File(Slots.stateDir(c), "slot" + slot + ".mode");
		if (mode < 0) {
			f.delete();
		} else {
			writeInt(f, mode);
		}
		send(c, slot, mode);
	}

	public static void cleanRam(Context c) {
		c.sendBroadcast(new Intent(ACTION_GC).setPackage(c.getPackageName()));
	}

	private static void send(Context c, int slot, int mode) {
		Intent i = new Intent(ACTION).setPackage(c.getPackageName());
		i.putExtra("slot", slot);
		i.putExtra("mode", mode);
		c.sendBroadcast(i);
	}

	// ---------- Hen gio tat ----------

	public static void setAutoStopHours(Context c, int hours) {
		File f = new File(Slots.stateDir(c), "autostop");
		if (hours <= 0) {
			f.delete();
		} else {
			writeLong(f, System.currentTimeMillis() + hours * 3600_000L);
		}
	}

	/** Thoi diem se tat (ms), 0 neu khong hen gio. */
	public static long autoStopAt(Context c) {
		return readLong(new File(Slots.stateDir(c), "autostop"), 0L);
	}

	public static void clearAutoStop(Context c) {
		new File(Slots.stateDir(c), "autostop").delete();
	}

	// ---------- tien ich file ----------

	private static int readInt(File f, int def) {
		return (int) readLong(f, def);
	}

	private static long readLong(File f, long def) {
		if (!f.exists()) {
			return def;
		}
		try (BufferedReader r = new BufferedReader(new FileReader(f))) {
			String s = r.readLine();
			return s == null ? def : Long.parseLong(s.trim());
		} catch (Exception e) {
			return def;
		}
	}

	private static void writeInt(File f, int v) {
		writeLong(f, v);
	}

	private static void writeLong(File f, long v) {
		try (FileWriter w = new FileWriter(f)) {
			w.write(Long.toString(v));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
