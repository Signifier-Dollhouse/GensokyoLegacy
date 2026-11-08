package dev.xkmc.gensokyolegacy.init.registrate.block;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xkmc.gensokyolegacy.content.block.nature.FallenLeavesBlock;
import dev.xkmc.gensokyolegacy.content.block.nature.FallenLeavesItem;
import dev.xkmc.gensokyolegacy.content.block.nature.SimpleBushBlock;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.List;

public class GLBlockDatagenHelper {
	public static BlockEntry<SimpleBushBlock> regTowerGrass(L2Registrate reg, String size) {
		return reg.block("tower_grass_" + size, SimpleBushBlock::new)
				.properties(p -> p.mapColor(MapColor.PLANT).instabreak().sound(SoundType.GRASS)
						.noOcclusion().noCollission().pushReaction(PushReaction.DESTROY))
				.blockstate((ctx, pvd) -> pvd.simpleBlock(ctx.get(), pvd.models()
						.withExistingParent(ctx.getName(), pvd.modLoc("custom/nature/tower_grass"))
						.texture("1", pvd.modLoc("block/nature/" + ctx.getName()))
						.texture("particle", pvd.modLoc("block/nature/" + ctx.getName())).renderType("cutout")))
				.loot(GLBlockDatagenHelper::towerGrassLoot)
				.item()
				.model((ctx, pvd) -> {
					var base = pvd.nested().parent(new ModelFile.UncheckedModelFile(
							pvd.modLoc("block/" + ctx.getName())));
					var icon = pvd.nested().parent(new ModelFile.UncheckedModelFile(
									pvd.modLoc("custom/nature/tower_grass_icon")))
							.texture("plant", pvd.modLoc("block/nature/" + ctx.getName()));
					pvd.getBuilder(ctx.getName()).customLoader(SeparateTransformsModelBuilder::begin)
							.base(base).perspective(ItemDisplayContext.GUI, icon).end()
							.guiLight(BlockModel.GuiLight.FRONT);
				})
				.build()
				.register();
	}

	private static void towerGrassLoot(RegistrateBlockLootTables tb, Block block) {
		var shearsOrSilk = MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS))
				.or(MatchTool.toolMatches(ItemPredicate.Builder.item()
						.withSubPredicate(ItemSubPredicates.ENCHANTMENTS,
								ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
										tb.getRegistries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
										MinMaxBounds.Ints.atLeast(1)))))));
		tb.add(block, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shearsOrSilk)
						.add(LootItem.lootTableItem(block)))
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(shearsOrSilk.invert())
						.add(LootItem.lootTableItem(GLItems.MYSTICAL_STRAW.get()))));
	}

	/**
	 * A carpet of fallen leaves, one block per tree: {@code <id>_layer1} and {@code <id>_layer2}.
	 */
	public static BlockEntry<FallenLeavesBlock> regFallenLeaves(L2Registrate reg, String id) {
		return reg.block(id, FallenLeavesBlock::new)
				.properties(p -> p.mapColor(MapColor.PLANT).sound(SoundType.GRASS).noOcclusion().noCollission().randomTicks())
				.blockstate((ctx, pvd) -> {
					var layer1 = pvd.models().getBuilder("block/" + ctx.getName() + "_layer1")
							.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/fallen_leaves")))
							.texture("all", pvd.modLoc("block/nature/" + id + "_layer1"))
							.renderType("cutout");
					var layer2 = pvd.models().getBuilder("block/" + ctx.getName() + "_layer2")
							.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/fallen_leaves")))
							.texture("all", pvd.modLoc("block/nature/" + id + "_layer2"))
							.renderType("cutout");
					var builder = pvd.getVariantBuilder(ctx.get());
					for (Direction dir : Direction.Plane.HORIZONTAL) {
						int yRot = (int) dir.toYRot() % 360;
						builder.partialState().with(FallenLeavesBlock.FACING, dir)
								.with(FallenLeavesBlock.LAYERS, 1)
								.modelForState().modelFile(layer1).rotationY(yRot).addModel();
						builder.partialState().with(FallenLeavesBlock.FACING, dir)
								.with(FallenLeavesBlock.LAYERS, 2)
								.modelForState().modelFile(layer2).rotationY(yRot).addModel();
					}
				})
				.loot(FallenLeavesBlock::loot)
				.item(FallenLeavesItem::new).model((ctx, pvd) -> pvd.getBuilder(ctx.getName())
						.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/nature/fallen_leaves")))
						.texture("all", pvd.modLoc("block/nature/" + id + "_layer1")))
				.dataMap(NeoForgeDataMaps.COMPOSTABLES, new Compostable(0.3f))
				.build()
				.register();
	}

	public static void genColumnState(DataGenContext<Block, ? extends Block> ctx, RegistrateBlockstateProvider pvd,
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

	public static ModelFile buildLanternModel(RegistrateBlockstateProvider pvd, String name, boolean connected) {
		return pvd.models().getBuilder("block/furniture/" + name + (connected ? "_connected" : ""))
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/lantern")))
				.texture("0", pvd.modLoc("block/furniture/" + name + (connected ? "_connected" : "")))
				.texture("particle", pvd.modLoc("block/furniture/" + name))
				.renderType("cutout");
	}
}
