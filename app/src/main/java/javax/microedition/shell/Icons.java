package javax.microedition.shell;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;

/** Helper to build tinted Android (Material) vector icon buttons. */
final class Icons {
	private Icons() {
	}

	static ImageView view(Context c, int res, int color, int padDp, View.OnClickListener click) {
		ImageView v = new ImageView(c);
		v.setImageResource(res);
		v.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		int p = (int) (padDp * c.getResources().getDisplayMetrics().density + 0.5f);
		v.setPadding(p, p, p, p);
		v.setScaleType(ImageView.ScaleType.FIT_CENTER);
		if (click != null) {
			v.setOnClickListener(click);
		}
		return v;
	}
}
