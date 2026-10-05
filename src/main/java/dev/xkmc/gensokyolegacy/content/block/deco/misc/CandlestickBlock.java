package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.AnimateTickBlockMethod;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.ShapeUpdateBlockMethod;
import dev.xkmc.l2modularblock.mult.SurviveBlockMethod;
import dev.xkmc.l2modularblock.mult.ToolModifyBlockMethod;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

/**
 * 烛台。落地摆件，底下要能站东西；可以像原版蜡烛一样点起来，只有点着的时候才发光。
 *
 * <p>模型来自 32×32 的单块贴图，四面基本对称，朝向状态只用来转模型角度。
 * 三个烛芯的顶端坐标在 {@link #FLAME_OFFSETS} 里，火焰粒子和熄灭的烟都从那三点出，
 * 并且跟着 {@link #FACING} 一起转。
 *
 * <p>点亮/熄灭照搬原版 {@code AbstractCandleBlock}：打火石（以及火球）经
 * {@link ItemAbilities#FIRESTARTER_LIGHT} 把它设成 lit，空手右键再掐灭。
 * 没有用原版的 {@code CandleBlock#canLight}，因为那条路径要求方块在
 * {@code #minecraft:candles} 标签里、并且同时带 {@code lit} 和 {@code waterlogged}
 * 两个属性，而烛台不蓄水。
 */
public class CandlestickBlock implements CreateBlockStateBlockMethod, DefaultStateBlockMethod,
		SurviveBlockMethod, ShapeUpdateBlockMethod, ShapeBlockMethod,
		AnimateTickBlockMethod, ToolModifyBlockMethod, UseItemOnBlockMethod {

	/** 朝向由 {@link BlockTemplates#HORIZONTAL} 加上，这里只引用它的属性。 */
	public static final DirectionProperty FACING = BlockTemplates.HORIZONTAL_FACING;

	/** 与原版蜡烛同一个属性名，这样打火石/火焰弹的默认处理才能认出它。 */
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	/**
	 * 底盘 y 0..1、杆身 y 1..8、烛台碗 y 9..13。长轴 x 比短轴 z 宽，所以朝向转了看得出来。
	 */
	public static final VoxelShape[] SHAPES =
			MagicTableBlock.allFaces(new VoxelBuilder(1, 0, 6, 15, 13, 10));

	/**
	 * 三支蜡烛的火焰位置，相对方块原点的偏移，按 {@link Direction#get2DDataValue()} 索引，
	 * 和 {@link #SHAPES} 同一套查法。中间那支的烛芯顶端在 y 14，两侧两支在 y 12；
	 * 火焰都比芯尖高一格，和原版把粒子放在 y 8（芯尖 y 7 之上）一样。
	 *
	 * <p>只列朝北的一份再转出来：两条侧臂要跟着朝向从 x 轴转到 z 轴上去。
	 * 转法照抄 {@link VoxelBuilder#rotateFromNorth}，也就是 {@link MagicTableBlock#allFaces}
	 * 用的那个，这样火焰跟碰撞盒、跟 blockstate 里转过 {@code toYRot + 180} 的模型是一处的。
	 * （`Vec3#yRot` 和 blockstate 的模型旋转互为逆旋转，严格说正负号是对不上的；这里看不出来，
	 * 因为两点关于方块中心对称，转哪边都是同一对。换成人造的不对称烛台就要重新对了。）
	 */
	private static final Vec3[][] FLAME_OFFSETS = allFaces(
			new Vec3(0.5D, 15 / 16D, 0.5D),
			new Vec3(3 / 16D, 13 / 16D, 0.5D),
			new Vec3(13 / 16D, 13 / 16D, 0.5D));

	/** 朝北的三个点转出四个朝向，角度与 {@link VoxelBuilder#rotateFromNorth} 一致。 */
	private static Vec3[][] allFaces(Vec3... northPoints) {
		Vec3[][] ans = new Vec3[4][];
		for (var dir : Direction.Plane.HORIZONTAL) {
			float angle = (float) ((180 - dir.toYRot()) / 180 * Math.PI);
			Vec3[] turned = new Vec3[northPoints.length];
			for (int i = 0; i < northPoints.length; i++) {
				// yRot 是绕原点转的，偏移得先挪到方块中心再转回来，跟 VoxelBuilder 减 8 加 8 一个意思
				turned[i] = northPoints[i].subtract(0.5D, 0, 0.5D).yRot(angle).add(0.5D, 0, 0.5D);
			}
			ans[dir.get2DDataValue()] = turned;
		}
		return ans;
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(FACING, Direction.NORTH).setValue(LIT, false);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	@Override
	public BlockState updateShape(Block self, BlockState current, BlockState old, Direction from,
			BlockState source, LevelAccessor level, BlockPos pos, BlockPos sourcePos) {
		if (from == Direction.DOWN && !source.isFaceSturdy(level, sourcePos, Direction.UP)) {
			return Blocks.AIR.defaultBlockState();
		}
		return current;
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(FACING).get2DDataValue()];
	}

	@Override
	public @Nullable BlockState getToolModifiedState(Block self, @Nullable BlockState current, BlockState old,
			UseOnContext ctx, ItemAbility ability, boolean simulate) {
		// 打火石和火焰弹都走 FIRESTARTER_LIGHT 这条路：前者自带音效和耐久，
		// 后者因此不会在地上放火，而是点着烛台——和原版蜡烛一致。
		// 其他能力交回 current，别把 delegate 已经算出来的结果抹掉。
		return ability == ItemAbilities.FIRESTARTER_LIGHT && !old.getValue(LIT)
				? old.setValue(LIT, true)
				: current;
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player pl, InteractionHand hand, BlockHitResult result) {
		// 同原版 CandleBlock：必须空手，且要有建造权限，避免拿着方块右键就误熄。
		if (stack.isEmpty() && pl.getAbilities().mayBuild && state.getValue(LIT)) {
			extinguish(pl, level, pos, state);
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT)) {
			for (Vec3 each : flames(state)) {
				addFlame(level, each.add(pos.getX(), pos.getY(), pos.getZ()), random);
			}
		}
	}

	/** 当前朝向下三个火焰的位置。 */
	private static Vec3[] flames(BlockState state) {
		return FLAME_OFFSETS[state.getValue(FACING).get2DDataValue()];
	}

	/** 掐灭：改状态、三点冒烟、播 {@code candle_extinguish}。 */
	public static void extinguish(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(LIT, false), 11);
		for (Vec3 each : flames(state)) {
			level.addParticle(ParticleTypes.SMOKE,
					pos.getX() + each.x, pos.getY() + each.y, pos.getZ() + each.z, 0, 0.1F, 0);
		}
		level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
	}

	/** 一处火焰：常驻的小火焰粒子，外加三成概率的烟和一成七概率的噼啪声。比例抄自原版。 */
	private static void addFlame(Level level, Vec3 pos, RandomSource random) {
		float f = random.nextFloat();
		if (f < 0.3F) {
			level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0, 0, 0);
			if (f < 0.17F) {
				level.playLocalSound(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
						SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS,
						1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
			}
		}
		level.addParticle(ParticleTypes.SMALL_FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		var model = pvd.models().getBuilder("block/" + name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/candlestick")))
				.texture("all", pvd.modLoc("block/deco/candlestick"))
				.texture("particle", pvd.modLoc("block/deco/candlestick"))
				.renderType("cutout");
		// 点亮和不点亮共用一个模型，跟原版一样——原版那张 candle_lit 贴图只是把烛身顶端
		// 烘了一点暖色进去，真正说明"点着了"的是三点火焰粒子和 15 级光照。
		pvd.horizontalBlock(block, state -> model);
	}

}
