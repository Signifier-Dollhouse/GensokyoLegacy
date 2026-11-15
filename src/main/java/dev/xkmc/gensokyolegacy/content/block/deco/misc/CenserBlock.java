package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.mult.AnimateTickBlockMethod;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.RandomTickBlockMethod;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import dev.xkmc.l2modularblock.one.LightBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * 香炉：火把点亮后换成插着香的贴图，随机刻自动熄灭。整体对称，碰撞箱不随朝向变化。
 */
public record CenserBlock() implements ShapeBlockMethod, CreateBlockStateBlockMethod, DefaultStateBlockMethod,
		UseItemOnBlockMethod, RandomTickBlockMethod, AnimateTickBlockMethod, LightBlockMethod {

	public static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 7, 13);
	public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public int getLightValue(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(VARIANT) == 1 ? 10 : 0;
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(VARIANT);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(VARIANT, 0);
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
										   Player pl, InteractionHand hand, BlockHitResult result) {
		if (!stack.is(Items.TORCH) || state.getValue(VARIANT) != 0) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!level.isClientSide()) {
			level.setBlockAndUpdate(pos, state.setValue(VARIANT, 1));
			if (!pl.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return ItemInteractionResult.SUCCESS;
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(VARIANT) == 1) {
			level.setBlockAndUpdate(pos, state.setValue(VARIANT, 0));
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(VARIANT) == 0) {
			return;
		}
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 13 / 16.0;
		double z = pos.getZ() + 0.5;
		level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
		level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
	}

	public static void buildStates(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd) {
		pvd.horizontalBlock(ctx.get(), state -> {
			int variant = state.getValue(VARIANT);
			String suffix = variant > 0 ? "_" + variant : "";
			return pvd.models().getBuilder("block/utensil/" + ctx.getName() + suffix)
					.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/utensil/censer")))
					.texture("all", pvd.modLoc("block/utensil/censer_" + variant))
					.renderType("cutout");
		});
	}

}
