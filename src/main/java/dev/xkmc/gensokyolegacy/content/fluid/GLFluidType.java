package dev.xkmc.gensokyolegacy.content.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

public class GLFluidType extends FluidType {

	private final ResourceLocation stillTexture;
	private final ResourceLocation flowingTexture;
	private final int color;

	public GLFluidType(FluidType.Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture, int color) {
		super(properties);
		this.stillTexture = stillTexture;
		this.flowingTexture = flowingTexture;
		this.color = color;
	}

	public int getColor() {
		return color;
	}

	public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
		consumer.accept(new ClientGLFluid(this));
	}

	public ResourceLocation getStillTexture() {
		return stillTexture;
	}

	public ResourceLocation getFlowingTexture() {
		return flowingTexture;
	}
}
