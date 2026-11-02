package javax.microedition.shell;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import javax.microedition.util.ContextHolder;

/** Quick Settings tile: cycles the power mode of the running games (lives in the :midlet process). */
@RequiresApi(api = Build.VERSION_CODES.N)
public class PowerTileService extends TileService {
	@Override
	public void onStartListening() {
		refresh();
	}

	@Override
	public void onClick() {
		MicroActivity a = ContextHolder.getActivity();
		if (a != null) {
			a.cyclePowerMode();
		}
		getQsTile().setLabel("Che779");
		refresh();
	}

	private void refresh() {
		Tile t = getQsTile();
		if (t == null) {
			return;
		}
		MicroActivity a = ContextHolder.getActivity();
		t.setLabel("Che779");
		if (a == null) {
			t.setState(Tile.STATE_UNAVAILABLE);
			if (Build.VERSION.SDK_INT >= 29) {
				t.setSubtitle("Không có game");
			}
		} else {
			int m = a.powerMode();
			t.setState(m == 0 ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
			if (Build.VERSION.SDK_INT >= 29) {
				t.setSubtitle(MicroActivity.POWER_NAMES[m]);
			}
		}
		t.updateTile();
	}
}
