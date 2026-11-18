/*
 *  Che779 Emulator - Android-style QWERTY keypad. Letters are sent as their character code, like the
 *  QWERTY J2ME phones did; arrows, OK and the soft keys use the usual J2ME key codes.
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


final class QwertyKeypadView extends LinearLayout {
	private static final String[] ROWS = {"1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"};

	private boolean caps;
	private final java.util.ArrayList<TextView> letters = new java.util.ArrayList<>();

	QwertyKeypadView(Context c, int keyHeightDp, int textSp, KeypadView.Sink sink) {
		super(c);
		final float dp = c.getResources().getDisplayMetrics().density;
		setOrientation(VERTICAL);
		setBackgroundColor(0xFFD3D3D6);
		int pad = (int) (4 * dp);
		setPadding(pad, pad, pad, pad);
		for (int r = 0; r < ROWS.length; r++) {
			LinearLayout row = new LinearLayout(c);
			if (r == 3) {
				TextView shift = key(c, "⇧", keyHeightDp, textSp, dp, 1.5f);
				shift.setOnClickListener(v -> setCaps(!caps));
				row.addView(shift);
			}
			for (char ch : ROWS[r].toCharArray()) {
				final char base = ch;
				TextView t = key(c, String.valueOf(ch), keyHeightDp, textSp, dp, 1f);
				if (Character.isLetter(ch)) {
					letters.add(t);
				}
				press(t, () -> new int[]{Character.isLetter(base) && caps ? Character.toUpperCase(base) : base}, sink);
				row.addView(t);
			}
			if (r == 3) {
				TextView del = key(c, "⌫", keyHeightDp, textSp, dp, 1.5f);
				press(del, () -> new int[]{-8}, sink);
				row.addView(del);
			}
			addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		LinearLayout bottom = new LinearLayout(c);
		String[] labels = {"L", "◀", "▲", "▼", "▶", "OK", "R"};
		int[] codes = {Canvas.KEY_SOFT_LEFT, Canvas.KEY_LEFT, Canvas.KEY_UP, Canvas.KEY_DOWN, Canvas.KEY_RIGHT,
				Canvas.KEY_FIRE, Canvas.KEY_SOFT_RIGHT};
		for (int i = 0; i < labels.length; i++) {
			final int code = codes[i];
			TextView t = key(c, labels[i], keyHeightDp, textSp, dp, 1f);
			press(t, () -> new int[]{code}, sink);
			bottom.addView(t);
		}
		addView(bottom, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		LinearLayout last = new LinearLayout(c);
		TextView space = key(c, "␣", keyHeightDp, textSp, dp, 4f);
		press(space, () -> new int[]{32}, sink);
		last.addView(space);
		TextView enter = key(c, "⏎", keyHeightDp, textSp, dp, 1.5f);
		press(enter, () -> new int[]{10}, sink);
		last.addView(enter);
		addView(last, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	private void setCaps(boolean on) {
		caps = on;
		for (TextView t : letters) {
			String s = t.getText().toString();
			t.setText(on ? s.toUpperCase() : s.toLowerCase());
		}
	}

	private TextView key(Context c, String text, int heightDp, int textSp, float dp, float weight) {
		TextView tv = new TextView(c);
		tv.setText(text);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(12, textSp - 4));
		tv.setGravity(Gravity.CENTER);
		tv.setTextColor(0xFF3B3B6B);
		GradientDrawable bg = new GradientDrawable();
		bg.setCornerRadius(8 * dp);
		bg.setColor(0xFFE4E4E8);
		bg.setStroke((int) Math.max(1, dp / 2), 0xFFC4C4CC);
		tv.setBackground(bg);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (Math.max(30, heightDp - 6) * dp), weight);
		int m = (int) (2 * dp);
		lp.setMargins(m, m, m, m);
		tv.setLayoutParams(lp);
		return tv;
	}

	private interface Codes {
		int[] get();
	}

	@android.annotation.SuppressLint("ClickableViewAccessibility")
	private void press(View v, Codes codes, KeypadView.Sink sink) {
		final int[][] sent = {null};
		v.setOnTouchListener((View x, MotionEvent e) -> {
			switch (e.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					x.setAlpha(0.55f);
					sent[0] = codes.get();
					sink.key(sent[0], true);
					return true;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					x.setAlpha(1f);
					if (sent[0] != null) {
						sink.key(sent[0], false);
						sent[0] = null;
					}
					return true;
				default:
					return true;
			}
		});
	}
}
