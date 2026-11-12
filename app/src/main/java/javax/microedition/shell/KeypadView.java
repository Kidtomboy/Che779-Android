/*
 *  Che779 Emulator - phone-style virtual keypad (same layout as the J2ME virtual keyboard),
 *  used as a popup in grid mode and inside the floating (bubble) window.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.microedition.lcdui.Canvas;

final class KeypadView extends LinearLayout {
	interface Sink {
		/** @param codes J2ME key codes (diagonals have two) */
		void key(int[] codes, boolean down);

		void close();
	}

	private static final int U = Canvas.KEY_UP, D = Canvas.KEY_DOWN, L = Canvas.KEY_LEFT, R = Canvas.KEY_RIGHT;

	KeypadView(Context c, int keyHeightDp, int textSp, Sink sink) {
		super(c);
		float dp = c.getResources().getDisplayMetrics().density;
		setOrientation(VERTICAL);
		setBackgroundColor(0xFFD3D3D6);
		int pad = (int) (4 * dp);
		setPadding(pad, pad, pad, pad);
		String[][] labels = {
				{"1", "2", "3", "", "✕", ""},
				{"4", "5", "6", "↖", "↑", "↗"},
				{"7", "8", "9", "←", "F", "→"},
				{"*", "0", "#", "↙", "↓", "↘"}};
		int[][][] codes = {
				{{Canvas.KEY_NUM1}, {Canvas.KEY_NUM2}, {Canvas.KEY_NUM3}, {Canvas.KEY_SOFT_LEFT}, null, {Canvas.KEY_SOFT_RIGHT}},
				{{Canvas.KEY_NUM4}, {Canvas.KEY_NUM5}, {Canvas.KEY_NUM6}, {U, L}, {U}, {U, R}},
				{{Canvas.KEY_NUM7}, {Canvas.KEY_NUM8}, {Canvas.KEY_NUM9}, {L}, {Canvas.KEY_FIRE}, {R}},
				{{Canvas.KEY_STAR}, {Canvas.KEY_NUM0}, {Canvas.KEY_POUND}, {D, L}, {D}, {D, R}}};
		// top-right row: L and R are the soft keys
		labels[0][3] = "L";
		labels[0][5] = "R";
		for (int r = 0; r < labels.length; r++) {
			LinearLayout row = new LinearLayout(c);
			for (int col = 0; col < 6; col++) {
				final int[] k = codes[r][col];
				boolean close = r == 0 && col == 4;
				TextView b = new TextView(c);
				b.setText(labels[r][col]);
				b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
				b.setGravity(Gravity.CENTER);
				b.setTextColor(close ? 0xFFB3261E : 0xFF5B5BA8);
				GradientDrawable bg = new GradientDrawable();
				bg.setCornerRadius(10 * dp);
				bg.setColor(0xFFE4E4E8);
				bg.setStroke((int) Math.max(1, dp / 2), 0xFFC4C4CC);
				boolean empty = labels[r][col].isEmpty();
				b.setBackground(empty ? null : bg);
				LayoutParams lp = new LayoutParams(0, (int) (keyHeightDp * dp), 1f);
				int m = (int) (2 * dp);
				lp.setMargins(m, m, m, m);
				if (!empty) {
					b.setOnTouchListener((View v, MotionEvent e) -> {
						switch (e.getActionMasked()) {
							case MotionEvent.ACTION_DOWN:
								if (close) {
									sink.close();
								} else {
									v.setAlpha(0.55f);
									sink.key(k, true);
								}
								return true;
							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_CANCEL:
								v.setAlpha(1f);
								if (!close) {
									sink.key(k, false);
								}
								return true;
							default:
								return true;
						}
					});
				}
				row.addView(b, lp);
			}
			addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
	}
}
