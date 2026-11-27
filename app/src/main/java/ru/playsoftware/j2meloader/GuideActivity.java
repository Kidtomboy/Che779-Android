package ru.playsoftware.j2meloader;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

/** Trang hướng dẫn sử dụng (chữ thuần, cuộn được). */
public class GuideActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.guide_title);
		float d = getResources().getDisplayMetrics().density;
		int pad = (int) (16 * d);
		TextView t = new TextView(this);
		t.setText(R.string.guide_body);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		t.setLineSpacing(0, 1.15f);
		t.setPadding(pad, pad, pad, pad);
		ScrollView sv = new ScrollView(this);
		sv.addView(t);
		setContentView(sv);
	}
}
