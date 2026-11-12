package dev.xkmc.gensokyolegacy.content.item.treasure;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * One derived recipe: what a stone becomes when a gem is spent on it. Stone tags stay tags, so a
 * datapack that edits one changes what the tower accepts without a rebuild.
 *
 * @see "doc/design/tower.md"
 */
public record OreRecipe(Item gem, Block ore, ArrayList<ResourceLocation> tags, ArrayList<Block> stones) {

	public boolean matches(BlockState state) {
		for (var id : tags)
			if (state.is(TagKey.create(Registries.BLOCK, id))) return true;
		return stones.contains(state.getBlock());
	}

	public List<TagKey<Block>> tagKeys() {
		return tags.stream().map(e -> TagKey.create(Registries.BLOCK, e)).toList();
	}

}
