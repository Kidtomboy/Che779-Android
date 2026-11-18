/*
 *  Che779 Emulator - tab bar (numbered tabs only; the function icons live in the bottom BottomBar).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 *  Layout idea: J2ME_NST (NST Tien).
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Top bar: only the tab numbers [1][2][3]. Tap selects, long press renames. */
final class TabBar extends LinearLayout {
	interface Listener {
		void onSelect(Session s);

		void onRename(Session s);
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
	}

	private int dp(int v) {
		return (int) (v * density + 0.5f);
	}

	void update(List<Session> order, Session active) {
		tabs.removeAllViews();
		for (int i = 0; i < order.size(); i++) {
			final Session s = order.get(i);
			TextView t = new TextView(getContext());
			t.setText(s.label != null && !s.label.isEmpty() ? s.slot + "·" + s.label : String.valueOf(s.slot));
			t.setSingleLine(true);
			t.setEllipsize(android.text.TextUtils.TruncateAt.END);
			t.setPadding(dp(6), 0, dp(6), 0);
			t.setOnLongClickListener(v -> {
				listener.onRename(s);
				return true;
			});
			t.setGravity(Gravity.CENTER);
			t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			t.setTextColor(s == active ? Color.WHITE : 0xFF9AA4B2);
			t.setBackgroundColor(s == active ? SELECTED : 0x00000000);
			t.setOnClickListener((View v) -> listener.onSelect(s));
			tabs.addView(t, new LayoutParams(s.label != null && !s.label.isEmpty() ? dp(88) : dp(44), dp(36)));
		}
		int idx = order.indexOf(active);
		if (idx >= 0) {
			final int x = idx * dp(60);
			scroll.post(() -> scroll.smoothScrollTo(Math.max(0, x - dp(88)), 0));
		}
	}
}
