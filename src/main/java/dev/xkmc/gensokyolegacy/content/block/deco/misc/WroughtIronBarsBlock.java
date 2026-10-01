package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

import java.util.Map;

/**
 * 铁艺护栏。连接逻辑与 {@link IronBarsBlock} 完全一致,额外记住自己是不是一竖列的最上面一格:
 * 是的话换用带顶横档的 {@code wrought_iron_bar_top} 贴图。
 */
public class WroughtIronBarsBlock extends IronBarsBlock {

	private static final MapCodec<WroughtIronBarsBlock> CODEC = simpleCodec(WroughtIronBarsBlock::new);

	/** 竖列顶端(正上方没有同类方块),此时使用带顶横档的贴图 */
	public static final BooleanProperty TOP = BooleanProperty.create("top");

	private static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
			Direction.NORTH, IronBarsBlock.NORTH,
			Direction.EAST, IronBarsBlock.EAST,
			Direction.SOUTH, IronBarsBlock.SOUTH,
			Direction.WEST, IronBarsBlock.WEST);

	public WroughtIronBarsBlock(Properties properties) {
		super(properties);
		// 孤零零的一格也是竖列顶端
		registerDefaultState(stateDefinition.any().setValue(TOP, true));
	}

	@Override
	public MapCodec<? extends WroughtIronBarsBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(TOP);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockGetter level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		return super.getStateForPlacement(context).setValue(TOP, isTop(level.getBlockState(pos.above())));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState facing, LevelAccessor level,
	                                BlockPos pos, BlockPos facingPos) {
		BlockState ans = super.updateShape(state, dir, facing, level, pos, facingPos);
		if (dir == Direction.UP) ans = ans.setValue(TOP, isTop(facing));
		return ans;
	}

	private boolean isTop(BlockState above) {
		return !above.is(this);
	}

	/**
	 * 分区照抄原版铁栏杆,额外按 {@link #TOP} 各出一套贴图。几何直接继承原版模型,
	 * 只有贴图不同。
	 */
	public static void buildStates(DataGenContext<Block, WroughtIronBarsBlock> ctx, RegistrateBlockstateProvider pvd) {
		String name = ctx.getName();
		// 立柱两端的封口只取到中间那道横档,两张贴图在这里一模一样,所以共用一个模型
		BlockModelBuilder ends = model(pvd, name, "post_ends", false);
		BlockModelBuilder post = model(pvd, name, "post", false);
		BlockModelBuilder postTop = model(pvd, name, "post", true);
		BlockModelBuilder cap = model(pvd, name, "cap", false);
		BlockModelBuilder capTop = model(pvd, name, "cap", true);
		BlockModelBuilder capAlt = model(pvd, name, "cap_alt", false);
		BlockModelBuilder capAltTop = model(pvd, name, "cap_alt", true);
		BlockModelBuilder side = model(pvd, name, "side", false);
		BlockModelBuilder sideTop = model(pvd, name, "side", true);
		BlockModelBuilder sideAlt = model(pvd, name, "side_alt", false);
		BlockModelBuilder sideAltTop = model(pvd, name, "side_alt", true);

		var builder = pvd.getMultipartBuilder(ctx.get());
		builder.part().modelFile(ends).addModel();
		for (boolean top : new boolean[]{false, true}) {
			// 四面都没连上的时候画完整的十字立柱
			var lonely = builder.part().modelFile(top ? postTop : post).addModel().condition(TOP, top);
			for (var dir : Direction.Plane.HORIZONTAL) lonely = lonely.condition(CONNECTIONS.get(dir), false);
			lonely.end();
			for (var dir : Direction.Plane.HORIZONTAL) {
				// 侧板分南北、东西两组贴图方向
				boolean alt = dir == Direction.SOUTH || dir == Direction.WEST;
				int rotY = (int) dir.toYRot() % 180;
				// 只连上一面时换成半截立柱,免得跟侧板重叠
				var capped = builder.part()
						.modelFile(top ? (alt ? capAltTop : capTop) : (alt ? capAlt : cap))
						.rotationY(rotY).addModel().condition(TOP, top).condition(CONNECTIONS.get(dir), true);
				for (var other : Direction.Plane.HORIZONTAL) {
					if (other != dir) capped = capped.condition(CONNECTIONS.get(other), false);
				}
				capped.end();
				// 连接方向上的侧板
				builder.part()
						.modelFile(top ? (alt ? sideAltTop : sideTop) : (alt ? sideAlt : side))
						.rotationY(rotY).addModel().condition(TOP, top).condition(CONNECTIONS.get(dir), true)
						.end();
			}
		}
	}

	/**
	 * @param part 借用原版铁栏杆的那一份几何,即 {@code minecraft:block/iron_bars_*}
	 */
	private static BlockModelBuilder model(RegistrateBlockstateProvider pvd, String name, String part, boolean top) {
		ResourceLocation tex = pvd.modLoc("block/deco/wrought_iron_bar" + (top ? "_top" : ""));
		return pvd.models().getBuilder("block/" + name + "_" + part + (top ? "_top" : ""))
				.parent(new ModelFile.UncheckedModelFile("block/iron_bars_" + part))
				.texture("bars", tex)
				.texture("edge", tex)
				.texture("particle", tex)
				.renderType("cutout");
	}

}