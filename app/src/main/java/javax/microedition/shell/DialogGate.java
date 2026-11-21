/*
 *  Che779 Emulator - one dialog at a time + tap throttle for toolbar buttons.
 *  Licensed under the Apache License, Version 2.0 (same as J2ME Loader).
 */
package javax.microedition.shell;

import android.app.Dialog;
import android.content.Context;
import android.os.SystemClock;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

/**
 * Spamming the toolbar used to stack many dialogs (dark layers on top of each other).
 * {@link #tap()} lets a toolbar action run only when no dialog is showing and the last
 * action was at least 500 ms ago. Every dialog shown through {@link #present(Dialog)}
 * (or {@link Builder#show()}) is tracked.
 */
final class DialogGate {
	private static final ArrayList<Dialog> SHOWN = new ArrayList<>();
	private static long last;

	private DialogGate() {
	}

	/** True while at least one tracked dialog is on screen. */
	static boolean busy() {
		for (int i = SHOWN.size() - 1; i >= 0; i--) {
			Dialog d = SHOWN.get(i);
			if (d.isShowing()) {
				return true;
			}
			SHOWN.remove(i);
		}
		return false;
	}

	/** Gate for toolbar buttons: false = ignore this tap. */
	static boolean tap() {
		long now = SystemClock.uptimeMillis();
		if (busy() || now - last < 500) {
			return false;
		}
		last = now;
		return true;
	}

	static void present(Dialog d) {
		SHOWN.add(d);
		d.show();
	}

	/** Builder whose show() registers the dialog with the gate. */
	static final class Builder extends AlertDialog.Builder {
		Builder(Context c) {
			super(c);
		}

		@Override
		public AlertDialog show() {
			AlertDialog d = create();
			present(d);
			return d;
		}
	}
}
