package ru.playsoftware.j2meloader;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Hướng dẫn từng trang (vuốt trái/phải hoặc Tiếp/Quay lại), có chấm báo trang. */
public class GuideActivity extends Activity {
	private String[] titles;
	private String[] bodies;
	private String[] emoji;
	private int page;
	private TextView icon;
	private TextView title;
	private TextView body;
	private TextView next;
	private TextView back;
	private TextView skip;
	private LinearLayout dots;
	private float downX;

	private int dp(int v) {
		return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.guide_title);
		titles = getResources().getStringArray(R.array.guide_titles);
		bodies = getResources().getStringArray(R.array.guide_bodies);
		emoji = getResources().getStringArray(R.array.guide_emoji);
		if (savedInstanceState != null) {
			page = Math.max(0, Math.min(titles.length - 1, savedInstanceState.getInt("page")));
		}

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(dp(20), dp(16), dp(20), dp(12));

		icon = new TextView(this);
		icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 52);
		icon.setGravity(Gravity.CENTER);
		GradientDrawable circle = new GradientDrawable();
		circle.setShape(GradientDrawable.OVAL);
		circle.setColor(0x223F7FD8);
		icon.setBackground(circle);
		LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(110), dp(110));
		ilp.gravity = Gravity.CENTER_HORIZONTAL;
		ilp.topMargin = dp(8);
		root.addView(icon, ilp);

		title = new TextView(this);
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		title.setGravity(Gravity.CENTER);
		title.setPadding(0, dp(16), 0, dp(10));
		root.addView(title);

		body = new TextView(this);
		body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		body.setLineSpacing(0, 1.2f);
		body.setPadding(dp(4), 0, dp(4), 0);
		GradientDrawable card = new GradientDrawable();
		card.setCornerRadius(dp(14));
		card.setColor(0x14888888);
		ScrollView sv = new ScrollView(this);
		sv.setBackground(card);
		LinearLayout inner = new LinearLayout(this);
		inner.setPadding(dp(14), dp(14), dp(14), dp(14));
		inner.addView(body);
		sv.addView(inner);
		root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		dots = new LinearLayout(this);
		dots.setGravity(Gravity.CENTER);
		dots.setPadding(0, dp(12), 0, dp(8));
		root.addView(dots);

		LinearLayout nav = new LinearLayout(this);
		nav.setGravity(Gravity.CENTER_VERTICAL);
		skip = navBtn(getString(R.string.guide_skip), 0x00000000, v -> finish());
		back = navBtn(getString(R.string.guide_back), 0x33888888, v -> go(page - 1));
		next = navBtn(getString(R.string.guide_next), 0xFF3F7FD8, v -> {
			if (page >= titles.length - 1) {
				finish();
			} else {
				go(page + 1);
			}
		});
		next.setTextColor(Color.WHITE);
		nav.addView(skip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		nav.addView(back, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
		nlp.leftMargin = dp(8);
		nav.addView(next, nlp);
		root.addView(nav);
		setContentView(root);
		show();
	}

	private TextView navBtn(String text, int color, View.OnClickListener l) {
		TextView t = new TextView(this);
		t.setText(text);
		t.setGravity(Gravity.CENTER);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		t.setPadding(dp(8), dp(12), dp(8), dp(12));
		GradientDrawable g = new GradientDrawable();
		g.setCornerRadius(dp(24));
		g.setColor(color);
		t.setBackground(g);
		t.setOnClickListener(l);
		return t;
	}

	private void go(int p) {
		if (p < 0 || p >= titles.length) {
			return;
		}
		page = p;
		show();
	}

	private void show() {
		icon.setText(emoji[page]);
		title.setText(titles[page]);
		String b = bodies[page];
		body.setText(b.contains("%1$d") ? String.format(b, titles.length) : b);
		back.setVisibility(page == 0 ? View.INVISIBLE : View.VISIBLE);
		skip.setVisibility(page == titles.length - 1 ? View.INVISIBLE : View.VISIBLE);
		next.setText(page == titles.length - 1 ? R.string.guide_done : R.string.guide_next);
		dots.removeAllViews();
		for (int i = 0; i < titles.length; i++) {
			View d = new View(this);
			GradientDrawable g = new GradientDrawable();
			g.setShape(GradientDrawable.OVAL);
			g.setColor(i == page ? 0xFF3F7FD8 : 0x55888888);
			d.setBackground(g);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(i == page ? 10 : 8), dp(i == page ? 10 : 8));
			lp.setMargins(dp(4), 0, dp(4), 0);
			dots.addView(d, lp);
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
			downX = e.getX();
		} else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
			float dx = e.getX() - downX;
			if (Math.abs(dx) > dp(80)) {
				go(dx < 0 ? page + 1 : page - 1);
			}
		}
		return super.dispatchTouchEvent(e);
	}

	@Override
	protected void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);
		out.putInt("page", page);
	}
}
