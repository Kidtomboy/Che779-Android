/*
 *  Che779 Emulator - lõi đa phiên (nhiều tab chạy song song).
 *  Giấy phép Apache License 2.0 (cùng giấy phép J2ME Loader).
 */
package javax.microedition.shell;

import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;

/**
 * Một game đang chạy ("tab"). Mọi thứ từng là biến static toàn tiến trình trong
 * engine (màn hình, hàng đợi sự kiện, bàn phím ảo, luồng midlet) nay nằm ở đây, và được
 * found through {@link #current()}.
 * <p>
 * Luồng do game tạo ra kế thừa phiên của nó, nên mã engine chạy trên các luồng đó tìm
 * đúng tab. Luồng giao diện Android không có phiên riêng và dùng
 * {@link #active()}, tab đang ở phía trước.
 */
public final class Session {
	private static final InheritableThreadLocal<Session> CURRENT = new InheritableThreadLocal<>();
	private static final ConcurrentHashMap<Integer, Session> ALL = new ConcurrentHashMap<>();
	private static volatile Session active;
	private static int nextId = 1;

	/** Chế độ lưới: chép phím của tab đang chọn sang các tab khác. */
	public static volatile boolean syncKeys;
	/** True khi các tab hiện cạnh nhau. */
	public static volatile boolean grid;

	/** "Treo" render mode: 0 = draw, 1 = 20 FPS, 2 = no drawing (games keep running). */
	public static volatile int treo;

	public static void setTreo(int mode) {
		treo = mode;
		for (Session s : ALL.values()) {
			javax.microedition.lcdui.Displayable d = s.current;
			if (d instanceof javax.microedition.lcdui.Canvas) {
				((javax.microedition.lcdui.Canvas) d).repaint();
			}
		}
	}

	public final int id;
	public final String appPath;
	public final DisplayConfig dc = new DisplayConfig();
	/** Proxy / chuyển hướng IP của tab này. */
	public final NetConfig net = new NetConfig();
	public volatile Display display;
	public volatile EventQueue queue;
	public volatile VirtualKeyboard vk;
	volatile MidletThread thread;
	// tài nguyên theo từng game (trước đây là static trong AppClassLoader)
	public volatile String dataDir;
	public volatile String dataSuffix = "";
	volatile java.io.File oldResDir;
	volatile net.lingala.zip4j.ZipFile resZip;
	volatile AppClassLoader classLoader;
	public volatile java.util.Map<String, String> props;
	// trạng thái giao diện theo từng game (trước đây là trường của MicroActivity)
	volatile MicroLoader microLoader;
	public volatile String appName;
	/** Tên tab do người dùng đặt (null = hiện số tab). */
	public volatile String label;
	/** True khi tab này tham gia đồng bộ phím (chế độ lưới). */
	public volatile boolean sync;
	/** Số tab cố định (từ 1); số đã đóng sẽ được tab mới kế tiếp dùng lại. */
	public volatile int slot;
	/** Tên các lớp game do tab này nạp (dùng để tìm các luồng của nó khi đóng). */
	public final java.util.Set<String> classes = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** Giới hạn fps trước khi chế độ pin đổi nó (-1 = chưa đổi). */
	public volatile int fpsBackup = -1;
	/** Hạn mức dump luồng chẩn đoán của tab này (tránh bản đồ theo tab ở cấp tiến trình). */
	final java.util.concurrent.atomic.AtomicInteger diagnosticDumps = new java.util.concurrent.atomic.AtomicInteger();

	/** Chạy khi tab này đã bị gỡ (dùng để mở lại: "khởi động lại tab"). */
	volatile Runnable restartAfter;
	/** Socket và kết nối HTTP do game của tab này mở, để khi khởi động lại có thể đóng hết. */
	private final java.util.Set<Object> connections =
			java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.WeakHashMap<Object, Boolean>()));

	/** Ghi nhớ một kết nối của tab này (giữ bằng tham chiếu yếu). */
	public void track(Object connection) {
		if (connection != null) {
			connections.add(connection);
		}
	}

	/** Đóng mọi kết nối tab này đã mở, để không socket cũ nào sống lâu hơn tab. */
	public void closeNet() {
		Object[] all;
		synchronized (connections) {
			all = connections.toArray();
			connections.clear();
		}
		for (Object o : all) {
			try {
				if (o instanceof java.net.Socket) {
					((java.net.Socket) o).close();
				} else if (o instanceof java.net.HttpURLConnection) {
					((java.net.HttpURLConnection) o).disconnect();
				} else if (o instanceof java.io.Closeable) {
					((java.io.Closeable) o).close();
				}
			} catch (Throwable ignored) {
			}
		}
	}

	/** Giải phóng những gì tab đã đóng còn giữ: kết nối và kho tài nguyên. */
	void release() {
		closeNet();
		try {
			net.lingala.zip4j.ZipFile z = resZip;
			if (z != null) {
				z.close();
			}
		} catch (Throwable ignored) {
		}
	}

	/** Ngắt mọi luồng đang chạy mã của game thuộc tab này. */
	public void killThreads() {
		try {
			for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
				Thread t = e.getKey();
				if (t == Thread.currentThread() || t.isDaemon() && t.getName().startsWith("Che779")) {
					continue;
				}
				for (StackTraceElement el : e.getValue()) {
					if (classes.contains(el.getClassName())) {
						t.interrupt();
						break;
					}
				}
			}
		} catch (Throwable ignored) {
		}
	}
	public volatile javax.microedition.lcdui.Displayable current;
	public volatile int menuKey;
	public volatile boolean multiTouch;
	public volatile String pointerNumber;

	private Session(int id, String appPath) {
		this.id = id;
		this.appPath = appPath;
	}

	/** Tạo phiên và hàng đợi sự kiện của nó; luồng hàng đợi kế thừa phiên mới. */
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

	/** Gắn luồng gọi (và mọi luồng nó tạo sau này) vào phiên này. */
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

	public static java.util.Collection<Session> all() {
		return ALL.values();
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
