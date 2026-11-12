package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.MagicalForestBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.MyourenTempleBarrensBiomeGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/**
 * The biome modifiers: the only thing in the mod that adds a mob to a world, and the reason no biome has to carry a
 * spawn table of its own.
 * <p>
 * A modifier is scoped to a biome tag rather than a dimension, so it also reaches the vanilla overworld's forests and
 * plains — which is why the tag nests both families of tags. See {@code GLBiomeGen#PLAIN_FAIRY_SPAWN}.
 */
public class GLBiomeModifierGen {

	public static void init(DataProviderInitializer init) {
		init.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
			ctx.register(biomeMod("plain_fairy"), new BiomeModifiers.AddSpawnsBiomeModifier(
					ctx.lookup(Registries.BIOME).getOrThrow(GLBiomeGen.PLAIN_FAIRY_SPAWN),
					List.of(new MobSpawnSettings.SpawnerData(GLEntities.PLAIN_FAIRY.get(), 10, 3, 3))
			));
			ctx.register(biomeMod("stray_doll"), new BiomeModifiers.AddSpawnsBiomeModifier(
					HolderSet.direct(ctx.lookup(Registries.BIOME).getOrThrow(MagicalForestBiomeGen.KEY)),
					List.of(new MobSpawnSettings.SpawnerData(GLEntities.STRAY_DOLL.get(), 10, 3, 5))
			));
			ctx.register(biomeMod("stray_ghost"), new BiomeModifiers.AddSpawnsBiomeModifier(
					HolderSet.direct(ctx.lookup(Registries.BIOME).getOrThrow(MyourenTempleBarrensBiomeGen.KEY)),
					List.of(new MobSpawnSettings.SpawnerData(GLEntities.STRAY_GHOST.get(), 10, 1, 1))
			));
		});
	}

	private static ResourceKey<BiomeModifier> biomeMod(String id) {
		return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GensokyoLegacy.loc(id));
	}

}