package dev.xkmc.gensokyolegacy.init.data.structure;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.GLBiomes;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class GLStructureTagGen {

	public static final ProviderType<RegistrateTagsProvider.Impl<Biome>> BIOME_TAG =
			ProviderType.registerDynamicTag("biome", "biome", Registries.BIOME);

	public static final TagKey<Biome> CIRNO_NEST = biomeTag("has_structure/cirno_nest");
	public static final TagKey<Biome> HAKUREI_SHRINE = biomeTag("has_structure/hakurei_shrine");
	public static final TagKey<Biome> MARISA_HOUSE = biomeTag("has_structure/marisa_house");
	public static final TagKey<Biome> MORICHIKA_SHOP = biomeTag("has_structure/morichika_shop");

	public static final TagKey<Block> CIRNO_PRIMARY = blockTag("structure_fix/cirno_nest/primary");
	public static final TagKey<Block> CIRNO_FIX = blockTag("structure_fix/cirno_nest/would_fix");
	public static final TagKey<Block> REIMU_PRIMARY = blockTag("structure_fix/hakurei_shrine/primary");
	public static final TagKey<Block> REIMU_FIX = blockTag("structure_fix/hakurei_shrine/would_fix");
	public static final TagKey<Block> MARISA_PRIMARY = blockTag("structure_fix/marisa_house/primary");
	public static final TagKey<Block> MARISA_FIX = blockTag("structure_fix/marisa_house/would_fix");
	public static final TagKey<Block> MORICHIKA_PRIMARY = blockTag("structure_fix/morichika_shop/primary");
	public static final TagKey<Block> MORICHIKA_FIX = blockTag("structure_fix/morichika_shop/would_fix");

	public static TagKey<Biome> biomeTag(String name) {
		return TagKey.create(Registries.BIOME, GensokyoLegacy.loc(name));
	}

	public static TagKey<Block> blockTag(String name) {
		return BlockTags.create(GensokyoLegacy.loc(name));
	}

	public static void genBiomeTag(RegistrateTagsProvider.Impl<Biome> pvd) {
		pvd.addTag(CIRNO_NEST)
				.add(Biomes.SNOWY_PLAINS)
				.add(Biomes.ICE_SPIKES)
				.add(Biomes.FROZEN_OCEAN)
				.add(Biomes.DEEP_FROZEN_OCEAN)
				.add(Biomes.GROVE)
				.add(Biomes.FROZEN_RIVER)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.SNOWY_BEACH);
		pvd.addTag(HAKUREI_SHRINE)
				.add(Biomes.CHERRY_GROVE)
				.add(GLBiomes.SAKURA_FOREST);
		pvd.addTag(MARISA_HOUSE)
				.add(GLBiomes.MAGICAL_FOREST);
		pvd.addTag(MORICHIKA_SHOP)
				.add(GLBiomes.MAGICAL_FOREST);
		pvd.addTag(Tags.Biomes.IS_MAGICAL).add(GLBiomes.MAGICAL_FOREST).add(GLBiomes.SAKURA_FOREST);
		pvd.addTag(Tags.Biomes.IS_FOREST).add(GLBiomes.MAGICAL_FOREST).add(GLBiomes.SAKURA_FOREST);
	}

	@SuppressWarnings({"unchecked"})
	public static void genBlockTag(RegistrateTagsProvider.IntrinsicImpl<Block> pvd) {
		// Marisa's house in the magical forest (30x13x25): witch hut shell + magic study furniture
		pvd.addTag(MARISA_PRIMARY).add(
				Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS,
				Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
				Blocks.BIRCH_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.OAK_SLAB,
				Blocks.COBBLESTONE, Blocks.COBBLESTONE_SLAB, Blocks.COBBLESTONE_WALL,
				Blocks.STONE, Blocks.STONE_SLAB, Blocks.POLISHED_ANDESITE,
				Blocks.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_ANDESITE_SLAB, Blocks.ANDESITE,
				Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES,
				Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_SLAB,
				Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB,
				Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.SANDSTONE,
				Blocks.DARK_OAK_DOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
				Blocks.SPRUCE_FENCE
		);
		pvd.addTag(MARISA_FIX).add(
				Blocks.CHEST, Blocks.BARREL, Blocks.BOOKSHELF, Blocks.CHISELED_BOOKSHELF,
				Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.BREWING_STAND,
				Blocks.ENCHANTING_TABLE, Blocks.LECTERN, Blocks.LOOM, Blocks.CARTOGRAPHY_TABLE,
				Blocks.SMITHING_TABLE, Blocks.CAULDRON, Blocks.LANTERN, Blocks.SOUL_LANTERN,
				Blocks.CAMPFIRE
		);
		pvd.addTag(MARISA_FIX).add(
				GLFurniture.DOOR_CABINET.get(), GLFurniture.BOOK_SHELF.get(),
				GLFurniture.BOOK_PILE.get(), GLFurniture.BOOK_STACK.get(), GLFurniture.CRATE.get()
		);
		pvd.addTag(MARISA_FIX).addOptional(GensokyoLegacy.loc("spruce_large_table"));
		pvd.addTag(MARISA_FIX).addOptional(GensokyoLegacy.loc("dark_oak_large_table"));
		pvd.addTag(MARISA_FIX).addOptional(GensokyoLegacy.loc("dark_oak_large_chair"));

		// Hakurei shrine in the sakura forest (40x15x45): shrine shell + shrine furniture
		pvd.addTag(REIMU_PRIMARY).add(
				Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_LOG,
				Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_MANGROVE_LOG,
				Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS,
				Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
				Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB,
				Blocks.OAK_STAIRS, Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB,
				Blocks.STONE, Blocks.STONE_BRICKS, Blocks.STONE_STAIRS, Blocks.STONE_SLAB,
				Blocks.POLISHED_ANDESITE, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_ANDESITE_SLAB,
				Blocks.ANDESITE_STAIRS, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE_SLAB,
				Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_QUARTZ_STAIRS,
				Blocks.COBBLESTONE, Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_STONE_BRICKS,
				Blocks.PACKED_MUD, Blocks.CALCITE, Blocks.TUFF, Blocks.RED_TERRACOTTA,
				Blocks.SPRUCE_DOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
				Blocks.OAK_TRAPDOOR, Blocks.MANGROVE_TRAPDOOR,
				Blocks.SPRUCE_FENCE, Blocks.SPRUCE_FENCE_GATE, Blocks.STONE_BRICK_WALL
		);
		pvd.addTag(REIMU_PRIMARY).addOptional(GensokyoLegacy.loc("gray_tiles"));
		pvd.addTag(REIMU_PRIMARY).addOptional(GensokyoLegacy.loc("gray_tiles_slab"));
		pvd.addTag(REIMU_PRIMARY).addOptional(GensokyoLegacy.loc("gray_tiles_stairs"));
		pvd.addTag(REIMU_PRIMARY).addOptional(GensokyoLegacy.loc("black_tiles_slab"));
		pvd.addTag(REIMU_PRIMARY).addOptional(GensokyoLegacy.loc("black_tiles_stairs"));
		pvd.addTag(REIMU_PRIMARY).addOptional(GensokyoLegacy.loc("brown_tiles_stairs"));
		pvd.addTag(REIMU_PRIMARY).addOptional(GensokyoLegacy.loc("oak_sliding_door"));
		pvd.addTag(REIMU_PRIMARY).addOptional(GensokyoLegacy.loc("dark_oak_plank_wall"));
		pvd.addTag(REIMU_FIX).add(
				Blocks.CHEST, Blocks.BARREL, Blocks.ENDER_CHEST, Blocks.BOOKSHELF,
				Blocks.BREWING_STAND, Blocks.FURNACE, Blocks.SMOKER, Blocks.CAMPFIRE,
				Blocks.CRAFTING_TABLE, Blocks.BELL, Blocks.LANTERN,
				Blocks.COMPOSTER, Blocks.BEEHIVE
		);
		pvd.addTag(REIMU_FIX).add(
				GLFurniture.DRAWER_CABINET.get(), GLFurniture.DOOR_CABINET.get(),
				GLFurniture.CRATE.get(), GLFurniture.BOOK_PILE.get(), GLFurniture.BOOK_STACK.get(),
				GLFurniture.DONATION_BOX_2.get()
		);
		pvd.addTag(REIMU_FIX).addOptional(GensokyoLegacy.loc("tatami_block"));
		pvd.addTag(REIMU_FIX).addOptional(GensokyoLegacy.loc("tea_table"));
		pvd.addTag(REIMU_FIX).addOptional(GensokyoLegacy.loc("red_cushion"));
		pvd.addTag(REIMU_FIX).addOptional(GensokyoLegacy.loc("spruce_large_table"));
		pvd.addTag(REIMU_FIX).addOptional(GensokyoLegacy.loc("spruce_large_chair"));
		pvd.addTag(REIMU_FIX).addOptional(GensokyoLegacy.loc("dark_oak_large_table"));

		// Kourindou (Morichika's shop) in the magical forest (33x18x33): shop shell + goods
		pvd.addTag(MORICHIKA_PRIMARY).add(
				Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_LOG,
				Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS,
				Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
				Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB,
				Blocks.POLISHED_DIORITE, Blocks.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE_STAIRS,
				Blocks.DIORITE, Blocks.TUFF, Blocks.PACKED_MUD, Blocks.CALCITE,
				Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA, Blocks.COBBLESTONE,
				Blocks.ACACIA_DOOR, Blocks.JUNGLE_DOOR, Blocks.SPRUCE_DOOR,
				Blocks.ACACIA_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR,
				Blocks.SPRUCE_FENCE, Blocks.DARK_OAK_FENCE, Blocks.GLASS_PANE
		);
		pvd.addTag(MORICHIKA_PRIMARY).addOptional(GensokyoLegacy.loc("orange_tiles"));
		pvd.addTag(MORICHIKA_PRIMARY).addOptional(GensokyoLegacy.loc("orange_tiles_slab"));
		pvd.addTag(MORICHIKA_PRIMARY).addOptional(GensokyoLegacy.loc("orange_tiles_stairs"));
		pvd.addTag(MORICHIKA_PRIMARY).addOptional(GensokyoLegacy.loc("black_tiles_stairs"));
		pvd.addTag(MORICHIKA_FIX).add(
				Blocks.CHEST, Blocks.BARREL, Blocks.ENDER_CHEST, Blocks.BOOKSHELF,
				Blocks.CHISELED_BOOKSHELF, Blocks.CRAFTING_TABLE, Blocks.ENCHANTING_TABLE,
				Blocks.BREWING_STAND, Blocks.GRINDSTONE, Blocks.STONECUTTER, Blocks.LOOM,
				Blocks.LECTERN, Blocks.JUKEBOX, Blocks.CARTOGRAPHY_TABLE, Blocks.SMITHING_TABLE,
				Blocks.CAULDRON, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CAMPFIRE,
				Blocks.BELL, Blocks.NOTE_BLOCK, Blocks.REDSTONE_LAMP,
				Blocks.LIGHTNING_ROD, Blocks.LODESTONE, Blocks.DECORATED_POT, Blocks.FLOWER_POT
		);
		pvd.addTag(MORICHIKA_FIX).add(
				GLFurniture.SHELF.get(), GLFurniture.CRATE.get(),
				GLFurniture.CARTON.get(), GLFurniture.CARTON_WHITE.get(), GLFurniture.CARTON_BLUE.get()
		);
		pvd.addTag(MORICHIKA_FIX).addOptional(GensokyoLegacy.loc("tatami_block"));
		pvd.addTag(MORICHIKA_FIX).addOptional(GensokyoLegacy.loc("spruce_large_table"));
		pvd.addTag(MORICHIKA_FIX).addOptional(GensokyoLegacy.loc("white_cushion"));
	}

}
