package dev.xkmc.gensokyolegacy.content.party.core.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the item form of a block food by permuting the block's registry id.
 *
 * <p>
 * Block foods commonly ship a matching item that holds the actual food values — the
 * slice of a cake, the contents of a bowl — and those items are named after the block
 * rather than being reachable from it. Youkai's Feasts, for instance, forms them by
 * concatenating {@code "_slice"} onto the block name, so {@code youkaisfeasts:tarte_lune}
 * has a {@code youkaisfeasts:tarte_lune_slice}.
 *
 * <p>
 * This is how those items are reached. Asking the block for its own item, or reflecting
 * a private field, both work less well: several of these blocks have no public accessor
 * at all, and a name convention survives a mod update that a field name does not.
 *
 * <p>
 * Every candidate is checked against the item registry, so a permutation that does not
 * exist simply misses.
 */
public final class Permutations {

	/** Suffixes tried against a block's id, in order. */
	private static final String[] SUFFIXES = {"_slice", "_dish", "_piece"};

	/**
	 * The food properties of the first permuted id that names an item carrying food,
	 * or null when none does.
	 */
	@Nullable
	public static FoodProperties foodOf(Block block) {
		var id = BuiltInRegistries.BLOCK.getKey(block);
		for (var suffix : SUFFIXES) {
			var candidate = ResourceLocation.tryParse(id.getNamespace() + ":" + id.getPath() + suffix);
			if (candidate == null) continue;
			var props = propsAt(candidate);
			if (props != null) return props;
		}
		return null;
	}

	/** The food properties of the item at this id, or null. */
	@Nullable
	public static FoodProperties propsAt(ResourceLocation id) {
		var item = BuiltInRegistries.ITEM.get(id);
		if (item == net.minecraft.world.item.Items.AIR) return null;
		return Servings.of(new ItemStack(item));
	}

	/** The item at this id, or an empty stack. */
	public static ItemStack stackAt(ResourceLocation id) {
		var item = BuiltInRegistries.ITEM.get(id);
		return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	private Permutations() {
	}
}