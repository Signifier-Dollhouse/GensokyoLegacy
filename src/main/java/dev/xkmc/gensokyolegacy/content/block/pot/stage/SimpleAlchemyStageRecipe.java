package dev.xkmc.gensokyolegacy.content.block.pot.stage;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

@SerialClass
public class SimpleAlchemyStageRecipe extends AlchemyStageRecipe<SimpleAlchemyStageRecipe> {

	@SerialField
	public int color = -1;
	@SerialField
	public final List<Ingredient> input = new ArrayList<>();

	public SimpleAlchemyStageRecipe() {
		super(GLRecipes.ALCHEMY_STAGE_SIMPLE.get());
	}

	@Override
	public int getColor(AlchemyInv inv, HolderLookup.Provider access) {
		return color;
	}

	@Override
	public int getPriority() {
		return input.size();
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (!super.matches(inv, level)) return false;
		List<Ingredient> remain = new ArrayList<>(input);
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			var itr = remain.iterator();
			while (itr.hasNext()) {
				var ing = itr.next();
				if (ing.test(stack)) {
					itr.remove();
					break;
				}
			}
		}
		return remain.isEmpty();
	}

	@Override
	public void removeConsumed(List<ItemStack> list) {
		List<Ingredient> remain = new ArrayList<>(input);
		for (int i = 0; i < list.size(); i++) {
			ItemStack stack = list.get(i);
			if (stack.isEmpty()) continue;
			var itr = remain.iterator();
			while (itr.hasNext()) {
				var ing = itr.next();
				if (ing.test(stack)) {
					itr.remove();
					list.set(i, ItemStack.EMPTY);
					break;
				}
			}
		}
	}
}
