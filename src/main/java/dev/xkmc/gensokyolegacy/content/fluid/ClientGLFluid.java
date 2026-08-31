package dev.xkmc.gensokyolegacy.content.fluid;

import net.minecraft.resources.ResourceLocation;
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
		return type.getColor();
	}
}
