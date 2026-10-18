package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateItemTagsProvider;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLDecoBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;


public class GLTagGen {

	public static final TagKey<Item> CURRENCY = item("currency");
	public static final TagKey<Item> CUSHIONS = item("cushions");
	public static final TagKey<Item> NOREN = item("noren");
	public static final TagKey<Item> STRIPED_NOREN = item("striped_noren");
	public static final TagKey<Item> WAVY_NOREN = item("wavy_noren");
	public static final TagKey<Item> LONG_NOREN = item("long_noren");
	public static final TagKey<Item> CARTONS = item("cartons");
	public static final TagKey<Item> HUGE_MUSHROOM = item("huge_mushroom");

	public static final TagKey<Item> TOUHOU_HAT = item("touhou_hat");

	public static final TagKey<Item> TALISMAN = item("talisman");

	/** The talisman papers themselves, without the folded charm every one of them turns into. */
	public static final TagKey<Item> TALISMAN_PAPERS = item("talisman_papers");

	public static final TagKey<Item> MORICHIKA_OFFERS = item("morichika_offers");

	/** The player-facing spell card items, distinct from danmaku_api's own {@code preset_spell}. */
	public static final TagKey<Item> SPELL_CARD = item("spell_card");

	/** The inspection tools: the debug wands and glasses, the structure wand and the party wand. */
	public static final TagKey<Item> DEBUG_ITEMS = item("debug_items");

	/**
	 * Items the mod website must not list: {@link #DEBUG_ITEMS}, {@link #SPELL_CARD} and the two dowser halves,
	 * which exist only as the render halves of {@code dowser} and are never obtainable.
	 */
	public static final TagKey<Item> WEBSITE_HIDDEN = item("website_hidden");

	/**
	 * Mirrors of the furniture block tags below, under the same id. Consumers that never see the block
	 * registry, like the patchouli guide or another mod's item-only checks, need the item registry copy.
	 */
	public static final TagKey<Item> TABLE_ITEM = item("table");
	public static final TagKey<Item> CHAIR_ITEM = item("chair");
	public static final TagKey<Item> STOOL_ITEM = item("stool");
	public static final TagKey<Item> SLIDING_DOOR_ITEM = item("sliding_door");
	public static final TagKey<Item> PLANK_WALL_ITEM = item("plank_wall");
	public static final TagKey<Item> TILE_VARIANTS_ITEM = item("tile_variants");

	public static final TagKey<Block> VERTICAL_SLAB = block("vertical_slab");
	public static final TagKey<Block> TEMPLATE_TRUNK = block("template_trunk");

	/**
	 * Tables, all of them. {@link #LARGE_TABLE} covers the wooden dining tables only and drives how
	 * neighbouring tables merge, so it stays separate.
	 */
	public static final TagKey<Block> TABLE = block("table");
	public static final TagKey<Block> LARGE_TABLE = block("large_table");
	public static final TagKey<Block> CHAIR = block("chair");
	public static final TagKey<Block> STOOL = block("stool");

	/**
	 * Tables a party may be laid out on. Built from {@link #LARGE_TABLE} rather than
	 * {@link #TABLE}, which also carries {@code tea_table} and {@code magic_table}.
	 */
	public static final TagKey<Block> PARTY_TABLE = block("party_table");
	/** Seats a guest may take. Chairs are a hard requirement for a party. */
	public static final TagKey<Block> PARTY_SEAT = block("party_seat");

	public static final TagKey<Block> SLIDING_DOOR = block("sliding_door");
	public static final TagKey<Block> PLANK_WALL = block("plank_wall");

	/** Every tile block, plus the stairs, slab and vertical slab cut from it. */
	public static final TagKey<Block> TILE_VARIANTS = block("tile_variants");

	public static final TagKey<EntityType<?>> FLESH_SOURCE = entity("flesh_source");
	public static final TagKey<EntityType<?>> YOUKAI_IGNORE = entity("youkai_ignore");

	public static final TagKey<EntityType<?>> SKULL_SOURCE = entity("drops_skeleton_skull");
	public static final TagKey<EntityType<?>> WITHER_SOURCE = entity("drops_wither_skull");
	public static final TagKey<EntityType<?>> ZOMBIE_SOURCE = entity("drops_zombie_head");
	public static final TagKey<EntityType<?>> CREEPER_SOURCE = entity("drops_creeper_head");
	public static final TagKey<EntityType<?>> PIGLIN_SOURCE = entity("drops_piglin_head");

	public static final TagKey<EntityType<?>> UMBRELLA_CAPTURE_BLACKLIST = entity("umbrella_capture_blacklist");

	private static void addTiles(RegistrateTagsProvider.IntrinsicImpl<Block> pvd, TagKey<Block> tag) {
		for (var set : GLDecoBlocks.TILE_SETS) {
			pvd.addTag(tag).add(set.block.get(), set.stairs.get(), set.slab.get(), set.vertical.get());
		}
	}

	private static void addTiles(RegistrateItemTagsProvider pvd, TagKey<Item> tag) {
		for (var set : GLDecoBlocks.TILE_SETS) {
			pvd.addTag(tag).add(set.block.get().asItem(), set.stairs.get().asItem(),
					set.slab.get().asItem(), set.vertical.get().asItem());
		}
	}

	public static void onBlockTagGen(RegistrateTagsProvider.IntrinsicImpl<Block> pvd) {
		GLStructureTagGen.genBlockTag(pvd);
		// trunk blocks of vegetation templates: may sink into dirt and are extended down to the ground
		pvd.addTag(TEMPLATE_TRUNK).addTag(BlockTags.LOGS).add(Blocks.MUSHROOM_STEM,
				GLNaturalBlocks.CYAN_MUSHROOM_STEM.get(), GLNaturalBlocks.PURPLE_MUSHROOM_STEM.get(),
				GLNaturalBlocks.RED_MUSHROOM_STEM.get());
		addTiles(pvd, TILE_VARIANTS);
		genPartyTag(pvd);
	}

	/**
	 * The party table and seat tags. <p> Generated, not hand-written: our own tables and seats come from the tags they
	 * already belong to. Youkai's Feasts' dining tables and chairs go in as optional entries, which is what keeps that
	 * mod optional, and it uses the same 11 wood names we do. <p> There is deliberately no {@code party_food} tag. Food
	 * is decided entirely in code, by whether a handler recognises the block and finds something worth eating in it;
	 * the {@code party_food} data map only revalues such a block and cannot make one.
	 */
	private static void genPartyTag(RegistrateTagsProvider.IntrinsicImpl<Block> pvd) {
		pvd.addTag(PARTY_TABLE).addTag(LARGE_TABLE);
		pvd.addTag(PARTY_SEAT).addTag(CHAIR).addTag(STOOL);
		for (var wood : YSF_WOODS) {
			// ResourceLocation.parse, not GensokyoLegacy.loc: loc() namespaces a
			// bare path with our own modid, so "youkaisfeasts:x" would gain one.
			pvd.addTag(PARTY_TABLE).addOptional(ResourceLocation.parse("youkaisfeasts:" + wood + "_dining_table"));
			pvd.addTag(PARTY_SEAT).addOptional(ResourceLocation.parse("youkaisfeasts:" + wood + "_dining_chair"));
		}
	}

	/**
	 * The wood names {@code GLDecoBlocks.WoodType} covers, spelled out rather than read from it. <p> A tag provider
	 * runs while the registries are still being walked, and touching {@code GLDecoBlocks} from there class-initialises
	 * it, which registers blocks and mutates the provider list mid-iteration. Datagen does not fail loudly — it simply
	 * never returns. Keep this list in step with {@code WoodType} by hand.
	 */
	private static final String[] YSF_WOODS = {
			"oak", "birch", "spruce", "jungle", "dark_oak", "acacia",
			"crimson", "warped", "mangrove", "cherry", "bamboo"
	};

	public static void onItemTagGen(RegistrateItemTagsProvider pvd) {
		pvd.addTag(CURRENCY).add(Items.EMERALD, Items.GOLD_INGOT);
		pvd.addTag(HUGE_MUSHROOM)
				.add(Items.MUSHROOM_STEM, Items.BROWN_MUSHROOM_BLOCK, Items.RED_MUSHROOM_BLOCK);
		addTiles(pvd, TILE_VARIANTS_ITEM);
		// the two halves are tagged on the items themselves; everything else arrives by whole tags
		pvd.addTag(WEBSITE_HIDDEN).addTag(DEBUG_ITEMS).addTag(SPELL_CARD);
	}

	public static void onEntityTagGen(RegistrateTagsProvider.IntrinsicImpl<EntityType<?>> pvd) {
		pvd.addTag(FLESH_SOURCE).add(EntityType.EVOKER, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.ILLUSIONER, EntityType.WITCH,
				EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.PLAYER);

		pvd.addTag(SKULL_SOURCE).add(EntityType.SKELETON, EntityType.STRAY);
		pvd.addTag(WITHER_SOURCE).add(EntityType.WITHER_SKELETON, EntityType.WITHER);
		pvd.addTag(ZOMBIE_SOURCE).add(EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED);
		pvd.addTag(CREEPER_SOURCE).add(EntityType.CREEPER);
		pvd.addTag(PIGLIN_SOURCE).add(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE);

		pvd.addTag(YOUKAI_IGNORE).add(EntityType.ENDER_DRAGON);

		pvd.addTag(UMBRELLA_CAPTURE_BLACKLIST).addTag(Tags.EntityTypes.BOSSES).add(EntityType.WARDEN);
	}

	public static TagKey<Item> item(String id) {
		return ItemTags.create(GensokyoLegacy.loc(id));
	}

	public static TagKey<Block> block(String id) {
		return BlockTags.create(GensokyoLegacy.loc(id));
	}

	public static TagKey<EntityType<?>> entity(String id) {
		return TagKey.create(Registries.ENTITY_TYPE, GensokyoLegacy.loc(id));
	}

}
