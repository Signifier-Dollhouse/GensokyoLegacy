package dev.xkmc.gensokyolegacy.content.fluid;

import dev.xkmc.gensokyolegacy.init.registrate.GLFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HexbrewWrapper implements IFluidHandlerItem {

	private ItemStack container;

	public HexbrewWrapper(ItemStack container) {
		this.container = container;
	}

	public HexbrewWrapper(ItemStack container, @Nullable Void ctx) {
		this(container);
	}

	@Override
	@NotNull
	public ItemStack getContainer() {
		return container;
	}

	private FluidStack getFluidFromBottle() {
		if (container.getItem() instanceof HexbrewBottleItem bottle) {
			return bottle.getFluidStack();
		}
		return FluidStack.EMPTY;
	}

	private void setFluid(FluidStack stack) {
		if (stack.isEmpty()) {
			container = new ItemStack(Items.GLASS_BOTTLE);
		} else if (stack.getFluid() instanceof GLHexFluid) {
			for (var e : GLFluids.Hexbrew.values()) {
				if (e.fluid.getSource() == stack.getFluid()) {
					container = e.bottle.asStack(1);
					return;
				}
			}
			container = new ItemStack(Items.GLASS_BOTTLE);
		}
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	@NotNull
	public FluidStack getFluidInTank(int tank) {
		return getFluidFromBottle();
	}

	@Override
	public int getTankCapacity(int tank) {
		return 250;
	}

	@Override
	public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
		return stack.getFluid() instanceof GLHexFluid;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (!getFluidFromBottle().isEmpty()) return 0;
		if (!(resource.getFluid() instanceof GLHexFluid)) return 0;
		if (resource.getAmount() < 250) return 0;
		if (action.execute()) {
			setFluid(resource);
		}
		return 250;
	}

	@Override
	@NotNull
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (container.getCount() != 1) return FluidStack.EMPTY;
		if (resource.getAmount() < 250) return FluidStack.EMPTY;
		FluidStack cur = getFluidFromBottle();
		if (!FluidStack.isSameFluidSameComponents(cur, resource)) return FluidStack.EMPTY;
		if (action.execute()) {
			setFluid(FluidStack.EMPTY);
		}
		return cur;
	}

	@Override
	@NotNull
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (container.getCount() != 1) return FluidStack.EMPTY;
		if (maxDrain < 250) return FluidStack.EMPTY;
		FluidStack cur = getFluidFromBottle();
		if (cur.isEmpty()) return FluidStack.EMPTY;
		if (action.execute()) {
			setFluid(FluidStack.EMPTY);
		}
		return cur;
	}
}
