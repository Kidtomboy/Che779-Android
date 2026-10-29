/*
 * Che779 Emulator - quan ly "tab" (moi tab = 1 tien trinh Android rieng).
 * Slot 0 = tien trinh ":midlet" (Tab 1), slot k>=1 = ":slotk" (Tab k+1).
 */
package ru.playsoftware.j2meloader.slots;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.microedition.shell.MicroActivity;
import javax.microedition.shell.MicroActivity1;
import javax.microedition.shell.MicroActivity10;
import javax.microedition.shell.MicroActivity11;
import javax.microedition.shell.MicroActivity2;
import javax.microedition.shell.MicroActivity3;
import javax.microedition.shell.MicroActivity4;
import javax.microedition.shell.MicroActivity5;
import javax.microedition.shell.MicroActivity6;
import javax.microedition.shell.MicroActivity7;
import javax.microedition.shell.MicroActivity8;
import javax.microedition.shell.MicroActivity9;

public final class Slots {
	public static final int COUNT = 12;

	private static final Class<?>[] ACTIVITIES = {
			MicroActivity.class, MicroActivity1.class, MicroActivity2.class, MicroActivity3.class,
			MicroActivity4.class, MicroActivity5.class, MicroActivity6.class, MicroActivity7.class,
			MicroActivity8.class, MicroActivity9.class, MicroActivity10.class, MicroActivity11.class
	};
	private static final Class<?>[] SERVICES = {
			SlotService0.class, SlotService1.class, SlotService2.class, SlotService3.class,
			SlotService4.class, SlotService5.class, SlotService6.class, SlotService7.class,
			SlotService8.class, SlotService9.class, SlotService10.class, SlotService11.class
	};

	private static int current = Integer.MIN_VALUE;
	private static volatile int target = -1;

	private Slots() {
	}

	public static final class Info {
		public final int slot;
		public final boolean running;
		public final String title;
		public final int taskId;

		Info(int slot, boolean running, String title, int taskId) {
			this.slot = slot;
			this.running = running;
			this.title = title;
			this.taskId = taskId;
		}
	}

	/** -1 = tien trinh chinh (giao dien), 0..COUNT-1 = tien trinh cua tab. */
	public static synchronized int current() {
		if (current == Integer.MIN_VALUE) {
			current = parse(processName());
		}
		return current;
	}

	private static int parse(String name) {
		int i = name.indexOf(':');
		if (i < 0) {
			return -1;
		}
		String s = name.substring(i + 1);
		if (s.equals("midlet")) {
			return 0;
		}
		if (s.startsWith("slot")) {
			try {
				int k = Integer.parseInt(s.substring(4));
				return k >= 1 && k < COUNT ? k : -1;
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return -1;
	}

	private static String processName() {
		try (BufferedReader r = new BufferedReader(new FileReader("/proc/self/cmdline"))) {
			String s = r.readLine();
			if (s == null) {
				return "";
			}
			int z = s.indexOf('\0');
			return z >= 0 ? s.substring(0, z) : s;
		} catch (IOException e) {
			return "";
		}
	}

	private static String suffix(int slot) {
		return slot == 0 ? ":midlet" : ":slot" + slot;
	}

	public static Class<?> activityClass(int slot) {
		return ACTIVITIES[slot];
	}

	public static Class<?> serviceClass(int slot) {
		return SERVICES[slot];
	}

	/** Dat tab dich cho lan mo game ke tiep (dung trong tien trinh chinh). */
	public static void setTarget(int slot) {
		target = slot;
	}

	/** Chon tab de mo game: tab hien tai (neu dang o trong tab), tab duoc chi dinh, hoac tab trong dau tien. -1 = het cho. */
	public static int pickSlot(Context c) {
		int cur = current();
		if (cur >= 0) {
			return cur;
		}
		int t = target;
		target = -1;
		if (t >= 0 && t < COUNT && !info(c, t).running) {
			return t;
		}
		for (int i = 0; i < COUNT; i++) {
			if (!info(c, i).running) {
				return i;
			}
		}
		return -1;
	}

	private static File dir(Context c) {
		File d = new File(c.getFilesDir(), "slots");
		if (!d.isDirectory()) {
			d.mkdirs();
		}
		return d;
	}

	public static void markRunning(Context c, String title, int taskId) {
		int slot = current();
		if (slot < 0) {
			return;
		}
		try (FileWriter w = new FileWriter(new File(dir(c), "slot" + slot))) {
			w.write(android.os.Process.myPid() + "\n" + taskId + "\n" + (title == null ? "" : title.replace('\n', ' ')) + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void markStopped(Context c) {
		int slot = current();
		if (slot >= 0) {
			new File(dir(c), "slot" + slot).delete();
		}
	}

	public static Info info(Context c, int slot) {
		File f = new File(dir(c), "slot" + slot);
		if (!f.exists()) {
			return new Info(slot, false, "", -1);
		}
		try (BufferedReader r = new BufferedReader(new FileReader(f))) {
			int pid = Integer.parseInt(r.readLine().trim());
			int task = Integer.parseInt(r.readLine().trim());
			String title = r.readLine();
			if (!isAlive(c, slot, pid)) {
				f.delete();
				return new Info(slot, false, "", -1);
			}
			return new Info(slot, true, title == null ? "" : title, task);
		} catch (Exception e) {
			f.delete();
			return new Info(slot, false, "", -1);
		}
	}

	private static boolean isAlive(Context c, int slot, int pid) {
		File cmd = new File("/proc/" + pid + "/cmdline");
		if (!cmd.exists()) {
			return false;
		}
		try (BufferedReader r = new BufferedReader(new FileReader(cmd))) {
			String s = r.readLine();
			return s != null && s.startsWith(c.getPackageName() + suffix(slot));
		} catch (IOException e) {
			return false;
		}
	}

	public static int runningCount(Context c) {
		int n = 0;
		for (int i = 0; i < COUNT; i++) {
			if (info(c, i).running) {
				n++;
			}
		}
		return n;
	}
}
