package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HyphaeBlock extends BushBlock {

	private static final MapCodec<HyphaeBlock> CODEC = simpleCodec(HyphaeBlock::new);

	public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);

	public static final BooleanProperty TRANSIENT = BooleanProperty.create("transient");

	public HyphaeBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TRANSIENT, false));
	}

	@Override
	public MapCodec<HyphaeBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TRANSIENT);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.isFaceSturdy(level, pos, Direction.UP);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		Vec3 vec3 = new Vec3(0.25, 0.05, 0.25);
		if (entity instanceof LivingEntity living && !living.hasEffect(GLEffects.HYPHAE_INFECTION.holder())) {
			vec3 = new Vec3(0.5, 0.25, 0.5);
		}
		entity.makeStuckInBlock(state, vec3);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(TRANSIENT);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(TRANSIENT)) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
	}

}