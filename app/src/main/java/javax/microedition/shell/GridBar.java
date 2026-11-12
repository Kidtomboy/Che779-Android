/*
 *  Che779 Emulator - toolbar of the grid mode (key sync, proxy, virtual keyboard, back to list).
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

final class GridBar extends LinearLayout {
	interface Listener {
		void onSyncDialog();

		void onProxy();

		void onKeyboard();

		void onBackToList();
	}

	private final TextView syncButton;

	void setSyncOn(boolean on) {
		syncButton.setText("Đồng bộ phím ▾");
		android.graphics.drawable.Drawable d = syncButton.getContext().getResources().getDrawable(
				on ? ru.playsoftware.j2meloader.R.drawable.ic_c_checked : ru.playsoftware.j2meloader.R.drawable.ic_c_unchecked).mutate();
		d.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
		float dd = syncButton.getResources().getDisplayMetrics().density;
		d.setBounds(0, 0, (int) (20 * dd), (int) (20 * dd));
		syncButton.setCompoundDrawables(d, null, null, null);
		syncButton.setCompoundDrawablePadding((int) (8 * dd));
	}

	GridBar(Context context, boolean syncOn, Listener l) {
		super(context);
		float d = context.getResources().getDisplayMetrics().density;
		setOrientation(HORIZONTAL);
		setBackgroundColor(0xFF1C222B);
		setGravity(Gravity.CENTER_VERTICAL);
		syncButton = button(context, "", v -> l.onSyncDialog(), d);
		syncButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
		setSyncOn(syncOn);
		addView(syncButton, new LayoutParams(0, (int) (36 * d), 1f));
		addView(button(context, "Proxy", v -> l.onProxy(), d));
		addView(Icons.view(context, ru.playsoftware.j2meloader.R.drawable.ic_c_keyboard, Color.WHITE, 8, v -> l.onKeyboard()), new LayoutParams((int) (44 * d), (int) (36 * d)));
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
