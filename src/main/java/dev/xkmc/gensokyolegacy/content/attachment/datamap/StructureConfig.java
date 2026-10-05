package dev.xkmc.gensokyolegacy.content.attachment.datamap;

import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public record StructureConfig(
		LinkedHashSet<EntityType<?>> entities,
		LinkedHashMap<EntityType<?>, CharacterVisit> visitors,
		ArrayList<BoundingBox> rooms,
		StructureInterior interior,
		ArrayList<BoundingBox> house,
		@Nullable ResourceLocation outsideBlock,
		@Nullable ResourceLocation primaryFix,
		@Nullable ResourceLocation wouldFix
) {

	/**
	 * The config of the structure this home is an instance of, or null when it
	 * has none.
	 */
	@Nullable
	public static StructureConfig of(RegistryAccess access, StructureKey key) {
		return access.<Structure>holder(key.getStructure())
				.map(h -> h.getData(GLMeta.STRUCTURE_DATA.reg())).orElse(null);
	}

	/**
	 * How the given character is allowed to visit this structure, or null when it
	 * never visits.
	 */
	@Nullable
	public static CharacterVisit visitOf(RegistryAccess access, StructureKey key, EntityType<?> guest) {
		var config = of(access, key);
		return config == null ? null : config.visitors().get(guest);
	}

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

	/**
	 * One character's visit schedule at this structure. Purely a length plus the
	 * share of days she shows up at all - when she does, the window itself is
	 * derived from the structure position, her type and the day (see
	 * {@code VisitTable}), so nothing here is per-day state.
	 *
	 * @param chance  share of days she visits at all, 0..1
	 * @param minStay shortest visit in ticks
	 * @param maxStay longest visit in ticks
	 */
	public record CharacterVisit(float chance, int minStay, int maxStay) {

		public CharacterVisit {
			chance = Mth.clamp(chance, 0f, 1f);
			minStay = Math.max(1, minStay);
			maxStay = Math.max(minStay, maxStay);
		}

		/**
		 * Whether she visits at all today. A daily gate rather than a spawn-time
		 * roll, so the whole schedule stays a pure function of the day seed.
		 */
		public boolean roll(RandomSource rand) {
			return rand.nextFloat() < chance;
		}

		/**
		 * Today's length in ticks, or 0 for no visit. Scaled by the global
		 * multiplier and capped at the visit window; the caller does the capping,
		 * so nothing here needs to know how long a day is.
		 */
		public int rollStay(RandomSource rand, double multiplier) {
			int min = scale(minStay, multiplier);
			int max = scale(maxStay, multiplier);
			return min >= max ? min : min + rand.nextInt(max - min + 1);
		}

		private static int scale(int stay, double multiplier) {
			return Math.max(1, (int) Math.round(stay * Math.max(0, multiplier)));
		}
	}

	public static class Builder {

		@Nullable
		ResourceLocation outSideBlock, primaryFix, wouldFix;

		LinkedHashSet<EntityType<?>> entities = new LinkedHashSet<>();
		LinkedHashMap<EntityType<?>, CharacterVisit> visitors = new LinkedHashMap<>();
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

		/**
		 * Let the given character visit this structure on a share of days.
		 */
		public Builder visit(EntityType<?> guest, float chance, int minStay, int maxStay) {
			return visit(guest, new CharacterVisit(chance, minStay, maxStay));
		}

		/**
		 * Let the given character visit this structure on a fixed-length visit.
		 */
		public Builder visitor(EntityType<?> guest, int stay) {
			return visit(guest, new CharacterVisit(1f, stay, stay));
		}

		/**
		 * Declare a prebuilt guest slot, as authored on the structure itself.
		 */
		public Builder visit(EntityType<?> guest, CharacterVisit visit) {
			visitors.put(guest, visit);
			return this;
		}

		public StructureConfig build() {
			return new StructureConfig(entities, new LinkedHashMap<>(visitors),
					new ArrayList<>(rooms), interior, new ArrayList<>(house),
					outSideBlock, primaryFix, wouldFix);
		}

	}

}
