/*
 *  Che779 Emulator - grid of tabs shown side by side.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 *  Idea: J2ME_NST (NST Tien) multi-game grid.
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Displayable;

/** Rows x columns of cells, one per tab. The cell touched last becomes the active tab. */
final class GridPanel extends LinearLayout {
	interface Listener {
		void onFocus(Session s);

		void onClose(Session s);

		void onFullscreen(Session s);

		void onProxy();

		void onAdd();

		void onAddMode();
	}

	private static final int ACTIVE = 0xFF1565D8;
	private static final int IDLE = 0xFF000000;

	private final Listener listener;
	private final Map<Session, Cell> cells = new HashMap<>();
	private final int border;

	GridPanel(Context context, Listener listener) {
		super(context);
		this.listener = listener;
		border = Math.max(1, (int) (2 * context.getResources().getDisplayMetrics().density));
		setOrientation(VERTICAL);
		setBackgroundColor(IDLE);
	}

	private int dp(int v) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
	}

	private GradientDrawable card() {
		GradientDrawable g = new GradientDrawable();
		g.setColor(0xFF1E1E1E);
		g.setCornerRadius(dp(10));
		return g;
	}

	private TextView glyph(Context c, String t, int color, View.OnClickListener l) {
		TextView v = new TextView(c);
		v.setText(t);
		v.setTextColor(color);
		v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		v.setGravity(Gravity.CENTER);
		v.setOnClickListener(l);
		v.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(34)));
		return v;
	}

	/** Cell in the style of Hùng Hero: title + close on top, proxy / stop / full screen below. */
	private final class Cell extends FrameLayout {
		final Session session;
		final FrameLayout content;

		Cell(Context c, Session s) {
			super(c);
			session = s;
			setPadding(border, border, border, border);
			LinearLayout col = new LinearLayout(c);
			col.setOrientation(VERTICAL);
			col.setBackground(card());
			LinearLayout head = new LinearLayout(c);
			head.setGravity(Gravity.CENTER_VERTICAL);
			TextView title = new TextView(c);
			String t = s.label != null && !s.label.isEmpty() ? s.label : s.appName;
			title.setText(t == null ? "Tab" : t);
			title.setTextColor(Color.WHITE);
			title.setTypeface(null, android.graphics.Typeface.BOLD);
			title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
			title.setSingleLine(true);
			title.setPadding(dp(10), 0, 0, 0);
			title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			head.addView(title);
			head.addView(glyph(c, "\u2715", 0xFFBBBBBB, v -> listener.onClose(session)));
			col.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
			content = new FrameLayout(c);
			col.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
			LinearLayout foot = new LinearLayout(c);
			foot.setGravity(Gravity.CENTER_VERTICAL);
			foot.setBackgroundColor(0xFF2A2A2A);
			foot.addView(glyph(c, "\uD83C\uDF10", Color.WHITE, v -> listener.onProxy()));
			View sp = new View(c);
			sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
			foot.addView(sp);
			foot.addView(glyph(c, "\u25A0", 0xFFE53935, v -> listener.onClose(session)));
			foot.addView(glyph(c, "\u26F6", 0xFF42A5F5, v -> listener.onFullscreen(session)));
			col.addView(foot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
			addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
				listener.onFocus(session);
			}
			return super.dispatchTouchEvent(ev);
		}
	}

	private View addCell() {
		FrameLayout f = new FrameLayout(getContext());
		f.setPadding(border, border, border, border);
		LinearLayout col = new LinearLayout(getContext());
		col.setOrientation(VERTICAL);
		col.setGravity(Gravity.CENTER);
		col.setBackground(card());
		TextView plus = new TextView(getContext());
		plus.setText("+");
		plus.setTextColor(Color.WHITE);
		plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
		plus.setGravity(Gravity.CENTER);
		TextView lab = new TextView(getContext());
		lab.setText("Thêm tab");
		lab.setTextColor(0xFF9E9E9E);
		lab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		lab.setGravity(Gravity.CENTER);
		col.addView(plus);
		col.addView(lab);
		col.setOnClickListener(v -> listener.onAdd());
		col.setOnLongClickListener(v -> {
			listener.onAddMode();
			return true;
		});
		f.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		return f;
	}

	/** Columns for n tabs so that cells stay as square as the panel shape allows. */
	static int columnsFor(int n, int w, int h) {
		if (n <= 1) {
			return 1;
		}
		double aspect = h <= 0 ? 1 : (double) w / h;
		int best = 1;
		double bestScore = Double.MAX_VALUE;
		for (int c = 1; c <= n; c++) {
			int r = (n + c - 1) / c;
			double cellAspect = (w / (double) c) / (h / (double) r + 0.0001);
			double score = Math.abs(Math.log(cellAspect)) + 0.05 * (c * r - n);
			if (aspect > 0 && score < bestScore) {
				bestScore = score;
				best = c;
			}
		}
		return best;
	}

	void rebuild(List<Session> sessions, Session active, int w, int h) {
		for (Cell c : cells.values()) {
			c.removeAllViews();
		}
		removeAllViews();
		cells.clear();
		int count = sessions.size();
		int n = count + 1; // the last cell is "+ Thêm tab"
		int cols = columnsFor(n, w, h);
		int rows = (n + cols - 1) / cols;
		int i = 0;
		for (int r = 0; r < rows; r++) {
			LinearLayout row = new LinearLayout(getContext());
			row.setOrientation(HORIZONTAL);
			addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
			for (int c = 0; c < cols; c++) {
				if (i == count) {
					i++;
					row.addView(addCell(), new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
				} else if (i < n) {
					Session s = sessions.get(i++);
					Cell cell = new Cell(getContext(), s);
					cells.put(s, cell);
					row.addView(cell, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
					show(s);
				} else {
					row.addView(new View(getContext()), new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
				}
			}
		}
		setActive(active);
	}

	/** (Re)attaches the current displayable of a tab to its cell. */
	void show(Session s) {
		Cell cell = cells.get(s);
		if (cell == null) {
			return;
		}
		cell.content.removeAllViews();
		Displayable d = s.current;
		if (d == null || d instanceof Alert) {
			return;
		}
		View v = d.getDisplayableView();
		if (v.getParent() instanceof ViewGroup) {
			((ViewGroup) v.getParent()).removeView(v);
		}
		// own background: lists and forms are transparent and would show the blue focus border colour
		FrameLayout content = new FrameLayout(getContext());
		content.setBackgroundColor(d instanceof javax.microedition.lcdui.Canvas ? 0xFF000000 : 0xFFFAFAFA);
		content.addView(v, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		cell.content.addView(content, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	void setActive(Session active) {
		for (Map.Entry<Session, Cell> e : cells.entrySet()) {
			e.getValue().setBackgroundColor(e.getKey() == active ? ACTIVE : IDLE);
		}
	}

	void detachAll() {
		for (Cell c : cells.values()) {
			c.removeAllViews();
		}
		removeAllViews();
		cells.clear();
	}
}
