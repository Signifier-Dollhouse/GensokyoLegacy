package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface HexBrewHandler {

	boolean isThrowable();

	void onHit(Level level, Vec3 pos,@Nullable Entity thrower);
}
