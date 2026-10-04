package dev.xkmc.gensokyolegacy.init.registrate.block;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.content.block.deco.cabinet.CabinetBlock;
import dev.xkmc.gensokyolegacy.content.block.deco.cabinet.CabinetBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.donation.DonationBoxBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.donation.DonationBoxShape;
import dev.xkmc.gensokyolegacy.content.block.deco.misc.*;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfBlock;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfRenderer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLRecipeGen;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.minecraft.world.item.ItemDisplayContext;

public class GLFurniture {

	public static final BlockEntry<DelegateBlock> DONATION_BOX;
	public static final BlockEntityEntry<DonationBoxBlockEntity> DONATION_BOX_BE;

	public static final BlockEntry<DelegateBlock> SHELF;
	public static final BlockEntityEntry<ShelfBlockEntity> SHELF_BE;

	public static final BlockEntry<DelegateBlock> DRAWER_CABINET, DOOR_CABINET;
	public static final BlockEntityEntry<CabinetBlockEntity> CABINET_BE;

	public static final BlockEntry<DelegateBlock> CARTON, CARTON_WHITE, CARTON_BLUE;
	public static final BlockEntry<DelegateBlock> TEA_TABLE;
	public static final BlockEntry<DelegateBlock> BOOK_SHELF;

	public static final BlockEntry<Block> CRATE;
	public static final BlockEntry<DelegateBlock> BOOK_PILE, BOOK_STACK;

	public static final BlockEntry<LanternBlock> RED_LANTERN, WHITE_LANTERN, LETTERED_RED_LANTERN, LETTERED_WHITE_LANTERN;

	static {

		var reg = GensokyoLegacy.REGISTRATE;

		// donation box, shelf, drawer cabinet
		{

			// 赛钱箱
			DONATION_BOX = reg.block("donation_box", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new DonationBoxShape(), DonationBoxBlockEntity.TE))
					.properties(p -> p.mapColor(MapColor.DIRT).strength(2.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/" + ctx.getName())))
									.texture("all", pvd.modLoc("block/utensil/" + ctx.getName()))
									.renderType("cutout")))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(300)).build()
					.register();

			DONATION_BOX_BE = reg.blockEntity("donation_box", DonationBoxBlockEntity::new)
					.validBlock(DONATION_BOX)
					.register();

			SHELF = reg.block("oak_shelf", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new ShelfBlock(), ShelfBlock.BE))
					.initialProperties(() -> Blocks.BIRCH_TRAPDOOR)
					.blockstate(ShelfBlock::buildStates)
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(300)).build().register();

			SHELF_BE = reg.blockEntity("shelf", ShelfBlockEntity::new)
					.validBlock(SHELF)
					.renderer(() -> ShelfRenderer::new)
					.register();

			DRAWER_CABINET = reg.block("drawer_cabinet",
							p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CabinetBlock(), CabinetBlock.BE))
					.initialProperties(() -> Blocks.OAK_PLANKS)
					.properties(BlockBehaviour.Properties::noOcclusion)
					.blockstate((ctx, pvd) -> CabinetBlock.buildStates(ctx, pvd, "cabinet_top"))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tab(GLDecoBlocks.TAB.key())
					.dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(300)).build()
					.register();

			DOOR_CABINET = reg.block("door_cabinet",
							p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CabinetBlock(), CabinetBlock.BE))
					.initialProperties(() -> Blocks.OAK_PLANKS)
					.properties(BlockBehaviour.Properties::noOcclusion)
					.blockstate((ctx, pvd) -> CabinetBlock.buildStates(ctx, pvd, "cabinet_side"))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tab(GLDecoBlocks.TAB.key())
					.dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(300)).build()
					.register();

			CABINET_BE = reg.blockEntity("cabinet", CabinetBlockEntity::new)
					.validBlocks(DRAWER_CABINET, DOOR_CABINET)
					.register();

		}

		// table, deco shelf
		{

			// 茶几
			TEA_TABLE = reg.block("tea_table", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new TeaTableBlock()))
					.initialProperties(() -> Blocks.OAK_PLANKS)
					.properties(BlockBehaviour.Properties::noOcclusion)
					.blockstate((ctx, pvd) -> {
						var originModel = pvd.models().getBuilder(ctx.getName())
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/tea_table")))
								.texture("all", pvd.modLoc("block/deco/tea_table"))
								.renderType("cutout");
						var emptyModel = pvd.models().getBuilder(ctx.getName() + "_empty")
								.texture("particle", pvd.modLoc("block/deco/tea_table"));
						var builder = pvd.getVariantBuilder(ctx.get());
						for (Direction dir : Direction.Plane.HORIZONTAL) {
							int yRot = (int) dir.toYRot() % 360;
							builder.partialState()
									.with(TeaTableBlock.ORIGIN, true)
									.with(BlockTemplates.HORIZONTAL_FACING, dir)
									.modelForState().modelFile(originModel).rotationY(yRot).addModel();
							builder.partialState()
									.with(TeaTableBlock.ORIGIN, false)
									.with(BlockTemplates.HORIZONTAL_FACING, dir)
									.modelForState().modelFile(emptyModel).addModel();
						}
					})
					.tag(GLTagGen.TABLE, BlockTags.MINEABLE_WITH_AXE)
					.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
							.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/tea_table_item")))
							.texture("all", pvd.modLoc("block/deco/tea_table"))
							.renderType("cutout"))
					.tag(GLTagGen.TABLE_ITEM)
					.dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(300))
					.build()
					.register();

			// 书架：空架可放书，最多 4 本，潜行右键取书
			BOOK_SHELF = reg.block("book_shelf", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new BookShelfBlock()))
					.initialProperties(() -> Blocks.BIRCH_TRAPDOOR)
					.blockstate(BookShelfBlock::buildStates)
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(300)).build()
					.register();

		}

		// 纸盒
		{

			CARTON = reg.block("carton_default", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton")))
									.texture("all", pvd.modLoc("block/utensil/carton_default"))
									.renderType("cutout")))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tag(GLTagGen.CARTONS).build()
					.register();

			CARTON_WHITE = reg.block("carton_white", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton")))
									.texture("all", pvd.modLoc("block/utensil/carton_white"))
									.renderType("cutout")))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tag(GLTagGen.CARTONS).build()
					.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
									RecipeCategory.DECORATIONS, ctx.get())::unlockedBy, Items.WHITE_DYE)
							.requires(GLTagGen.CARTONS).requires(DyeColor.WHITE.getTag()).save(pvd))
					.register();

			CARTON_BLUE = reg.block("carton_blue", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton")))
									.texture("all", pvd.modLoc("block/utensil/carton_blue"))
									.renderType("cutout")))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tag(GLTagGen.CARTONS).build()
					.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
									RecipeCategory.DECORATIONS, ctx.get())::unlockedBy, Items.BLUE_DYE)
							.requires(GLTagGen.CARTONS).requires(DyeColor.BLUE.getTag()).save(pvd))
					.register();

		}

		// crate, book stack
		{

			// 板条箱
			CRATE = reg.block("crate", Block::new)
					.properties(p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD))
					.blockstate((ctx, pvd) ->
							pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(), pvd.modLoc("block/utensil/" + ctx.getName()))))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(300)).build().register();

			BOOK_PILE = reg.block("book_pile", BookPile::create)
					.properties(p -> p.noOcclusion().strength(0F).offsetType(BlockBehaviour.OffsetType.XZ)
							.mapColor(MapColor.WOOD).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY).dynamicShape())
					.blockstate(BookPile::buildStates).loot(BookPile::buildLoot).simpleItem()
					.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
							RecipeCategory.DECORATIONS, ctx.get(), 1)::unlockedBy, Items.BOOK).requires(Items.BOOK, 5).save(pvd))
					.register();

			BOOK_STACK = reg.block("book_stack", BookStack::create)
					.properties(p -> p.noOcclusion().strength(0F).offsetType(BlockBehaviour.OffsetType.XZ)
							.mapColor(MapColor.WOOD).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY).dynamicShape())
					.blockstate(BookStack::buildStates).loot(BookStack::buildLoot).simpleItem()
					.recipe((ctx, pvd) -> GLRecipeGen.unlock(pvd, ShapelessRecipeBuilder.shapeless(
							RecipeCategory.DECORATIONS, ctx.get(), 1)::unlockedBy, Items.BOOK).requires(Items.BOOK, 5).save(pvd))
					.register();


		}

		// 灯笼：四种共用同一模型，下方有任意灯笼时使用连接材质
		{
			RED_LANTERN = registerLantern(reg, "red_lantern", MapColor.COLOR_RED);
			WHITE_LANTERN = registerLantern(reg, "white_lantern", MapColor.SNOW);
			LETTERED_RED_LANTERN = registerLantern(reg, "lettered_red_lantern", MapColor.COLOR_RED);
			LETTERED_WHITE_LANTERN = registerLantern(reg, "lettered_white_lantern", MapColor.SNOW);
		}
	}

	private static BlockEntry<LanternBlock> registerLantern(L2Registrate reg, String id, MapColor color) {
		return reg.block(id, LanternBlock::new)
				.properties(p -> p.mapColor(color).strength(0.5F).sound(SoundType.WOOD).noOcclusion()
						.lightLevel(s -> 15))
				.blockstate((ctx, pvd) -> {
					var normal = buildLanternModel(pvd, ctx.getName(), false);
					var connected = buildLanternModel(pvd, ctx.getName(), true);
					var builder = pvd.getVariantBuilder(ctx.get());
					for (Direction dir : Direction.Plane.HORIZONTAL) {
						// 文字面在模型北面：north 不旋转，south 180，west 90，east 270
						int yRot = ((int) dir.toYRot() + 180) % 360;
						builder.partialState()
								.with(LanternBlock.FACING, dir)
								.with(LanternBlock.CONNECTED, false)
								.modelForState().modelFile(normal).rotationY(yRot).addModel();
						builder.partialState()
								.with(LanternBlock.FACING, dir)
								.with(LanternBlock.CONNECTED, true)
								.modelForState().modelFile(connected).rotationY(yRot).addModel();
					}
				})
				.tag(BlockTags.MINEABLE_WITH_AXE)
				.item()
				.model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("block/furniture/" + ctx.getName())))
						.renderType("cutout")
						// 文字面在北面，使用标准方块 GUI 视角使其朝向玩家可见
						.transforms()
						.transform(ItemDisplayContext.GUI).rotation(30, 225, 0).scale(0.625f).end()
						.transform(ItemDisplayContext.FIXED).scale(0.5f).end()
						.transform(ItemDisplayContext.GROUND).translation(0, 3, 0).scale(0.25f).end()
						.transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end()
						.transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end()
						.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 225, 0).scale(0.4f).end()
						.transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 225, 0).scale(0.4f).end()
						.end())
				.tab(GLDecoBlocks.TAB.key())
				.dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(300))
				.build()
				.register();
	}

	private static ModelFile buildLanternModel(RegistrateBlockstateProvider pvd, String name, boolean connected) {
		return pvd.models().getBuilder("block/furniture/" + name + (connected ? "_connected" : ""))
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/lantern")))
				.texture("0", pvd.modLoc("block/furniture/" + name + (connected ? "_connected" : "")))
				.texture("particle", pvd.modLoc("block/furniture/" + name))
				.renderType("cutout");
	}

	public static void register() {

	}

}
