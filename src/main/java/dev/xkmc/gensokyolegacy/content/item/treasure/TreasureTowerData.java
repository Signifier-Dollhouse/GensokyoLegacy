package dev.xkmc.gensokyolegacy.content.item.treasure;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The finished recipe list, read by the item and by JEI. Small enough that a lookup is a scan over
 * the recipes of one gem rather than a per-block index, which would need the stone tags expanded.
 *
 * @see "doc/design/tower.md"
 */
public final class TreasureTowerData {

	public static final TreasureTowerData EMPTY = new TreasureTowerData(List.of());

	private final List<OreRecipe> recipes;
	private final Map<Item, List<OreRecipe>> byGem;

	public TreasureTowerData(List<OreRecipe> recipes) {
		this.recipes = List.copyOf(recipes);
		Map<Item, List<OreRecipe>> map = new LinkedHashMap<>();
		for (var recipe : this.recipes)
			map.computeIfAbsent(recipe.gem(), e -> new ArrayList<>()).add(recipe);
		map.replaceAll((k, v) -> List.copyOf(v));
		this.byGem = Collections.unmodifiableMap(map);
	}

	public List<OreRecipe> recipes() {
		return recipes;
	}

	/**
	 * The ore {@code gem} turns {@code state} into, or null when the stone is not one this gem accepts.
	 */
	public @Nullable Block oreFor(BlockState state, Item gem) {
		for (var recipe : byGem.getOrDefault(gem, List.of()))
			if (recipe.matches(state)) return recipe.ore();
		return null;
	}

	public @Nullable Block oreFor(BlockState state, ItemStack stack) {
		return stack.isEmpty() ? null : oreFor(state, stack.getItem());
	}

	public boolean accepts(BlockState state) {
		for (var recipe : recipes)
			if (recipe.matches(state)) return true;
		return false;
	}

}
