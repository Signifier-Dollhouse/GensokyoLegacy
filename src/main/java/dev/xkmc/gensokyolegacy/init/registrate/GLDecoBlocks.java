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
import dev.xkmc.gensokyolegacy.content.block.door.SlidingDoor;
import dev.xkmc.gensokyolegacy.content.block.door.SlidingDoorJsons;
import dev.xkmc.gensokyolegacy.content.block.misc.TatamiBlock;
import dev.xkmc.gensokyolegacy.content.block.seat.CushionBlock;
import dev.xkmc.gensokyolegacy.content.block.seat.WoodChairBlock;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
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

	public static final BlockEntry<DelegateBlock> TATAMI, TATAMI_BLOCK;

	public static final TreeSet BLUE_FUR_SET;

	public static final MushroomSet GHOST_FIRE_MUSHROOM_SET, DREAM_MUSHROOM_SET, DEMONIC_MIASMA_MUSHROOM_SET;

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
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cubeBottomTop(ctx.getName(),
							pvd.modLoc("block/wood/" + name + "_plank_wall"), e.top(), e.top())))
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

		BLUE_FUR_SET = new TreeSet(
				reg, "blue_fir",
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG).mapColor(MapColor.COLOR_CYAN),
				BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES),
				TreeType.BLUE_FIR
		);

		GHOST_FIRE_MUSHROOM_SET = new MushroomSet(
				reg, "ghost_fire_mushroom", "cyan_mushroom", false, 3, true,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(b -> 5),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.COLOR_CYAN).lightLevel(b -> 5),
				MushroomTreeType.GHOST_FIRE.cfKey
		);

		DREAM_MUSHROOM_SET = new MushroomSet(
				reg, "dream_mushroom", "purple_mushroom", false, 3, false,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_PURPLE),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.COLOR_PURPLE),
				null
		);

		DEMONIC_MIASMA_MUSHROOM_SET = new MushroomSet(
				reg, "demonic_miasma_mushroom", "red_mushroom", false, 2, false,
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.CRIMSON_HYPHAE),
				BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.CRIMSON_HYPHAE),
				null
		);

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

	public static class TreeSet {

		public final BlockEntry<RotatedPillarBlock> log;
		public final BlockEntry<LeavesBlock> leaves;
		public final BlockEntry<SaplingBlock> sapling;

		public TreeSet(L2Registrate reg, String id,
		               BlockBehaviour.Properties logProp, BlockBehaviour.Properties leafProp,
		               TreeType type) {
			log = reg.block(id + "_log", RotatedPillarBlock::new)
					.properties(p -> logProp)
					.blockstate((ctx, pvd) -> genColumnState(ctx, pvd,
							pvd.modLoc("block/wood/" + ctx.getName() + "_side"),
							pvd.modLoc("block/wood/" + ctx.getName() + "_top")))
					.tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS)
					.simpleItem()
					.register();
			leaves = reg.block(id + "_leaves", LeavesBlock::new)
					.properties(p -> leafProp)
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(),
							pvd.modLoc("block/wood/" + ctx.getName())).renderType("cutout")))
					.loot(TreeSet::genLeavesLoot)
					.tag(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES)
					.simpleItem()
					.register();
			sapling = reg.block(id + "_sapling", p -> new SaplingBlock(new TreeGrower(
							id + "_tree", Optional.empty(), Optional.of(type.cfKey), Optional.empty()), p))
					.properties(p -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cross(ctx.getName(),
							pvd.modLoc("block/wood/" + ctx.getName())).renderType("cutout")))
					.loot(RegistrateBlockLootTables::dropSelf)
					.tag(BlockTags.SAPLINGS)
					.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
							.parent(new ModelFile.UncheckedModelFile("item/generated"))
							.texture("layer0", pvd.modLoc("block/wood/" + ctx.getName()))).build()
					.register();
		}

		private static void genLeavesLoot(RegistrateBlockLootTables tb, Block block) {
			var enchantments = tb.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
			var silkTouch = MatchTool.toolMatches(ItemPredicate.Builder.item()
							.withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
									ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
											enchantments.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1))))))
					.or(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS)));
			tb.add(block, LootTable.lootTable()
					.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(LootItem.lootTableItem(block).when(silkTouch)))
					.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(silkTouch.invert())
							.add(tb.applyExplosionDecay(block, LootItem.lootTableItem(Items.STICK)
									.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
									.when(BonusLevelTableCondition.bonusLevelFlatChance(
											enchantments.getOrThrow(Enchantments.FORTUNE),
											0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F))))));
		}
	}

	public static class MushroomSet {

		private static final Map<String, BlockEntry<Block>> STEMS = new HashMap<>();

		public final BlockEntry<Block> stem;
		public final BlockEntry<Block> block;
		public final BlockEntry<? extends Block> cap;

		public MushroomSet(L2Registrate reg, String id, String stemTex, boolean pillarStem, int capVariants,
		                   boolean emissive, BlockBehaviour.Properties blockProp,
		                   BlockBehaviour.Properties capProp,
		                   @Nullable ResourceKey<ConfiguredFeature<?, ?>> feature) {
			stem = STEMS.computeIfAbsent(stemTex + ":" + pillarStem, key -> {
				var stemProp = BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM);
				var stemBuilder = reg.block(id + "_stem", p -> pillarStem ? new RotatedPillarBlock(p) : new Block(p))
						.properties(p -> stemProp)
						.tag(BlockTags.MINEABLE_WITH_AXE);
				if (pillarStem) {
					stemBuilder.blockstate((ctx, pvd) -> {
						var side = pvd.modLoc("block/mushroom/" + stemTex + "_stem_side");
						var top = pvd.modLoc("block/mushroom/" + stemTex + "_stem_top");
						genColumnState(ctx, pvd, side, top);
					});
				} else {
					stemBuilder.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(),
							pvd.modLoc("block/mushroom/" + stemTex + "_stem"))));
				}
				return stemBuilder
						.loot(RegistrateBlockLootTables::dropWhenSilkTouch)
						.simpleItem()
						.register();
			});

			NonNullFunction<BlockBehaviour.Properties, ? extends Block> capFactory;
			if (feature == null) {
				capFactory = HugeMushroomBlock::new;
			} else {
				capFactory = p -> new MushroomBlock(feature, p);
			}
			cap = reg.block(id, capFactory)
					.properties(p -> capProp)
					.blockstate((ctx, pvd) -> genCapState(ctx, pvd, capVariants, emissive))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().model((ctx, pvd) -> genFlatItemModel(ctx.getName(), pvd,
							pvd.modLoc("block/mushroom/" + capModelName(ctx.getName(), capVariants, 1)), emissive)).build()
					.register();

			block = reg.block(id + "_block", Block::new)
					.properties(p -> blockProp)
					.blockstate((ctx, pvd) -> genPlainState(ctx, pvd, emissive))
					.loot((tb, blk) -> tb.add(blk, tb.createMushroomBlockDrop(blk, cap)))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.simpleItem()
					.register();
		}

		private static void genFlatItemModel(String name, RegistrateItemModelProvider pvd, ResourceLocation tex, boolean emissive) {
			var builder = pvd.getBuilder(name)
					.texture("layer0", tex)
					.texture("particle", tex)
					.transforms()
					.transform(ItemDisplayContext.GROUND).translation(0, 2, 0).scale(0.5f).end()
					.transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).translation(0, 13, 7).end()
					.transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).translation(0, 3, 1).scale(0.55f).end()
					.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, -90, 25).translation(1.13f, 3.2f, 1.13f).scale(0.68f).end()
					.transform(ItemDisplayContext.FIXED).rotation(0, 180, 0).end()
					.end();
			if (emissive) {
				builder.guiLight(BlockModel.GuiLight.FRONT).ao(false);
			}
			var element = builder.element()
					.from(0, 0, 7.5f).to(16, 16, 8.5f);
			if (emissive) {
				element.shade(false).emissivity(15, 15);
			}
			element.allFaces((dir, f) -> f.texture("#layer0"))
					.end();
		}

		private static void genPlainState(DataGenContext<Block, ? extends Block> ctx, RegistrateBlockstateProvider pvd, boolean emissive) {
			if (emissive) {
				pvd.simpleBlock(ctx.get(), emissiveCube(pvd, ctx.getName(),
						pvd.modLoc("block/mushroom/" + ctx.getName())));
			} else {
				pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(),
						pvd.modLoc("block/mushroom/" + ctx.getName())));
			}
		}

		private static BlockModelBuilder emissiveCube(RegistrateBlockstateProvider pvd, String name, ResourceLocation tex) {
			return pvd.models().withExistingParent(name, ResourceLocation.withDefaultNamespace("block/block"))
					.texture("all", tex)
					.texture("particle", tex)
					.element().from(0, 0, 0).to(16, 16, 16)
					.emissivity(15, 15)
					.cube("#all")
					.end();
		}

		private static BlockModelBuilder emissiveCross(RegistrateBlockstateProvider pvd, String name, ResourceLocation tex) {
			return pvd.models().withExistingParent(name, ResourceLocation.withDefaultNamespace("block/block"))
					.ao(false)
					.renderType("cutout")
					.texture("cross", tex)
					.texture("particle", tex)
					.element()
					.from(0.8f, 0, 8).to(15.2f, 16, 8)
					.shade(false)
					.emissivity(15, 15)
					.rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end()
					.face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#cross").end()
					.face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#cross").end()
					.end()
					.element()
					.from(8, 0, 0.8f).to(8, 16, 15.2f)
					.shade(false)
					.emissivity(15, 15)
					.rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end()
					.face(Direction.WEST).uvs(0, 0, 16, 16).texture("#cross").end()
					.face(Direction.EAST).uvs(0, 0, 16, 16).texture("#cross").end()
					.end();
		}

		private static String capModelName(String name, int variants, int index) {
			return variants <= 1 ? name : name + "_" + index;
		}

		private static void genCapState(DataGenContext<Block, ? extends Block> ctx, RegistrateBlockstateProvider pvd,
		                                int variants, boolean emissive) {
			ConfiguredModel[] models = new ConfiguredModel[variants];
			for (int i = 1; i <= variants; i++) {
				var name = capModelName(ctx.getName(), variants, i);
				var tex = pvd.modLoc("block/mushroom/" + name);
				models[i - 1] = new ConfiguredModel(emissive
						? emissiveCross(pvd, name, tex)
						: pvd.models().cross(name, tex).renderType("cutout"));
			}
			pvd.getVariantBuilder(ctx.get()).partialState().setModels(models);
		}
	}

	private static void genColumnState(DataGenContext<Block, ? extends Block> ctx, RegistrateBlockstateProvider pvd,
	                                   ResourceLocation side, ResourceLocation end) {
		var vertical = pvd.models().cubeColumn(ctx.getName(), side, end);
		var horizontal = pvd.models().cubeColumnHorizontal(ctx.getName() + "_horizontal", side, end);
		pvd.getVariantBuilder(ctx.get())
				.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Y)
				.modelForState().modelFile(vertical).addModel()
				.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.Z)
				.modelForState().modelFile(horizontal).rotationX(90).addModel()
				.partialState().with(RotatedPillarBlock.AXIS, Direction.Axis.X)
				.modelForState().modelFile(horizontal).rotationX(90).rotationY(90).addModel();
	}

}
