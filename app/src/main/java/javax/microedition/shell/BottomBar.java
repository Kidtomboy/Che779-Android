/*
 *  Che779 Emulator - bottom action bar: every function is an icon with an equal-width cell, so icons
 *  never overlap. Bubble, close tab, new tab, list/grid, game picker, Nokia / Android keypad, key sync
 *  and proxy (grid mode only), and the three-dot menu.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import ru.playsoftware.j2meloader.R;

final class BottomBar extends LinearLayout {
	static final int HEIGHT_DP = 40;

	interface Listener {
		void onBubble();

		void onClose();

		void onClone();

		void onPickGame();

		void onGrid();

		void onNokia();

		void onAndroid();

		void onSync();

		void onProxy();

		void onMenu();
	}

	private final ImageView grid;
	private final ImageView sync;
	private final ImageView proxy;
	private final ImageView nokia;
	private final ImageView androidKb;

	BottomBar(Context c, Listener l) {
		super(c);
		setOrientation(HORIZONTAL);
		setBackgroundColor(0xFF14181F);
		setGravity(Gravity.CENTER_VERTICAL);
		ImageView bubble = Icons.view(c, R.drawable.che779_bubble, 0, 6, v -> l.onBubble());
		bubble.clearColorFilter();
		add(bubble, "Bong bóng");
		ImageView close = Icons.view(c, R.drawable.ic_c_remove, Color.WHITE, 9, v -> l.onClose());
		add(close, "Đóng tab");
		ImageView plus = Icons.view(c, R.drawable.ic_c_add, Color.WHITE, 9, v -> l.onClone());
		plus.setOnLongClickListener(v -> {
			l.onPickGame();
			return true;
		});
		add(plus, "Tab mới (giữ để chọn game)");
		grid = Icons.view(c, R.drawable.ic_c_grid, Color.WHITE, 9, v -> l.onGrid());
		add(grid, "Chuyển danh sách / lưới");
		ImageView pad = Icons.view(c, R.drawable.ic_che779_gamepad, 0, 8, v -> l.onPickGame());
		pad.clearColorFilter();
		add(pad, "Chọn game");
		nokia = Icons.view(c, R.drawable.ic_c_dialpad, Color.WHITE, 9, v -> l.onNokia());
		add(nokia, "Bàn phím Nokia");
		androidKb = Icons.view(c, R.drawable.ic_c_keyboard, Color.WHITE, 9, v -> l.onAndroid());
		add(androidKb, "Bàn phím Android");
		sync = Icons.view(c, R.drawable.ic_c_unchecked, Color.WHITE, 9, v -> l.onSync());
		add(sync, "Đồng bộ phím");
		proxy = Icons.view(c, R.drawable.ic_c_swap, Color.WHITE, 9, v -> l.onProxy());
		add(proxy, "Proxy");
		ImageView more = Icons.view(c, R.drawable.ic_c_more, Color.WHITE, 9, v -> l.onMenu());
		add(more, "Menu");
		setGridMode(false, false);
	}

	private void add(ImageView v, String description) {
		v.setContentDescription(description);
		float d = getResources().getDisplayMetrics().density;
		addView(v, new LayoutParams(0, (int) (HEIGHT_DP * d), 1f));
	}

	/** Grid mode shows the grid-only icons (key sync, proxy) and a list icon to go back. */
	void setGridMode(boolean grid, boolean syncOn) {
		this.grid.setImageResource(grid ? R.drawable.ic_c_list : R.drawable.ic_c_grid);
		this.grid.setContentDescription(grid ? "Về danh sách" : "Chế độ lưới");
		sync.setVisibility(grid ? View.VISIBLE : View.GONE);
		proxy.setVisibility(grid ? View.VISIBLE : View.GONE);
		setSyncOn(syncOn);
	}

	void setSyncOn(boolean on) {
		sync.setImageResource(on ? R.drawable.ic_c_checked : R.drawable.ic_c_unchecked);
	}

	/** Highlights the keypad kind that is open (or none). */
	void setKeypad(int type, boolean open) {
		nokia.setColorFilter(open && type == KeypadFactory.NOKIA ? 0xFF4C8DFF : Color.WHITE);
		androidKb.setColorFilter(open && type == KeypadFactory.ANDROID ? 0xFF4C8DFF : Color.WHITE);
	}

	static int heightPx(Context c) {
		return (int) (HEIGHT_DP * c.getResources().getDisplayMetrics().density + 0.5f);
	}
}
