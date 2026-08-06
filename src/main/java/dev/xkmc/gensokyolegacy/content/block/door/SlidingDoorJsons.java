package dev.xkmc.gensokyolegacy.content.block.door;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import static dev.xkmc.gensokyolegacy.content.block.door.SlidingDoor.HINGE;
import static dev.xkmc.gensokyolegacy.content.block.door.SlidingDoor.MAX;
import static dev.xkmc.gensokyolegacy.content.block.door.SlidingDoor.STACK;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class SlidingDoorJsons {

	private static final BlockModelBuilder[] BASE = new BlockModelBuilder[MAX];
	private static final BlockModelBuilder[] BASE_R = new BlockModelBuilder[MAX];

	public static void buildBlockState(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd,
	                                   ResourceLocation top, ResourceLocation bottom, ResourceLocation side) {
		pvd.getVariantBuilder(ctx.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model(ctx, pvd, state, top, bottom, side))
				.rotationY(((int) (state.getValue(HORIZONTAL_FACING).toYRot() + 180)) % 360).uvLock(false).build());
	}

	private static BlockModelBuilder model(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd,
	                                       BlockState state, ResourceLocation top, ResourceLocation bottom, ResourceLocation side) {
		boolean right = state.getValue(HINGE) == DoorHingeSide.RIGHT;
		int stack = state.getValue(STACK);
		boolean topHalf = state.getValue(HALF) == Half.TOP;
		return pvd.models().getBuilder("block/" + ctx.getName() + (topHalf ? "_top" : "_bottom") + "_s" + stack
						+ (right ? "_r" : ""))
				.parent(base(pvd, stack, right))
				.texture("front", topHalf ? top : bottom)
				.texture("side", side)
				.renderType("cutout");
	}

	private static BlockModelBuilder base(RegistrateBlockstateProvider pvd, int stack, boolean right) {
		var cache = right ? BASE_R : BASE;
		if (cache[stack - 1] == null) {
			cache[stack - 1] = pvd.models().withExistingParent("sliding_door_s" + stack + (right ? "_r" : ""), "block/block");
			cube(cache[stack - 1], stack + 1, right);
			cache[stack - 1].texture("particle", "#side");
		}
		return cache[stack - 1];
	}

	private static void cube(ModelBuilder<?> builder, int thickness, boolean right) {
		var elem = builder.element();
		elem.from(0, 0, 0).to(16, 16, thickness);
		if (!right) {
			elem.face(Direction.NORTH).uvs(16, 0, 0, 16).texture("#front").cullface(Direction.NORTH).end();
			elem.face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#front").end();
			elem.face(Direction.WEST).uvs(0, 0, thickness, 16).texture("#side").end();
			elem.face(Direction.EAST).uvs(thickness, 0, 0, 16).texture("#side").end();
			elem.face(Direction.UP).uvs(0, 0, thickness, 16).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#side").end();
			elem.face(Direction.DOWN).uvs(0, 0, thickness, 16).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#side").end();
		} else {
			elem.face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#front").cullface(Direction.NORTH).end();
			elem.face(Direction.SOUTH).uvs(16, 0, 0, 16).texture("#front").end();
			elem.face(Direction.WEST).uvs(thickness, 0, 0, 16).texture("#side").end();
			elem.face(Direction.EAST).uvs(0, 0, thickness, 16).texture("#side").end();
			elem.face(Direction.UP).uvs(thickness, 0, 0, 16).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#side").end();
			elem.face(Direction.DOWN).uvs(thickness, 0, 0, 16).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#side").end();
		}
	}

	public static void genLoot(RegistrateBlockLootTables pvd, DelegateBlock block) {
		var helper = new LootHelper(pvd);
		var pool = LootPool.lootPool();
		for (int i = 1; i <= MAX; i++) {
			pool.add(helper.item(block.asItem(), i)
					.when(helper.intState(block, STACK, i))
					.when(helper.enumState(block, HALF, Half.BOTTOM)));
		}
		pvd.add(block, LootTable.lootTable().withPool(pvd.applyExplosionCondition(block, pool)));
	}

	public static void genItemModel(DataGenContext<Item, BlockItem> ctx, RegistrateItemModelProvider pvd,
	                                ResourceLocation top, ResourceLocation bottom, ResourceLocation side) {
		itemBase(pvd);
		pvd.getBuilder(ctx.getName())
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("item/sliding_door")))
				.texture("front_top", top)
				.texture("front_bottom", bottom)
				.texture("side", side);
	}

	private static ItemModelBuilder ITEM_BASE;

	private static ItemModelBuilder itemBase(RegistrateItemModelProvider pvd) {
		if (ITEM_BASE == null) {
			ITEM_BASE = pvd.getBuilder("sliding_door")
					.parent(new ModelFile.UncheckedModelFile("gensokyolegacy:custom/double_block_display"))
					.texture("particle", "#side")
					.renderType("cutout");
			panel(ITEM_BASE, 0, 16, "front_bottom", "side");
			panel(ITEM_BASE, 16, 32, "front_top", "side");
		}
		return ITEM_BASE;
	}

	private static void panel(ModelBuilder<?> builder, int y0, int y1, String front, String side) {
		var elem = builder.element();
		elem.from(0, y0, 0).to(16, y1, 2);
		elem.face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#" + front).end();
		elem.face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#" + front).end();
		elem.face(Direction.WEST).uvs(0, 0, 2, 16).texture("#" + side).end();
		elem.face(Direction.EAST).uvs(2, 0, 0, 16).texture("#" + side).end();
		elem.face(Direction.UP).uvs(0, 0, 2, 16).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#" + side).end();
		elem.face(Direction.DOWN).uvs(0, 0, 2, 16).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#" + side).end();
		elem.end();
	}

}
