package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.content.item.debug.StructureMirrorData.Entry;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug stick that records a piece tree and places vanilla jigsaw connectors between its pieces.
 *
 * @see "doc/design/structure/mirror_stick.md"
 */
public class StructureMirrorStick extends Item {

	public StructureMirrorStick(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
			StructureMirrorData data = GLItems.STRUCTURE_MIRROR.get(stack);
			if (player.isShiftKeyDown()) repair(server, sp, data);
			else print(server, sp, data);
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		// Consume the click on the client so it never falls through to a second `use` (air action).
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		if (!(context.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.PASS;
		ItemStack stack = context.getItemInHand();
		StructureMirrorData data = GLItems.STRUCTURE_MIRROR.get(stack);
		if (data == null || data.isEmpty()) return acquire(level, sp, stack, context.getClickedPos());
		return attach(level, sp, stack, data, context.getClickedPos());
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		if (!state.is(Blocks.JIGSAW)) return true;
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
			// Delete after the break event: it sends the old jigsaw state, which would otherwise stick.
			ItemStack stack = player.getMainHandItem();
			server.getServer().execute(() -> delete(server, sp, stack, pos));
		}
		return false;
	}

	/**
	 * With no data, the clicked block must lie in a structure block's bound; that piece becomes the root.
	 */
	private static InteractionResult acquire(ServerLevel level, ServerPlayer sp, ItemStack stack, BlockPos pos) {
		var found = StructureMirrorHelper.findPiece(level, pos);
		if (found == null) return fail(sp, GLLang.ItemDebug.MIRROR_NO_STRUCTURE.get());
		BoundingBox bound = found.bound();
		Entry root = new Entry(bound, found.block(), -1, null);
		stack.set(GLItems.STRUCTURE_MIRROR, new StructureMirrorData(List.of(root)));
		sp.sendSystemMessage(GLLang.ItemDebug.MIRROR_ROOT.get(found.name().toString()));
		GensokyoLegacy.LOGGER.info("Structure mirror: root {} at {} bound {}..{}",
				found.name(), found.block().toShortString(), bound.minX() + "," + bound.minY() + "," + bound.minZ(),
				bound.maxX() + "," + bound.maxY() + "," + bound.maxZ());
		return InteractionResult.SUCCESS;
	}

	/**
	 * With data, a face click links the recorded piece to the unrecorded piece just outside that face.
	 */
	private static InteractionResult attach(ServerLevel level, ServerPlayer sp, ItemStack stack,
	                                       StructureMirrorData data, BlockPos pos) {
		int parentIndex = data.pieceAt(pos);
		if (parentIndex < 0) return fail(sp, GLLang.ItemDebug.MIRROR_NOT_RECORDED.get());
		Entry parent = data.entries().get(parentIndex);
		List<Direction> outside = new ArrayList<>(6);
		for (Direction dir : Direction.values()) {
			if (!parent.contains(pos.relative(dir))) outside.add(dir);
		}
		if (outside.isEmpty()) return fail(sp, GLLang.ItemDebug.MIRROR_NOT_FACE.get());
		if (outside.size() > 1) {
			return fail(sp, GLLang.ItemDebug.MIRROR_CORNER.get(outside.size()));
		}
		Direction direction = outside.getFirst();
		BlockPos childJig = pos.relative(direction);
		var child = StructureMirrorHelper.findPiece(level, childJig);
		if (child == null) {
			return fail(sp, GLLang.ItemDebug.MIRROR_NO_CHILD.get(childJig.toShortString()));
		}
		if (data.recorded(child.block()) >= 0 || data.pieceAt(childJig) >= 0) {
			return fail(sp, GLLang.ItemDebug.MIRROR_ALREADY.get());
		}
		ResourceLocation parentName = StructureMirrorHelper.nameAt(level, parent.block());
		if (parentName == null) {
			return fail(sp, GLLang.ItemDebug.MIRROR_NO_PARENT_NAME.get(parent.block().toShortString()));
		}
		InteractionResult blocked = checkFree(level, sp, pos, childJig);
		if (blocked != null) return blocked;
		if (!StructureMirrorHelper.applyPair(level, pos, childJig, parentName, child.name())) {
			return fail(sp, GLLang.ItemDebug.MIRROR_PLACE_FAILED.get());
		}
		stack.set(GLItems.STRUCTURE_MIRROR, data.addChild(parentIndex, child.bound(), child.block(), childJig));
		sp.sendSystemMessage(GLLang.ItemDebug.MIRROR_LINKED.get(parentName.toString(), child.name().toString()));
		GensokyoLegacy.LOGGER.info("Structure mirror: linked {} (parent) -> {} (child), jigsaws {} -> {}",
				parentName, child.name(), pos.toShortString(), childJig.toShortString());
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult checkFree(ServerLevel level, ServerPlayer sp, BlockPos parentJig, BlockPos childJig) {
		for (BlockPos pos : List.of(parentJig, childJig)) {
			if (level.getBlockState(pos).is(Blocks.JIGSAW)) {
				return fail(sp, GLLang.ItemDebug.MIRROR_JIGSAW.get(pos.toShortString()));
			}
			if (level.getBlockState(pos).is(Blocks.STRUCTURE_BLOCK)) {
				return fail(sp, GLLang.ItemDebug.MIRROR_STRUCTURE_BLOCK.get(pos.toShortString()));
			}
		}
		return null;
	}

	private static void print(ServerLevel level, ServerPlayer sp, StructureMirrorData data) {
		if (data == null || data.isEmpty()) {
			sp.sendSystemMessage(GLLang.ItemDebug.MIRROR_TREE_EMPTY.get());
			GensokyoLegacy.LOGGER.info("Structure mirror tree: <empty>");
			return;
		}
		List<String> lines = StructureMirrorHelper.describe(level, data);
		sp.sendSystemMessage(GLLang.ItemDebug.MIRROR_TREE.get(data.entries().size(), data.linkCount()));
		for (String line : lines) sp.sendSystemMessage(Component.literal(line));
		GensokyoLegacy.LOGGER.info("Structure mirror tree ({} entries):\n{}",
				data.entries().size(), String.join("\n", lines));
	}

	private static void repair(ServerLevel level, ServerPlayer sp, StructureMirrorData data) {
		if (data == null || data.isEmpty()) {
			fail(sp, GLLang.ItemDebug.MIRROR_NO_DATA.get());
			return;
		}
		var report = StructureMirrorHelper.repair(level, data);
		if (report.failed() == 0 && report.fixed() == 0) {
			sp.sendSystemMessage(GLLang.ItemDebug.MIRROR_REPAIR_OK.get(report.total()));
		} else {
			sp.sendSystemMessage(GLLang.ItemDebug.MIRROR_REPAIR_SUMMARY.get(
							report.total(), report.valid(), report.fixed(), report.failed())
					.withStyle(report.failed() > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN));
		}
		for (String line : report.log()) sp.sendSystemMessage(Component.literal(line));
		GensokyoLegacy.LOGGER.info("Structure mirror repair: {} entries, {} valid, {} fixed, {} failed\n{}",
				report.total(), report.valid(), report.fixed(), report.failed(), String.join("\n", report.log()));
	}

	/**
	 * Left-click handler: deletes a recorded pair together with its leaf child entry.
	 */
	public static void delete(ServerLevel level, ServerPlayer sp, ItemStack stack, BlockPos pos) {
		StructureMirrorData data = GLItems.STRUCTURE_MIRROR.get(stack);
		if (data == null || data.isEmpty()) {
			fail(sp, GLLang.ItemDebug.MIRROR_NO_DATA.get());
			return;
		}
		int index = data.pairAt(pos);
		if (index == -2) {
			fail(sp, GLLang.ItemDebug.MIRROR_AMBIGUOUS.get());
			return;
		}
		if (index < 0) {
			fail(sp, GLLang.ItemDebug.MIRROR_NOT_PAIR.get());
			return;
		}
		if (!data.isLeaf(index)) {
			fail(sp, GLLang.ItemDebug.MIRROR_NOT_LEAF.get(index, data.childCount(index)));
			return;
		}
		removeJigsaw(level, data.entries().get(index).childJigsaw());
		removeJigsaw(level, data.parentJigsaw(index));
		stack.set(GLItems.STRUCTURE_MIRROR, data.removeLeaf(index));
		sp.sendSystemMessage(GLLang.ItemDebug.MIRROR_DELETED.get(index));
		GensokyoLegacy.LOGGER.info("Structure mirror: removed pair and entry #{} at {}", index, pos.toShortString());
	}

	private static void removeJigsaw(ServerLevel level, BlockPos pos) {
		if (pos != null && level.getBlockState(pos).is(Blocks.JIGSAW)) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private static InteractionResult fail(ServerPlayer sp, Component reason) {
		sp.sendSystemMessage(reason.copy().withStyle(ChatFormatting.RED));
		GensokyoLegacy.LOGGER.info("Structure mirror: {}", reason.getString());
		return InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
		lines.add(GLLang.ItemDebug.MIRROR_USE_HINT.get());
		lines.add(GLLang.ItemDebug.MIRROR_LINK_HINT.get());
		lines.add(GLLang.ItemDebug.MIRROR_DELETE_HINT.get());
		lines.add(GLLang.ItemDebug.MIRROR_AIR_HINT.get());
		StructureMirrorData data = GLItems.STRUCTURE_MIRROR.get(stack);
		if (data != null && !data.isEmpty()) {
			lines.add(GLLang.ItemDebug.MIRROR_PIECES.get(data.entries().size()));
		}
	}

}