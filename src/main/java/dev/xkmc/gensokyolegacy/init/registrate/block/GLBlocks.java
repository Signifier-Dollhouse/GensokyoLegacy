package dev.xkmc.gensokyolegacy.init.registrate.block;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.content.block.deco.bed.*;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyPotBlock;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyPotBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyPotRenderer;
import dev.xkmc.gensokyolegacy.content.block.functional.barriers.SealingPotBlock;
import dev.xkmc.gensokyolegacy.content.block.functional.barriers.SealingPotShape;
import dev.xkmc.gensokyolegacy.content.block.functional.portal.*;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.core.Holder;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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


	public static final BlockEntry<BasePortalBlock> GAP_PORTAL;
	public static final BlockEntityEntry<GapPortalBlockEntity> GAP_BE;

	public static final BlockEntry<DelegateBlock> ALCHEMY_POT;
	public static final BlockEntityEntry<AlchemyPotBlockEntity> ALCHEMY_POT_BE;

	public static final BlockEntry<DelegateBlock> SEALING_POT;

	public static final BlockEntry<YoukaiBedBlock>[] BEDS;
	public static final BlockEntityEntry<YoukaiBedBlockEntity> BE_BED;


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
			SEALING_POT = GensokyoLegacy.REGISTRATE.block("sealing_pot", p -> DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new SealingPotShape(), new SealingPotBlock()))
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
