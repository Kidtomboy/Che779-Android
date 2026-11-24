/*
 *  Che779 Emulator - dựng bàn phím trên màn hình theo loại đã chọn (số kiểu Nokia hoặc QWERTY Android).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;

import ru.playsoftware.j2meloader.R;

final class KeypadFactory {
	static final int NOKIA = 0, ANDROID = 1;
	private static final String PREF = "che779_keypad_type";
	private static final int BG = 0xFFD3D3D6;

	private KeypadFactory() {
	}

	static int type(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c).getInt(PREF, NOKIA) == ANDROID ? ANDROID : NOKIA;
	}

	static void setType(Context c, int type) {
		PreferenceManager.getDefaultSharedPreferences(c).edit().putInt(PREF, type).apply();
	}

	/**
	 * Một cột: hàng chuyển nhỏ (biểu tượng Nokia, Android, đóng) phía trên bàn phím.
	 *
	 * @param rebuild được gọi sau khi người dùng chọn loại khác, để vẽ lại bàn phím
	 */
	static LinearLayout create(Context c, int type, int keyDp, int textSp, KeypadView.Sink sink, Runnable rebuild) {
		float dp = c.getResources().getDisplayMetrics().density;
		LinearLayout col = new LinearLayout(c);
		col.setOrientation(LinearLayout.VERTICAL);
		col.setBackgroundColor(BG);
		LinearLayout head = new LinearLayout(c);
		head.setOrientation(LinearLayout.HORIZONTAL);
		head.setGravity(Gravity.CENTER_VERTICAL);
		head.addView(switchIcon(c, R.drawable.ic_c_dialpad, type == NOKIA, v -> pick(c, NOKIA, type, rebuild)), cell(dp));
		head.addView(switchIcon(c, R.drawable.ic_c_keyboard, type == ANDROID, v -> pick(c, ANDROID, type, rebuild)), cell(dp));
		View spacer = new View(c);
		head.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
		head.addView(Icons.view(c, R.drawable.ic_c_close, 0xFFB3261E, 6, v -> sink.close()), cell(dp));
		col.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		View pad = type == ANDROID ? new QwertyKeypadView(c, keyDp, textSp, sink) : new KeypadView(c, keyDp, textSp, sink);
		col.addView(pad, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return col;
	}

	private static void pick(Context c, int wanted, int current, Runnable rebuild) {
		if (wanted == current) {
			return;
		}
		setType(c, wanted);
		rebuild.run();
	}

	private static LinearLayout.LayoutParams cell(float dp) {
		return new LinearLayout.LayoutParams((int) (44 * dp), (int) (34 * dp));
	}

	private static ImageView switchIcon(Context c, int res, boolean active, View.OnClickListener l) {
		ImageView v = Icons.view(c, res, active ? 0xFF1565D8 : 0xFF8A8A94, 6, l);
		v.setContentDescription(res == R.drawable.ic_c_dialpad ? "Bàn phím Nokia" : "Bàn phím Android");
		return v;
	}
}
