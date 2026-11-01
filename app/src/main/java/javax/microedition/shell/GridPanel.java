/*
 *  Che779 Emulator - grid of tabs shown side by side.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 *  Idea: J2ME_NST (NST Tien) multi-game grid.
 */
package javax.microedition.shell;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Displayable;

/** Rows x columns of cells, one per tab. The cell touched last becomes the active tab. */
final class GridPanel extends LinearLayout {
	interface Listener {
		void onFocus(Session s);
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

	private final class Cell extends FrameLayout {
		final Session session;

		Cell(Context c, Session s) {
			super(c);
			session = s;
			setPadding(border, border, border, border);
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
				listener.onFocus(session);
			}
			return super.dispatchTouchEvent(ev);
		}
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
		int n = sessions.size();
		if (n == 0) {
			return;
		}
		int cols = columnsFor(n, w, h);
		int rows = (n + cols - 1) / cols;
		int i = 0;
		for (int r = 0; r < rows; r++) {
			LinearLayout row = new LinearLayout(getContext());
			row.setOrientation(HORIZONTAL);
			addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
			for (int c = 0; c < cols; c++) {
				if (i < n) {
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
		cell.removeAllViews();
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
		cell.addView(content, new FrameLayout.LayoutParams(
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
