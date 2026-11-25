/*
 *  Che779 Emulator - bảng điều khiển macro nổi trong ứng dụng: ghi, phát, lặp, xóa; kéo thả được.
 *  Giấy phép Apache License 2.0 (cùng giấy phép J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Cửa sổ nhỏ nổi để điều khiển macro; kéo bằng nút ⠿. */
final class MacroPanel extends LinearLayout {
	interface Host {
		/** Số tab đang chọn (để lưu/đọc macro theo tab), hoặc -1 nếu chưa có. */
		int slot();

		void close();
	}

	private static final int[] LOOPS = {1, 10, 0};

	private final MacroManager macro;
	private final Host host;
	private final TextView rec;
	private final TextView play;
	private final TextView loop;
	private final TextView status;
	private int loopIdx;
	private float downX;
	private float downY;
	private float startTx;
	private float startTy;

	MacroPanel(Context c, MacroManager macro, Host host) {
		super(c);
		this.macro = macro;
		this.host = host;
		final float dp = c.getResources().getDisplayMetrics().density;
		setOrientation(VERTICAL);
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(0xEE14181F);
		bg.setCornerRadius(10 * dp);
		bg.setStroke((int) dp, 0xFF3A4252);
		setBackground(bg);
		int p = (int) (6 * dp);
		setPadding(p, p, p, p);
		LinearLayout row = new LinearLayout(c);
		row.setOrientation(HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		TextView handle = chip(c, "⠿", dp);
		handle.setOnTouchListener(this::drag);
		row.addView(handle);
		rec = chip(c, "Ghi", dp);
		rec.setOnClickListener(v -> toggleRecord());
		row.addView(rec);
		play = chip(c, "Phát", dp);
		play.setOnClickListener(v -> togglePlay());
		row.addView(play);
		loop = chip(c, "×1", dp);
		loop.setOnClickListener(v -> {
			loopIdx = (loopIdx + 1) % LOOPS.length;
			refresh();
		});
		row.addView(loop);
		TextView clear = chip(c, "Xóa", dp);
		clear.setOnClickListener(v -> {
			int s = host.slot();
			if (s >= 0) {
				macro.clear(s);
			}
			refresh();
		});
		row.addView(clear);
		TextView close = chip(c, "✕", dp);
		close.setOnClickListener(v -> host.close());
		row.addView(close);
		addView(row);
		status = new TextView(c);
		status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		status.setTextColor(0xFF9AA4B2);
		status.setPadding(p, p / 2, p, 0);
		addView(status);
		refresh();
	}

	private static TextView chip(Context c, String text, float dp) {
		TextView t = new TextView(c);
		t.setText(text);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		t.setTextColor(0xFFFFFFFF);
		t.setGravity(Gravity.CENTER);
		t.setPadding((int) (9 * dp), (int) (6 * dp), (int) (9 * dp), (int) (6 * dp));
		GradientDrawable g = new GradientDrawable();
		g.setColor(0xFF2A3140);
		g.setCornerRadius(6 * dp);
		t.setBackground(g);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.setMargins((int) (2 * dp), 0, (int) (2 * dp), 0);
		t.setLayoutParams(lp);
		return t;
	}

	private void toggleRecord() {
		int s = host.slot();
		if (s < 0) {
			return;
		}
		if (macro.isRecording()) {
			int n = macro.stopRecording(s);
			status.setText("Đã ghi " + n + " lần bấm cho tab " + s);
			refreshButtons();
			return;
		}
		macro.startRecording();
		refresh();
	}

	private void togglePlay() {
		int s = host.slot();
		if (s < 0) {
			return;
		}
		if (macro.isPlaying()) {
			macro.stop();
		} else if (macro.has(s)) {
			macro.play(s, LOOPS[loopIdx]);
		}
		refresh();
	}

	/** Cập nhật chữ trên nút và dòng trạng thái theo trạng thái macro. */
	void refresh() {
		int s = host.slot();
		refreshButtons();
		if (macro.isRecording()) {
			status.setText("Đang ghi: hãy bấm phím trên bàn phím Che779");
		} else if (macro.isPlaying()) {
			status.setText(LOOPS[loopIdx] == 0 ? "Đang phát liên tục" : "Đang phát");
		} else if (s >= 0 && macro.has(s)) {
			status.setText("Tab " + s + ": có macro (" + macro.pressCount(s) + " lần bấm)");
		} else {
			status.setText("Tab " + (s < 0 ? "?" : s) + ": chưa có macro");
		}
	}

	private void refreshButtons() {
		rec.setText(macro.isRecording() ? "Dừng ghi" : "Ghi");
		play.setText(macro.isPlaying() ? "Dừng" : "Phát");
		int l = LOOPS[loopIdx];
		loop.setText(l == 0 ? "∞" : "×" + l);
	}

	private boolean drag(View v, MotionEvent e) {
		switch (e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				downX = e.getRawX();
				downY = e.getRawY();
				startTx = getTranslationX();
				startTy = getTranslationY();
				return true;
			case MotionEvent.ACTION_MOVE:
				setTranslationX(startTx + e.getRawX() - downX);
				setTranslationY(startTy + e.getRawY() - downY);
				return true;
			default:
				return true;
		}
	}
}
