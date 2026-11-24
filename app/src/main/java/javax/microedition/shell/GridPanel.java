/*
 *  Che779 Emulator - lưới các tab hiển thị cạnh nhau.
 *  Giấy phép Apache License 2.0 (cùng giấy phép J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.view.Gravity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Displayable;

/** Lưới hàng x cột, mỗi ô một tab. Ô chạm gần nhất trở thành tab đang chọn. */
final class GridPanel extends LinearLayout {
	interface Listener {
		void onFocus(Session s);
	}

	private static final int ACTIVE = 0xFF1565D8;
	private static final int IDLE = 0xFF000000;

	private final Listener listener;
	private final Map<Session, Cell> cells = new HashMap<>();
	private final int border;
	private boolean fill;
	private boolean fit;
	private int builtW, builtH;
	private float aspect = 0.75f;

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

	/** Số cột cho n tab sao cho ô gần vuông nhất có thể theo hình dạng khung. */
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

	/** Chế độ lấp đầy: ô theo hình dạng game (không khe hở), 2 hoặc 3 cột, cuộn khi 3 cột chưa đủ. */
	void setFill(boolean fill, float aspect) {
		this.fill = fill;
		this.aspect = aspect > 0.2f && aspect < 5f ? aspect : 0.75f;
	}

	private int dp(int v) {
		return (int) (v * getResources().getDisplayMetrics().density);
	}

	void setFit(boolean fit) {
		this.fit = fit;
	}

	boolean sizeChanged(int w, int h) {
		return builtW > 0 && (Math.abs(w - builtW) > 2 || Math.abs(h - builtH) > 2);
	}

	boolean needsRelayout(float newAspect) {
		return fill && newAspect > 0.2f && newAspect < 5f && Math.abs(newAspect - aspect) > 0.02f;
	}

	private void buildFilled(List<Session> sessions, Session active, int w, int h) {
		int n = sessions.size();
		int cols, cellW, cellH;
		if (n == 1) {
			cols = 1;
			cellH = Math.min(h, (int) (w / aspect));
			cellW = (int) (cellH * aspect);
		} else {
			cols = 2;
			cellW = w / 2;
			cellH = (int) (cellW / aspect) + 2 * border;
			if (((n + 1) / 2) * cellH > h) {
				cols = 3;
				cellW = w / 3;
				cellH = (int) (cellW / aspect) + 2 * border;
			}
		}
		if (fit) { // cửa sổ nổi: chia đều chiều ngang cho tối đa 3 ô mỗi dòng, ô theo tỉ lệ game, cuộn dọc nếu dài
			cols = Math.min(2, n); // mặc định 2 ô/dòng; đủ rộng (cửa sổ ≥ 85% chiều ngang màn hình) mới dàn 3 ô/dòng
			if (n >= 3 && w >= getResources().getDisplayMetrics().widthPixels * 0.85f) {
				cols = 3;
			}
			cellW = w / cols;
			cellH = (int) ((cellW - 2 * border) / aspect) + 2 * border;
		}
		int rows = (n + cols - 1) / cols;
		ScrollView sv = new ScrollView(getContext());
		sv.setFillViewport(true);
		sv.setVerticalScrollBarEnabled(false);
		LinearLayout holder = new LinearLayout(getContext());
		holder.setOrientation(VERTICAL);
		holder.setGravity(Gravity.CENTER_HORIZONTAL | (rows * cellH <= h ? Gravity.CENTER_VERTICAL : Gravity.TOP));
		int i = 0;
		for (int r = 0; r < rows; r++) {
			LinearLayout row = new LinearLayout(getContext());
			row.setOrientation(HORIZONTAL);
			for (int c = 0; c < cols && i < n; c++) {
				Session s = sessions.get(i++);
				Cell cell = new Cell(getContext(), s);
				cells.put(s, cell);
				row.addView(cell, new LayoutParams(cellW, cellH));
				show(s);
			}
			holder.addView(row, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, cellH));
		}
		sv.addView(holder, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		addView(sv, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		setActive(active);
	}

	private List<Session> lastSessions;
	private Session lastActive;
	private boolean relayoutPending;

	/** Khung bị đổi kích thước (kéo cửa sổ nổi, xoay màn hình): xếp lại các ô theo kích thước mới. */
	@Override
	protected void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);
		if (lastSessions != null && w > 0 && h > 0 && (Math.abs(w - builtW) > 2 || Math.abs(h - builtH) > 2)
				&& !relayoutPending) {
			relayoutPending = true;
			post(() -> {
				relayoutPending = false;
				if (lastSessions != null && getWidth() > 0 && getHeight() > 0) {
					rebuild(lastSessions, lastActive, getWidth(), getHeight());
				}
			});
		}
	}

	void rebuild(List<Session> sessions, Session active, int w, int h) {
		lastSessions = sessions;
		lastActive = active;
		builtW = w;
		builtH = h;
		for (Cell c : cells.values()) {
			c.removeAllViews();
		}
		removeAllViews();
		cells.clear();
		int n = sessions.size();
		if (n == 0) {
			return;
		}
		if ((fill || fit) && w > 0 && h > 0) {
			buildFilled(sessions, active, w, h);
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

	/** Gắn (lại) màn hình hiện tại của một tab vào ô của nó. */
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
		// nền riêng: danh sách và form trong suốt, sẽ lộ màu viền xanh của ô đang chọn
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
