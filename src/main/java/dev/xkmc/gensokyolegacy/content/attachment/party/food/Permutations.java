package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the item form of a block food by appending a suffix to the block's registry id —
 * {@code youkaisfeasts:tarte_lune} → {@code youkaisfeasts:tarte_lune_slice}. Preferred over
 * reflecting a field, because a naming convention survives a mod update.
 *
 * @see "doc/design/party/food.md"
 */
public final class Permutations {

	/**
	 * Suffixes tried against a block's id, in order.
	 */
	private static final String[] SUFFIXES = {"_slice", "_dish", "_piece"};

	/**
	 * The food properties of the first permuted id that names an item carrying food,
	 * or null when none does.
	 */
	@Nullable
	public static FoodProperties foodOf(Block block) {
		var item = itemOf(block);
		return item == null ? null : Servings.of(new ItemStack(item));
	}

	/**
	 * The first permuted id that names a real item carrying food, or null. The item rather than
	 * just its properties, so a caller can also <i>name</i> the serving.
	 */
	@Nullable
	public static Item itemOf(Block block) {
		var id = BuiltInRegistries.BLOCK.getKey(block);
		for (var suffix : SUFFIXES) {
			var candidate = ResourceLocation.tryParse(id.getNamespace() + ":" + id.getPath() + suffix);
			if (candidate == null) continue;
			var item = BuiltInRegistries.ITEM.get(candidate);
			if (item == net.minecraft.world.item.Items.AIR) continue;
			if (Servings.of(new ItemStack(item)) != null) return item;
		}
		return null;
	}

	/**
	 * The food properties of the item at this id, or null.
	 */
	@Nullable
	public static FoodProperties propsAt(ResourceLocation id) {
		var item = BuiltInRegistries.ITEM.get(id);
		if (item == net.minecraft.world.item.Items.AIR) return null;
		return Servings.of(new ItemStack(item));
	}

	/**
	 * The item at this id, or an empty stack.
	 */
	public static ItemStack stackAt(ResourceLocation id) {
		var item = BuiltInRegistries.ITEM.get(id);
		return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	private Permutations() {
	}
}