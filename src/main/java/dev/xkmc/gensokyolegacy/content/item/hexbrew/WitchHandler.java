package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WitchHandler implements AbstractPotionHandler {

	@Override
	public boolean isThrowable() {
		return false;
	}

	@Override
	public void onHit(Level level, Vec3 pos, @Nullable Entity thrower) {
	}

	@Override
	public boolean isDrinkable() {
		return true;
	}

	@Override
	public List<DataComponentType<?>> getComponentsToCopy() {
		return List.of(DataComponents.POTION_CONTENTS);
	}

}
