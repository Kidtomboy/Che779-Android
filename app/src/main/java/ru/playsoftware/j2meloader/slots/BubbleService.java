package ru.playsoftware.j2meloader.slots;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import ru.playsoftware.j2meloader.MainActivity;
import ru.playsoftware.j2meloader.R;

/** Bong bong noi: an/hien ung dung va chuyen nhanh giua cac tab. */
public class BubbleService extends Service {
	private static final String CHANNEL = "che779_bubble";
	private static final int NOTIF_ID = 900;

	private WindowManager wm;
	private FrameLayout root;
	private TextView bubble;
	private LinearLayout panel;
	private WindowManager.LayoutParams lp;
	private boolean panelVisible;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable refresh = new Runnable() {
		@Override
		public void run() {
			updateBadge();
			if (panelVisible) {
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
				.setContentText("Chạm bong bóng để chuyển tab hoặc ẩn ứng dụng")
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setContentIntent(pi)
				.build();
	}

	private void createBubble() {
		wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		root = new FrameLayout(this);
		LinearLayout column = new LinearLayout(this);
		column.setOrientation(LinearLayout.VERTICAL);
		root.addView(column);

		bubble = new TextView(this);
		bubble.setGravity(Gravity.CENTER);
		bubble.setTextColor(Color.WHITE);
		bubble.setTextSize(15);
		bubble.setTypeface(null, android.graphics.Typeface.BOLD);
		GradientDrawable bg = new GradientDrawable();
		bg.setShape(GradientDrawable.OVAL);
		bg.setColor(0xDD2E7D32);
		bg.setStroke(dp(2), Color.WHITE);
		bubble.setBackground(bg);
		column.addView(bubble, new LinearLayout.LayoutParams(dp(48), dp(48)));

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

		bubble.setOnTouchListener(new View.OnTouchListener() {
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
		if (bubble == null) {
			return;
		}
		int n = Slots.runningCount(this);
		bubble.setText(n > 0 ? String.valueOf(n) : "C");
	}

	private void togglePanel() {
		panelVisible = !panelVisible;
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
		t.setOnClickListener(l);
		return t;
	}

	private void rebuildPanel() {
		panel.removeAllViews();
		TextView title = new TextView(this);
		title.setText("Che779 - các tab");
		title.setTextColor(Color.WHITE);
		title.setTextSize(13);
		title.setPadding(dp(4), 0, dp(4), dp(6));
		panel.addView(title);
		final int cols = 4;
		LinearLayout row = null;
		for (int i = 0; i < Slots.COUNT; i++) {
			if (i % cols == 0) {
				row = new LinearLayout(this);
				row.setOrientation(LinearLayout.HORIZONTAL);
				panel.addView(row);
			}
			final Slots.Info info = Slots.info(this, i);
			String label = String.valueOf(i + 1);
			if (info.running && !info.title.isEmpty()) {
				String t = info.title.length() > 7 ? info.title.substring(0, 7) + "…" : info.title;
				label += "\n" + t;
			}
			final int slot = i;
			TextView b = button(label, info.running ? 0xFF2E7D32 : 0xFF555555, v -> openSlot(slot, info));
			LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(64), dp(48));
			blp.setMargins(dp(2), dp(2), dp(2), dp(2));
			row.addView(b, blp);
		}
		LinearLayout actions = new LinearLayout(this);
		actions.setOrientation(LinearLayout.HORIZONTAL);
		actions.setPadding(0, dp(6), 0, 0);
		LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		alp.setMargins(dp(2), 0, dp(2), 0);
		actions.addView(button("Ẩn", 0xFF1565C0, v -> goHome()), alp);
		actions.addView(button("Mở Che779", 0xFF6A1B9A, v -> openMain()), alp);
		actions.addView(button("Tắt", 0xFFC62828, v -> {
			Bubble.setEnabledFromService(this);
		}), alp);
		panel.addView(actions);
	}

	private void closePanel() {
		panelVisible = false;
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
