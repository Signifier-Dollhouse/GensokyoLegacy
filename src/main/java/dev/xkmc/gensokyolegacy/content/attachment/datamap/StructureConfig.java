package dev.xkmc.gensokyolegacy.content.attachment.datamap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record StructureConfig(
		LinkedHashSet<EntityType<?>> entities,
		ArrayList<BoundingBox> rooms,
		StructureInterior interior,
		ArrayList<BoundingBox> house,
		@Nullable ResourceLocation outsideBlock,
		@Nullable ResourceLocation primaryFix,
		@Nullable ResourceLocation wouldFix
) {

	public static Builder builder() {
		return new Builder();
	}

	public boolean isOutside(Level level, BlockPos ans) {
		if (level.canSeeSky(ans)) return true;
		if (outsideBlock == null) return false;
		BlockState floor = level.getBlockState(ans.below());
		BlockState on = level.getBlockState(ans);
		TagKey<Block> key = TagKey.create(Registries.BLOCK, outsideBlock);
		return floor.is(key) || on.is(key);
	}

	public boolean isPrimary(BlockState state) {
		return is(primaryFix, state);
	}

	public boolean wouldFix(BlockState state) {
		return is(wouldFix, state);
	}

	private boolean is(@Nullable ResourceLocation tag, BlockState state) {
		if (tag == null) return false;
		TagKey<Block> key = TagKey.create(Registries.BLOCK, tag);
		return state.is(key);
	}

	public static class Builder {

		@Nullable
		ResourceLocation outSideBlock, primaryFix, wouldFix;

		LinkedHashSet<EntityType<?>> entities = new LinkedHashSet<>();
		List<BoundingBox> rooms = new ArrayList<>();
		StructureInterior interior = StructureInterior.empty();
		List<BoundingBox> house = new ArrayList<>();

		public Builder house(List<BoundingBox> house) {
			this.house = new ArrayList<>(house);
			return this;
		}

		public Builder rooms(List<BoundingBox> rooms) {
			this.rooms = new ArrayList<>(rooms);
			return this;
		}

		public Builder interior(StructureInterior interior) {
			this.interior = interior;
			return this;
		}

		public Builder outside(TagKey<Block> tag) {
			this.outSideBlock = tag.location();
			return this;
		}

		public Builder primary(TagKey<Block> tag) {
			this.primaryFix = tag.location();
			return this;
		}

		public Builder wouldFix(TagKey<Block> tag) {
			this.wouldFix = tag.location();
			return this;
		}


		public void addEntity(EntityType<?> type) {
			entities.add(type);
		}

		public StructureConfig build() {
			return new StructureConfig(entities,
					new ArrayList<>(rooms), interior, new ArrayList<>(house),
					outSideBlock, primaryFix, wouldFix);
		}

	}

}
