/*
 *  Che779 Emulator - tab bar (numbered tabs, -, +, game picker).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 *  Layout idea: J2ME_NST (NST Tien).
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import ru.playsoftware.j2meloader.R;

/** Compact bar: [1][2][3] ... [-] [+] [pad]. */
final class TabBar extends LinearLayout {
	interface Listener {
		void onSelect(Session s);

		void onClose();

		void onClone();

		void onPickGame();

		void onGrid();

		void onBubble();

		void onMenu();
	}

	private static final int BG = 0xFF14181F;
	private static final int SELECTED = 0xFF1565D8;

	private final LinearLayout tabs;
	private final HorizontalScrollView scroll;
	private final Listener listener;
	private final float density;

	TabBar(Context context, Listener listener) {
		super(context);
		this.listener = listener;
		density = context.getResources().getDisplayMetrics().density;
		setOrientation(HORIZONTAL);
		setBackgroundColor(BG);
		setGravity(Gravity.CENTER_VERTICAL);
		scroll = new HorizontalScrollView(context);
		scroll.setHorizontalScrollBarEnabled(false);
		tabs = new LinearLayout(context);
		tabs.setOrientation(HORIZONTAL);
		scroll.addView(tabs);
		addView(scroll, new LayoutParams(0, dp(36), 1f));
		FrameLayout bubbleCell = new FrameLayout(context);
		ImageView bubbleBtn = new ImageView(context);
		bubbleBtn.setImageResource(R.drawable.che779_bubble);
		bubbleBtn.setOnClickListener(v -> listener.onBubble());
		bubbleCell.addView(bubbleBtn, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
		addView(bubbleCell, new LayoutParams(0, dp(36), 0.5f));
		addView(button(context, "−", v -> listener.onClose()));
		addView(button(context, "+", v -> listener.onClone()));
		addView(button(context, "▦", v -> listener.onGrid()));
		ImageView pad = new ImageView(context);
		pad.setImageResource(R.drawable.ic_che779_gamepad);
		pad.setPadding(dp(10), dp(8), dp(10), dp(8));
		pad.setOnClickListener(v -> listener.onPickGame());
		addView(pad, new LayoutParams(dp(44), dp(36)));
		addView(button(context, "⋮", v -> listener.onMenu()));
	}

	private int dp(int v) {
		return (int) (v * density + 0.5f);
	}

	private TextView button(Context context, String text, OnClickListener click) {
		TextView t = new TextView(context);
		t.setText(text);
		t.setGravity(Gravity.CENTER);
		t.setTextColor(Color.WHITE);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		t.setTypeface(Typeface.DEFAULT_BOLD);
		t.setOnClickListener(click);
		t.setLayoutParams(new LayoutParams(dp(40), dp(36)));
		return t;
	}

	void update(List<Session> order, Session active) {
		tabs.removeAllViews();
		for (int i = 0; i < order.size(); i++) {
			final Session s = order.get(i);
			TextView t = new TextView(getContext());
			t.setText(String.valueOf(i + 1));
			t.setGravity(Gravity.CENTER);
			t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			t.setTextColor(s == active ? Color.WHITE : 0xFF9AA4B2);
			t.setBackgroundColor(s == active ? SELECTED : 0x00000000);
			t.setOnClickListener((View v) -> listener.onSelect(s));
			tabs.addView(t, new LayoutParams(dp(44), dp(36)));
		}
		int idx = order.indexOf(active);
		if (idx >= 0) {
			final int x = idx * dp(44);
			scroll.post(() -> scroll.smoothScrollTo(Math.max(0, x - dp(88)), 0));
		}
	}
}
