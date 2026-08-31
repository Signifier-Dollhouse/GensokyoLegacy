package dev.xkmc.gensokyolegacy.content.fluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Supplier;

public class HexbrewBottleItem extends Item {

	private final Supplier<GLHexFluid> fluidSupplier;

	public HexbrewBottleItem(Supplier<GLHexFluid> supplier, Properties properties) {
		super(properties.craftRemainder(Items.GLASS_BOTTLE));
		this.fluidSupplier = supplier;
	}

	public GLHexFluid getFluid() {
		return fluidSupplier.get();
	}

	public FluidStack getFluidStack() {
		return new FluidStack(fluidSupplier.get(), 250);
	}
}
