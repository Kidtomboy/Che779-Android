/*
 *  Che779 Emulator - macro phím: ghi lại thao tác trên bàn phím Che779 rồi phát lại (có đồng bộ nhóm khi bật).
 *  Giấy phép Apache License 2.0 (cùng giấy phép J2ME Loader).
 */
package javax.microedition.shell;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ghi và phát macro phím cho tab đang chọn. Chạy trên luồng giao diện. */
final class MacroManager {
	/** Thời gian phát liên tục tối đa (an toàn): 10 phút. */
	private static final long MAX_PLAY_MS = 10 * 60 * 1000L;
	private static final long LOOP_GAP_MS = 300;

	private final MicroActivity act;
	private final Handler handler = new Handler(Looper.getMainLooper());
	/** Mỗi phần tử: {độ trễ ms so với lúc bắt đầu, mã phím, 1 = nhấn / 0 = nhả}. */
	private final List<long[]> events = new ArrayList<>();
	private final Set<Integer> held = new HashSet<>();
	private boolean recording;
	private boolean playing;
	private long startedAt;
	private long playUntil;

	MacroManager(MicroActivity act) {
		this.act = act;
	}

	boolean isRecording() {
		return recording;
	}

	boolean isPlaying() {
		return playing;
	}

	/** Được gọi mỗi khi bàn phím Che779 gửi phím (không gọi khi đang phát). */
	void onKey(int code, boolean down) {
		if (recording) {
			events.add(new long[]{SystemClock.uptimeMillis() - startedAt, code, down ? 1 : 0});
		}
	}

	void startRecording() {
		stop();
		events.clear();
		startedAt = SystemClock.uptimeMillis();
		recording = true;
	}

	/** Dừng ghi và lưu cho tab có số {@code slot}. @return số sự kiện đã ghi */
	int stopRecording(int slot) {
		recording = false;
		save(slot);
		return events.size();
	}

	boolean has(int slot) {
		load(slot);
		return !events.isEmpty();
	}

	void clear(int slot) {
		stop();
		events.clear();
		prefs().edit().remove(key(slot)).apply();
	}

	/** @param times số lần phát; 0 = lặp liên tục tới khi dừng hoặc hết 10 phút */
	void play(int slot, int times) {
		stop();
		load(slot);
		if (events.isEmpty()) {
			return;
		}
		playing = true;
		playUntil = SystemClock.uptimeMillis() + MAX_PLAY_MS;
		round(times);
	}

	private void round(final int times) {
		if (!playing) {
			return;
		}
		long last = 0;
		for (final long[] e : events) {
			last = Math.max(last, e[0]);
			handler.postDelayed(() -> {
				if (!playing) {
					return;
				}
				int code = (int) e[1];
				boolean down = e[2] == 1;
				if (down) {
					held.add(code);
				} else {
					held.remove(code);
				}
				act.sendKey(code, down);
			}, e[0]);
		}
		handler.postDelayed(() -> {
			if (!playing) {
				return;
			}
			if (times == 1 || SystemClock.uptimeMillis() >= playUntil) {
				stop();
				return;
			}
			round(times == 0 ? 0 : times - 1);
		}, last + LOOP_GAP_MS);
	}

	/** Dừng ghi/phát và nhả mọi phím đang giữ. */
	void stop() {
		boolean wasPlaying = playing;
		playing = false;
		recording = false;
		handler.removeCallbacksAndMessages(null);
		if (wasPlaying) {
			for (Integer c : new ArrayList<>(held)) {
				act.sendKey(c, false);
			}
		}
		held.clear();
	}

	private SharedPreferences prefs() {
		return act.getSharedPreferences("che779_macro", android.content.Context.MODE_PRIVATE);
	}

	private static String key(int slot) {
		return "tab_" + slot;
	}

	private void save(int slot) {
		StringBuilder sb = new StringBuilder();
		for (long[] e : events) {
			sb.append(e[0]).append(',').append(e[1]).append(',').append(e[2]).append(';');
		}
		prefs().edit().putString(key(slot), sb.toString()).apply();
	}

	private void load(int slot) {
		events.clear();
		String s = prefs().getString(key(slot), "");
		if (s == null || s.isEmpty()) {
			return;
		}
		try {
			for (String part : s.split(";")) {
				if (part.isEmpty()) {
					continue;
				}
				String[] f = part.split(",");
				events.add(new long[]{Long.parseLong(f[0]), Long.parseLong(f[1]), Long.parseLong(f[2])});
			}
		} catch (NumberFormatException ex) {
			events.clear();
		}
	}

	/** Đã hiện cảnh báo lần đầu chưa. */
	boolean warned() {
		return prefs().getBoolean("warned", false);
	}

	void setWarned() {
		prefs().edit().putBoolean("warned", true).apply();
	}
}
