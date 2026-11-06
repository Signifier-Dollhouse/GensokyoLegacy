package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.l2core.serial.loot.LootHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * Golden lotus growing through three sizes as one block; only the largest size is a double plant.
 */
public class GoldenLotusBlock extends BushBlock implements BonemealableBlock {

	public static final MapCodec<GoldenLotusBlock> CODEC = simpleCodec(GoldenLotusBlock::new);

	public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 2);
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

	public static final int LARGE = 2;
	private static final int GROW_CHANCE = 7;
	private static final int LARGE_GROW_CHANCE = 100;
	private static final float BONEMEAL_CHANCE = 0.2F;
	private static final float LARGE_BONEMEAL_CHANCE = 0.03F;

	public GoldenLotusBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SIZE, 0).setValue(PART, Part.SINGLE));
	}

	@Override
	public MapCodec<GoldenLotusBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SIZE, PART);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (state.getValue(SIZE) == LARGE && state.getValue(PART) == Part.LOWER) {
			level.setBlock(pos.above(), state.setValue(PART, Part.UPPER), 3);
		}
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(PART) == Part.UPPER) return;
		if (!(level.getBlockState(pos.below()).getBlock() instanceof FarmBlock)) return;
		int size = state.getValue(SIZE);
		if (size >= LARGE) return;
		int chance = size + 1 == LARGE ? LARGE_GROW_CHANCE : GROW_CHANCE;
		if (random.nextInt(chance) != 0) return;
		grow(level, pos, state);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
									 LevelAccessor level, BlockPos pos, BlockPos facingPos) {
		Part part = state.getValue(PART);
		if (part != Part.SINGLE && facing.getAxis() == Direction.Axis.Y) {
			boolean joined = facingState.is(this) && facingState.getValue(PART) != part;
			if (part == Part.LOWER && facing == Direction.UP && !joined) return Blocks.AIR.defaultBlockState();
			if (part == Part.UPPER && facing == Direction.DOWN && !joined) return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, facing, facingState, level, pos, facingPos);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(PART) == Part.UPPER) {
			BlockState below = level.getBlockState(pos.below());
			return below.is(this) && below.getValue(PART) == Part.LOWER && below.getValue(SIZE) == LARGE;
		}
		return super.canSurvive(state, level, pos);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		if (state.getValue(PART) == Part.UPPER) return false;
		int size = state.getValue(SIZE);
		return size < LARGE && (size + 1 < LARGE || canGrowLarge(level, pos));
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return random.nextFloat() < (state.getValue(SIZE) + 1 < LARGE ? BONEMEAL_CHANCE : LARGE_BONEMEAL_CHANCE);
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		grow(level, pos, state);
	}

	private static void grow(ServerLevel level, BlockPos pos, BlockState state) {
		int size = state.getValue(SIZE);
		if (size + 1 < LARGE) {
			level.setBlock(pos, state.setValue(SIZE, size + 1), 3);
		} else if (canGrowLarge(level, pos)) {
			BlockState lower = state.setValue(SIZE, LARGE).setValue(PART, Part.LOWER);
			level.setBlock(pos, lower, 3);
			level.setBlock(pos.above(), lower.setValue(PART, Part.UPPER), 3);
		}
	}

	private static boolean canGrowLarge(LevelReader level, BlockPos pos) {
		return pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced();
	}

	public enum Part implements StringRepresentable {
		SINGLE("single"), LOWER("lower"), UPPER("upper");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}


	public static void genGoldenLotusState(DataGenContext<Block, GoldenLotusBlock> ctx, RegistrateBlockstateProvider pvd) {
		var small = pvd.models().cross(ctx.getName() + "_0",
				pvd.modLoc("block/nature/golden_lotus_0")).renderType("cutout");
		var medium = pvd.models().cross(ctx.getName() + "_1",
				pvd.modLoc("block/nature/golden_lotus_1")).renderType("cutout");
		var large = pvd.modLoc("block/nature/golden_lotus_2");
		var lower = genGoldenLotusHalf(pvd, ctx.getName() + "_2_lower", large, 8);
		var upper = genGoldenLotusHalf(pvd, ctx.getName() + "_2_upper", large, 0);
		var builder = pvd.getVariantBuilder(ctx.get());
		for (int size = 0; size <= GoldenLotusBlock.LARGE; size++) {
			for (GoldenLotusBlock.Part part : GoldenLotusBlock.Part.values()) {
				var model = size == 0 ? small : size == 1 ? medium :
						part == GoldenLotusBlock.Part.UPPER ? upper : lower;
				builder.partialState().with(GoldenLotusBlock.SIZE, size).with(GoldenLotusBlock.PART, part)
						.modelForState().modelFile(model).addModel();
			}
		}
	}

	private static ModelFile genGoldenLotusHalf(RegistrateBlockstateProvider pvd, String name,
												ResourceLocation texture, int v) {
		return pvd.models().cross(name, texture)
				.renderType("cutout")
				.element().from(0.8f, 0, 8).to(15.2f, 16, 8).shade(false)
				.rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end()
				.face(Direction.NORTH).uvs(0, v, 16, v + 8).texture("#cross").end()
				.face(Direction.SOUTH).uvs(0, v, 16, v + 8).texture("#cross").end()
				.end()
				.element().from(8, 0, 0.8f).to(8, 16, 15.2f).shade(false)
				.rotation().origin(8, 8, 8).axis(Direction.Axis.Y).angle(45).rescale(true).end()
				.face(Direction.WEST).uvs(0, v, 16, v + 8).texture("#cross").end()
				.face(Direction.EAST).uvs(0, v, 16, v + 8).texture("#cross").end()
				.end();
	}

	public static void genGoldenLotusLoot(RegistrateBlockLootTables pvd, GoldenLotusBlock block) {
		var helper = new LootHelper(pvd);
		pvd.add(block, LootTable.lootTable().withPool(pvd.applyExplosionCondition(block,
				LootPool.lootPool().add(AlternativesEntry.alternatives(
						LootItem.lootTableItem(block)
								.apply(SetItemCountFunction.setCount(ConstantValue.exactly(2)))
								.when(helper.enumState(block, PART, Part.LOWER)),
						LootItem.lootTableItem(block)
								.when(helper.enumState(block, PART, Part.SINGLE))
				)))));
	}

}
