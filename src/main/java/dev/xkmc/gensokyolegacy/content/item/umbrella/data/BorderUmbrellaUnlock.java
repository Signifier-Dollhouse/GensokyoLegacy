package dev.xkmc.gensokyolegacy.content.item.umbrella.data;

public record BorderUmbrellaUnlock(boolean travelUnlocked, boolean captureUnlocked) {

	public static BorderUmbrellaUnlock DEFAULT = new BorderUmbrellaUnlock(false, false);

	public BorderUmbrellaUnlock withTravel(boolean v) {
		return new BorderUmbrellaUnlock(v, captureUnlocked);
	}

	public BorderUmbrellaUnlock withCapture(boolean v) {
		return new BorderUmbrellaUnlock(travelUnlocked, v);
	}
}
