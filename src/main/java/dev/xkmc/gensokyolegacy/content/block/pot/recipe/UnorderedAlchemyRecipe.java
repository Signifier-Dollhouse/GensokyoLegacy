package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

@SerialClass
public class UnorderedAlchemyRecipe extends AlchemyRecipe<UnorderedAlchemyRecipe> {

	@SerialField
	public final ArrayList<Ingredient> input = new ArrayList<>();

	public UnorderedAlchemyRecipe() {
		super(GLRecipes.ALCHEMY_UNORDERED.get());
	}

	@Override
	public List<Ingredient> getInputItems() {
		return input;
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (!super.matches(inv, level)) return false;
		if (inv.size() > input.size()) return false;
		List<Ingredient> remain = new ArrayList<>(input);
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			var itr = remain.iterator();
			boolean matched = false;
			while (itr.hasNext()) {
				var ing = itr.next();
				if (ing.test(stack)) {
					itr.remove();
					matched = true;
					break;
				}
			}
			if (!matched) return false;
		}
		return !inv.isComplete() || remain.isEmpty();
	}

	@Override
	public List<Ingredient> getHints(Level level, AlchemyInv inv) {
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
		// only hints if fluid matches; caller already ensures
		return remain;
	}
}
