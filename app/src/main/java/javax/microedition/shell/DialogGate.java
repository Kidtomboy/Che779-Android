/*
 *  Che779 Emulator - mỗi lúc chỉ một hộp thoại + giới hạn tốc độ chạm cho nút thanh công cụ.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.app.Dialog;
import android.content.Context;
import android.os.SystemClock;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

/**
 * Bấm liên tục thanh công cụ từng làm chồng nhiều hộp thoại (các lớp tối chồng lên nhau).
 * {@link #tap()} chỉ cho hành động thanh công cụ chạy khi không có hộp thoại nào đang hiện và lần
 * action was at least 500 ms ago. Every dialog shown through {@link #present(Dialog)}
 * (hoặc {@link Builder#show()}) được theo dõi.
 */
final class DialogGate {
	private static final ArrayList<Dialog> SHOWN = new ArrayList<>();
	private static long last;

	private DialogGate() {
	}

	/** True khi còn ít nhất một hộp thoại được theo dõi trên màn hình. */
	static boolean busy() {
		for (int i = SHOWN.size() - 1; i >= 0; i--) {
			Dialog d = SHOWN.get(i);
			if (d.isShowing()) {
				return true;
			}
			SHOWN.remove(i);
		}
		return false;
	}

	/** Cổng cho nút thanh công cụ: false = bỏ qua lần chạm này. */
	static boolean tap() {
		long now = SystemClock.uptimeMillis();
		if (busy() || now - last < 500) {
			return false;
		}
		last = now;
		return true;
	}

	static void present(Dialog d) {
		SHOWN.add(d);
		d.show();
	}

	/** Builder có show() đăng ký hộp thoại với cổng. */
	static final class Builder extends AlertDialog.Builder {
		Builder(Context c) {
			super(c);
		}

		@Override
		public AlertDialog show() {
			AlertDialog d = create();
			present(d);
			return d;
		}
	}
}
