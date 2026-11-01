/*
 *  Che779 Emulator - multi-session core.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 *  Concept inspired by the multi-tab sessions of J2ME_NST (NST Tien) and J2ME Loader v10 (Hung Hero).
 */
package javax.microedition.shell;

import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;

/**
 * One running game ("tab"). Everything that used to be a process-wide static in the
 * engine (display, event queue, virtual keyboard, midlet thread) now lives here, and is
 * found through {@link #current()}.
 * <p>
 * Threads created by a game inherit its session, so engine code running on them resolves
 * to the right tab. The Android UI thread has no session of its own and falls back to
 * {@link #active()}, the tab currently in front.
 */
public final class Session {
	private static final InheritableThreadLocal<Session> CURRENT = new InheritableThreadLocal<>();
	private static final ConcurrentHashMap<Integer, Session> ALL = new ConcurrentHashMap<>();
	private static volatile Session active;
	private static int nextId = 1;

	public final int id;
	public final String appPath;
	public volatile Display display;
	public volatile EventQueue queue;
	public volatile VirtualKeyboard vk;
	volatile MidletThread thread;
	// per-game resources (were statics in AppClassLoader)
	public volatile String dataDir;
	public volatile String dataSuffix = "";
	volatile java.io.File oldResDir;
	volatile net.lingala.zip4j.ZipFile resZip;
	volatile AppClassLoader classLoader;
	public volatile java.util.Map<String, String> props;
	// per-game UI state (were fields of MicroActivity)
	volatile MicroLoader microLoader;
	public volatile String appName;
	public volatile javax.microedition.lcdui.Displayable current;
	public volatile int menuKey;
	public volatile boolean multiTouch;
	public volatile String pointerNumber;

	private Session(int id, String appPath) {
		this.id = id;
		this.appPath = appPath;
	}

	/** Creates a session and its event queue; the queue thread inherits the new session. */
	public static synchronized Session create(String appPath, String dataSuffix) {
		Session s = new Session(nextId++, appPath);
		s.dataSuffix = dataSuffix == null ? "" : dataSuffix;
		Session prev = CURRENT.get();
		CURRENT.set(s);
		try {
			s.queue = new EventQueue();
			s.queue.startProcessing();
		} finally {
			CURRENT.set(prev);
		}
		ALL.put(s.id, s);
		if (active == null) {
			active = s;
		}
		return s;
	}

	/** Binds the calling thread (and every thread it creates later) to this session. */
	public void bindCurrentThread() {
		CURRENT.set(this);
	}

	public static Session current() {
		Session s = CURRENT.get();
		return s != null ? s : active;
	}

	public static Session active() {
		return active;
	}

	public static void setActive(Session s) {
		active = s;
	}

	public static Session get(int id) {
		return ALL.get(id);
	}

	public static void clearAll() {
		ALL.clear();
		active = null;
	}

	public static int count() {
		return ALL.size();
	}

	public void remove() {
		ALL.remove(id);
		if (active == this) {
			active = null;
			for (Session o : ALL.values()) {
				active = o;
				break;
			}
		}
	}
}
