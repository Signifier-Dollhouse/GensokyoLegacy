package dev.xkmc.gensokyolegacy.content.fluid;

import net.minecraft.resources.ResourceLocation;
import dev.xkmc.gensokyolegacy.util.PotionData;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

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
		var contents = PotionData.read(stack);
		if (!contents.isEmpty()) {
			int c = contents.getColor();
			if (c != -1) {
				return 0xFF000000 | c;
			}
		}
		return type.getColor();
	}
}
