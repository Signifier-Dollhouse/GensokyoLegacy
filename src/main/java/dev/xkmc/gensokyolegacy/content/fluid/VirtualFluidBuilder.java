package dev.xkmc.gensokyolegacy.content.fluid;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class VirtualFluidBuilder<T extends BaseFlowingFluid, P> extends FluidBuilder<T, P> {

	public VirtualFluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
	                           ResourceLocation stillTexture, ResourceLocation flowingTexture,
	                           FluidBuilder.FluidTypeFactory typeFactory, NonNullFunction<BaseFlowingFluid.Properties, T> factory) {
		super(owner, parent, name, callback, stillTexture, flowingTexture, typeFactory, factory);
		this.source(factory);
	}
}
