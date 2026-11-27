/*
 *  Che779 Emulator - kiểu dáng bàn phím Che779: độ trong suốt, cỡ nút, giao diện sáng/tối/trong suốt, ảnh nền.
 *  Giấy phép Apache License 2.0 (cùng giấy phép J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Đọc cấu hình kiểu dáng từ Cài đặt (tiến trình khác) và áp dụng lên bàn phím. */
public final class KeypadStyle {
	static final String FILE = "keypad_bg.png";
	private static final int MAX_SIDE = 1080;

	private KeypadStyle() {
	}

	private static SharedPreferences prefs(Context c) {
		return c.getSharedPreferences(c.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
	}

	/** Cỡ nút nhân theo tỉ lệ người dùng chọn (70..140 %). */
	static int scaledKeyDp(Context c, int keyDp) {
		int pct = Math.max(70, Math.min(140, prefs(c).getInt("pref_kp_size", 100)));
		return Math.max(20, keyDp * pct / 100);
	}

	/** Áp dụng giao diện, ảnh nền và độ trong suốt lên bàn phím đã dựng. */
	static void apply(Context c, View column, View pad) {
		SharedPreferences p = prefs(c);
		String theme = p.getString("pref_kp_theme", "light");
		int bg = 0xFFD3D3D6;
		if ("dark".equals(theme)) {
			bg = 0xFF2A2D34;
		} else if ("clear".equals(theme)) {
			bg = 0x00000000;
		}
		Drawable img = loadBackground(c);
		if (img != null) {
			column.setBackground(img);
			pad.setBackgroundColor(0x00000000);
		} else {
			column.setBackgroundColor(bg);
			pad.setBackgroundColor(bg);
		}
		int op = Math.max(30, Math.min(100, p.getInt("pref_kp_opacity", 100)));
		column.setAlpha(op / 100f);
	}

	private static Drawable loadBackground(Context c) {
		File f = new File(c.getFilesDir(), FILE);
		if (!f.isFile()) {
			return null;
		}
		try {
			Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
			return b == null ? null : new BitmapDrawable(c.getResources(), b);
		} catch (Throwable e) {
			return null;
		}
	}

	/** Đọc ảnh người dùng chọn, thu nhỏ rồi lưu trong thư mục riêng của ứng dụng. */
	public static boolean saveBackground(Context c, Uri uri) {
		try {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			try (InputStream in = c.getContentResolver().openInputStream(uri)) {
				BitmapFactory.decodeStream(in, null, o);
			}
			int s = 1;
			while (Math.max(o.outWidth, o.outHeight) / s > MAX_SIDE) {
				s *= 2;
			}
			o = new BitmapFactory.Options();
			o.inSampleSize = s;
			Bitmap b;
			try (InputStream in = c.getContentResolver().openInputStream(uri)) {
				b = BitmapFactory.decodeStream(in, null, o);
			}
			if (b == null) {
				return false;
			}
			try (FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), FILE))) {
				b.compress(Bitmap.CompressFormat.PNG, 100, out);
			}
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	public static void clearBackground(Context c) {
		new File(c.getFilesDir(), FILE).delete();
	}
}
