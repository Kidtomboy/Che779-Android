/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2018 Nikita Shakarun
 * Copyright 2019-2022 Yury Kharchenko
 * Copyright 2022-2024 Arman Jussupgaliyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.shell;

import static ru.playsoftware.j2meloader.util.Constants.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.ErrorReporter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.ViewHandler;
import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.location.LocationProviderImpl;
import javax.microedition.util.ContextHolder;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import ru.playsoftware.j2meloader.BuildConfig;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.databinding.ActivityMicroBinding;
import ru.playsoftware.j2meloader.util.Constants;
import ru.playsoftware.j2meloader.util.LogUtils;

public class MicroActivity extends AppCompatActivity {
	private static final int ORIENTATION_DEFAULT = 0;
	private static final int ORIENTATION_AUTO = 1;
	private static final int ORIENTATION_PORTRAIT = 2;
	private static final int ORIENTATION_LANDSCAPE = 3;

	private boolean visible;
	private boolean actionBarEnabled;
	private boolean statusBarEnabled;
	private InputMethodManager inputMethodManager;
	/** Tabs in creation order (tab numbers shown in the bar). */
	private final java.util.ArrayList<Session> tabs = new java.util.ArrayList<>();
	private TabBar tabBar;
	private boolean gridMode;
	private GridPanel gridPanel;
	private GridBar gridBar;
	private boolean gridVkOn = true;
	private boolean floating;

	public ActivityMicroBinding binding;

	private Displayable cur() {
		Session s = Session.current();
		return s == null ? null : s.current;
	}

	private MicroLoader loader() {
		return Session.active().microLoader;
	}

	private String name() {
		Session s = Session.active();
		return s == null || s.appName == null ? "" : s.appName;
	}

	private int menuKey() {
		Session s = Session.active();
		return s == null ? KeyEvent.KEYCODE_BACK : s.menuKey;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		lockNightMode();
		super.onCreate(savedInstanceState);
		ContextHolder.setCurrentActivity(this);

		binding = ActivityMicroBinding.inflate(getLayoutInflater());
		View view = binding.getRoot();
		setContentView(view);
		setSupportActionBar(binding.toolbar);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		actionBarEnabled = sp.getBoolean(PREF_TOOLBAR, false);
		statusBarEnabled = sp.getBoolean(PREF_STATUSBAR, false);
		if (sp.getBoolean(PREF_ADD_CUTOUT_AREA, true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			getWindow().getAttributes().layoutInDisplayCutoutMode =
					WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}
		if (sp.getBoolean(PREF_KEEP_SCREEN, false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		ContextHolder.setVibration(sp.getBoolean(PREF_VIBRATION, true));
		Canvas.setScreenshotRawMode(sp.getBoolean(PREF_SCREENSHOT_SWITCH, false));
		setupTabBar();
		if (sp.getBoolean("pref_bubble_enabled", true) && FloatingController.canDraw(this)) {
			try {
				Intent svc = new Intent(this, BubbleService.class);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					startForegroundService(svc);
				} else {
					startService(svc);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		handleLaunchIntent(getIntent(), false);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		if (binding != null && intent.getData() != null) {
			handleLaunchIntent(intent, true);
		}
	}

	private void setupTabBar() {
		tabBar = new TabBar(this, new TabBar.Listener() {
			@Override
			public void onSelect(Session s) {
				if (gridMode) {
					focusGrid(s);
				} else {
					activate(s);
				}
			}

			@Override
			public void onClose() {
				confirmCloseActive();
			}

			@Override
			public void onClone() {
				cloneFirstTab();
			}

			@Override
			public void onPickGame() {
				pickGameForNewTab();
			}

			@Override
			public void onGrid() {
				if (gridMode) {
					exitGrid();
				} else {
					promptGrid();
				}
			}
		});
		binding.virtualDisplay.addView(tabBar, 1);
	}

	/** Starts a game described by the intent: first launch, or a new tab when already running. */
	private void handleLaunchIntent(Intent intent, boolean additional) {
		String appName;
		String appPath;
		if (BuildConfig.FULL_EMULATOR) {
			appName = intent.getStringExtra(KEY_MIDLET_NAME);
			Uri data = intent.getData();
			if (data == null) {
				showErrorDialog("Invalid intent: app path is null");
				return;
			}
			appPath = data.toString();
		} else {
			appName = getTitle().toString();
			appPath = getApplicationInfo().dataDir + "/files/converted/midlet";
			File dir = new File(appPath);
			if (!dir.exists() && !dir.mkdirs()) {
				throw new RuntimeException("Can't access file system");
			}
		}
		String arguments = intent.getStringExtra(KEY_START_ARGUMENTS);
		if (arguments != null) {
			MidletSystem.setProperty("com.nokia.mid.cmdline", arguments);
			String[] arr = arguments.split(";");
			for (String s : arr) {
				if (s.length() == 0) {
					continue;
				}
				if (s.contains("=")) {
					int i = s.indexOf('=');
					MidletSystem.setProperty(s.substring(0, i), s.substring(i + 1));
				} else {
					MidletSystem.setProperty(s, "");
				}
			}
		}
		MidletSystem.setProperty("com.nokia.mid.cmdline.instance", "1");
		addSession(appName, appPath, arguments);
	}

	/** Number of already open tabs running the same game (decides the data folder suffix). */
	private String nextDataSuffix(String appPath) {
		int same = 0;
		for (Session t : tabs) {
			if (appPath.equals(t.appPath)) {
				same++;
			}
		}
		return same == 0 ? "" : "-t" + (same + 1);
	}

	private void addSession(String appName, String appPath, String arguments) {
		Session session = Session.create(appPath, nextDataSuffix(appPath));
		session.appName = appName;
		Session prev = Session.active();
		if (prev != null && prev != session) {
			detachSession(prev);
			if (prev.current != null && !gridMode) {
				prev.current.clearDisplayableView();
			}
		}
		Session.setActive(session);
		MicroLoader microLoader = new MicroLoader(this, appPath);
		session.microLoader = microLoader;
		if (!microLoader.init()) {
			session.remove();
			if (prev != null) {
				Session.setActive(prev);
			}
			Config.startApp(this, appName, appPath, true, arguments);
			if (tabs.isEmpty()) {
				finish();
			}
			return;
		}
		tabs.add(session);
		microLoader.applyConfiguration();
		VirtualKeyboard vk = ContextHolder.getVk();
		int orientation = microLoader.getOrientation();
		if (vk != null) {
			vk.setView(binding.overlayView);
			binding.overlayView.addLayer(vk);
			if (vk.isPhone()) {
				orientation = ORIENTATION_PORTRAIT;
			}
		}
		setOrientation(orientation);
		session.menuKey = microLoader.getMenuKeyCode();
		inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		refreshTabBar();
		if (gridMode) {
			rebuildGrid();
		}
		try {
			loadMIDlet();
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog(e.toString());
		}
	}

	/** Removes the visible parts of a tab that is going to the background. */
	private void detachSession(Session s) {
		VirtualKeyboard vk = s.vk;
		if (vk != null) {
			binding.overlayView.removeLayer(vk);
		}
	}

	private void activate(Session next) {
		Session prev = Session.active();
		if (next == null || prev == next || !tabs.contains(next)) {
			return;
		}
		if (prev != null) {
			detachSession(prev);
		}
		Session.setActive(next);
		if (next.microLoader != null) {
			next.microLoader.applyDisplayConfiguration();
		}
		VirtualKeyboard vk = next.vk;
		int orientation = next.microLoader != null ? next.microLoader.getOrientation() : ORIENTATION_DEFAULT;
		if (vk != null) {
			vk.setView(binding.overlayView);
			binding.overlayView.addLayer(vk);
			if (vk.isPhone()) {
				orientation = ORIENTATION_PORTRAIT;
			}
		}
		setOrientation(orientation);
		ViewHandler.postEvent(new SetCurrentEvent(prev != null ? prev.current : null, next.current));
		refreshTabBar();
		MidletThread.resumeApp();
	}

	/** Height of the tab bar; the overlay (virtual keyboard) must be shifted by it as well. */
	private int tabBarHeight() {
		if (tabBar == null) {
			return 0;
		}
		int rows = gridMode ? 2 : 1;
		return (int) (36 * rows * getResources().getDisplayMetrics().density + 0.5f);
	}

	private void refreshTabBar() {
		if (tabBar != null) {
			tabBar.update(tabs, Session.active());
		}
	}

	private void cloneFirstTab() {
		if (tabs.isEmpty()) {
			return;
		}
		Session first = tabs.get(0);
		addSession(first.appName, first.appPath, null);
	}

	private void pickGameForNewTab() {
		final java.util.ArrayList<String> names = new java.util.ArrayList<>();
		final java.util.ArrayList<String> paths = new java.util.ArrayList<>();
		File appsDir = new File(Config.getAppDir());
		String[] list = appsDir.list();
		if (list != null) {
			java.util.Arrays.sort(list);
			for (String folder : list) {
				File dir = new File(appsDir, folder);
				if (!new File(dir, Config.MIDLET_DEX_FILE).isFile()) {
					continue;
				}
				String title = folder;
				try {
					title = new ru.woesss.j2me.jar.Descriptor(
							new File(dir, Config.MIDLET_MANIFEST_FILE), false).getName();
				} catch (Exception ignored) {
				}
				names.add(title);
				paths.add(dir.getAbsolutePath());
			}
		}
		new AlertDialog.Builder(this)
				.setTitle("Chọn game mở ở tab mới")
				.setItems(names.toArray(new String[0]), (d, which) ->
						addSession(names.get(which), paths.get(which), null))
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void confirmCloseActive() {
		final Session s = Session.active();
		if (s == null) {
			return;
		}
		final int no = tabs.indexOf(s) + 1;
		new AlertDialog.Builder(this)
				.setTitle(R.string.CONFIRMATION_REQUIRED)
				.setMessage("Đóng tab " + no + "?")
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					if (tabs.size() <= 1) {
						MidletThread.destroyApp();
					} else {
						closeSession(s);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	/** Asks the game of a tab to exit; the tab disappears when it does. */
	private void closeSession(Session s) {
		MidletThread t = s.thread;
		if (t != null) {
			t.requestDestroy();
		} else {
			removeSession(s);
		}
	}

	/** Called (from any thread) when a tab's game has ended. */
	public void removeSession(final Session s) {
		runOnUiThread(() -> {
			if (!tabs.contains(s)) {
				return;
			}
			int idx = tabs.indexOf(s);
			boolean wasActive = Session.active() == s;
			Session neighbour = null;
			if (tabs.size() > 1) {
				neighbour = tabs.get(idx > 0 ? idx - 1 : 1);
			}
			if (wasActive) {
				detachSession(s);
				Displayable old = s.current;
				if (old != null) {
					old.clearDisplayableView();
				}
				Session.setActive(null);
			}
			tabs.remove(s);
			s.remove();
			if (s.queue != null) {
				s.queue.stopProcessing();
			}
			if (s.thread != null) {
				s.thread.quitSession();
			}
			if (gridMode) {
				if (!wasActive && s.current != null) {
					s.current.clearDisplayableView();
				}
				if (wasActive && neighbour != null) {
					Session.setActive(neighbour);
					attachVk(neighbour);
				}
				rebuildGrid();
				refreshTabBar();
				return;
			}
			if (wasActive && neighbour != null) {
				Session.setActive(neighbour);
				neighbour.microLoader.applyDisplayConfiguration();
				VirtualKeyboard vk = neighbour.vk;
				if (vk != null) {
					vk.setView(binding.overlayView);
					binding.overlayView.addLayer(vk);
				}
				host().removeAllViews();
				ViewHandler.postEvent(new SetCurrentEvent(null, neighbour.current));
				MidletThread.resumeApp();
			}
			refreshTabBar();
		});
	}

	private void attachVk(Session s) {
		VirtualKeyboard vk = s.vk;
		if (vk != null && gridVkOn) {
			vk.setView(binding.overlayView);
			binding.overlayView.addLayer(vk);
		}
		binding.overlayView.setVisibility(true);
	}

	private void promptGrid() {
		if (tabs.isEmpty()) {
			return;
		}
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		input.setText(String.valueOf(Math.max(2, tabs.size())));
		input.setSelectAllOnFocus(true);
		new AlertDialog.Builder(this)
				.setTitle("Chế độ lưới: nhập số ô (N)")
				.setMessage("Thiếu ô nào sẽ tự mở thêm bản sao game của tab 1. Mỗi ô tốn thêm RAM.")
				.setView(input)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					int n = tabs.size();
					try {
						n = Integer.parseInt(input.getText().toString().trim());
					} catch (NumberFormatException ignored) {
					}
					startGrid(Math.max(1, Math.min(n, 36)));
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void startGrid(int n) {
		Session keep = Session.active();
		while (tabs.size() < n) {
			int before = tabs.size();
			cloneFirstTab();
			if (tabs.size() == before) {
				break;
			}
		}
		if (keep != null && keep != Session.active() && tabs.contains(keep)) {
			detachSession(Session.active());
			Session.setActive(keep);
		}
		enterGrid();
	}

	private void enterGrid() {
		gridMode = true;
		Session.grid = true;
		host().removeAllViews();
		gridPanel = new GridPanel(this, this::focusGrid);
		gridBar = new GridBar(this, Session.syncKeys, new GridBar.Listener() {
			@Override
			public void onSyncKeys(boolean on) {
				Session.syncKeys = on;
			}

			@Override
			public void onProxy() {
				Toast.makeText(MicroActivity.this,
						"Proxy riêng cho từng ô sẽ có ở bản sau", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onKeyboard() {
				gridVkOn = !gridVkOn;
				Session a = Session.active();
				if (a != null && a.vk != null) {
					if (gridVkOn) {
						attachVk(a);
					} else {
						binding.overlayView.removeLayer(a.vk);
					}
				}
			}

			@Override
			public void onBackToList() {
				exitGrid();
			}
		});
		binding.virtualDisplay.addView(gridBar, 2);
		host().addView(gridPanel, new android.widget.FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) binding.toolbar.getLayoutParams();
		lp.height = 0;
		binding.toolbar.setLayoutParams(lp);
		Objects.requireNonNull(getSupportActionBar()).hide();
		hideSystemUI();
		binding.overlayView.setLocation(0, tabBarHeight());
		Session a = Session.active();
		if (a != null) {
			attachVk(a);
		}
		refreshTabBar();
		rebuildGrid();
		MidletThread.resumeApp();
	}

	private void rebuildGrid() {
		host().post(() -> {
			if (!gridMode || gridPanel == null) {
				return;
			}
			ViewGroup h = host();
			gridPanel.rebuild(tabs, Session.active(), h.getWidth(), h.getHeight());
		});
	}

	private void focusGrid(Session s) {
		Session prev = Session.active();
		if (s == null || prev == s || !tabs.contains(s)) {
			return;
		}
		if (prev != null) {
			detachSession(prev);
		}
		Session.setActive(s);
		if (s.microLoader != null) {
			s.microLoader.applyDisplayConfiguration();
		}
		attachVk(s);
		if (gridPanel != null) {
			gridPanel.setActive(s);
		}
		Displayable d = s.current;
		if (d != null) {
			d.getDisplayableView().requestFocus();
		}
		refreshTabBar();
	}

	private void exitGrid() {
		if (!gridMode) {
			return;
		}
		gridMode = false;
		Session.grid = false;
		Session act = Session.active();
		if (gridPanel != null) {
			gridPanel.detachAll();
		}
		host().removeAllViews();
		if (gridBar != null) {
			binding.virtualDisplay.removeView(gridBar);
		}
		gridPanel = null;
		gridBar = null;
		for (Session o : tabs) {
			if (o != act) {
				if (o.current != null) {
					o.current.clearDisplayableView();
				}
				detachSession(o);
			}
		}
		if (act != null) {
			attachVk(act);
			ViewHandler.postEvent(new SetCurrentEvent(null, act.current));
		}
		refreshTabBar();
		MidletThread.resumeApp();
	}

	public void lockNightMode() {
		int current = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if (current == Configuration.UI_MODE_NIGHT_YES) {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
		} else {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		}
	}

	private boolean bubbleAllowed() {
		return !isFinishing() && !tabs.isEmpty()
				&& PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_bubble_enabled", true)
				&& FloatingController.canDraw(this);
	}

	private ViewGroup host() {
		return floating ? FloatingController.get(this).host() : binding.displayableContainer;
	}

	private void updateFloatLabel() {
		if (floating) {
			Session a = Session.active();
			FloatingController.get(this).setLabel(
					a == null ? "" : (tabs.indexOf(a) + 1) + "/" + tabs.size());
		}
	}

	/** Activity left the screen: games keep running in the bubble. */
	private void startFloating() {
		if (floating || !bubbleAllowed() || binding == null) {
			return;
		}
		floating = true;
		FloatingController fc = FloatingController.get(this);
		android.view.View content = binding.displayableContainer.getChildCount() > 0
				? binding.displayableContainer.getChildAt(0) : null;
		binding.displayableContainer.removeAllViews();
		if (content != null) {
			fc.host().removeAllViews();
			fc.host().addView(content, new android.widget.FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
		fc.show(new FloatingController.Callbacks() {
			@Override
			public void onDock() {
				Intent i = new Intent(getApplicationContext(), MicroActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(i);
			}

			@Override
			public void onStar() {
				Session a = Session.active();
				if (a != null && a.current instanceof Canvas) {
					final Canvas c = (Canvas) a.current;
					c.postKeyPressed(Canvas.KEY_STAR);
					ViewHandler.postDelayed(() -> c.postKeyReleased(Canvas.KEY_STAR), 80);
				}
			}

			@Override
			public void onCycle(int delta) {
				if (tabs.size() < 2) {
					return;
				}
				int idx = tabs.indexOf(Session.active());
				Session next = tabs.get((idx + delta + tabs.size()) % tabs.size());
				if (gridMode) {
					focusGrid(next);
				} else {
					activate(next);
				}
				updateFloatLabel();
			}
		});
		updateFloatLabel();
	}

	/** Back on screen: put the game view back and hide the bubble. */
	private void stopFloating() {
		if (!floating) {
			return;
		}
		FloatingController fc = FloatingController.get(this);
		android.view.View content = fc.host().getChildCount() > 0 ? fc.host().getChildAt(0) : null;
		fc.host().removeAllViews();
		fc.hide();
		floating = false;
		binding.displayableContainer.removeAllViews();
		if (content != null) {
			binding.displayableContainer.addView(content, new android.widget.FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
		if (gridMode) {
			rebuildGrid();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		startFloating();
	}

	@Override
	public void onResume() {
		super.onResume();
		stopFloating();
		visible = true;
		MidletThread.resumeApp();
	}

	@Override
	public void onPause() {
		visible = false;
		hideSoftInput();
		if (!bubbleAllowed()) {
			MidletThread.pauseApp();
		}
		super.onPause();
	}

	private void hideSoftInput() {
		if (inputMethodManager != null) {
			IBinder windowToken = binding.displayableContainer.getWindowToken();
			inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
				cur() instanceof Canvas) {
			hideSystemUI();
		}
	}

	@SuppressLint("SourceLockedOrientationActivity")
	private void setOrientation(int orientation) {
		switch (orientation) {
			case ORIENTATION_AUTO:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
				break;
			case ORIENTATION_PORTRAIT:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
				break;
			case ORIENTATION_LANDSCAPE:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
				break;
			case ORIENTATION_DEFAULT:
			default:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				break;
		}
	}

	private void loadMIDlet() throws Exception {
		LinkedHashMap<String, String> midlets = loader().loadMIDletList();
		int size = midlets.size();
		String[] midletsNameArray = midlets.values().toArray(new String[0]);
		String[] midletsClassArray = midlets.keySet().toArray(new String[0]);
		if (size == 0) {
			throw new Exception("No MIDlets found");
		} else if (size == 1) {
			MidletThread.create(loader(), midletsClassArray[0]);
		} else {
			showMidletDialog(midletsNameArray, midletsClassArray);
		}
	}

	private void showMidletDialog(String[] names, final String[] classes) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.select_dialog_title)
				.setItems(names, (d, n) -> {
					String clazz = classes[n];
					ErrorReporter errorReporter = ACRA.getErrorReporter();
					String report = errorReporter.getCustomData(Constants.KEY_APPCENTER_ATTACHMENT);
					StringBuilder sb = new StringBuilder();
					if (report != null) {
						sb.append(report).append("\n");
					}
					sb.append("Begin app: ").append(names[n]).append(", ").append(clazz);
					errorReporter.putCustomData(Constants.KEY_APPCENTER_ATTACHMENT, sb.toString());
					MidletThread.create(loader(), clazz);
					MidletThread.resumeApp();
				})
				.setOnCancelListener(d -> {
					d.dismiss();
					MidletThread.notifyDestroyed();
				});
		builder.show();
	}

	void showErrorDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.error)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok, (d, w) -> MidletThread.notifyDestroyed());
		builder.setOnCancelListener(dialogInterface -> MidletThread.notifyDestroyed());
		builder.show();
	}

	private int getToolBarHeight() {
		int[] attrs = new int[]{androidx.appcompat.R.attr.actionBarSize};
		TypedArray ta = obtainStyledAttributes(attrs);
		int toolBarHeight = ta.getDimensionPixelSize(0, -1);
		ta.recycle();
		return toolBarHeight;
	}

	private void hideSystemUI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			if (!statusBarEnabled) {
				flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
			}
			getWindow().getDecorView().setSystemUiVisibility(flags);
		} else if (!statusBarEnabled) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	private void showSystemUI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	public void setCurrent(Displayable displayable) {
		Session s = Session.current();
		if (s == null) {
			return;
		}
		Displayable prev = s.current;
		s.current = displayable;
		if (gridMode) {
			final Session fs = s;
			ViewHandler.postEvent(() -> {
				if (prev != null && prev != displayable) {
					prev.clearDisplayableView();
				}
				if (gridPanel != null) {
					gridPanel.show(fs);
				}
			});
		} else if (s == Session.active()) {
			ViewHandler.postEvent(new SetCurrentEvent(prev, displayable));
		}
	}

	public Displayable getCurrent() {
		return cur();
	}

	public boolean isVisible() {
		return visible;
	}

	public void showExitConfirmation() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(R.string.CONFIRMATION_REQUIRED)
				.setMessage(R.string.FORCE_CLOSE_CONFIRMATION)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					hideSoftInput();
					MidletThread.destroyApp();
				})
				.setNeutralButton(R.string.action_settings, (d, w) -> {
					hideSoftInput();
					Config.startApp(this, name(), Session.active().appPath, true);
					MidletThread.destroyApp();
				})
				.setNegativeButton(android.R.string.cancel, null);
		alertBuilder.create().show();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU)
			if (cur() instanceof Canvas && binding.displayableContainer.dispatchKeyEvent(event)) {
				return true;
			} else if (event.getAction() == KeyEvent.ACTION_DOWN) {
				if (event.getRepeatCount() == 0) {
					event.startTracking();
					return true;
				} else if (event.isLongPress()) {
					return onKeyLongPress(event.getKeyCode(), event);
				}
			} else if (event.getAction() == KeyEvent.ACTION_UP) {
				return onKeyUp(event.getKeyCode(), event);
			}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void openOptionsMenu() {
		if (!actionBarEnabled &&
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && cur() instanceof Canvas) {
			showSystemUI();
		}
		super.openOptionsMenu();
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == menuKey() || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
			showExitConfirmation();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if ((keyCode == menuKey() || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
				&& (event.getFlags() & (KeyEvent.FLAG_LONG_PRESS | KeyEvent.FLAG_CANCELED)) == 0) {
			openOptionsMenu();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		// Intentionally overridden by empty due to support for back-key remapping.
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.midlet_displayable, menu);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			menu.findItem(R.id.action_lock_orientation).setVisible(true);
		}
		if (actionBarEnabled) {
			menu.findItem(R.id.action_ime_keyboard).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.findItem(R.id.action_take_screenshot).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		if (inputMethodManager == null) {
			menu.findItem(R.id.action_ime_keyboard).setVisible(false);
		}
		if (ContextHolder.getVk() == null) {
			menu.findItem(R.id.action_submenu_vk).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (cur() instanceof Canvas) {
			menu.setGroupVisible(R.id.action_group_canvas, true);
			VirtualKeyboard vk = ContextHolder.getVk();
			if (vk != null) {
				boolean visible = vk.getLayoutEditMode() != VirtualKeyboard.LAYOUT_EOF;
				menu.findItem(R.id.action_layout_edit_finish).setVisible(visible);
			}
		} else {
			menu.setGroupVisible(R.id.action_group_canvas, false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_exit_midlet) {
			showExitConfirmation();
		} else if (id == R.id.action_save_log) {
			saveLog();
		} else if (id == R.id.action_lock_orientation) {
			if (item.isChecked()) {
				VirtualKeyboard vk = ContextHolder.getVk();
				int orientation = vk != null && vk.isPhone() ? ORIENTATION_PORTRAIT : loader().getOrientation();
				setOrientation(orientation);
				item.setChecked(false);
			} else {
				item.setChecked(true);
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
			}
		} else if (id == R.id.action_ime_keyboard) {
			inputMethodManager.toggleSoftInputFromWindow(binding.displayableContainer.getWindowToken(),
					InputMethodManager.SHOW_FORCED, 0);
		} else if (id == R.id.action_take_screenshot) {
			takeScreenshot();
		} else if (id == R.id.action_limit_fps) {
			showLimitFpsDialog();
		} else if (ContextHolder.getVk() != null) {
			// Handled only when virtual keyboard is enabled
			handleVkOptions(id);
		}
		return true;
	}

	private void handleVkOptions(int id) {
		VirtualKeyboard vk = ContextHolder.getVk();
		if (id == R.id.action_layout_edit_mode) {
			vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_KEYS);
			Toast.makeText(this, R.string.layout_edit_mode, Toast.LENGTH_SHORT).show();
		} else if (id == R.id.action_layout_scale_mode) {
			vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_SCALES);
			Toast.makeText(this, R.string.layout_scale_mode, Toast.LENGTH_SHORT).show();
		} else if (id == R.id.action_layout_edit_finish) {
			vk.setLayoutEditMode(VirtualKeyboard.LAYOUT_EOF);
			Toast.makeText(this, R.string.layout_edit_finished, Toast.LENGTH_SHORT).show();
			showSaveVkAlert(false);
		} else if (id == R.id.action_layout_switch) {
			showSetLayoutDialog();
		} else if (id == R.id.action_hide_buttons) {
			showHideButtonDialog();
		}
	}

	@SuppressLint("CheckResult")
	private void takeScreenshot() {
		loader().takeScreenshot((Canvas) cur(), new SingleObserver<String>() {
			@Override
			public void onSubscribe(@NonNull Disposable d) {
			}

			@Override
			public void onSuccess(@NonNull String s) {
				Toast.makeText(MicroActivity.this, getString(R.string.screenshot_saved)
						+ " " + s, Toast.LENGTH_LONG).show();
			}

			@Override
			public void onError(@NonNull Throwable e) {
				e.printStackTrace();
				Toast.makeText(MicroActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void saveLog() {
		try {
			LogUtils.writeLog();
			Toast.makeText(this, R.string.log_saved, Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
		}
	}

	private void showHideButtonDialog() {
		final VirtualKeyboard vk = ContextHolder.getVk();
		boolean[] states = vk.getKeysVisibility();
		boolean[] changed = states.clone();
		new AlertDialog.Builder(this)
				.setTitle(R.string.hide_buttons)
				.setMultiChoiceItems(vk.getKeyNames(), changed, (dialog, which, isChecked) -> {})
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					if (!Arrays.equals(states, changed)) {
						vk.setKeysVisibility(changed);
						showSaveVkAlert(true);
					}
				}).show();
	}

	private void showSaveVkAlert(boolean keepScreenPreferred) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.CONFIRMATION_REQUIRED);
		builder.setMessage(R.string.pref_vk_save_alert);
		builder.setNegativeButton(android.R.string.no, null);
		AlertDialog dialog = builder.create();

		final VirtualKeyboard vk = ContextHolder.getVk();
		if (vk.isPhone()) {
			AppCompatCheckBox cb = new AppCompatCheckBox(this);
			cb.setText(R.string.opt_save_screen_params);
			cb.setChecked(keepScreenPreferred);

			TypedValue out = new TypedValue();
			getTheme().resolveAttribute(androidx.appcompat.R.attr.dialogPreferredPadding, out, true);
			int paddingH = getResources().getDimensionPixelOffset(out.resourceId);
			int paddingT = getResources().getDimensionPixelOffset(androidx.appcompat.R.dimen.abc_dialog_padding_top_material);
			dialog.setView(cb, paddingH, paddingT, paddingH, 0);

			dialog.setButton(dialog.BUTTON_POSITIVE, getText(android.R.string.yes), (d, w) -> {
				if (cb.isChecked()) {
					vk.saveScreenParams();
				}
				vk.onLayoutChanged(VirtualKeyboard.TYPE_CUSTOM);
			});
		} else {
			dialog.setButton(dialog.BUTTON_POSITIVE, getText(android.R.string.yes), (d, w) ->
					ContextHolder.getVk().onLayoutChanged(VirtualKeyboard.TYPE_CUSTOM));
		}
		dialog.show();
	}

	private void showSetLayoutDialog() {
		final VirtualKeyboard vk = ContextHolder.getVk();
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.layout_switch)
				.setSingleChoiceItems(R.array.PREF_VK_TYPE_ENTRIES, vk.getLayout(), null)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					vk.setLayout(((AlertDialog) d).getListView().getCheckedItemPosition());
					if (vk.isPhone()) {
						setOrientation(ORIENTATION_PORTRAIT);
					} else {
						setOrientation(loader().getOrientation());
					}
				});
		builder.show();
	}

	private void showLimitFpsDialog() {
		EditText editText = new EditText(this);
		editText.setHint(R.string.unlimited);
		editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
		editText.setMaxLines(1);
		editText.setSingleLine(true);
		float density = getResources().getDisplayMetrics().density;
		LinearLayout linearLayout = new LinearLayout(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		int margin = (int) (density * 20);
		params.setMargins(margin, 0, margin, 0);
		linearLayout.addView(editText, params);
		int paddingVertical = (int) (density * 16);
		int paddingHorizontal = (int) (density * 8);
		editText.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
		new AlertDialog.Builder(this)
				.setTitle(R.string.PREF_LIMIT_FPS)
				.setView(linearLayout)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					Editable text = editText.getText();
					int fps = 0;
					try {
						fps = TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text.toString().trim());
					} catch (NumberFormatException ignored) {
					}
					loader().setLimitFps(fps);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.setNeutralButton(R.string.reset, ((d, which) -> loader().setLimitFps(-1)))
				.show();
	}

	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		if (cur() instanceof Form) {
			((Form) cur()).contextMenuItemSelected(item);
		} else if (cur() instanceof List) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			((List) cur()).contextMenuItemSelected(item, info.position);
		}

		return super.onContextItemSelected(item);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		ContextHolder.notifyOnActivityResult(requestCode, resultCode, data);
	}

	public String getAppName() {
		return name();
	}

	private class SetCurrentEvent extends SimpleEvent {
		private final Displayable current;
		private final Displayable next;

		private SetCurrentEvent(Displayable current, Displayable next) {
			this.current = current;
			this.next = next;
		}

		@Override
		public void process() {
			closeOptionsMenu();
			if (current != null) {
				current.clearDisplayableView();
			}
			if (next instanceof Alert) {
				return;
			}
			host().removeAllViews();
			ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) binding.toolbar.getLayoutParams();
			int toolbarHeight = 0;
			if (next instanceof Canvas) {
				hideSystemUI();
				if (!actionBarEnabled) {
					actionBar.hide();
				} else {
					final String title = next.getTitle();
					actionBar.setTitle(title == null ? name() : title);
					toolbarHeight = (int) (getToolBarHeight() / 1.5);
					layoutParams.height = toolbarHeight;
				}
			} else {
				showSystemUI();
				actionBar.show();
				final String title = next != null ? next.getTitle() : null;
				actionBar.setTitle(title == null ? name() : title);
				toolbarHeight = getToolBarHeight();
				layoutParams.height = toolbarHeight;
			}
			binding.overlayView.setLocation(0, toolbarHeight + tabBarHeight());
			binding.toolbar.setLayoutParams(layoutParams);
			invalidateOptionsMenu();
			if (next != null) {
				host().addView(next.getDisplayableView());
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (floating) {
			FloatingController.get(this).host().removeAllViews();
			FloatingController.get(this).hide();
			floating = false;
		}
		try {
			stopService(new Intent(this, BubbleService.class));
		} catch (Exception ignored) {
		}
		binding = null;
		super.onDestroy();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1) {
			synchronized (LocationProviderImpl.permissionLock) {
				LocationProviderImpl.permissionLock.notify();
			}
			LocationProviderImpl.permissionResult = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
		}
	}
}
