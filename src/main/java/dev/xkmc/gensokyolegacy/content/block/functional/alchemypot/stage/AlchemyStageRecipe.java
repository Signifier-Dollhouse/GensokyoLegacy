package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage;

import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.l2library.serial.recipe.BaseRecipe;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.xkmc.gensokyolegacy.compat.l2lib.FluidIngredient;

import java.util.List;

/**
 * Visual-only intermediate: matches the pot contents, tints the fluid and says which items it
 * would consume, but produces nothing itself.
 *
 * @see "doc/design/alchemy_pot.md"
 */
@SerialClass
public abstract class AlchemyStageRecipe<T extends AlchemyStageRecipe<T>> extends BaseRecipe<T, AlchemyStageRecipe<?>, AlchemyInv> {

	@SerialField
	public FluidIngredient inputFluid = FluidIngredient.empty();

	public AlchemyStageRecipe(ResourceLocation id, RecType<T, AlchemyStageRecipe<?>, AlchemyInv> fac) {
		super(id, fac);
	}

	public abstract int getPriority();

	public abstract void removeConsumed(List<ItemStack> list);

	public abstract int getColor(AlchemyInv inv, RegistryAccess access);

	@Override
	public boolean canCraftInDimensions(int i, int j) {
		return true;
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (inv.fluid().isEmpty()) return false;
		return inputFluid.isEmpty() || inputFluid.test(inv.fluid());
	}

	@Override
	public ItemStack assemble(AlchemyInv inv, RegistryAccess access) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getResultItem(RegistryAccess access) {
		return ItemStack.EMPTY;
	}
}
