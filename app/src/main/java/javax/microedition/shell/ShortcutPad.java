/*
 *  Che779 Emulator - bàn phím "Phím tắt": nút do người dùng tự thêm/sửa/xóa, mỗi nút bấm một chuỗi phím.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import javax.microedition.lcdui.Canvas;

final class ShortcutPad extends LinearLayout {
	private static final String PREF = "che779_shortcuts";
	private static final int COLS = 4;
	private static final long STEP_MS = 70;
	/** Các phím có thể đưa vào chuỗi. */
	private static final String[] TOKENS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#",
			"LÊN", "XUỐNG", "TRÁI", "PHẢI", "OK", "PHÍM MỀM TRÁI", "PHÍM MỀM PHẢI"};

	private final Context ctx;
	private final KeypadView.Sink sink;
	private final int keyDp;
	private final int textSp;
	private final Handler h = new Handler(Looper.getMainLooper());
	private final ArrayList<String[]> items = new ArrayList<>(); // {tên, chuỗi phím cách nhau bằng dấu cách}

	ShortcutPad(Context c, int keyDp, int textSp, KeypadView.Sink sink) {
		super(c);
		this.ctx = c;
		this.keyDp = keyDp;
		this.textSp = textSp;
		this.sink = sink;
		setOrientation(VERTICAL);
		int p = (int) (4 * c.getResources().getDisplayMetrics().density);
		setPadding(p, p, p, p);
		load();
		build();
	}

	private SharedPreferences prefs() {
		return ctx.getSharedPreferences(ctx.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
	}

	private void load() {
		items.clear();
		for (String line : prefs().getString(PREF, "").split("\n")) {
			int t = line.indexOf('\t');
			if (t > 0) {
				items.add(new String[]{line.substring(0, t), line.substring(t + 1)});
			}
		}
	}

	private void save() {
		StringBuilder sb = new StringBuilder();
		for (String[] i : items) {
			sb.append(i[0].replace('\t', ' ').replace('\n', ' ')).append('\t').append(i[1]).append('\n');
		}
		prefs().edit().putString(PREF, sb.toString()).apply();
		Che779Backup.saveAsync(ctx);
	}

	private int dp(int v) {
		return (int) (v * ctx.getResources().getDisplayMetrics().density + 0.5f);
	}

	private void build() {
		removeAllViews();
		int total = items.size() + 1; // + nút thêm
		LinearLayout row = null;
		for (int i = 0; i < total; i++) {
			if (i % COLS == 0) {
				row = new LinearLayout(ctx);
				addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}
			final boolean add = i == items.size();
			final int idx = i;
			TextView b = new TextView(ctx);
			b.setGravity(Gravity.CENTER);
			b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
			b.setMaxLines(2);
			b.setTextColor(add ? 0xFF1565D8 : 0xFF5B5BA8);
			b.setText(add ? "+" : items.get(i)[0]);
			GradientDrawable bg = new GradientDrawable();
			bg.setCornerRadius(dp(10));
			bg.setColor(add ? 0xFFDDE8FA : 0xFFE4E4E8);
			bg.setStroke(Math.max(1, dp(1) / 2), 0xFFC4C4CC);
			b.setBackground(bg);
			LayoutParams lp = new LayoutParams(0, dp(keyDp), 1f);
			lp.setMargins(dp(2), dp(2), dp(2), dp(2));
			if (add) {
				b.setOnClickListener(v -> edit(-1));
			} else {
				b.setOnClickListener(v -> play(items.get(idx)[1]));
				b.setOnLongClickListener(v -> {
					edit(idx);
					return true;
				});
			}
			row.addView(b, lp);
		}
		if (row != null) {
			for (int k = row.getChildCount(); k < COLS; k++) {
				View sp = new View(ctx);
				LayoutParams lp = new LayoutParams(0, dp(keyDp), 1f);
				lp.setMargins(dp(2), dp(2), dp(2), dp(2));
				row.addView(sp, lp);
			}
		}
	}

	private static int[] codes(String tok) {
		switch (tok) {
			case "*":
				return new int[]{Canvas.KEY_STAR};
			case "#":
				return new int[]{Canvas.KEY_POUND};
			case "LÊN":
				return new int[]{Canvas.KEY_UP};
			case "XUỐNG":
				return new int[]{Canvas.KEY_DOWN};
			case "TRÁI":
				return new int[]{Canvas.KEY_LEFT};
			case "PHẢI":
				return new int[]{Canvas.KEY_RIGHT};
			case "OK":
				return new int[]{Canvas.KEY_FIRE};
			case "PHÍM MỀM TRÁI":
				return new int[]{Canvas.KEY_SOFT_LEFT};
			case "PHÍM MỀM PHẢI":
				return new int[]{Canvas.KEY_SOFT_RIGHT};
			default:
				if (tok.length() == 1 && tok.charAt(0) >= '0' && tok.charAt(0) <= '9') {
					return new int[]{Canvas.KEY_NUM0 + (tok.charAt(0) - '0')};
				}
				return null;
		}
	}

	/** Bấm lần lượt từng phím trong chuỗi (nhấn rồi thả), cách nhau một nhịp ngắn. */
	private void play(String seq) {
		long t = 0;
		for (String tok : seq.split("\\|")) {
			final int[] k = codes(tok.trim());
			if (k == null) {
				continue;
			}
			h.postDelayed(() -> sink.key(k, true), t);
			t += STEP_MS;
			h.postDelayed(() -> sink.key(k, false), t);
			t += STEP_MS;
		}
	}

	/** Thêm (idx = -1) hoặc sửa/xóa một phím tắt. */
	private void edit(final int idx) {
		final String[] cur = idx >= 0 ? items.get(idx) : new String[]{"", ""};
		final ArrayList<String> seq = new ArrayList<>();
		for (String t : cur[1].split("\\|")) {
			if (!t.trim().isEmpty()) {
				seq.add(t.trim());
			}
		}
		LinearLayout box = new LinearLayout(ctx);
		box.setOrientation(VERTICAL);
		box.setPadding(dp(16), dp(8), dp(16), 0);
		final EditText name = new EditText(ctx);
		name.setHint("Tên nút (ví dụ: Chào)");
		name.setText(cur[0]);
		name.setSingleLine(true);
		box.addView(name);
		final TextView shown = new TextView(ctx);
		shown.setPadding(0, dp(8), 0, dp(4));
		box.addView(shown);
		final Runnable refresh = () -> shown.setText(seq.isEmpty() ? "Chuỗi phím: (chưa có, bấm các phím bên dưới)"
				: "Chuỗi phím: " + android.text.TextUtils.join(" → ", seq));
		refresh.run();
		LinearLayout line = null;
		for (int i = 0; i < TOKENS.length; i++) {
			if (i % 4 == 0) {
				line = new LinearLayout(ctx);
				box.addView(line);
			}
			final String tok = TOKENS[i];
			TextView t = new TextView(ctx);
			t.setText(tok);
			t.setGravity(Gravity.CENTER);
			t.setTextSize(13);
			t.setPadding(dp(4), dp(8), dp(4), dp(8));
			t.setBackgroundColor(0x33888888);
			t.setOnClickListener(v -> {
				seq.add(tok);
				refresh.run();
			});
			LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
			lp.setMargins(dp(2), dp(2), dp(2), dp(2));
			line.addView(t, lp);
		}
		TextView undo = new TextView(ctx);
		undo.setText("Xóa phím cuối");
		undo.setGravity(Gravity.CENTER);
		undo.setPadding(dp(4), dp(10), dp(4), dp(10));
		undo.setBackgroundColor(0x33888888);
		undo.setOnClickListener(v -> {
			if (!seq.isEmpty()) {
				seq.remove(seq.size() - 1);
				refresh.run();
			}
		});
		LayoutParams ulp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ulp.topMargin = dp(6);
		box.addView(undo, ulp);
		android.widget.ScrollView sv = new android.widget.ScrollView(ctx);
		sv.addView(box);
		AlertDialog.Builder b = new AlertDialog.Builder(ctx)
				.setTitle(idx >= 0 ? "Sửa phím tắt" : "Thêm phím tắt")
				.setView(sv)
				.setPositiveButton("Lưu", (d, w) -> {
					String n = name.getText().toString().trim();
					if (n.isEmpty() || seq.isEmpty()) {
						android.widget.Toast.makeText(ctx, "Cần có tên và ít nhất một phím", android.widget.Toast.LENGTH_SHORT).show();
						return;
					}
					String[] it = {n, android.text.TextUtils.join("|", seq)};
					if (idx >= 0) {
						items.set(idx, it);
					} else {
						items.add(it);
					}
					save();
					build();
				})
				.setNegativeButton(android.R.string.cancel, null);
		if (idx >= 0) {
			b.setNeutralButton("Xóa nút", (d, w) -> {
				items.remove(idx);
				save();
				build();
			});
		}
		AlertDialog d = b.create();
		DialogGate.present(d);
	}
}
