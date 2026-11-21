/*
 *  Che779 Emulator - proxy screens: single form (list mode) and per-tab table (grid / floating window).
 *  Both read and write {@link ProxyStore}, so they always show the same data.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.microemu.cldc.NetHooks;

import java.util.List;
import java.util.function.Consumer;

final class ProxyUi {
	interface Host {
		Context ctx();

		/** Shows the dialog (overlay type while floating); null if it could not be shown. */
		AlertDialog show(AlertDialog.Builder b);

		void pickFile(Consumer<String> onText);

		List<Session> tabs();

		int slotOf(Session s);

		Session active();

		SharedPreferences prefs();

		void toast(String msg);

		void redirect();
	}

	private ProxyUi() {
	}

	private static int dp(Context c, int v) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
	}

	private static TextView dim(Context c, String text) {
		TextView t = new TextView(c);
		t.setText(text);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
		t.setTextColor(Color.GRAY);
		t.setPadding(0, dp(c, 8), 0, 0);
		return t;
	}

	private static EditText field(Context c, String hint, boolean number, boolean secret) {
		EditText e = new EditText(c);
		e.setHint(hint);
		e.setSingleLine(true);
		e.setInputType(number ? InputType.TYPE_CLASS_NUMBER
				: InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		return e;
	}

	private static String label(NetConfig c) {
		return c == null ? "(không dùng)" : c.label();
	}

	private static void applyTab(Host h, Session s) {
		ProxyStore.applyTo(h.prefs(), h.slotOf(s), s);
	}

	// =============================================================== list mode: single form

	/** Form for one proxy of tab {@code s}: Host / Port / User / Pass + HTTP, SOCKS4, SOCKS5. */
	static void showForm(final Host h, final Session s, final Runnable after) {
		final Context c = h.ctx();
		final int no = h.slotOf(s);
		final NetConfig cur = ProxyStore.tabProxy(h.prefs(), no);
		final String[] editKey = {cur == null ? null : cur.serialize()};

		LinearLayout box = new LinearLayout(c);
		box.setOrientation(LinearLayout.VERTICAL);
		box.setPadding(dp(c, 20), dp(c, 8), dp(c, 20), 0);
		final EditText host = field(c, "Host", false, false);
		final EditText port = field(c, "Port", true, false);
		final EditText user = field(c, "User (có thể để trống)", false, false);
		final EditText pass = field(c, "Pass (có thể để trống)", false, true);
		box.addView(host);
		box.addView(port);
		box.addView(user);
		box.addView(pass);
		final RadioGroup grp = new RadioGroup(c);
		grp.setOrientation(RadioGroup.HORIZONTAL);
		final RadioButton rHttp = new RadioButton(c), r4 = new RadioButton(c), r5 = new RadioButton(c);
		rHttp.setText("HTTP");
		r4.setText("SOCKS4");
		r5.setText("SOCKS5");
		rHttp.setId(View.generateViewId());
		r4.setId(View.generateViewId());
		r5.setId(View.generateViewId());
		grp.addView(rHttp);
		grp.addView(r4);
		grp.addView(r5);
		rHttp.setChecked(true);
		box.addView(grp);
		final Runnable[] fill = new Runnable[1];
		final NetConfig[] src = {cur};
		fill[0] = () -> {
			NetConfig x = src[0];
			if (x == null) {
				return;
			}
			host.setText(x.host);
			port.setText(String.valueOf(x.port));
			user.setText(x.user == null ? "" : x.user);
			pass.setText(x.pass == null ? "" : x.pass);
			(x.type == NetConfig.SOCKS5 ? r5 : x.type == NetConfig.SOCKS4 ? r4 : rHttp).setChecked(true);
		};
		fill[0].run();

		final TextView result = new TextView(c);
		result.setPadding(0, dp(c, 8), 0, 0);
		box.addView(result);

		LinearLayout row = new LinearLayout(c);
		row.setOrientation(LinearLayout.HORIZONTAL);
		final TextView bTest = btn(c, "Kiểm tra");
		final TextView bPick = btn(c, "Chọn / sửa");
		final TextView bImp = btn(c, "Nhập DS");
		row.addView(bTest);
		row.addView(bPick);
		row.addView(bImp);
		box.addView(row);
		box.addView(dim(c, "Đang sửa cho tab " + no + ". \"Lưu\" thêm vào danh sách (hoặc ghi đè proxy đang sửa) "
				+ "và bật cho tab này. Áp dụng cho kết nối mới."));

		ScrollView sv = new ScrollView(c);
		sv.addView(box);

		final NetConfig[] built = new NetConfig[1];
		final java.util.function.Supplier<NetConfig> read = () -> {
			String hs = host.getText().toString().trim();
			int p;
			try {
				p = Integer.parseInt(port.getText().toString().trim());
			} catch (NumberFormatException e) {
				return null;
			}
			NetConfig n = new NetConfig();
			n.type = grp.getCheckedRadioButtonId() == r5.getId() ? NetConfig.SOCKS5
					: grp.getCheckedRadioButtonId() == r4.getId() ? NetConfig.SOCKS4 : NetConfig.HTTP;
			n.host = hs;
			n.port = p;
			String u = user.getText().toString().trim();
			n.user = u.isEmpty() ? null : u;
			n.pass = pass.getText().toString();
			if (hs.isEmpty() || hs.contains(":") || hs.contains("@") || hs.contains("/") || p < 1 || p > 65535) {
				return null;
			}
			return NetConfig.parse(n.serialize());
		};

		bTest.setOnClickListener(v -> {
			final NetConfig n = read.get();
			if (n == null) {
				result.setTextColor(Color.RED);
				result.setText("Host/Port chưa hợp lệ");
				return;
			}
			result.setTextColor(Color.GRAY);
			result.setText("Đang kiểm tra…");
			new Thread(() -> {
				String msg;
				int col;
				try {
					long ms = NetHooks.testProxy(n, "www.google.com", 443);
					msg = "Proxy hoạt động (" + ms + " ms)";
					col = 0xFF2E7D32;
				} catch (Exception e) {
					msg = "Lỗi: " + e.getMessage();
					col = Color.RED;
				}
				final String m = msg;
				final int cc = col;
				result.post(() -> {
					result.setTextColor(cc);
					result.setText(m);
				});
			}, "proxy-test").start();
		});
		bPick.setOnClickListener(v -> {
			final List<NetConfig> l = ProxyStore.list(h.prefs());
			if (l.isEmpty()) {
				h.toast("Danh sách proxy đang trống");
				return;
			}
			String[] names = new String[l.size()];
			for (int i = 0; i < names.length; i++) {
				names[i] = (i + 1) + ". " + l.get(i).label();
			}
			h.show(new DialogGate.Builder(c).setTitle("Chọn proxy để điền vào form")
					.setItems(names, (d, w) -> {
						src[0] = l.get(w);
						editKey[0] = src[0].serialize();
						fill[0].run();
					}).setNegativeButton(android.R.string.cancel, null));
		});
		bImp.setOnClickListener(v -> showImport(h, () -> h.toast("Đã cập nhật danh sách proxy")));

		AlertDialog d = h.show(new DialogGate.Builder(c)
				.setTitle("Proxy tab " + no)
				.setView(sv)
				.setPositiveButton("Lưu", null)
				.setNegativeButton("Hủy", null)
				.setNeutralButton("Tắt proxy", (dd, w) -> {
					ProxyStore.setTab(h.prefs(), no, null, false);
					applyTab(h, s);
					h.toast("Đã tắt proxy tab " + no + " (áp dụng cho kết nối mới)");
					if (after != null) {
						after.run();
					}
				}));
		if (d != null) {
			d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
				NetConfig n = read.get();
				if (n == null) {
					result.setTextColor(Color.RED);
					result.setText("Host/Port chưa hợp lệ (host không chứa dấu : hoặc @, port 1-65535)");
					return;
				}
				ProxyStore.replace(h.prefs(), editKey[0], n);
				ProxyStore.setTab(h.prefs(), no, n, true);
				applyTab(h, s);
				h.toast("Đã lưu proxy cho tab " + no + " (áp dụng cho kết nối mới)");
				d.dismiss();
				if (after != null) {
					after.run();
				}
			});
		}
	}

	private static TextView btn(Context c, String text) {
		TextView t = new TextView(c);
		t.setText(text);
		t.setGravity(Gravity.CENTER);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		t.setTextColor(0xFF1565C0);
		t.setPadding(dp(c, 6), dp(c, 12), dp(c, 6), dp(c, 12));
		t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		return t;
	}

	// =============================================================== grid / floating: table

	static void showTable(final Host h) {
		final Context c = h.ctx();
		LinearLayout root = new LinearLayout(c);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), 0);
		final LinearLayout rows = new LinearLayout(c);
		rows.setOrientation(LinearLayout.VERTICAL);
		ScrollView sv = new ScrollView(c);
		sv.addView(rows);
		sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		final Runnable[] refresh = new Runnable[1];
		refresh[0] = () -> buildRows(h, rows, refresh[0]);
		refresh[0].run();

		LinearLayout bar = new LinearLayout(c);
		bar.setOrientation(LinearLayout.HORIZONTAL);
		TextView add = btn(c, "+ Thêm");
		TextView imp = btn(c, "Nhập file/dán");
		TextView rr = btn(c, "Gán lần lượt");
		TextView red = btn(c, "Chuyển hướng IP");
		bar.addView(add);
		bar.addView(imp);
		bar.addView(rr);
		bar.addView(red);
		red.setOnClickListener(v -> h.redirect());
		add.setOnClickListener(v -> {
			Session a = h.active();
			if (a != null) {
				showForm(h, a, refresh[0]);
			}
		});
		imp.setOnClickListener(v -> showImport(h, refresh[0]));
		rr.setOnClickListener(v -> {
			List<NetConfig> l = ProxyStore.list(h.prefs());
			if (l.isEmpty()) {
				h.toast("Danh sách proxy đang trống");
				return;
			}
			List<Session> t = h.tabs();
			for (int i = 0; i < t.size(); i++) {
				ProxyStore.setTab(h.prefs(), h.slotOf(t.get(i)), l.get(i % l.size()), true);
				applyTab(h, t.get(i));
			}
			h.toast("Đã gán lần lượt cho " + t.size() + " tab (áp dụng cho kết nối mới)");
			refresh[0].run();
		});
		root.addView(sv);
		root.addView(bar);
		root.addView(dim(c, "Nhập được: host:port · host:port:user:pass · user:pass@host:port · "
				+ "giao_thức://… (http, socks4, socks5) · TXT/CSV/JSON. Bấm \"Nhập file/dán\" để xem đầy đủ."));

		h.show(new DialogGate.Builder(c)
				.setTitle("Proxy các tab")
				.setView(root)
				.setPositiveButton("Đóng", null));
	}

	private static void buildRows(final Host h, LinearLayout rows, final Runnable refresh) {
		final Context c = h.ctx();
		rows.removeAllViews();
		rows.addView(headerRow(c));
		final SharedPreferences sp = h.prefs();
		for (final Session s : h.tabs()) {
			final int no = h.slotOf(s);
			final NetConfig cur = ProxyStore.tabProxy(sp, no);
			LinearLayout r = new LinearLayout(c);
			r.setOrientation(LinearLayout.HORIZONTAL);
			r.setGravity(Gravity.CENTER_VERTICAL);
			r.setPadding(0, dp(c, 3), 0, dp(c, 3));

			TextView num = new TextView(c);
			num.setText(String.valueOf(no));
			num.setGravity(Gravity.CENTER);
			num.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 34), ViewGroup.LayoutParams.WRAP_CONTENT));
			r.addView(num);

			LinearLayout mid = new LinearLayout(c);
			mid.setOrientation(LinearLayout.HORIZONTAL);
			mid.setGravity(Gravity.CENTER_VERTICAL);
			mid.setBackgroundColor(0x14000000);
			mid.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			TextView sel = new TextView(c);
			sel.setText(label(cur) + "  ▾");
			sel.setSingleLine(true);
			sel.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
			sel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			sel.setPadding(dp(c, 6), dp(c, 8), dp(c, 6), dp(c, 8));
			sel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			sel.setOnClickListener(v -> pickForTab(h, s, refresh));
			mid.addView(sel);
			TextView edit = icon(c, "✎");
			edit.setOnClickListener(v -> showForm(h, s, refresh));
			TextView del = icon(c, "🗑");
			del.setOnClickListener(v -> {
				if (cur == null) {
					h.toast("Tab " + no + " chưa chọn proxy");
					return;
				}
				h.show(new DialogGate.Builder(c).setTitle("Xoá proxy khỏi danh sách?")
						.setMessage(cur.label() + "\nCác tab đang dùng proxy này cũng sẽ bị bỏ.")
						.setPositiveButton("Xoá", (d, w) -> {
							String key = cur.serialize();
							ProxyStore.remove(sp, key);
							for (Session t : h.tabs()) {
								NetConfig x = ProxyStore.tabProxy(sp, h.slotOf(t));
								if (x != null && x.serialize().equals(key)) {
									ProxyStore.setTab(sp, h.slotOf(t), null, false);
									applyTab(h, t);
								}
							}
							refresh.run();
						}).setNegativeButton(android.R.string.cancel, null));
			});
			mid.addView(edit);
			mid.addView(del);
			r.addView(mid);

			CheckBox on = new CheckBox(c);
			on.setChecked(cur != null && ProxyStore.tabEnabled(sp, no));
			on.setEnabled(cur != null);
			on.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 48), ViewGroup.LayoutParams.WRAP_CONTENT));
			on.setOnCheckedChangeListener((b, checked) -> {
				if (cur == null) {
					return;
				}
				ProxyStore.setTab(sp, no, cur, checked);
				applyTab(h, s);
			});
			r.addView(on);
			rows.addView(r);
		}
	}

	private static TextView icon(Context c, String g) {
		TextView t = new TextView(c);
		t.setText(g);
		t.setGravity(Gravity.CENTER);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		t.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 34), dp(c, 36)));
		return t;
	}

	private static View headerRow(Context c) {
		LinearLayout r = new LinearLayout(c);
		r.setOrientation(LinearLayout.HORIZONTAL);
		TextView a = new TextView(c), b = new TextView(c), d = new TextView(c);
		a.setText("Tab");
		b.setText("Proxy (chọn từ danh sách)");
		d.setText("Dùng");
		for (TextView t : new TextView[]{a, b, d}) {
			t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
			t.setTextColor(Color.GRAY);
		}
		a.setGravity(Gravity.CENTER);
		d.setGravity(Gravity.CENTER);
		a.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 34), ViewGroup.LayoutParams.WRAP_CONTENT));
		b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		d.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 48), ViewGroup.LayoutParams.WRAP_CONTENT));
		r.addView(a);
		r.addView(b);
		r.addView(d);
		return r;
	}

	/** Drop-down of the stored proxies for one tab (scroll to the end for "+ add"). */
	private static void pickForTab(final Host h, final Session s, final Runnable refresh) {
		final Context c = h.ctx();
		final List<NetConfig> l = ProxyStore.list(h.prefs());
		final String[] items = new String[l.size() + 2];
		items[0] = "(không dùng proxy)";
		for (int i = 0; i < l.size(); i++) {
			items[i + 1] = (i + 1) + ". " + l.get(i).label();
		}
		items[items.length - 1] = "＋ Thêm proxy mới…";
		h.show(new DialogGate.Builder(c).setTitle("Proxy cho tab " + h.slotOf(s))
				.setItems(items, (d, w) -> {
					int no = h.slotOf(s);
					if (w == 0) {
						ProxyStore.setTab(h.prefs(), no, null, false);
						applyTab(h, s);
						refresh.run();
					} else if (w == items.length - 1) {
						ProxyStore.setTab(h.prefs(), no, null, false);
						showForm(h, s, refresh);
					} else {
						ProxyStore.setTab(h.prefs(), no, l.get(w - 1), true);
						applyTab(h, s);
						h.toast("Đã đặt proxy tab " + no + " (áp dụng cho kết nối mới)");
						refresh.run();
					}
				}).setNegativeButton(android.R.string.cancel, null));
	}

	// =============================================================== import

	static void showImport(final Host h, final Runnable after) {
		final Context c = h.ctx();
		final Consumer<String> done = text -> {
			ProxyStore.Parsed p = ProxyStore.parseImport(text);
			int added = 0;
			for (NetConfig n : p.ok) {
				if (ProxyStore.add(h.prefs(), n)) {
					added++;
				}
			}
			h.toast("Thêm " + added + " proxy mới, trùng " + (p.ok.size() - added) + ", lỗi " + p.bad);
			if (after != null) {
				after.run();
			}
		};
		LinearLayout box = new LinearLayout(c);
		box.setOrientation(LinearLayout.VERTICAL);
		box.setPadding(dp(c, 20), dp(c, 8), dp(c, 20), 0);
		TextView help = dim(c, ProxyStore.FORMAT_HELP);
		box.addView(help);
		ScrollView sv = new ScrollView(c);
		sv.addView(box);
		h.show(new DialogGate.Builder(c).setTitle("Nhập danh sách proxy").setView(sv)
				.setPositiveButton("Chọn file", (d, w) -> h.pickFile(done))
				.setNeutralButton("Dán văn bản", (d, w) -> {
					final EditText in = new EditText(c);
					in.setMinLines(6);
					in.setGravity(Gravity.TOP);
					in.setHint("Mỗi dòng một proxy, hoặc dán nội dung JSON/CSV");
					h.show(new DialogGate.Builder(c).setTitle("Dán danh sách").setView(in)
							.setPositiveButton("Nhập", (d2, w2) -> done.accept(in.getText().toString()))
							.setNegativeButton("Hủy", null));
				})
				.setNegativeButton("Hủy", null));
	}
}
