package dev.xkmc.gensokyolegacy.init.registrate.block;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.content.block.deco.cabinet.CabinetBlock;
import dev.xkmc.gensokyolegacy.content.block.deco.cabinet.CabinetBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.donation.DonationBoxBlock;
import dev.xkmc.gensokyolegacy.content.block.deco.donation.DonationBoxBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.donation.DonationShape;
import dev.xkmc.gensokyolegacy.content.block.deco.donation.DoubleBlockHorizontal;
import dev.xkmc.gensokyolegacy.content.block.deco.misc.*;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfBlock;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.ShelfRenderer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLRecipeGen;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class GLFurniture {

	public static final BlockEntry<DonationBoxBlock> DONATION_BOX;
	public static final BlockEntry<DelegateBlock> DONATION_BOX_2;
	public static final BlockEntityEntry<DonationBoxBlockEntity> DONATION_BOX_BE;

	public static final BlockEntry<DelegateBlock> SHELF;
	public static final BlockEntityEntry<ShelfBlockEntity> SHELF_BE;

	public static final BlockEntry<DelegateBlock> DRAWER_CABINET, DOOR_CABINET;
	public static final BlockEntityEntry<CabinetBlockEntity> CABINET_BE;

	public static final BlockEntry<DelegateBlock> CARTON, CARTON_WHITE, CARTON_BLUE;
	public static final BlockEntry<DelegateBlock> TEA_TABLE;
	public static final BlockEntry<DelegateBlock> SHELF_EMPTY, SHELF_BOOK;

	public static final BlockEntry<Block> CRATE;
	public static final BlockEntry<DelegateBlock> BOOK_PILE, BOOK_STACK;

	static {

		var reg = GensokyoLegacy.REGISTRATE;

		// donation box, shelf, drawer cabinet
		{

			DONATION_BOX = reg.block("donation_box", p -> new DonationBoxBlock(p,
							BlockTemplates.HORIZONTAL, new DoubleBlockHorizontal(), new DonationShape(), DonationBoxBlock.TE
					)).properties(p -> p.noLootTable().strength(2.0F).sound(SoundType.WOOD)
							.mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS))
					.blockstate(DonationBoxBlock::buildStates)
					.simpleItem()
					.loot((pvd, block) -> pvd.add(block, LootTable.lootTable()))
					.register();

			// 赛钱箱
			DONATION_BOX_2 = reg.block("donation_box_2", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new DonationBox2Shape(), DonationBoxBlock.TE))
					.properties(p -> p.mapColor(MapColor.DIRT).strength(2.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/" + ctx.getName())))
									.texture("all", pvd.modLoc("block/utensil/" + ctx.getName()))
									.renderType("cutout")))
					.simpleItem()
					.register();

			DONATION_BOX_BE = reg.blockEntity("donation_box", DonationBoxBlockEntity::new)
					.validBlocks(DONATION_BOX, DONATION_BOX_2)
					.register();

			SHELF = reg.block("birch_shelf", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new ShelfBlock(), ShelfBlock.BE))
					.initialProperties(() -> Blocks.BIRCH_TRAPDOOR)
					.blockstate(ShelfBlock::buildStates)
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.simpleItem().register();

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
					.item().tab(GLDecoBlocks.TAB.key()).build()
					.register();

			DOOR_CABINET = reg.block("door_cabinet",
							p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CabinetBlock(), CabinetBlock.BE))
					.initialProperties(() -> Blocks.OAK_PLANKS)
					.properties(BlockBehaviour.Properties::noOcclusion)
					.blockstate((ctx, pvd) -> CabinetBlock.buildStates(ctx, pvd, "cabinet_side"))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tab(GLDecoBlocks.TAB.key()).build()
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
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/tea_table")))
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
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
							.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/tea_table_item")))
							.texture("all", pvd.modLoc("block/deco/tea_table"))
							.renderType("cutout"))
					.build()
					.register();

			// 空厨架
			SHELF_EMPTY = reg.block("shelf_empty", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new SimpleShelfBlock()))
					.initialProperties(() -> Blocks.BIRCH_TRAPDOOR)
					.blockstate(SimpleShelfBlock::buildStates)
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.simpleItem()
					.register();

			// 书架
			SHELF_BOOK = reg.block("shelf_book", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new SimpleShelfBlock()))
					.initialProperties(() -> Blocks.BIRCH_TRAPDOOR)
					.blockstate(SimpleShelfBlock::buildStates)
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.simpleItem()
					.register();

		}

		// 纸盒
		{

			CARTON = reg.block("carton_default", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton_default")))
									.texture("all", pvd.modLoc("block/utensil/carton_default"))
									.renderType("cutout")))
					.simpleItem()
					.register();

			CARTON_WHITE = reg.block("carton_white", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton_white")))
									.texture("all", pvd.modLoc("block/utensil/carton_white"))
									.renderType("cutout")))
					.simpleItem()
					.register();

			CARTON_BLUE = reg.block("carton_blue", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton_blue")))
									.texture("all", pvd.modLoc("block/utensil/carton_blue"))
									.renderType("cutout")))
					.simpleItem()
					.register();

		}

		// crate, book stack
		{

			// 板条箱
			CRATE = reg.block("crate", Block::new)
					.properties(p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD))
					.blockstate((ctx, pvd) ->
							pvd.simpleBlock(ctx.get(), pvd.models().cubeAll(ctx.getName(), pvd.modLoc("block/utensil/" + ctx.getName()))))
					.tag(BlockTags.MINEABLE_WITH_AXE).simpleItem().register();

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
	}

	public static void register() {

	}

}
