package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;

import java.util.List;

public record WitchHandler(boolean throwable) implements AbstractPotionHandler {

	@Override
	public boolean isThrowable() {
		return throwable;
	}

	@Override
	public boolean isDrinkable() {
		return !throwable;
	}

	@Override
	public List<DataComponentType<?>> getComponentsToCopy() {
		return List.of(DataComponents.POTION_CONTENTS);
	}

}
