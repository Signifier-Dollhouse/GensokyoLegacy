package dev.xkmc.gensokyolegacy.content.dimension;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.GapBiomeGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.OptionalLong;

public class GLDimensionGen {

	public static final ResourceKey<DimensionType> DT_GAP = ResourceKey.create(Registries.DIMENSION_TYPE, loc("gap"));
	public static final ResourceKey<LevelStem> LEVEL_GAP = ResourceKey.create(Registries.LEVEL_STEM, loc("gap"));
	public static final ResourceKey<Level> GAP = Registries.levelStemToLevel(LEVEL_GAP);

	public static void init(DataProviderInitializer init) {

		GensokyoDimension.init(init);

		// biome, dimension type and level stem must all be registered by a single bootstrap each,
		// otherwise the later one overwrites what the earlier one collected
		init.add(Registries.DIMENSION_TYPE, (ctx) -> {
			var spawn = new DimensionType.MonsterSettings(true, false,
					UniformInt.of(0, 7), 0);
			ctx.register(DT_GAP, new DimensionType(
					OptionalLong.of(18000L),
					false, false, false, false,
					1, true, false,
					0, 256, 256,
					BlockTags.INFINIBURN_OVERWORLD,
					BuiltinDimensionTypes.END_EFFECTS, 1f, spawn
			));
			ctx.register(GensokyoDimension.DT_GENSOKYO, GensokyoDimension.dimensionType());
		});

		init.add(Registries.LEVEL_STEM, (ctx) -> {
			var dt = ctx.lookup(Registries.DIMENSION_TYPE);
			var biome = ctx.lookup(Registries.BIOME);
			ctx.register(LEVEL_GAP, new LevelStem(dt.getOrThrow(DT_GAP),
					new EmptyChunkGenerator(biome.getOrThrow(GapBiomeGen.KEY))));
			ctx.register(GensokyoDimension.LEVEL_GENSOKYO, new LevelStem(
					dt.getOrThrow(GensokyoDimension.DT_GENSOKYO), GensokyoDimension.chunkGenerator(ctx)));
		});

	}

	private static ResourceLocation loc(String id) {
		return GensokyoLegacy.loc(id);
	}

}
