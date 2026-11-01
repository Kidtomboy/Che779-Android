/*
 *  Che779 Emulator - toolbar of the grid mode (key sync, proxy, virtual keyboard, back to list).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

final class GridBar extends LinearLayout {
	interface Listener {
		void onSyncKeys(boolean on);

		void onProxy();

		void onKeyboard();

		void onBackToList();
	}

	GridBar(Context context, boolean syncOn, Listener l) {
		super(context);
		float d = context.getResources().getDisplayMetrics().density;
		setOrientation(HORIZONTAL);
		setBackgroundColor(0xFF1C222B);
		setGravity(Gravity.CENTER_VERTICAL);
		CheckBox sync = new CheckBox(context);
		sync.setText("Đồng bộ phím");
		sync.setTextColor(Color.WHITE);
		sync.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		sync.setChecked(syncOn);
		sync.setOnCheckedChangeListener((b, on) -> l.onSyncKeys(on));
		addView(sync, new LayoutParams(0, (int) (36 * d), 1f));
		addView(button(context, "Proxy", v -> l.onProxy(), d));
		addView(button(context, "⌨", v -> l.onKeyboard(), d));
		addView(button(context, "Danh sách", v -> l.onBackToList(), d));
	}

	private TextView button(Context c, String text, OnClickListener click, float d) {
		TextView t = new TextView(c);
		t.setText(text);
		t.setGravity(Gravity.CENTER);
		t.setTextColor(Color.WHITE);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		t.setPadding((int) (10 * d), 0, (int) (10 * d), 0);
		t.setOnClickListener(click);
		t.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, (int) (36 * d)));
		return t;
	}
}
