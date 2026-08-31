package dev.xkmc.gensokyolegacy.content.fluid;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public record ClientGLFluid(GLFluidType type) implements IClientFluidTypeExtensions {

	@Override
	public ResourceLocation getStillTexture() {
		return type.getStillTexture();
	}

	@Override
	public ResourceLocation getFlowingTexture() {
		return type.getFlowingTexture();
	}

	@Override
	public int getTintColor() {
		return type.getColor();
	}

	@Override
	public int getTintColor(FluidStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents != null) {
			int c = contents.getColor();
			if (c != -1) {
				return 0xFF000000 | c;
			}
		}
		return type.getColor();
	}
}
