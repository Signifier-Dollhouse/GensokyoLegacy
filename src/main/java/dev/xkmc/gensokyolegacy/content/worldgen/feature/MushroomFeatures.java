package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import com.mojang.serialization.Codec;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.mushroom.DemonicMiasmaMushroomFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.mushroom.DreamMushroomFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.mushroom.GhostFireMushroomFeature;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class MushroomFeatures {

	public enum MushroomTreeType {
		GHOST_FIRE("ghost_fire_mushroom", GhostFireMushroomFeature::new,
				() -> GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET, null),
		DREAM("dream_mushroom", DreamMushroomFeature::new,
				() -> GLNaturalBlocks.DREAM_MUSHROOM_SET, 2),
		DEMONIC_MIASMA("demonic_miasma_mushroom", DemonicMiasmaMushroomFeature::new,
				() -> GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET, 2);

		public final String id;
		public final ResourceKey<ConfiguredFeature<?, ?>> cfKey;
		public final Function<Codec<HugeMushroomFeatureConfiguration>, AbstractHugeMushroomFeature> factory;
		public final Supplier<GLNaturalBlocks.MushroomSet> set;
		public final int radius;

		MushroomTreeType(String id,
		                 Function<Codec<HugeMushroomFeatureConfiguration>, AbstractHugeMushroomFeature> factory,
		                 Supplier<GLNaturalBlocks.MushroomSet> set, @Nullable Integer radius) {
			this.id = id;
			this.cfKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc(id));
			this.factory = factory;
			this.set = set;
			this.radius = radius == null ? 0 : radius;
		}
	}

}
