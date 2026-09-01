package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import dev.xkmc.gensokyolegacy.content.block.deco.*;
import dev.xkmc.gensokyolegacy.content.block.deco.IBlockSet;
import dev.xkmc.gensokyolegacy.content.block.deco.LargeTableBlock;
import dev.xkmc.gensokyolegacy.content.block.deco.TableClothImpl;
import dev.xkmc.gensokyolegacy.content.block.deco.VerticalSlabBlock;
import dev.xkmc.gensokyolegacy.content.block.misc.BookPile;
import dev.xkmc.gensokyolegacy.content.block.misc.BookStack;
import dev.xkmc.gensokyolegacy.content.block.nature.CedarFallenLeavesBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.EvergreenVineBodyBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.EvergreenVineHeadBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.WaterloggedCrossBlock;
import dev.xkmc.gensokyolegacy.content.block.deco.WoodTableBlock;
import dev.xkmc.gensokyolegacy.content.block.door.SlidingDoor;
import dev.xkmc.gensokyolegacy.content.block.door.SlidingDoorJsons;
import dev.xkmc.gensokyolegacy.content.block.misc.TatamiBlock;
import dev.xkmc.gensokyolegacy.content.block.seat.CushionBlock;
import dev.xkmc.gensokyolegacy.content.block.seat.WoodChairBlock;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftType;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.MushroomFeatures.MushroomTreeType;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLRecipeGen;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class GLDecoBlocks {

	public enum WoodType implements IBlockSet {
		OAK(Blocks.OAK_PLANKS, Blocks.OAK_FENCE, Items.STRIPPED_OAK_WOOD, Blocks.OAK_SLAB, Blocks.OAK_STAIRS),
		BIRCH(Blocks.BIRCH_PLANKS, Blocks.BIRCH_FENCE, Items.STRIPPED_BIRCH_WOOD, Blocks.BIRCH_SLAB, Blocks.BRICK_STAIRS),
		SPRUCE(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_FENCE, Items.STRIPPED_SPRUCE_WOOD, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS),
		JUNGLE(Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_FENCE, Items.STRIPPED_JUNGLE_WOOD, Blocks.JUNGLE_SLAB, Blocks.JUNGLE_STAIRS),
		DARK_OAK(Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE, Items.STRIPPED_DARK_OAK_WOOD, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_STAIRS),
		ACACIA(Blocks.ACACIA_PLANKS, Blocks.ACACIA_FENCE, Items.STRIPPED_ACACIA_WOOD, Blocks.ACACIA_SLAB, Blocks.ACACIA_STAIRS),
		CRIMSON(Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_FENCE, Items.STRIPPED_CRIMSON_HYPHAE, Blocks.CRIMSON_SLAB, Blocks.CRIMSON_STAIRS),
		WARPED(Blocks.WARPED_PLANKS, Blocks.WARPED_FENCE, Items.STRIPPED_WARPED_HYPHAE, Blocks.WARPED_SLAB, Blocks.WARPED_STAIRS),
		MANGROVE(Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_FENCE, Items.STRIPPED_MANGROVE_WOOD, Blocks.MANGROVE_SLAB, Blocks.MANGROVE_STAIRS),
		CHERRY(Blocks.CHERRY_PLANKS, Blocks.CHERRY_FENCE, Items.STRIPPED_CHERRY_WOOD, Blocks.CHERRY_SLAB, Blocks.CHERRY_STAIRS),
		BAMBOO(Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_FENCE, Items.STRIPPED_BAMBOO_BLOCK, Blocks.BAMBOO_SLAB, Blocks.BAMBOO_STAIRS),
		;

		private final Block plankProp, fenceProp, slab, stairs;
		private final String name;
		private final ResourceLocation tex;
		public final ItemLike plank, strippedWood;
		public BlockEntry<DelegateBlock> table;
		public BlockEntry<WoodChairBlock> seat;
		public BlockEntry<VerticalSlabBlock> vertical;
		public BlockEntry<Block> wall;
		public BlockEntry<DelegateBlock> door;

		WoodType(Block plankProp, Block fenceProp, ItemLike strippedWood, Block slab, Block stairs) {
			this.plankProp = plankProp;
			this.fenceProp = fenceProp;
			this.plank = plankProp;
			this.strippedWood = strippedWood;
			this.slab = slab;
			this.stairs = stairs;
			name = name().toLowerCase(Locale.ROOT);
			tex = ResourceLocation.withDefaultNamespace("block/" + name + "_planks");
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public BlockBehaviour.Properties prop() {
			return BlockBehaviour.Properties.ofFullCopy(plankProp);
		}

		@Override
		public Holder<Block> base() {
			return plankProp.builtInRegistryHolder();
		}

		@Override
		public Holder<Block> stairs() {
			return stairs.builtInRegistryHolder();
		}

		@Override
		public Holder<Block> slab() {
			return slab.builtInRegistryHolder();
		}

		@Override
		public Holder<Block> vertical() {
			return vertical;
		}

		@Override
		public ResourceLocation top() {
			return tex;
		}

		@Override
		public ResourceLocation side() {
			return tex;
		}

	}

	public static final SimpleEntry<CreativeModeTab> TAB;

	public static final BrickSet PACKED_ICE_SET, SNOW_SET, ICE_BRICK_SET, SNOW_BRICK_SET;

	public static final StoneAndBrickSet DARKSTONE;

	public static final BlockEntry<DelegateBlock> TATAMI, TATAMI_BLOCK, TEDDY_BEAR;

	public static final BlockEntry<Block> GLASS;
	public static final BlockEntry<IronBarsBlock> GLASS_PANE;
	public static final BlockEntry<Block> CRATE;


	public static final BlockEntry<DelegateBlock> BOOK_PILE, BOOK_STACK;

	static {
		var reg = GensokyoLegacy.REGISTRATE;
		TAB = reg.buildModCreativeTab("building_blocks", "Gensokyo Legacy - Building Blocks",
				e -> e.icon(() -> GLDecoBlocks.ICE_BRICK_SET.block.get().asItem().getDefaultInstance()));

		TATAMI = reg.block("tatami", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new TatamiBlock(), new TatamiBlock.Carpet()))
				.properties(p -> p.mapColor(MapColor.SAND).strength(0).sound(SoundType.WOOL))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(), state -> {
					var kind = state.getValue(TatamiBlock.KIND);
					var suffix = kind == TatamiBlock.Kind.SQUARE ? "" : "_" + kind.getSerializedName();
					return pvd.models().carpet(ctx.getName() + suffix, pvd.modLoc("block/tatami/tatami" + suffix));
				}))
				.simpleItem()
				.register();
		TATAMI_BLOCK = reg.block("tatami_block", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new TatamiBlock()))
				.properties(p -> p.mapColor(MapColor.SAND).strength(0.2f).sound(SoundType.WOOL))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(), state -> {
					var kind = state.getValue(TatamiBlock.KIND);
					var suffix = kind == TatamiBlock.Kind.SQUARE ? "" : "_" + kind.getSerializedName();
					return pvd.models().cubeTop(ctx.getName() + suffix, pvd.modLoc("block/tatami/tatami"), pvd.modLoc("block/tatami/tatami" + suffix));
				}))
				.simpleItem()
				.register();

		BOOK_PILE = GensokyoLegacy.REGISTRATE.block("book_pile", BookPile::create)
				.properties(p -> p.noOcclusion().strength(0F).offsetType(BlockBehaviour.OffsetType.XZ)
						.mapColor(MapColor.WOOD).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY).dynamicShape())
				.blockstate(BookPile::buildStates).loot(BookPile::buildLoot).simpleItem()
				.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
						RecipeCategory.DECORATIONS, ctx.get(), 1)::unlockedBy, Items.BOOK).requires(Items.BOOK, 5).save(pvd))
				.register();

		BOOK_STACK = GensokyoLegacy.REGISTRATE.block("book_stack", BookStack::create)
				.properties(p -> p.noOcclusion().strength(0F).offsetType(BlockBehaviour.OffsetType.XZ)
						.mapColor(MapColor.WOOD).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY).dynamicShape())
				.blockstate(BookStack::buildStates).loot(BookStack::buildLoot).simpleItem()
				.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
						RecipeCategory.DECORATIONS, ctx.get(), 1)::unlockedBy, Items.BOOK).requires(Items.BOOK, 5).save(pvd))
				.register();



		// 纸窗方块
		GLASS = reg.block("paper_window", Block::new)
				.properties(p -> p.mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.WOOD).noOcclusion().noLootTable())
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
						pvd.models().cubeAll(ctx.getName(),
								pvd.modLoc("block/deco/paper_window"))))
				.loot((pvd, block) -> pvd.add(block, LootTable.lootTable()))
				.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.simpleItem()
				.register();

		// 纸窗板
		GLASS_PANE = reg.block("paper_window_pane", IronBarsBlock::new)
				.properties(p -> p.mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.WOOD).noOcclusion().noLootTable())
				.blockstate((ctx, pvd) -> pvd.paneBlock(ctx.get(),
						pvd.modLoc("block/deco/paper_window"),
						pvd.modLoc("block/deco/paper_window")))
				.loot((pvd, block) -> pvd.add(block, LootTable.lootTable()))
				.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.item().model((ctx, pvd) -> pvd.withExistingParent(ctx.getName(), "item/generated")
						.texture("layer0", pvd.modLoc("block/deco/paper_window")))
				.build()
				.register();

		reg.block("cushion", CushionBlock::new)
				.properties(p -> p.mapColor(MapColor.SAND).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).noOcclusion().noCollission())
				.blockstate(CushionBlock::buildStates)
				.item().tag(GLTagGen.CUSHIONS).build()
				.register();

		for (DyeColor col : DyeColor.values()) {
			reg.block(col.getName() + "_cushion", CushionBlock::new)
					.properties(p -> p.mapColor(MapColor.byId(14 + col.getId())).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).noOcclusion().noCollission())
					.blockstate(CushionBlock::buildStates)
					.item().tag(GLTagGen.CUSHIONS).build()
					.register();
		}

		TEDDY_BEAR = reg.block("teddy_bear",
						p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL))
				.initialProperties(() -> Blocks.WHITE_WOOL)
				.properties(p -> p.noOcclusion().strength(0.8F))
				.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
						pvd.models().getBuilder("block/" + ctx.getName())
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/teddy_bear")))
								.renderType("cutout")))
				.item()
				.properties(p -> p.stacksTo(1).rarity(Rarity.RARE))
				.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/gift/" + ctx.getName())))
				.dataMap(GLMeta.GIFT_DATA.reg(), new GiftItemData(5, 1000, GiftType.TOY))
				.tab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, GensokyoLegacy.loc("ingredients")))
				.build()
				.register();

		for (var e : WoodType.values()) {
			String name = e.name().toLowerCase(Locale.ROOT);

			e.table = reg.block(name + "_dining_table", p -> DelegateBlock.newBaseBlock(p, new WoodTableBlock(), new TableClothImpl()))
					.initialProperties(() -> e.plankProp)
					.blockstate(WoodTableBlock::buildStates)
					.simpleItem().tag(BlockTags.MINEABLE_WITH_AXE)
					.recipe((ctx, pvd) -> WoodTableBlock.genRecipe(pvd, e, ctx))
					.loot(WoodTableBlock::genLoot)
					.register();

			e.seat = reg.block(name + "_dining_chair", p -> new WoodChairBlock(
							BlockBehaviour.Properties.ofFullCopy(e.plankProp)))
					.blockstate(WoodChairBlock::buildStates)
					.simpleItem().tag(BlockTags.MINEABLE_WITH_AXE)
					.recipe((ctx, pvd) -> WoodChairBlock.genRecipe(pvd, e, ctx))
					.register();

			reg.block(name + "_large_table", p -> DelegateBlock.newBaseBlock(p, new LargeTableBlock(), new TableClothImpl()))
					.initialProperties(() -> e.plankProp)
					.blockstate(LargeTableBlock::buildStates)
					.tag(GLTagGen.LARGE_TABLE)
					.simpleItem().tag(BlockTags.MINEABLE_WITH_AXE)
					//.recipe((ctx, pvd) -> WoodTableBlock.genRecipe(pvd, e, ctx))
					.loot(LargeTableBlock::genLoot)
					.register();

			e.wall = reg.block(name + "_plank_wall", Block::new)
					.initialProperties(() -> e.plankProp)
					.blockstate((ctx, pvd) -> {
						var wallTop = pvd.modLoc("block/wood/" + name + "_plank_wall_top");
						pvd.simpleBlock(ctx.get(), pvd.models().cubeColumn(ctx.getName(),
								pvd.modLoc("block/wood/" + name + "_plank_wall"),
                                pvd.modLoc("block/wood/" + name + "_plank_wall_top")));
					})
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.simpleItem()
					.register();

			var doorTop = GensokyoLegacy.loc("block/wood/" + name + "_sliding_door_top");
			var doorBottom = GensokyoLegacy.loc("block/wood/" + name + "_sliding_door_bottom");
			var doorSide = GensokyoLegacy.loc("block/wood/" + name + "_sliding_door_side");
			e.door = reg.block(name + "_sliding_door", p -> SlidingDoor.create(p))
					.initialProperties(() -> e.plankProp)
					.blockstate((ctx, pvd) -> SlidingDoorJsons.buildBlockState(ctx, pvd, doorTop, doorBottom, doorSide))
					.tag(GLTagGen.SLIDING_DOOR)
					.item().model((ctx, pvd) -> SlidingDoorJsons.genItemModel(ctx, pvd, doorTop, doorBottom, doorSide))
					.build()
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.loot(SlidingDoorJsons::genLoot)
					.register();
		}


		SNOW_SET = new BrickSet(reg, "snow", BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW_BLOCK),
				ResourceLocation.withDefaultNamespace("block/snow"), () -> Blocks.SNOW_BLOCK,
				BlockTags.MINEABLE_WITH_SHOVEL);
		SNOW_BRICK_SET = new BrickSet(reg, "snow", BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
				.requiresCorrectToolForDrops().strength(0.2F).sound(SoundType.SNOW),
				(ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())::unlockedBy, Items.SNOW_BLOCK)
						.pattern("XX").pattern("XX").define('X', Items.SNOW_BLOCK).save(pvd));

		PACKED_ICE_SET = new BrickSet(reg, "packed_ice", BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_ICE),
				ResourceLocation.withDefaultNamespace("block/packed_ice"), () -> Blocks.PACKED_ICE,
				BlockTags.MINEABLE_WITH_PICKAXE);

		ICE_BRICK_SET = new BrickSet(reg, "ice", BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
				.instrument(NoteBlockInstrument.CHIME)
				.requiresCorrectToolForDrops().strength(0.5F).sound(SoundType.GLASS),
				(ctx, pvd) -> pvd.stonecutting(DataIngredient.items(Blocks.PACKED_ICE), RecipeCategory.BUILDING_BLOCKS, ctx));

		DARKSTONE = new StoneAndBrickSet(reg, "darkstone", MapColor.COLOR_BLACK, 1F,
				SoundType.DEEPSLATE, SoundType.DEEPSLATE_BRICKS);

		var tiles = new DyeColor[]{DyeColor.CYAN, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.BROWN, DyeColor.BLUE, DyeColor.BLACK, DyeColor.GRAY};
		var strips = new DyeColor[]{DyeColor.BLUE};

		for (DyeColor col : tiles) {
			new BrickSet(reg, col.getName() + "_tiles",
					BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.byId(14 + col.getId())),
					"tiles/", BlockTags.MINEABLE_WITH_PICKAXE);
		}

		for (DyeColor col : strips) {
			reg.block(col.getName() + "_strips_terracota", Block::new)
					.initialProperties(() -> Blocks.WHITE_GLAZED_TERRACOTTA)
					.properties(p -> p.mapColor(MapColor.byId(14 + col.getId())))
					.blockstate((ctx, pvd) ->
							pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(), GensokyoLegacy.loc("block/strips/" + ctx.getName()))))
					.tag(BlockTags.MINEABLE_WITH_PICKAXE).simpleItem().register();
		}

		// 板条箱
		CRATE = reg.block("crate", Block::new)
				.properties(p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD))
				.blockstate((ctx, pvd) ->
						pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(), pvd.modLoc("block/utensil/" + ctx.getName()))))
				.tag(BlockTags.MINEABLE_WITH_AXE).simpleItem().register();


	}

	public static void register() {

	}

	public static class BrickSet {

		public final Supplier<Block> block;
		public final BlockEntry<StairBlock> stairs;
		public final BlockEntry<SlabBlock> slab;
		public final BlockEntry<VerticalSlabBlock> vertical;

		private boolean suppressCraft;

		public BrickSet(L2Registrate reg, String id, BlockBehaviour.Properties prop,
		                NonNullBiConsumer<DataGenContext<Block, Block>, RegistrateRecipeProvider> recipe) {
			this(reg, id + "_brick", prop, GensokyoLegacy.loc("block/deco/" + id + "_bricks"),
					reg.block(id + "_bricks", p -> new Block(prop))
							.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
									pvd.models().cubeAll(ctx.getName(), pvd.modLoc("block/deco/" + ctx.getName()))))
							.tag(BlockTags.MINEABLE_WITH_PICKAXE).recipe(recipe)
							.simpleItem().register(),
					BlockTags.MINEABLE_WITH_PICKAXE);
		}

		public BrickSet(L2Registrate reg, String id, BlockBehaviour.Properties prop, TagKey<Block> tool) {
			this(reg, id, prop, GensokyoLegacy.loc("block/deco/" + id),
					reg.block(id, p -> new Block(prop))
							.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
									pvd.models().cubeAll(ctx.getName(), pvd.modLoc("block/deco/" + ctx.getName()))))
							.tag(tool).simpleItem().register(), tool);
		}

		public BrickSet(L2Registrate reg, String id, BlockBehaviour.Properties prop, String prefix, TagKey<Block> tool) {
			this(reg, id, prop, GensokyoLegacy.loc("block/" + prefix + id),
					reg.block(id, p -> new Block(prop))
							.blockstate((ctx, pvd) ->
									pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(), GensokyoLegacy.loc("block/" + prefix + id))))
							.tag(tool).simpleItem().register(), tool);
		}

		public BrickSet(L2Registrate reg, String id, BlockBehaviour.Properties prop, ResourceLocation side, Supplier<Block> base, TagKey<Block> tool) {
			block = base;
			stairs = reg.block(id + "_stairs", p ->
							new StairBlock(block.get().defaultBlockState(), prop))
					.blockstate((ctx, pvd) -> pvd.stairsBlock(ctx.get(), id, side))
					.tag(tool, BlockTags.STAIRS).item().tag(ItemTags.STAIRS).build()
					.recipe(this::genStair).register();
			slab = reg.block(id + "_slab", p ->
							new SlabBlock(prop))
					.blockstate((ctx, pvd) -> pvd.slabBlock(ctx.get(),
							pvd.models().slab(ctx.getName(), side, side, side),
							pvd.models().slabTop(ctx.getName() + "_top", side, side, side),
							new ModelFile.UncheckedModelFile(side)))
					.tag(tool, BlockTags.SLABS).item().tag(ItemTags.SLABS).build()
					.recipe(this::genSlab).register();
			vertical = reg.block(id + "_vertical_slab", p ->
							new VerticalSlabBlock(prop))
					.blockstate((ctx, pvd) -> VerticalSlabBlock.buildBlockState(ctx, pvd, side, side))
					.tag(GLTagGen.VERTICAL_SLAB, tool).item().build()
					.recipe(this::genVertical).register();
		}

		private BrickSet suppressCraft() {
			suppressCraft = true;
			return this;
		}

		private void genStair(DataGenContext<Block, StairBlock> ctx, RegistrateRecipeProvider pvd) {
			if (suppressCraft) {
				pvd.stonecutting(DataIngredient.items(block.get()), RecipeCategory.BUILDING_BLOCKS, ctx);
				return;
			}
			pvd.stairs(DataIngredient.items(block.get()), RecipeCategory.BUILDING_BLOCKS, ctx, null, true);
		}

		private void genSlab(DataGenContext<Block, SlabBlock> ctx, RegistrateRecipeProvider pvd) {
			if (suppressCraft) {
				pvd.stonecutting(DataIngredient.items(block.get()), RecipeCategory.BUILDING_BLOCKS, ctx, 2);
				return;
			}
			pvd.slab(DataIngredient.items(block.get()), RecipeCategory.BUILDING_BLOCKS, ctx, null, true);
		}

		private void genVertical(DataGenContext<Block, VerticalSlabBlock> ctx, RegistrateRecipeProvider pvd) {
			if (suppressCraft) {
				pvd.stonecutting(DataIngredient.items(block.get()), RecipeCategory.BUILDING_BLOCKS, ctx, 2);
				return;
			}
			VerticalSlabBlock.genRecipe(pvd, block, ctx);
		}

	}

	public static class StoneAndBrickSet {

		public BrickSet stone, brick;
		public BlockEntry<Block> chiseled;

		public StoneAndBrickSet(L2Registrate reg, String id, MapColor color, float strength, SoundType stoneSound, SoundType brickSound) {
			stone = new BrickSet(reg, id, BlockBehaviour.Properties.of().mapColor(color)
					.requiresCorrectToolForDrops().strength(strength).sound(stoneSound),
					BlockTags.MINEABLE_WITH_PICKAXE);
			var brickProp = BlockBehaviour.Properties.of().mapColor(color)
					.requiresCorrectToolForDrops().strength(strength).sound(brickSound);
			brick = new BrickSet(reg, id, brickProp, this::brick);
			chiseled = reg.block("chiseled_" + id + "_bricks", Block::new)
					.properties(p -> brickProp)
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
							pvd.models().cubeAll(ctx.getName(), pvd.modLoc("block/deco/" + ctx.getName()))))
					.tag(BlockTags.MINEABLE_WITH_PICKAXE)
					.simpleItem()
					.recipe(this::chisel)
					.register();
		}

		private void chisel(DataGenContext<Block, Block> ctx, RegistrateRecipeProvider pvd) {
			pvd.stonecutting(DataIngredient.items(stone.block.get()), RecipeCategory.BUILDING_BLOCKS, ctx);
			pvd.stonecutting(DataIngredient.items(brick.block.get()), RecipeCategory.BUILDING_BLOCKS, ctx);
			GLRecipeGen.unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())::unlockedBy,
							brick.slab.asItem()).pattern("A").pattern("A").define('A', brick.slab.asItem())
					.save(pvd);
		}

		private void brick(DataGenContext<Block, Block> ctx, RegistrateRecipeProvider pvd) {
			pvd.stonecutting(DataIngredient.items(stone.block.get()), RecipeCategory.BUILDING_BLOCKS, ctx);
			GLRecipeGen.unlock(pvd, ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)::unlockedBy,
							brick.block.get().asItem()).pattern("AA").pattern("AA").define('A', brick.block.get())
					.save(pvd);
		}
	}


}
