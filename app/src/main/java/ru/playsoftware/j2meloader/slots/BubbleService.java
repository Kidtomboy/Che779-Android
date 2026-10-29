package ru.playsoftware.j2meloader.slots;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import ru.playsoftware.j2meloader.MainActivity;
import ru.playsoftware.j2meloader.R;

/** Bong bong noi: an/hien ung dung, chuyen tab, che do Treo. */
public class BubbleService extends Service {
	private static final String CHANNEL = "che779_bubble";
	private static final int NOTIF_ID = 900;
	private static final int[] TIMER_HOURS = {0, 1, 3, 6, 12};

	private WindowManager wm;
	private FrameLayout root;
	private FrameLayout bubbleBox;
	private TextView badge;
	private LinearLayout panel;
	private WindowManager.LayoutParams lp;
	private boolean panelVisible;
	private int selectedSlot = -1;
	private volatile long lastTouch;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable refresh = new Runnable() {
		@Override
		public void run() {
			updateBadge();
			if (panelVisible && SystemClock.uptimeMillis() - lastTouch > 1500) {
				rebuildPanel();
			}
			handler.postDelayed(this, 2000);
		}
	};

	private int dp(int v) {
		return Math.round(v * getResources().getDisplayMetrics().density);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startForeground(NOTIF_ID, buildNotification());
		if (!Bubble.canDraw(this)) {
			stopSelf();
			return START_NOT_STICKY;
		}
		if (root == null) {
			createBubble();
		}
		return START_STICKY;
	}

	private Notification buildNotification() {
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			if (nm.getNotificationChannel(CHANNEL) == null) {
				nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Che779 - bong bóng",
						NotificationManager.IMPORTANCE_MIN));
			}
		}
		Intent open = new Intent(this, MainActivity.class);
		open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pi = PendingIntent.getActivity(this, 0, open,
				Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
		return new NotificationCompat.Builder(this, CHANNEL)
				.setSmallIcon(R.drawable.ic_che779_notif)
				.setContentTitle("Che779 - bong bóng đang bật")
				.setContentText("Chạm bong bóng để chuyển tab, treo hoặc ẩn ứng dụng")
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setContentIntent(pi)
				.build();
	}

	private void createBubble() {
		wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		root = new FrameLayout(this) {
			@Override
			public boolean dispatchTouchEvent(MotionEvent ev) {
				lastTouch = SystemClock.uptimeMillis();
				return super.dispatchTouchEvent(ev);
			}
		};
		LinearLayout column = new LinearLayout(this);
		column.setOrientation(LinearLayout.VERTICAL);
		root.addView(column);

		// Bong bong = icon Che779 (cat tron) + huy hieu so tab dang chay
		bubbleBox = new FrameLayout(this);
		GradientDrawable ring = new GradientDrawable();
		ring.setShape(GradientDrawable.OVAL);
		ring.setColor(Color.WHITE);
		bubbleBox.setBackground(ring);
		bubbleBox.setPadding(dp(2), dp(2), dp(2), dp(2));
		ImageView icon = new ImageView(this);
		icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.che779_bubble);
		if (bmp != null) {
			RoundedBitmapDrawable rd = RoundedBitmapDrawableFactory.create(getResources(), bmp);
			rd.setCircular(true);
			icon.setImageDrawable(rd);
		}
		bubbleBox.addView(icon, new FrameLayout.LayoutParams(dp(48), dp(48)));
		badge = new TextView(this);
		badge.setTextColor(Color.WHITE);
		badge.setTextSize(11);
		badge.setGravity(Gravity.CENTER);
		badge.setTypeface(null, android.graphics.Typeface.BOLD);
		GradientDrawable bb = new GradientDrawable();
		bb.setShape(GradientDrawable.OVAL);
		bb.setColor(0xFFD32F2F);
		bb.setStroke(dp(1), Color.WHITE);
		badge.setBackground(bb);
		FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(dp(20), dp(20));
		blp.gravity = Gravity.TOP | Gravity.END;
		bubbleBox.addView(badge, blp);
		column.addView(bubbleBox, new LinearLayout.LayoutParams(dp(52), dp(52)));

		panel = new LinearLayout(this);
		panel.setOrientation(LinearLayout.VERTICAL);
		GradientDrawable pbg = new GradientDrawable();
		pbg.setColor(0xF0222222);
		pbg.setCornerRadius(dp(12));
		panel.setBackground(pbg);
		panel.setPadding(dp(8), dp(8), dp(8), dp(8));
		panel.setVisibility(View.GONE);
		LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		plp.topMargin = dp(6);
		column.addView(panel, plp);

		int type = Build.VERSION.SDK_INT >= 26
				? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
				: WindowManager.LayoutParams.TYPE_PHONE;
		lp = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
				type,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.TRANSLUCENT);
		lp.gravity = Gravity.TOP | Gravity.START;
		lp.x = dp(8);
		lp.y = dp(160);

		bubbleBox.setOnTouchListener(new View.OnTouchListener() {
			private int startX;
			private int startY;
			private float touchX;
			private float touchY;
			private boolean moved;

			@Override
			public boolean onTouch(View v, MotionEvent e) {
				switch (e.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startX = lp.x;
						startY = lp.y;
						touchX = e.getRawX();
						touchY = e.getRawY();
						moved = false;
						return true;
					case MotionEvent.ACTION_MOVE:
						float dx = e.getRawX() - touchX;
						float dy = e.getRawY() - touchY;
						if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) {
							moved = true;
						}
						if (moved) {
							lp.x = startX + (int) dx;
							lp.y = startY + (int) dy;
							wm.updateViewLayout(root, lp);
						}
						return true;
					case MotionEvent.ACTION_UP:
						if (!moved) {
							togglePanel();
						}
						return true;
					default:
						return false;
				}
			}
		});
		try {
			wm.addView(root, lp);
		} catch (Exception e) {
			e.printStackTrace();
			stopSelf();
			return;
		}
		updateBadge();
		handler.postDelayed(refresh, 2000);
	}

	private void updateBadge() {
		if (badge == null) {
			return;
		}
		int n = Slots.runningCount(this);
		badge.setText(String.valueOf(n));
		badge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
	}

	private void togglePanel() {
		panelVisible = !panelVisible;
		selectedSlot = -1;
		if (panelVisible) {
			rebuildPanel();
			panel.setVisibility(View.VISIBLE);
		} else {
			panel.setVisibility(View.GONE);
		}
		wm.updateViewLayout(root, lp);
	}

	private TextView button(String text, int color, View.OnClickListener l) {
		TextView t = new TextView(this);
		t.setText(text);
		t.setTextColor(Color.WHITE);
		t.setTextSize(12);
		t.setGravity(Gravity.CENTER);
		t.setPadding(dp(6), dp(6), dp(6), dp(6));
		GradientDrawable g = new GradientDrawable();
		g.setColor(color);
		g.setCornerRadius(dp(8));
		t.setBackground(g);
		if (l != null) {
			t.setOnClickListener(l);
		}
		return t;
	}

	private LinearLayout row(int topPad) {
		LinearLayout r = new LinearLayout(this);
		r.setOrientation(LinearLayout.HORIZONTAL);
		r.setPadding(0, dp(topPad), 0, 0);
		return r;
	}

	private LinearLayout.LayoutParams weight() {
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		p.setMargins(dp(2), 0, dp(2), 0);
		return p;
	}

	private TextView label(String s) {
		TextView t = new TextView(this);
		t.setText(s);
		t.setTextColor(0xFFBBBBBB);
		t.setTextSize(11);
		t.setPadding(dp(4), dp(6), dp(4), dp(2));
		return t;
	}

	private void rebuildPanel() {
		panel.removeAllViews();
		if (selectedSlot >= 0) {
			buildTabPanel(selectedSlot);
			return;
		}
		TextView title = new TextView(this);
		title.setText("Che779 - các tab (giữ để ngủ/đóng)");
		title.setTextColor(Color.WHITE);
		title.setTextSize(13);
		title.setPadding(dp(4), 0, dp(4), dp(6));
		panel.addView(title);

		final int cols = 4;
		LinearLayout rowView = null;
		for (int i = 0; i < Slots.COUNT; i++) {
			if (i % cols == 0) {
				rowView = row(0);
				panel.addView(rowView);
			}
			final Slots.Info info = Slots.info(this, i);
			boolean sleeping = info.running && Treo.getSlot(this, i) == Treo.OFF;
			String label = String.valueOf(i + 1);
			if (info.running) {
				String t = info.title.length() > 7 ? info.title.substring(0, 7) + "…" : info.title;
				label += sleeping ? " zZ" : "";
				if (!t.isEmpty()) {
					label += "\n" + t;
				}
			}
			final int slot = i;
			int color = !info.running ? 0xFF555555 : sleeping ? 0xFF37474F : 0xFF2E7D32;
			TextView b = button(label, color, v -> openSlot(slot, info));
			b.setOnLongClickListener(v -> {
				if (info.running) {
					selectedSlot = slot;
					rebuildPanel();
				}
				return true;
			});
			LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(64), dp(48));
			blp.setMargins(dp(2), dp(2), dp(2), dp(2));
			rowView.addView(b, blp);
		}

		// Che do Treo (ap dung cho tat ca tab)
		panel.addView(label("Treo (tất cả tab): game vẫn chạy, bớt vẽ để tiết kiệm pin"));
		int g = Treo.getGlobal(this);
		LinearLayout treo = row(0);
		treo.addView(button("Vẽ", g == Treo.DRAW ? 0xFF2E7D32 : 0xFF555555,
				v -> setTreo(Treo.DRAW)), weight());
		treo.addView(button("20 FPS", g == Treo.ECO ? 0xFF2E7D32 : 0xFF555555,
				v -> setTreo(Treo.ECO)), weight());
		treo.addView(button("Không vẽ", g == Treo.OFF ? 0xFF2E7D32 : 0xFF555555,
				v -> setTreo(Treo.OFF)), weight());
		panel.addView(treo);

		// Hen gio, don RAM, pin
		LinearLayout tools = row(6);
		long at = Treo.autoStopAt(this);
		String timer = "Hẹn giờ tắt: Tắt";
		if (at > System.currentTimeMillis()) {
			long min = (at - System.currentTimeMillis()) / 60000;
			timer = "Tắt sau " + (min / 60) + "g" + (min % 60) + "p";
		}
		tools.addView(button(timer, at > System.currentTimeMillis() ? 0xFFEF6C00 : 0xFF555555,
				v -> cycleTimer()), weight());
		tools.addView(button("Dọn RAM", 0xFF555555, v -> {
			Treo.cleanRam(this);
			rebuildPanel();
		}), weight());
		panel.addView(tools);
		if (!ignoringBattery()) {
			LinearLayout pin = row(4);
			pin.addView(button("⚠ Tắt tối ưu pin để treo ổn định (chạm)", 0xFFC62828, v -> requestBattery()),
					weight());
			panel.addView(pin);
		}

		LinearLayout actions = row(6);
		actions.addView(button("Ẩn", 0xFF1565C0, v -> goHome()), weight());
		actions.addView(button("Mở Che779", 0xFF6A1B9A, v -> openMain()), weight());
		actions.addView(button("Tắt", 0xFFC62828, v -> Bubble.setEnabledFromService(this)), weight());
		panel.addView(actions);
	}

	private void buildTabPanel(final int slot) {
		final Slots.Info info = Slots.info(this, slot);
		TextView title = new TextView(this);
		title.setText("Tab " + (slot + 1) + (info.title.isEmpty() ? "" : " - " + info.title));
		title.setTextColor(Color.WHITE);
		title.setTextSize(13);
		title.setPadding(dp(4), 0, dp(4), dp(6));
		panel.addView(title);
		boolean sleeping = Treo.getSlot(this, slot) == Treo.OFF;
		LinearLayout r1 = row(0);
		r1.addView(button("Chuyển tới", 0xFF2E7D32, v -> openSlot(slot, info)), weight());
		r1.addView(button(sleeping ? "Đánh thức" : "Cho ngủ (không vẽ)", 0xFF37474F, v -> {
			Treo.setSlot(this, slot, sleeping ? -1 : Treo.OFF);
			selectedSlot = -1;
			rebuildPanel();
		}), weight());
		panel.addView(r1);
		LinearLayout r2 = row(6);
		r2.addView(button("Đóng tab", 0xFFC62828, v -> {
			Slots.killSlot(this, slot);
			Treo.setSlot(this, slot, -1);
			selectedSlot = -1;
			rebuildPanel();
			updateBadge();
		}), weight());
		r2.addView(button("Quay lại", 0xFF555555, v -> {
			selectedSlot = -1;
			rebuildPanel();
		}), weight());
		panel.addView(r2);
	}

	private void setTreo(int mode) {
		Treo.setGlobal(this, mode);
		rebuildPanel();
	}

	private void cycleTimer() {
		long at = Treo.autoStopAt(this);
		int cur = 0;
		if (at > System.currentTimeMillis()) {
			long hoursLeft = Math.round((at - System.currentTimeMillis()) / 3600000.0);
			for (int h : TIMER_HOURS) {
				if (h >= hoursLeft && h > 0) {
					cur = h;
					break;
				}
			}
			if (cur == 0) {
				cur = TIMER_HOURS[TIMER_HOURS.length - 1];
			}
		}
		int idx = 0;
		for (int i = 0; i < TIMER_HOURS.length; i++) {
			if (TIMER_HOURS[i] == cur) {
				idx = i;
			}
		}
		int next = TIMER_HOURS[(idx + 1) % TIMER_HOURS.length];
		Treo.setAutoStopHours(this, next);
		rebuildPanel();
	}

	private boolean ignoringBattery() {
		if (Build.VERSION.SDK_INT < 23) {
			return true;
		}
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		return pm.isIgnoringBatteryOptimizations(getPackageName());
	}

	private void requestBattery() {
		closePanel();
		try {
			Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
					Uri.parse("package:" + getPackageName()));
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		} catch (Exception e) {
			Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}
	}

	private void closePanel() {
		panelVisible = false;
		selectedSlot = -1;
		panel.setVisibility(View.GONE);
		wm.updateViewLayout(root, lp);
	}

	private void openSlot(int slot, Slots.Info info) {
		closePanel();
		if (info.running) {
			boolean ok = false;
			try {
				ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
				am.moveTaskToFront(info.taskId, ActivityManager.MOVE_TASK_WITH_HOME);
				ok = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!ok) {
				Intent i = new Intent(this, Slots.activityClass(slot));
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(i);
			}
		} else {
			Slots.setTarget(slot);
			openMain();
		}
	}

	private void openMain() {
		closePanel();
		Intent i = new Intent(this, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(i);
	}

	private void goHome() {
		closePanel();
		Intent home = new Intent(Intent.ACTION_MAIN);
		home.addCategory(Intent.CATEGORY_HOME);
		home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(home);
	}

	@Override
	public void onDestroy() {
		handler.removeCallbacks(refresh);
		if (root != null && wm != null) {
			try {
				wm.removeView(root);
			} catch (Exception ignored) {
			}
		}
		root = null;
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
