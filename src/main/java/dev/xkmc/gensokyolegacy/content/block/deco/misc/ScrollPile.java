package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.gensokyolegacy.content.block.base.SurviveImpl;
import dev.xkmc.gensokyolegacy.content.block.base.VariantImpl;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * 卷轴堆。四个变体共用一套模型，靠贴图透明区决定露出的卷轴数量，越往后堆得越高。
 * 放置时固定为变体 0，右键依次切换。
 */
public class ScrollPile implements ShapePathFindBlockMethod {

	public static final VariantImpl VARIANT = new VariantImpl(3, false);

	private static final VoxelShape[][] SHAPES = {
			MagicTableBlock.allFaces(new VoxelBuilder(1, 0, 1, 15, 4, 15)),
			MagicTableBlock.allFaces(new VoxelBuilder(2, 0, 1, 11, 4, 15)),
			MagicTableBlock.allFaces(new VoxelBuilder(2, 0, 1, 15, 4, 15)),
			MagicTableBlock.allFaces(new VoxelBuilder(1, 0, 1, 16, 8, 15)),
	};

	public static DelegateBlock create(BlockBehaviour.Properties p) {
		return DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new ScrollPile(), VARIANT, new SurviveImpl());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		int variant = state.getValue(VARIANT.variant());
		int facing = state.getValue(BlockTemplates.HORIZONTAL_FACING).get2DDataValue();
		Vec3 vec3 = state.getOffset(level, pos);
		return SHAPES[variant][facing].move(vec3.x, vec3.y, vec3.z);
	}

	public static void buildStates(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd) {
		pvd.horizontalBlock(ctx.get(), state -> {
			int variant = state.getValue(VARIANT.variant());
			String suffix = variant > 0 ? "_" + variant : "";
			var tex = pvd.modLoc("block/deco/" + ctx.getName() + "_" + variant);
			return pvd.models().getBuilder("block/" + ctx.getName() + suffix)
					.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/scroll_pile")))
					.texture("0", tex)
					.texture("particle", tex)
					.renderType("cutout");
		});
	}

}
