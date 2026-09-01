package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.content.block.bed.*;
import dev.xkmc.gensokyolegacy.content.block.cabinet.CabinetBlock;
import dev.xkmc.gensokyolegacy.content.block.cabinet.CabinetBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.misc.TeaTableBlock;
import dev.xkmc.gensokyolegacy.content.block.donation.DonationBoxBlock;
import dev.xkmc.gensokyolegacy.content.block.donation.DonationBoxBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.donation.DonationShape;
import dev.xkmc.gensokyolegacy.content.block.donation.DoubleBlockHorizontal;
import dev.xkmc.gensokyolegacy.content.block.misc.CartonShape;
import dev.xkmc.gensokyolegacy.content.block.misc.DonationBox2Shape;
import dev.xkmc.gensokyolegacy.content.block.misc.SealingPotShape;
import dev.xkmc.gensokyolegacy.content.block.portal.*;
import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyPotBlock;
import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyPotBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyPotRenderer;
import dev.xkmc.gensokyolegacy.content.block.shelf.ShelfBlock;
import dev.xkmc.gensokyolegacy.content.block.shelf.ShelfBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.shelf.ShelfRenderer;
import dev.xkmc.gensokyolegacy.content.block.misc.SimpleShelfBlock;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.Locale;

public class GLBlocks {

	public enum Beds {
		CIRNO(Blocks.BLUE_BED, new FlatBedShape()),
		RUMIA(Blocks.BLACK_BED, new FlatBedShape()),
		REIMU(Blocks.RED_BED, new FlatBedShape()),
		MORICHIKA(Blocks.LIGHT_BLUE_BED, new FlatBedShape()),
		MARISA(Blocks.BLACK_BED, new HighBedShape());

		private final BedBlock template;
		private final BedShape shape;
		private final DyeColor wool;

		Beds(Block template, BedShape shape) {
			this.template = (BedBlock) template;
			this.shape = shape;
			this.wool = this.template.getColor();
		}

		public YoukaiBedBlock get() {
			return BEDS[ordinal()].get();
		}

		public Holder<Block> holder() {
			return BEDS[ordinal()];
		}
	}

	public static final BlockEntry<DonationBoxBlock> DONATION_BOX;
	public static final BlockEntityEntry<DonationBoxBlockEntity> DONATION_BOX_BE;

	public static final BlockEntry<DelegateBlock> SHELF;
	public static final BlockEntityEntry<ShelfBlockEntity> SHELF_BE;

	public static final BlockEntry<DelegateBlock> DRAWER_CABINET, DOOR_CABINET;
	public static final BlockEntityEntry<CabinetBlockEntity> CABINET_BE;

	public static final BlockEntry<BasePortalBlock> GAP_PORTAL;
	public static final BlockEntityEntry<GapPortalBlockEntity> GAP_BE;

	public static final BlockEntry<DelegateBlock> ALCHEMY_POT;
	public static final BlockEntityEntry<AlchemyPotBlockEntity> ALCHEMY_POT_BE;

	public static final BlockEntry<DelegateBlock> SEALING_POT;
	public static final BlockEntry<DelegateBlock> DONATION_BOX_2;
	public static final BlockEntry<DelegateBlock> CARTON, CARTON_WHITE, CARTON_BLUE;

	public static final BlockEntry<YoukaiBedBlock>[] BEDS;
	public static final BlockEntityEntry<YoukaiBedBlockEntity> BE_BED;

	//public static final BlockEntry<MarisaBedBlock> MARISA_BED;
	public static final BlockEntry<DelegateBlock> TEA_TABLE;
	public static final BlockEntry<DelegateBlock> SHELF_EMPTY;
	public static final BlockEntry<DelegateBlock> SHELF_BOOK;

	static {

		// gap
		{
			GAP_PORTAL = GensokyoLegacy.REGISTRATE.block("gap_portal", GapPortalBlock::of)
					.initialProperties(() -> Blocks.END_PORTAL)
					.properties(BlockBehaviour.Properties::noLootTable)
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
							pvd.models().getBuilder(ctx.getName()).parent(new ModelFile.UncheckedModelFile("builtin/entity"))
									.texture("particle", pvd.mcLoc("block/obsidian"))))
					.item(GapPortalItem::new).model((ctx, pvd) ->
							pvd.generated(ctx, pvd.modLoc("item/gap"))).build()
					.register();

			GAP_BE = GensokyoLegacy.REGISTRATE.blockEntity("gap_portal", GapPortalBlockEntity::new)
					.validBlock(GAP_PORTAL)
					.renderer(() -> GapPortalRenderer::new)
					.register();
		}

		// pots
		{
			ALCHEMY_POT = GensokyoLegacy.REGISTRATE.block("alchemy_pot", p -> DelegateBlock.newBaseBlock(p,
							new AlchemyPotBlock(), AlchemyPotBlock.BE))
					.initialProperties(() -> Blocks.COPPER_BLOCK)
					.properties(BlockBehaviour.Properties::noOcclusion)
					.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(),
							pvd.models().getBuilder(ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/" + ctx.getName())))
									.texture("all", pvd.modLoc("block/utensil/" + ctx.getName()))
									.renderType("cutout")
					))
					.simpleItem()
					.register();
			ALCHEMY_POT_BE = GensokyoLegacy.REGISTRATE.blockEntity("alchemy_pot", AlchemyPotBlockEntity::new)
					.validBlock(ALCHEMY_POT)
					.renderer(() -> AlchemyPotRenderer::new)
					.register();

			// 封魔之壶
			SEALING_POT = GensokyoLegacy.REGISTRATE.block("sealing_pot", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new SealingPotShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(2.0F).sound(SoundType.STONE).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/" + ctx.getName())))
									.texture("all", pvd.modLoc("block/utensil/" + ctx.getName()))
									.renderType("cutout")))
					.item().model((ctx, pvd) -> pvd.withExistingParent(ctx.getName(), "item/generated")
							.texture("layer0", pvd.modLoc("item/utensil/sealing_pot")))
					.build()
					.register();

		}

		// donation box, shelf, drawer cabinet
		{

			DONATION_BOX = GensokyoLegacy.REGISTRATE.block("donation_box", p -> new DonationBoxBlock(p,
							BlockTemplates.HORIZONTAL, new DoubleBlockHorizontal(), new DonationShape(), DonationBoxBlock.TE
					)).properties(p -> p.noLootTable().strength(2.0F).sound(SoundType.WOOD)
							.mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS))
					.blockstate(DonationBoxBlock::buildStates)
					.simpleItem()
					.loot((pvd, block) -> pvd.add(block, LootTable.lootTable()))
					.register();

			// 赛钱箱
			DONATION_BOX_2 = GensokyoLegacy.REGISTRATE.block("donation_box_2", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new DonationBox2Shape(), DonationBoxBlock.TE))
					.properties(p -> p.mapColor(MapColor.DIRT).strength(2.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/" + ctx.getName())))
									.texture("all", pvd.modLoc("block/utensil/" + ctx.getName()))
									.renderType("cutout")))
					.simpleItem()
					.register();

			DONATION_BOX_BE = GensokyoLegacy.REGISTRATE.blockEntity("donation_box", DonationBoxBlockEntity::new)
					.validBlocks(DONATION_BOX, DONATION_BOX_2)
					.register();

			SHELF = GensokyoLegacy.REGISTRATE.block("birch_shelf", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new ShelfBlock(), ShelfBlock.BE))
					.initialProperties(() -> Blocks.BIRCH_TRAPDOOR)
					.blockstate(ShelfBlock::buildStates)
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.simpleItem().register();

			SHELF_BE = GensokyoLegacy.REGISTRATE.blockEntity("shelf", ShelfBlockEntity::new)
					.validBlock(SHELF)
					.renderer(() -> ShelfRenderer::new)
					.register();

			DRAWER_CABINET = GensokyoLegacy.REGISTRATE.block("drawer_cabinet",
							p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CabinetBlock(), CabinetBlock.BE))
					.initialProperties(() -> Blocks.OAK_PLANKS)
					.properties(BlockBehaviour.Properties::noOcclusion)
					.blockstate((ctx, pvd) -> CabinetBlock.buildStates(ctx, pvd, "cabinet_top"))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tab(GLDecoBlocks.TAB.key()).build()
					.register();

			DOOR_CABINET = GensokyoLegacy.REGISTRATE.block("door_cabinet",
							p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CabinetBlock(), CabinetBlock.BE))
					.initialProperties(() -> Blocks.OAK_PLANKS)
					.properties(BlockBehaviour.Properties::noOcclusion)
					.blockstate((ctx, pvd) -> CabinetBlock.buildStates(ctx, pvd, "cabinet_side"))
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.item().tab(GLDecoBlocks.TAB.key()).build()
					.register();

			CABINET_BE = GensokyoLegacy.REGISTRATE.blockEntity("cabinet", CabinetBlockEntity::new)
					.validBlocks(DRAWER_CABINET, DOOR_CABINET)
					.register();

		}

		// table, deco shelf
		{

			// 茶几
			TEA_TABLE = GensokyoLegacy.REGISTRATE.block("tea_table", p -> DelegateBlock.newBaseBlock(p,
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
			SHELF_EMPTY = GensokyoLegacy.REGISTRATE.block("shelf_empty", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new SimpleShelfBlock()))
					.initialProperties(() -> Blocks.BIRCH_TRAPDOOR)
					.blockstate(SimpleShelfBlock::buildStates)
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.simpleItem()
					.register();

			// 书架
			SHELF_BOOK = GensokyoLegacy.REGISTRATE.block("shelf_book", p -> DelegateBlock.newBaseBlock(p,
							BlockTemplates.HORIZONTAL, new SimpleShelfBlock()))
					.initialProperties(() -> Blocks.BIRCH_TRAPDOOR)
					.blockstate(SimpleShelfBlock::buildStates)
					.tag(BlockTags.MINEABLE_WITH_AXE)
					.simpleItem()
					.register();

		}

		// 纸盒
		{

			CARTON = GensokyoLegacy.REGISTRATE.block("carton_default", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton_default")))
									.texture("all", pvd.modLoc("block/utensil/carton_default"))
									.renderType("cutout")))
					.simpleItem()
					.register();

			CARTON_WHITE = GensokyoLegacy.REGISTRATE.block("carton_white", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton_white")))
									.texture("all", pvd.modLoc("block/utensil/carton_white"))
									.renderType("cutout")))
					.simpleItem()
					.register();

			CARTON_BLUE = GensokyoLegacy.REGISTRATE.block("carton_blue", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new CartonShape()))
					.properties(p -> p.mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.WOOD).noOcclusion())
					.blockstate((ctx, pvd) -> pvd.horizontalBlock(ctx.get(),
							pvd.models().getBuilder("block/" + ctx.getName())
									.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/carton_blue")))
									.texture("all", pvd.modLoc("block/utensil/carton_blue"))
									.renderType("cutout")))
					.simpleItem()
					.register();
		}

		BEDS = new BlockEntry[Beds.values().length];
		for (var e : Beds.values()) {
			String name = e.name().toLowerCase(Locale.ROOT);
			BEDS[e.ordinal()] = GensokyoLegacy.REGISTRATE.block(name + "_bed", p -> new YoukaiBedBlock(p, e.shape))
					.initialProperties(() -> e.template)
					.blockstate(e.shape::buildStates)
					.item(BedItem::new)
					.model(e.shape::buildItemModel)
					.build()
					.loot(YoukaiBedBlock::buildLoot)
					.register();
		}
		BE_BED = GensokyoLegacy.REGISTRATE.blockEntity("youkai_bed", YoukaiBedBlockEntity::new)
				//.validBlock(MARISA_BED)
				.validBlocks(BEDS)
				.register();
	}

	public static void register() {

	}

}
