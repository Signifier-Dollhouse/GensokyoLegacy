package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * 铁艺立柱。只有竖直方向的首尾会连接:上下相邻都是同类方块时把那一面的端面藏起来,
 * 四侧永远不连接,所以没有任何连接状态,只有一个默认状态。
 */
public class WroughtIronPillarBlock extends Block {

	private static final MapCodec<WroughtIronPillarBlock> CODEC = simpleCodec(WroughtIronPillarBlock::new);

	public static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

	public WroughtIronPillarBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends WroughtIronPillarBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
		return (side.getAxis().isVertical() && adjacent.is(this)) || super.skipRendering(state, adjacent, side);
	}

	public static void buildStates(DataGenContext<Block, WroughtIronPillarBlock> ctx, RegistrateBlockstateProvider pvd) {
		pvd.simpleBlock(ctx.get(), pvd.models().getBuilder("block/" + ctx.getName())
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/iron/pillar")))
				.texture("all", pvd.modLoc("block/deco/wrought_iron_pillar"))
				.renderType("cutout"));
	}

}