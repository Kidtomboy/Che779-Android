package ru.playsoftware.j2meloader.slots;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** O Cai dat nhanh: bat/tat che do Treo (khong ve) cho tat ca cac tab. */
public class TreoTileService extends TileService {
	@Override
	public void onStartListening() {
		update();
	}

	@Override
	public void onClick() {
		int cur = Treo.getGlobal(this);
		Treo.setGlobal(this, cur == Treo.DRAW ? Treo.OFF : Treo.DRAW);
		update();
	}

	private void update() {
		Tile t = getQsTile();
		if (t == null) {
			return;
		}
		int m = Treo.getGlobal(this);
		t.setLabel(m == Treo.DRAW ? "Che779: Vẽ" : m == Treo.ECO ? "Che779: 20 FPS" : "Che779: Treo");
		t.setState(m == Treo.DRAW ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
		t.updateTile();
	}
}
