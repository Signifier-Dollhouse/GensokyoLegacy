package dev.xkmc.gensokyolegacy.content.block.deco.door;

import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.PlacementBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 门帘。一张贴在中线上、双面可见的帘子,贴着贴脸的一格挂,长款往下多吊 {@link #HANGING} 格。
 */
public class NorenBlock {

	/** 长款比普通款往下多吊的长度 */
	public static final int HANGING = 4;

	public static DelegateBlock create(BlockBehaviour.Properties p, boolean hanging) {
		return DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new Shape(hanging));
	}

	/**
	 * 门帘的花纹。贴图按颜色和花纹各出一份,名字就是方块 id;
	 * 染色配方按花纹各用一个标签,这样染完颜色不会把花纹弄丢。
	 */
	public enum Kind {

		PLAIN("_noren", false, GLTagGen.NOREN),
		STRIPED("_striped_noren", false, GLTagGen.STRIPED_NOREN),
		WAVY("_wavy_noren", false, GLTagGen.WAVY_NOREN),
		LONG("_long_noren", true, GLTagGen.LONG_NOREN);

		private final String suffix;
		private final boolean hanging;
		private final TagKey<Item> tag;

		Kind(String suffix, boolean hanging, TagKey<Item> tag) {
			this.suffix = suffix;
			this.hanging = hanging;
			this.tag = tag;
		}

		public String name(DyeColor color) {
			return color.getName() + suffix;
		}

		public boolean hanging() {
			return hanging;
		}

		/** 同花纹的各色门帘,染色时当底料用 */
		public TagKey<Item> tag() {
			return tag;
		}
	}

	/**
	 * 帘子本体只有一格厚,而且贴着挂它的那面墙:rotationY=0 对应 facing=south,
	 * 墙就在北面,所以帘子落在 z=0 那条边上。碰撞由方块属性的 {@code noCollission} 关掉,
	 * 这里只管选中范围。
	 */
	public record Shape(boolean hanging) implements ShapeBlockMethod, PlacementBlockMethod {

		private static final VoxelShape[] SHORT = shapes(0);
		private static final VoxelShape[] LONG = shapes(-HANGING);

		private static VoxelShape[] shapes(int y0) {
			var builder = new VoxelBuilder(0, y0, 0, 16, 16, 1);
			VoxelShape[] ans = new VoxelShape[4];
			for (int i = 0; i < ans.length; i++) {
				ans[i] = builder.rotateFromNorth(Direction.from2DDataValue(i));
			}
			return ans;
		}

		@Override
		public BlockState getStateForPlacement(BlockState state, BlockPlaceContext blockPlaceContext) {
			return state.setValue(BlockStateProperties.HORIZONTAL_FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
		}

		@Override
		public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
			var shapes = hanging ? LONG : SHORT;
			return shapes[state.getValue(BlockTemplates.HORIZONTAL_FACING).get2DDataValue()];
		}
	}

}