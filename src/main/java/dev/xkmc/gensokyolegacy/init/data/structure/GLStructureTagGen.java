package dev.xkmc.gensokyolegacy.init.data.structure;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLDecoBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The tags on the structure registry, and the block tags the structure material generator reads.
 * <p>
 * The biome tags that say where a structure may spawn are {@code GLBiomeGen}'s, not this file's: a branch adding a
 * biome and a branch adding a structure each have one file to open. {@code GLStructureGen} is the only thing that
 * reads both halves.
 */
public class GLStructureTagGen {

	/** Tags on the structure registry itself, as opposed to the biome tags which say where a structure
	 *  may spawn. These are what {@code ServerLevel#findNearestMapStructure} takes, so "point me at Reimu's
	 *  shrine" style lookups are written against them. */
	public static final ProviderType<RegistrateTagsProvider.Impl<Structure>> STRUCTURE_TAG =
			ProviderType.registerDynamicTag("structure", "structure", Registries.STRUCTURE);

	public static final TagKey<Structure> CIRNO_NEST_STRUCTURE = structureTag("cirno_nest");
	public static final TagKey<Structure> HAKUREI_SHRINE_STRUCTURE = structureTag("hakurei_shrine");
	public static final TagKey<Structure> MARISA_HOUSE_STRUCTURE = structureTag("marisa_house");
	public static final TagKey<Structure> MORICHIKA_SHOP_STRUCTURE = structureTag("morichika_shop");
	public static final TagKey<Structure> ALICE_HOUSE_STRUCTURE = structureTag("alice_house");

	/** Every character home at once, so a lookup answers "which of them is closest" (what the catch-all flower in
	 *  {@code FairyDustItem} wants). <p> A datapack can widen any of these tags, so the one a given flower
	 *  points at is data, not code: it reaches further afield without a code change. */
	public static final TagKey<Structure> CHARACTER_HOMES = structureTag("character_homes");

	public static final TagKey<Block> CIRNO_PRIMARY = blockTag("structure_fix/cirno_nest/primary");
	public static final TagKey<Block> CIRNO_FIX = blockTag("structure_fix/cirno_nest/would_fix");
	public static final TagKey<Block> REIMU_PRIMARY = blockTag("structure_fix/hakurei_shrine/primary");
	public static final TagKey<Block> REIMU_FIX = blockTag("structure_fix/hakurei_shrine/would_fix");
	public static final TagKey<Block> MARISA_PRIMARY = blockTag("structure_fix/marisa_house/primary");
	public static final TagKey<Block> MARISA_FIX = blockTag("structure_fix/marisa_house/would_fix");
	public static final TagKey<Block> MORICHIKA_PRIMARY = blockTag("structure_fix/morichika_shop/primary");
	public static final TagKey<Block> MORICHIKA_FIX = blockTag("structure_fix/morichika_shop/would_fix");
	public static final TagKey<Block> ALICE_PRIMARY = blockTag("structure_fix/alice_house/primary");
	public static final TagKey<Block> ALICE_FIX = blockTag("structure_fix/alice_house/would_fix");

	public static TagKey<Structure> structureTag(String name) {
		return TagKey.create(Registries.STRUCTURE, GensokyoLegacy.loc(name));
	}

	public static TagKey<Block> blockTag(String name) {
		return BlockTags.create(GensokyoLegacy.loc(name));
	}

	public static void genStructureTag(RegistrateTagsProvider.Impl<Structure> pvd) {
		pvd.addTag(CIRNO_NEST_STRUCTURE).add(structureKey(GLStructureGen.CIRNO_NEST));
		pvd.addTag(HAKUREI_SHRINE_STRUCTURE).add(structureKey(GLStructureGen.HAKUREI_SHRINE));
		pvd.addTag(MARISA_HOUSE_STRUCTURE).add(structureKey(GLStructureGen.MARISA_HOUSE));
		pvd.addTag(MORICHIKA_SHOP_STRUCTURE).add(structureKey(GLStructureGen.MORICHIKA_SHOP));
		pvd.addTag(ALICE_HOUSE_STRUCTURE).add(structureKey(GLStructureGen.ALICE_HOUSE));
		pvd.addTag(CHARACTER_HOMES)
				.addTag(CIRNO_NEST_STRUCTURE)
				.addTag(HAKUREI_SHRINE_STRUCTURE)
				.addTag(MARISA_HOUSE_STRUCTURE)
				.addTag(MORICHIKA_SHOP_STRUCTURE)
				.addTag(ALICE_HOUSE_STRUCTURE);
	}

	private static ResourceKey<Structure> structureKey(ResourceLocation id) {
		return ResourceKey.create(Registries.STRUCTURE, id);
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
				GLFurniture.DONATION_BOX.get()
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

		// Alice's house in the magical forest (root 29x23x18): quartz/terracotta
		// shell with the west tower, blue-tiled gable roofs and the iron awnings
		pvd.addTag(ALICE_PRIMARY).add(
				Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.SMOOTH_QUARTZ_STAIRS,
				Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_TRAPDOOR, Blocks.SPRUCE_SLAB,
				Blocks.STRIPPED_SPRUCE_LOG,
				Blocks.BLACK_TERRACOTTA, Blocks.CALCITE, Blocks.DIORITE,
				Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE,
				Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.PACKED_MUD,
				Blocks.WHITE_STAINED_GLASS_PANE,
				Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_TRAPDOOR
		);
		pvd.addTag(ALICE_PRIMARY).addOptional(GensokyoLegacy.loc("blue_tiles"));
		pvd.addTag(ALICE_PRIMARY).addOptional(GensokyoLegacy.loc("blue_tiles_slab"));
		pvd.addTag(ALICE_PRIMARY).addOptional(GensokyoLegacy.loc("blue_tiles_stairs"));
		pvd.addTag(ALICE_PRIMARY).addOptional(GensokyoLegacy.loc("black_tiles_slab"));
		pvd.addTag(ALICE_PRIMARY).addOptional(GensokyoLegacy.loc("wrought_iron_pillar"));
		pvd.addTag(ALICE_PRIMARY).addOptional(GensokyoLegacy.loc("wrought_iron_bars"));
		pvd.addTag(ALICE_PRIMARY).addOptional(GensokyoLegacy.loc("white_long_noren"));
		pvd.addTag(ALICE_FIX).add(
				Blocks.BOOKSHELF, Blocks.LANTERN, Blocks.CAMPFIRE
		);
		pvd.addTag(ALICE_FIX).add(
				GLFurniture.BOOK_SHELF.get(), GLFurniture.DOOR_CABINET.get(),
				GLFurniture.DRAWER_CABINET.get()
		);
		pvd.addTag(ALICE_FIX).addOptional(GensokyoLegacy.loc("dark_oak_large_table"));
		pvd.addTag(ALICE_FIX).addOptional(GensokyoLegacy.loc("candlestick"));

		// Cirno's nest in the snowy biomes (13x8x13): ice brick dome + its furniture
		pvd.addTag(CIRNO_PRIMARY).add(
				Blocks.PACKED_ICE, Blocks.SNOW_BLOCK,
				// the door leaf is shell, not furniture: it is the one block that keeps the chamber sealed,
				// so a fix pass that cleared it to air would let the snow in and undo the nest
				Blocks.SPRUCE_DOOR
		);
		pvd.addTag(CIRNO_PRIMARY).addOptional(GensokyoLegacy.loc("ice_bricks"));
		// snow layers, grass, dirt, glow lichen, cave vines and the spore blossom are
		// in neither tag: the fix pass clears them out rather than rebuilds them
		pvd.addTag(CIRNO_FIX).add(
				Blocks.BARREL, Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT
		);
		pvd.addTag(CIRNO_FIX).add(GLDecoBlocks.STURDY_TEDDY_BEAR.get());
		pvd.addTag(CIRNO_FIX).addOptional(GensokyoLegacy.loc("ice_brick_slab"));
		pvd.addTag(CIRNO_FIX).addOptional(GensokyoLegacy.loc("ice_brick_stairs"));
		pvd.addTag(CIRNO_FIX).addOptional(GensokyoLegacy.loc("spruce_large_table"));
		pvd.addTag(CIRNO_FIX).addOptional(GensokyoLegacy.loc("spruce_chair"));
	}

}
