package dev.xkmc.gensokyolegacy.content.rpg.requirement;

import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientList;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SerialClass
public class RolledIngredientList extends QuestRequirementData implements IngredientList {

	@SerialField
	public final List<ItemStack> rolled = new ArrayList<>();

	public RolledIngredientList() {
	}

	public RolledIngredientList(List<ItemStack> rolled) {
		this.rolled.addAll(rolled);
	}

	@Override
	public List<IngredientEntry> ingredients() {
		List<IngredientEntry> ans = new ArrayList<>();
		for (var stack : rolled) {
			if (stack.isEmpty()) continue;
			ans.add(new IngredientEntry(Ingredient.of(stack), stack.getCount(), Optional.empty()));
		}
		return ans;
	}

}
