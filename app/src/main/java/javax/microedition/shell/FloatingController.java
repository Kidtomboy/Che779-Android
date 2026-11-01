/*
 *  Che779 Emulator - unified bubble: control bubble <-> floating game window.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 *  Concept: control bubble of J2ME_NST (NST Tien) + floating window of J2ME Loader v10 (Hung Hero).
 */
package javax.microedition.shell;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import ru.playsoftware.j2meloader.R;

/**
 * Lives in the :midlet process. Shown while the game screen is in the background so the
 * games keep running and can still be seen and operated in a small window.
 */
final class FloatingController {
	interface Callbacks {
		/** Bring the full game screen back. */
		void onDock();

		/** The "*" button (switches tabs of merged games). */
		void onStar();

		/** Previous (-1) / next (+1) tab. */
		void onCycle(int delta);

		/** Hide level changed (0 OFF, 1 SOFT, 2 STEALTH, 3 NINJA). */
		void onHideLevel(int level);
	}

	private static FloatingController instance;

	static FloatingController get(Context context) {
		if (instance == null) {
			instance = new FloatingController(context.getApplicationContext());
		}
		return instance;
	}

	static boolean canDraw(Context c) {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(c);
	}

	private static final String K_W = "float_w", K_H = "float_h", K_X = "float_x", K_Y = "float_y",
			K_BX = "bubble_x", K_BY = "bubble_y";

	private final Context ctx;
	private final WindowManager wm;
	private final SharedPreferences sp;
	private final float density;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final FrameLayout host;
	private Callbacks callbacks;
	private ImageView bubble;
	private WindowManager.LayoutParams bubbleLp;
	private FrameLayout window;
	private WindowManager.LayoutParams windowLp;
	private TextView label;
	private boolean showing;
	private boolean expanded;

	private FloatingController(Context c) {
		ctx = c;
		wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		sp = PreferenceManager.getDefaultSharedPreferences(c);
		density = c.getResources().getDisplayMetrics().density;
		host = new FrameLayout(c);
		host.setBackgroundColor(Color.BLACK);
	}

	ViewGroup host() {
		return host;
	}

	boolean isShowing() {
		return showing;
	}

	private int dp(int v) {
		return (int) (v * density + 0.5f);
	}

	private int overlayType() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
				? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
				: WindowManager.LayoutParams.TYPE_PHONE;
	}

	private WindowManager.LayoutParams params(int w, int h, int x, int y) {
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h, overlayType(),
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				PixelFormat.TRANSLUCENT);
		lp.gravity = Gravity.TOP | Gravity.START;
		lp.x = x;
		lp.y = y;
		return lp;
	}

	/** Shows the bubble (or the window, when it was open before). */
	void show(Callbacks cb) {
		callbacks = cb;
		if (showing) {
			return;
		}
		showing = true;
		if (expanded) {
			showWindow();
		} else {
			showBubble();
		}
		applyHide();
	}

	void hide() {
		if (!showing) {
			return;
		}
		showing = false;
		hideMenu();
		removeBubble();
		removeWindow();
	}

	void setLabel(String text) {
		if (label != null) {
			label.setText(text);
		}
	}

	// ---------------------------------------------------------------- bubble

	@SuppressLint("ClickableViewAccessibility")
	private void showBubble() {
		expanded = false;
		removeWindow();
		if (bubble != null) {
			return;
		}
		bubble = new ImageView(ctx);
		bubble.setImageResource(R.drawable.che779_bubble);
		bubble.setScaleType(ImageView.ScaleType.FIT_CENTER);
		int size = dp(56);
		bubbleLp = params(size, size, sp.getInt(K_BX, dp(8)), sp.getInt(K_BY, dp(200)));
		final int slop = dp(8);
		final Runnable longPress = this::showMenu;
		bubble.setOnTouchListener(new View.OnTouchListener() {
			float downX, downY;
			int startX, startY;
			boolean moved;

			@Override
			public boolean onTouch(View v, MotionEvent e) {
				switch (e.getActionMasked()) {
					case MotionEvent.ACTION_DOWN:
						downX = e.getRawX();
						downY = e.getRawY();
						startX = bubbleLp.x;
						startY = bubbleLp.y;
						moved = false;
						handler.postDelayed(longPress, 700);
						return true;
					case MotionEvent.ACTION_MOVE:
						float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
						if (!moved && Math.abs(dx) + Math.abs(dy) > slop) {
							moved = true;
							handler.removeCallbacks(longPress);
						}
						if (moved) {
							bubbleLp.x = startX + (int) dx;
							bubbleLp.y = startY + (int) dy;
							wm.updateViewLayout(bubble, bubbleLp);
						}
						return true;
					case MotionEvent.ACTION_UP:
						handler.removeCallbacks(longPress);
						if (moved) {
							sp.edit().putInt(K_BX, bubbleLp.x).putInt(K_BY, bubbleLp.y).apply();
						} else {
							showWindow();
						}
						return true;
					case MotionEvent.ACTION_CANCEL:
						handler.removeCallbacks(longPress);
						return true;
				}
				return false;
			}
		});
		try {
			wm.addView(bubble, bubbleLp);
			applyHide();
		} catch (Exception e) {
			e.printStackTrace();
			bubble = null;
		}
	}

	static final String[] HIDE_NAMES = {"OFF", "SOFT", "STEALTH", "NINJA"};

	int hideLevel() {
		return sp.getInt("hide_level", 0);
	}

	/** SOFT: out of recents. STEALTH: small and faint bubble. NINJA: almost invisible bubble. */
	void applyHide() {
		int level = hideLevel();
		if (callbacks != null) {
			callbacks.onHideLevel(level);
		}
		if (bubble == null) {
			return;
		}
		int size = level == 2 ? dp(40) : level == 3 ? dp(32) : dp(56);
		bubble.setAlpha(level == 2 ? 0.4f : level == 3 ? 0.08f : 1f);
		bubbleLp.width = size;
		bubbleLp.height = size;
		try {
			wm.updateViewLayout(bubble, bubbleLp);
		} catch (Exception ignored) {
		}
	}

	private View menu;

	private void hideMenu() {
		if (menu != null) {
			try {
				wm.removeView(menu);
			} catch (Exception ignored) {
			}
			menu = null;
		}
	}

	private void menuItem(LinearLayout col, String text, Runnable action) {
		TextView t = new TextView(ctx);
		t.setText(text);
		t.setTextColor(Color.WHITE);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		t.setPadding(dp(14), dp(10), dp(14), dp(10));
		t.setOnClickListener(v -> {
			hideMenu();
			action.run();
		});
		col.addView(t);
	}

	@SuppressLint("ClickableViewAccessibility")
	private void showMenu() {
		hideMenu();
		if (bubble == null) {
			return;
		}
		LinearLayout col = new LinearLayout(ctx);
		col.setOrientation(LinearLayout.VERTICAL);
		col.setBackgroundColor(0xF01C222B);
		menuItem(col, "Mở lại app", () -> {
			if (callbacks != null) {
				callbacks.onDock();
			}
		});
		String[] treoNames = {"Treo: Vẽ bình thường", "Treo: Vẽ 20 FPS", "Treo: Không vẽ"};
		for (int i = 0; i < 3; i++) {
			final int mode = i;
			menuItem(col, (Session.treo == i ? "✓ " : "    ") + treoNames[i], () -> Session.setTreo(mode));
		}
		menuItem(col, "Mức ẩn: " + HIDE_NAMES[hideLevel()] + " (chạm để đổi)", () -> {
			sp.edit().putInt("hide_level", (hideLevel() + 1) % 4).apply();
			applyHide();
		});
		col.setOnTouchListener((v, e) -> {
			if (e.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
				hideMenu();
			}
			return false;
		});
		WindowManager.LayoutParams lp = params(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, bubbleLp.x + bubbleLp.width, bubbleLp.y);
		lp.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
		menu = col;
		try {
			wm.addView(menu, lp);
		} catch (Exception e) {
			e.printStackTrace();
			menu = null;
		}
	}

	private void removeBubble() {
		if (bubble != null) {
			try {
				wm.removeView(bubble);
			} catch (Exception ignored) {
			}
			bubble = null;
		}
	}

	// ---------------------------------------------------------------- window

	private TextView button(String text, View.OnClickListener l) {
		TextView t = new TextView(ctx);
		t.setText(text);
		t.setGravity(Gravity.CENTER);
		t.setTextColor(Color.WHITE);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		t.setPadding(dp(8), 0, dp(8), 0);
		t.setOnClickListener(l);
		t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)));
		return t;
	}

	@SuppressLint("ClickableViewAccessibility")
	private void showWindow() {
		expanded = true;
		removeBubble();
		if (window != null) {
			return;
		}
		DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
		int w = sp.getInt(K_W, Math.min(dm.widthPixels, dm.heightPixels) * 3 / 4);
		int h = sp.getInt(K_H, w * 4 / 3);
		windowLp = params(w, h, sp.getInt(K_X, dp(16)), sp.getInt(K_Y, dp(80)));
		window = new FrameLayout(ctx);
		window.setBackgroundColor(0xFF14181F);

		LinearLayout column = new LinearLayout(ctx);
		column.setOrientation(LinearLayout.VERTICAL);
		LinearLayout bar = new LinearLayout(ctx);
		bar.setOrientation(LinearLayout.HORIZONTAL);
		bar.setGravity(Gravity.CENTER_VERTICAL);
		bar.setBackgroundColor(0xFF1C222B);
		bar.addView(button("‹", v -> cycle(-1)));
		label = new TextView(ctx);
		label.setTextColor(0xFF9AA4B2);
		label.setGravity(Gravity.CENTER);
		label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		label.setLayoutParams(new LinearLayout.LayoutParams(0, dp(32), 1f));
		bar.addView(button("›", v -> cycle(1)), 0 + 1);
		bar.addView(label);
		bar.addView(button("*", v -> {
			if (callbacks != null) {
				callbacks.onStar();
			}
		}));
		bar.addView(button("–", v -> showBubble()));
		bar.addView(button("🔍", v -> {
			if (callbacks != null) {
				callbacks.onDock();
			}
		}));
		column.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));
		if (host.getParent() instanceof ViewGroup) {
			((ViewGroup) host.getParent()).removeView(host);
		}
		column.addView(host, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		window.addView(column, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		TextView grip = new TextView(ctx);
		grip.setText("◢");
		grip.setTextColor(0xCCFFFFFF);
		grip.setGravity(Gravity.CENTER);
		window.addView(grip, new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.BOTTOM | Gravity.END));

		bar.setOnTouchListener(new View.OnTouchListener() {
			float downX, downY;
			int startX, startY;

			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
					downX = e.getRawX();
					downY = e.getRawY();
					startX = windowLp.x;
					startY = windowLp.y;
				} else if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
					windowLp.x = startX + (int) (e.getRawX() - downX);
					windowLp.y = startY + (int) (e.getRawY() - downY);
					wm.updateViewLayout(window, windowLp);
				} else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
					sp.edit().putInt(K_X, windowLp.x).putInt(K_Y, windowLp.y).apply();
				}
				return true;
			}
		});
		grip.setOnTouchListener(new View.OnTouchListener() {
			float downX, downY;
			int startW, startH;

			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
					downX = e.getRawX();
					downY = e.getRawY();
					startW = windowLp.width;
					startH = windowLp.height;
				} else if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
					DisplayMetrics m = ctx.getResources().getDisplayMetrics();
					windowLp.width = Math.max(dp(140), Math.min(m.widthPixels, startW + (int) (e.getRawX() - downX)));
					windowLp.height = Math.max(dp(140), Math.min(m.heightPixels, startH + (int) (e.getRawY() - downY)));
					wm.updateViewLayout(window, windowLp);
				} else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
					sp.edit().putInt(K_W, windowLp.width).putInt(K_H, windowLp.height).apply();
				}
				return true;
			}
		});
		try {
			wm.addView(window, windowLp);
		} catch (Exception e) {
			e.printStackTrace();
			window = null;
		}
	}

	private void cycle(int d) {
		if (callbacks != null) {
			callbacks.onCycle(d);
		}
	}

	private void removeWindow() {
		if (window != null) {
			try {
				wm.removeView(window);
			} catch (Exception ignored) {
			}
			// keep the content host alive for the next show
			if (host.getParent() instanceof ViewGroup) {
				((ViewGroup) host.getParent()).removeView(host);
			}
			window = null;
			label = null;
		}
	}
}
