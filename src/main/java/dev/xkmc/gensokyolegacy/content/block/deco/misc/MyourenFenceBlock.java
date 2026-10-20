package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

/**
 * 命莲寺栅栏。整格宽的板垣一段,贴着格子北边摆。不跟邻格连接,一个方块就是一整段。
 */
public class MyourenFenceBlock implements ShapePathFindBlockMethod, DefaultStateBlockMethod {

	/** 朝向由 {@link BlockTemplates#HORIZONTAL} 加上，这里只引用它的属性。 */
	public static final DirectionProperty FACING = BlockTemplates.HORIZONTAL_FACING;

	/** 模型就是 x 0..16、y 0..16、z 0..12 的一整块。z 比 x 短,朝向转了看得出来。 */
	public static final VoxelShape[] SHAPES =
			MagicTableBlock.allFaces(new VoxelBuilder(0, 0, 0, 16, 16, 12));

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(FACING, Direction.NORTH);
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(FACING).get2DDataValue()];
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		pvd.horizontalBlock(block, pvd.models().getBuilder("block/" + name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/myouren_fence")))
				.texture("all", pvd.modLoc("block/deco/myouren_fence"))
				.texture("particle", pvd.modLoc("block/deco/myouren_fence"))
				.renderType("cutout"));
	}

}
