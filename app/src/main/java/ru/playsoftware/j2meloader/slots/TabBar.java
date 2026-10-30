/*
 * Che779 Emulator - thanh tab hien thi tren cung man hinh game.
 * Y tuong giao dien: J2ME NST (thanh tab so, -, +). Viet lai boi Che779.
 */
package ru.playsoftware.j2meloader.slots;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.playsoftware.j2meloader.R;

public class TabBar extends LinearLayout {
	public interface Host {
		void onTab(int slot);

		void onTabLong(int slot);

		void onClose();

		void onAdd();

		void onAddLong();

		void onKeyboard();

		void onShot();

		void onPad(View anchor);

		void onMenu(View anchor);

		/** Chieu cao hien tai cua thanh (px) da thay doi. */
		void onHeight(int px);
	}

	private static final int BG = 0xFF1B2733;
	private static final int SEL = 0xFF1565C0;
	private static final int REFRESH_MS = 1500;

	private final Host host;
	private final float dp;
	private final LinearLayout full;
	private final TextView mini;
	private final HorizontalScrollView scroll;
	private final LinearLayout strip;
	private final ImageButton btnKeyboard;
	private final ImageButton btnPad;
	private boolean collapsed;
	private String signature = "";
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable refresh = new Runnable() {
		@Override
		public void run() {
			update();
			handler.postDelayed(this, REFRESH_MS);
		}
	};

	public TabBar(Context context, boolean collapsed, Host host) {
		super(context);
		this.host = host;
		this.collapsed = collapsed;
		dp = context.getResources().getDisplayMetrics().density;
		setOrientation(VERTICAL);
		setBackgroundColor(BG);

		full = new LinearLayout(context);
		full.setOrientation(HORIZONTAL);
		full.setGravity(Gravity.CENTER_VERTICAL);
		addView(full, new LayoutParams(LayoutParams.MATCH_PARENT, px(40)));

		TextView up = text("▴", 16);
		up.setOnClickListener(v -> setCollapsed(true));
		full.addView(up, new LayoutParams(px(28), LayoutParams.MATCH_PARENT));

		scroll = new HorizontalScrollView(context);
		scroll.setHorizontalScrollBarEnabled(false);
		scroll.setOverScrollMode(OVER_SCROLL_NEVER);
		strip = new LinearLayout(context);
		strip.setOrientation(HORIZONTAL);
		scroll.addView(strip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		LayoutParams sp = new LayoutParams(0, LayoutParams.MATCH_PARENT);
		sp.weight = 1;
		full.addView(scroll, sp);

		btnPad = icon(R.drawable.ic_che779_gamepad);
		btnPad.setOnClickListener(host::onPad);
		full.addView(btnPad, new LayoutParams(px(38), LayoutParams.MATCH_PARENT));
		btnKeyboard = icon(R.drawable.ic_action_keyboard);
		btnKeyboard.setOnClickListener(v -> host.onKeyboard());
		full.addView(btnKeyboard, new LayoutParams(px(38), LayoutParams.MATCH_PARENT));
		ImageButton shot = icon(R.drawable.ic_action_screenshot);
		shot.setOnClickListener(v -> host.onShot());
		full.addView(shot, new LayoutParams(px(38), LayoutParams.MATCH_PARENT));
		TextView more = text("⋮", 20);
		more.setOnClickListener(host::onMenu);
		full.addView(more, new LayoutParams(px(32), LayoutParams.MATCH_PARENT));

		mini = text("", 12);
		mini.setOnClickListener(v -> setCollapsed(false));
		addView(mini, new LayoutParams(LayoutParams.MATCH_PARENT, px(18)));

		applyCollapsed();
	}

	private int px(int dpv) {
		return Math.round(dpv * dp);
	}

	private TextView text(String s, int sp) {
		TextView t = new TextView(getContext());
		t.setText(s);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
		t.setTextColor(Color.WHITE);
		t.setGravity(Gravity.CENTER);
		t.setTypeface(Typeface.DEFAULT_BOLD);
		return t;
	}

	private ImageButton icon(int res) {
		ImageButton b = new ImageButton(getContext());
		b.setImageResource(res);
		b.setBackgroundColor(Color.TRANSPARENT);
		b.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
		b.setPadding(px(7), px(7), px(7), px(7));
		return b;
	}

	public void setPadVisible(boolean v) {
		btnPad.setVisibility(v ? VISIBLE : GONE);
	}

	public void setKeyboardVisible(boolean v) {
		btnKeyboard.setVisibility(v ? VISIBLE : GONE);
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public int heightPx() {
		return px(collapsed ? 18 : 40);
	}

	public void setCollapsed(boolean c) {
		collapsed = c;
		applyCollapsed();
		host.onHeight(heightPx());
	}

	private void applyCollapsed() {
		full.setVisibility(collapsed ? GONE : VISIBLE);
		mini.setVisibility(collapsed ? VISIBLE : GONE);
		signature = "";
		update();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		handler.removeCallbacks(refresh);
		handler.post(refresh);
	}

	@Override
	protected void onDetachedFromWindow() {
		handler.removeCallbacks(refresh);
		super.onDetachedFromWindow();
	}

	public void refreshNow() {
		update();
	}

	private void update() {
		Context c = getContext();
		int cur = Slots.current();
		StringBuilder sig = new StringBuilder().append(cur).append(collapsed).append('|');
		int count = 0;
		boolean[] run = new boolean[Slots.COUNT];
		String[] titles = new String[Slots.COUNT];
		for (int i = 0; i < Slots.COUNT; i++) {
			Slots.Info in = Slots.info(c, i);
			run[i] = in.running || i == cur;
			titles[i] = in.title;
			if (run[i]) {
				count++;
				sig.append(i).append(',');
			}
		}
		if (collapsed) {
			String s = "▾  Tab " + (cur + 1) + "  ·  " + count + " tab đang chạy";
			if (!s.contentEquals(mini.getText())) {
				mini.setText(s);
			}
			return;
		}
		String sg = sig.toString();
		if (sg.equals(signature)) {
			return;
		}
		signature = sg;
		strip.removeAllViews();
		View curView = null;
		for (int i = 0; i < Slots.COUNT; i++) {
			if (!run[i]) {
				continue;
			}
			final int slot = i;
			TextView t = text(String.valueOf(i + 1), 15);
			if (i == cur) {
				t.setBackgroundColor(SEL);
				curView = t;
			} else {
				t.setAlpha(0.8f);
			}
			t.setOnClickListener(v -> {
				if (slot != Slots.current()) {
					host.onTab(slot);
				}
			});
			t.setOnLongClickListener(v -> {
				host.onTabLong(slot);
				return true;
			});
			strip.addView(t, new LayoutParams(px(42), LayoutParams.MATCH_PARENT));
		}
		TextView minus = text("−", 20);
		minus.setOnClickListener(v -> host.onClose());
		strip.addView(minus, new LayoutParams(px(40), LayoutParams.MATCH_PARENT));
		TextView plus = text("+", 20);
		plus.setOnClickListener(v -> host.onAdd());
		plus.setOnLongClickListener(v -> {
			host.onAddLong();
			return true;
		});
		strip.addView(plus, new LayoutParams(px(40), LayoutParams.MATCH_PARENT));
		final View target = curView;
		if (target != null) {
			scroll.post(() -> scroll.scrollTo(Math.max(0, target.getLeft() - px(80)), 0));
		}
	}
}
