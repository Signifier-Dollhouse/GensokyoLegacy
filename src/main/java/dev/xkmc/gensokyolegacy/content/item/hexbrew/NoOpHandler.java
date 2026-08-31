package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class NoOpHandler implements HexBrewHandler {

	@Override
	public boolean isThrowable() {
		return false;
	}

	@Override
	public void onHit(Level level, Vec3 pos, Entity thrower) {
	}
}
