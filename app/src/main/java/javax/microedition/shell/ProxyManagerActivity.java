/*
 *  Che779 Emulator - quản lý proxy trong Thiết lập: lọc theo loại, nhập file/dán, kiểm tra hàng loạt, xóa hàng loạt.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Proxy không giới hạn số lượng; chia theo HTTP/SOCKS4/SOCKS5; kiểm tra và xóa hàng loạt. */
public class ProxyManagerActivity extends Activity {
	private static final int REQ_FILE = 7801;
	private static final int THREADS = 20;
	private static final int TIMEOUT_MS = 6000;
	private static final String[] FILTERS = {"Tất cả", "HTTP", "SOCKS4", "SOCKS5"};
	private static final String[] MODES = {"Tự động", "HTTP", "SOCKS4", "SOCKS5"};

	private static final class Result {
		int ms; // >=0 sống; -1 chết
	}

	private final Handler ui = new Handler(Looper.getMainLooper());
	private SharedPreferences sp;
	private final ArrayList<NetConfig> all = new ArrayList<>();
	private final ArrayList<NetConfig> shown = new ArrayList<>();
	private final HashSet<String> sel = new HashSet<>();
	private final HashMap<String, Result> res = new HashMap<>();
	private int filter; // 0 tất cả, 1 http, 2 socks4, 3 socks5
	private int mode; // kiểu kiểm tra: 0 tự động ...
	private BaseAdapter adapter;
	private TextView status;
	private TextView[] chips;
	private TextView modeBtn;
	private EditText importText;
	private volatile boolean testing;
	private volatile ExecutorService pool;
	private boolean refreshQueued;

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setTitle("Quản lý proxy");
		sp = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
		buildUi();
		reload();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopTest();
	}

	private int dp(int v) {
		return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
	}

	private TextView button(String text, View.OnClickListener l) {
		TextView t = new TextView(this);
		t.setText(text);
		t.setGravity(Gravity.CENTER);
		t.setPadding(dp(12), dp(8), dp(12), dp(8));
		t.setBackgroundColor(0x33888888);
		t.setSingleLine(true);
		t.setOnClickListener(l);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.setMargins(0, 0, dp(6), dp(6));
		t.setLayoutParams(lp);
		return t;
	}

	private HorizontalScrollView row() {
		HorizontalScrollView sv = new HorizontalScrollView(this);
		sv.setHorizontalScrollBarEnabled(false);
		LinearLayout l = new LinearLayout(this);
		l.setOrientation(LinearLayout.HORIZONTAL);
		sv.addView(l);
		return sv;
	}

	private LinearLayout inner(HorizontalScrollView sv) {
		return (LinearLayout) sv.getChildAt(0);
	}

	private void buildUi() {
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(dp(10), dp(10), dp(10), dp(6));

		HorizontalScrollView r1 = row();
		chips = new TextView[FILTERS.length];
		for (int i = 0; i < FILTERS.length; i++) {
			final int f = i;
			chips[i] = button(FILTERS[i], v -> {
				filter = f;
				applyFilter();
			});
			inner(r1).addView(chips[i]);
		}
		root.addView(r1);

		HorizontalScrollView r2 = row();
		inner(r2).addView(button("Nhập", v -> showImport()));
		inner(r2).addView(button("Kiểm tra", v -> startTest()));
		inner(r2).addView(button("Dừng", v -> stopTest()));
		modeBtn = button("Kiểu: " + MODES[0], v -> {
			mode = (mode + 1) % MODES.length;
			modeBtn.setText("Kiểu: " + MODES[mode]);
		});
		inner(r2).addView(modeBtn);
		root.addView(r2);

		HorizontalScrollView r3 = row();
		inner(r3).addView(button("Chọn tất cả", v -> {
			for (NetConfig c : shown) {
				sel.add(c.serialize());
			}
			refresh();
		}));
		inner(r3).addView(button("Bỏ chọn", v -> {
			sel.clear();
			refresh();
		}));
		inner(r3).addView(button("Xóa đã chọn", v -> deleteSelected()));
		inner(r3).addView(button("Xóa proxy chết", v -> deleteDead()));
		inner(r3).addView(button("Xóa tất cả (đang lọc)", v -> deleteShown()));
		root.addView(r3);

		status = new TextView(this);
		status.setPadding(0, dp(4), 0, dp(4));
		root.addView(status);

		ListView lv = new ListView(this);
		adapter = new BaseAdapter() {
			@Override
			public int getCount() {
				return shown.size();
			}

			@Override
			public Object getItem(int i) {
				return shown.get(i);
			}

			@Override
			public long getItemId(int i) {
				return i;
			}

			@Override
			public View getView(int i, View convert, ViewGroup parent) {
				LinearLayout l;
				CheckBox cb;
				TextView label;
				TextView st;
				if (convert == null) {
					l = new LinearLayout(ProxyManagerActivity.this);
					l.setGravity(Gravity.CENTER_VERTICAL);
					l.setPadding(0, dp(4), 0, dp(4));
					cb = new CheckBox(ProxyManagerActivity.this);
					cb.setFocusable(false);
					cb.setClickable(false);
					label = new TextView(ProxyManagerActivity.this);
					label.setSingleLine(true);
					label.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
					st = new TextView(ProxyManagerActivity.this);
					st.setPadding(dp(8), 0, 0, 0);
					l.addView(cb);
					l.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
					l.addView(st);
				} else {
					l = (LinearLayout) convert;
					cb = (CheckBox) l.getChildAt(0);
					label = (TextView) l.getChildAt(1);
					st = (TextView) l.getChildAt(2);
				}
				NetConfig c = shown.get(i);
				String k = c.serialize();
				cb.setChecked(sel.contains(k));
				label.setText(c.label());
				Result r = res.get(k);
				if (r == null) {
					st.setText("");
				} else if (r.ms >= 0) {
					st.setText("✓ " + r.ms + " ms");
					st.setTextColor(0xFF2E9E4F);
				} else {
					st.setText("✗ chết");
					st.setTextColor(0xFFD04040);
				}
				return l;
			}
		};
		lv.setAdapter(adapter);
		lv.setOnItemClickListener((p, v, pos, id) -> {
			String k = shown.get(pos).serialize();
			if (!sel.remove(k)) {
				sel.add(k);
			}
			refresh();
		});
		root.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		setContentView(root);
	}

	private void reload() {
		new Thread(() -> {
			final List<NetConfig> l = ProxyStore.list(sp);
			ui.post(() -> {
				all.clear();
				all.addAll(l);
				applyFilter();
			});
		}, "che779-proxy-load").start();
	}

	private void applyFilter() {
		shown.clear();
		int[] cnt = new int[4];
		for (NetConfig c : all) {
			int idx = c.type == NetConfig.HTTP ? 1 : c.type == NetConfig.SOCKS4 ? 2 : 3;
			cnt[0]++;
			cnt[idx]++;
			if (filter == 0 || filter == idx) {
				shown.add(c);
			}
		}
		for (int i = 0; i < chips.length; i++) {
			chips[i].setText(FILTERS[i] + " (" + cnt[i] + ")");
			chips[i].setBackgroundColor(i == filter ? 0x663F7FD8 : 0x33888888);
		}
		refresh();
	}

	private void refresh() {
		int alive = 0;
		int dead = 0;
		for (NetConfig c : all) {
			Result r = res.get(c.serialize());
			if (r != null) {
				if (r.ms >= 0) {
					alive++;
				} else {
					dead++;
				}
			}
		}
		status.setText("Hiện " + shown.size() + " / " + all.size() + " · chọn " + sel.size()
				+ " · sống " + alive + " · chết " + dead);
		adapter.notifyDataSetChanged();
	}

	private void refreshSoon() {
		if (refreshQueued) {
			return;
		}
		refreshQueued = true;
		ui.postDelayed(() -> {
			refreshQueued = false;
			refresh();
		}, 300);
	}

	// ---- kiểm tra hàng loạt

	private void startTest() {
		if (testing) {
			Toast.makeText(this, "Đang kiểm tra", Toast.LENGTH_SHORT).show();
			return;
		}
		final ArrayList<NetConfig> targets = new ArrayList<>();
		for (NetConfig c : shown) {
			if (sel.isEmpty() || sel.contains(c.serialize())) {
				targets.add(c);
			}
		}
		if (targets.isEmpty()) {
			Toast.makeText(this, "Không có proxy để kiểm tra", Toast.LENGTH_SHORT).show();
			return;
		}
		testing = true;
		final int total = targets.size();
		final AtomicInteger done = new AtomicInteger();
		final int testMode = mode;
		final ExecutorService ex = Executors.newFixedThreadPool(THREADS);
		pool = ex;
		Toast.makeText(this, "Kiểm tra " + total + " proxy…", Toast.LENGTH_SHORT).show();
		for (final NetConfig c : targets) {
			ex.execute(() -> {
				if (!testing) {
					return;
				}
				final int[] types = typesToTry(c, testMode);
				int ms = -1;
				int okType = c.type;
				for (int t : types) {
					if (!testing) {
						return;
					}
					ms = ProxyTester.test(c, t, TIMEOUT_MS);
					if (ms >= 0) {
						okType = t;
						break;
					}
				}
				final int fms = ms;
				final int ft = okType;
				ui.post(() -> {
					String old = c.serialize();
					if (fms >= 0 && testMode == 0 && ft != c.type) {
						c.type = ft; // tự nhận ra loại đúng
						String nk = c.serialize();
						if (sel.remove(old)) {
							sel.add(nk);
						}
						res.remove(old);
					}
					Result r = new Result();
					r.ms = fms;
					res.put(c.serialize(), r);
					if (done.incrementAndGet() >= total) {
						finishTest(total);
					} else {
						refreshSoon();
					}
				});
			});
		}
	}

	private static int[] typesToTry(NetConfig c, int testMode) {
		if (testMode == 1) {
			return new int[]{NetConfig.HTTP};
		} else if (testMode == 2) {
			return new int[]{NetConfig.SOCKS4};
		} else if (testMode == 3) {
			return new int[]{NetConfig.SOCKS5};
		}
		int[] order = {NetConfig.HTTP, NetConfig.SOCKS5, NetConfig.SOCKS4};
		int[] out = new int[3];
		out[0] = c.type;
		int n = 1;
		for (int t : order) {
			if (t != c.type) {
				out[n++] = t;
			}
		}
		return out;
	}

	private void finishTest(int total) {
		testing = false;
		ExecutorService ex = pool;
		if (ex != null) {
			ex.shutdown();
		}
		saveAll();
		applyFilter();
		Toast.makeText(this, "Kiểm tra xong " + total + " proxy", Toast.LENGTH_SHORT).show();
	}

	private void stopTest() {
		testing = false;
		ExecutorService ex = pool;
		if (ex != null) {
			ex.shutdownNow();
			pool = null;
		}
		if (adapter != null) {
			saveAll();
			applyFilter();
		}
	}

	/** Ghi lại cả danh sách (bỏ trùng sau khi loại proxy được nhận ra khác loại). */
	private void saveAll() {
		HashSet<String> seen = new HashSet<>();
		ArrayList<NetConfig> keep = new ArrayList<>();
		for (NetConfig c : all) {
			if (seen.add(c.serialize())) {
				keep.add(c);
			}
		}
		all.clear();
		all.addAll(keep);
		ProxyStore.saveList(sp, all);
	}

	// ---- xóa hàng loạt

	private void deleteSelected() {
		confirmDelete("đã chọn", k -> sel.contains(k));
	}

	private void deleteDead() {
		confirmDelete("chết (theo kết quả kiểm tra)", k -> {
			Result r = res.get(k);
			return r != null && r.ms < 0;
		});
	}

	private void deleteShown() {
		final Set<String> keys = new HashSet<>();
		for (NetConfig c : shown) {
			keys.add(c.serialize());
		}
		confirmDelete("đang hiện theo bộ lọc", keys::contains);
	}

	private void confirmDelete(final String what, final java.util.function.Predicate<String> match) {
		int n = 0;
		for (NetConfig c : all) {
			if (match.test(c.serialize())) {
				n++;
			}
		}
		if (n == 0) {
			Toast.makeText(this, "Không có proxy " + what, Toast.LENGTH_SHORT).show();
			return;
		}
		final int count = n;
		new AlertDialog.Builder(this)
				.setTitle("Xóa " + count + " proxy?")
				.setMessage("Xóa các proxy " + what + ". Không hoàn tác được.")
				.setPositiveButton("Xóa", (d, w) -> {
					ArrayList<NetConfig> keep = new ArrayList<>();
					for (NetConfig c : all) {
						String k = c.serialize();
						if (match.test(k)) {
							sel.remove(k);
							res.remove(k);
						} else {
							keep.add(c);
						}
					}
					all.clear();
					all.addAll(keep);
					ProxyStore.saveList(sp, all);
					applyFilter();
					Toast.makeText(this, "Đã xóa " + count + " proxy", Toast.LENGTH_SHORT).show();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	// ---- nhập

	private void showImport() {
		LinearLayout box = new LinearLayout(this);
		box.setOrientation(LinearLayout.VERTICAL);
		box.setPadding(dp(16), dp(8), dp(16), 0);
		TextView hint = new TextView(this);
		hint.setText(ProxyStore.FORMAT_HELP);
		hint.setTextSize(12);
		importText = new EditText(this);
		importText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		importText.setMinLines(5);
		importText.setGravity(Gravity.TOP);
		importText.setHint("Dán danh sách proxy ở đây, hoặc bấm \"Chọn file\"");
		TextView lbl = new TextView(this);
		lbl.setText("Loại cho mọi dòng nhập (Tự động = giữ loại trong dòng, kiểm tra sau):");
		final Spinner type = new Spinner(this);
		type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MODES));
		box.addView(importText);
		box.addView(lbl);
		box.addView(type);
		box.addView(hint);
		android.widget.ScrollView sv = new android.widget.ScrollView(this);
		sv.addView(box);
		new AlertDialog.Builder(this)
				.setTitle("Nhập proxy")
				.setView(sv)
				.setPositiveButton("Nhập", (d, w) -> doImport(importText.getText().toString(), type.getSelectedItemPosition()))
				.setNeutralButton("Chọn file", (d, w) -> {
					try {
						Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
						i.addCategory(Intent.CATEGORY_OPENABLE);
						i.setType("*/*");
						startActivityForResult(i, REQ_FILE);
						pendingType = type.getSelectedItemPosition();
					} catch (Exception e) {
						Toast.makeText(this, "Không mở được trình chọn file, hãy dán văn bản", Toast.LENGTH_LONG).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private int pendingType;

	@Override
	protected void onActivityResult(int req, int result, Intent data) {
		super.onActivityResult(req, result, data);
		if (req != REQ_FILE || result != RESULT_OK || data == null || data.getData() == null) {
			return;
		}
		final Uri uri = data.getData();
		final int t = pendingType;
		new Thread(() -> {
			String text = null;
			try (InputStream in = getContentResolver().openInputStream(uri)) {
				ByteArrayOutputStream bo = new ByteArrayOutputStream();
				byte[] buf = new byte[65536];
				int n;
				while (in != null && (n = in.read(buf)) > 0) {
					bo.write(buf, 0, n);
				}
				text = bo.toString("UTF-8");
			} catch (Exception ignored) {
			}
			final String fin = text;
			ui.post(() -> {
				if (fin == null) {
					Toast.makeText(this, "Không đọc được file", Toast.LENGTH_LONG).show();
				} else {
					doImport(fin, t);
				}
			});
		}, "che779-proxy-file").start();
	}

	private void doImport(final String text, final int typeMode) {
		Toast.makeText(this, "Đang nhập…", Toast.LENGTH_SHORT).show();
		new Thread(() -> {
			ProxyStore.Parsed p = ProxyStore.parseImport(text);
			if (typeMode > 0) {
				int t = typeMode == 1 ? NetConfig.HTTP : typeMode == 2 ? NetConfig.SOCKS4 : NetConfig.SOCKS5;
				for (NetConfig c : p.ok) {
					c.type = t;
				}
			}
			final int added = ProxyStore.addAll(sp, p.ok);
			final int dup = p.ok.size() - added;
			final int bad = p.bad;
			final List<NetConfig> l = ProxyStore.list(sp);
			ui.post(() -> {
				all.clear();
				all.addAll(l);
				applyFilter();
				Toast.makeText(this, "Thêm " + added + ", trùng " + dup + ", sai định dạng " + bad, Toast.LENGTH_LONG).show();
			});
		}, "che779-proxy-import").start();
	}
}
