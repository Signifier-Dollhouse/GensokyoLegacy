package dev.xkmc.gensokyolegacy.content.dimension;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarverDebugSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

/**
 * The overworld's two cave carvers, with the lava level eight blocks higher than vanilla's.
 * <p>
 * Everything else is vanilla's numbers, so the change is exactly the lava level and nothing can be blamed on
 * anything else. See {@link #LAVA_LEVEL}.
 *
 * @see "doc/design/gensokyo_dimension.md" §11
 */
public class GensokyoCarvers {

	public static final ResourceKey<ConfiguredWorldCarver<?>> CAVE = key("cave");
	public static final ResourceKey<ConfiguredWorldCarver<?>> CAVE_EXTRA_UNDERGROUND = key("cave_extra_underground");

	/**
	 * Height above the world bottom at which the cave carver stops filling lava and starts leaving air, counting
	 * the bottom block as zero. Vanilla overworld: {@code above_bottom(8)}, which is y −56 on a −64 floor.
	 * <p>
	 * This is the only lava level in the dimension that a datapack can move. Two others look like one and are not:
	 * {@code NoiseBasedChunkGenerator.createFluidPicker} hard-codes the aquifer's lava to {@code min(-54, seaLevel)},
	 * and {@code Aquifer.NoiseBasedAquifer.computeFluidType} hard-codes the aquifer's lava level to {@code -10}. Both
	 * are constants in the engine, so the carver's anchor is the whole of the knob.
	 * <p>
	 * Eight blocks up puts the lava sea's surface at y −48, which is where {@link GensokyoDimension}'s caverns fade
	 * back in over the floor — so the lava now reaches the bottom of the caves instead of stopping eight blocks
	 * short of it, and it stops eight blocks below where it used to.
	 */
	private static final int LAVA_LEVEL = 16;

	public static void init(DataProviderInitializer init) {
		init.add(Registries.CONFIGURED_CARVER, GensokyoCarvers::carvers);
	}

	private static void carvers(BootstrapContext<ConfiguredWorldCarver<?>> ctx) {
		HolderGetter<Block> blocks = ctx.lookup(Registries.BLOCK);
		ctx.register(CAVE, WorldCarver.CAVE.configured(new CaveCarverConfiguration(
				0.15F,
				UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(180)),
				UniformFloat.of(0.1F, 0.9F),
				VerticalAnchor.aboveBottom(LAVA_LEVEL),
				CarverDebugSettings.of(false, Blocks.CRIMSON_BUTTON.defaultBlockState()),
				blocks.getOrThrow(BlockTags.OVERWORLD_CARVER_REPLACEABLES),
				UniformFloat.of(0.7F, 1.4F),
				UniformFloat.of(0.8F, 1.3F),
				UniformFloat.of(-1.0F, -0.4F)
		)));
		ctx.register(CAVE_EXTRA_UNDERGROUND, WorldCarver.CAVE.configured(new CaveCarverConfiguration(
				0.07F,
				UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(47)),
				UniformFloat.of(0.1F, 0.9F),
				VerticalAnchor.aboveBottom(LAVA_LEVEL),
				CarverDebugSettings.of(false, Blocks.OAK_BUTTON.defaultBlockState()),
				blocks.getOrThrow(BlockTags.OVERWORLD_CARVER_REPLACEABLES),
				UniformFloat.of(0.7F, 1.4F),
				UniformFloat.of(0.8F, 1.3F),
				UniformFloat.of(-1.0F, -0.4F)
		)));
	}

	private static ResourceKey<ConfiguredWorldCarver<?>> key(String id) {
		return ResourceKey.create(Registries.CONFIGURED_CARVER, GensokyoLegacy.loc(id));
	}

}