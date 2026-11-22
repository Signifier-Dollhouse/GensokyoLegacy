package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Authoring wand for a structure's interior: room boxes and the routing nodes between them. Records
 * a structure-block origin, then edits bounds and waypoints in the five modes; clicks never break
 * blocks.
 *
 * @see "doc/design/structure/interior_wand.md"
 */
public class InteriorWandItem extends Item {

	/** Creative resends the action every 5 ticks while the button is held; treat that as one click. */
	private static final int CLICK_COOLDOWN = 6;

	private static final Map<Player, Integer> LAST_CLICK = new WeakHashMap<>();

	public InteriorWandItem(Properties properties) {
		super(properties);
	}

	public static InteriorWandMode getMode(ItemStack stack) {
		return GLItems.INTERIOR_WAND_MODE.getOrDefault(stack, InteriorWandMode.BOUND);
	}

	/**
	 * Switches modes and drops any half-finished click, which belongs to the old mode.
	 */
	public static void setMode(ItemStack stack, InteriorWandMode mode) {
		GLItems.INTERIOR_WAND_MODE.set(stack, mode);
		InteriorWandData data = GLItems.INTERIOR_WAND_DATA.get(stack);
		if (data != null) GLItems.INTERIOR_WAND_DATA.set(stack, data.resetTransient());
	}

	public static ItemStack iconStack(InteriorWandMode mode) {
		ItemStack stack = new ItemStack(GLItems.INTERIOR_WAND.get());
		setMode(stack, mode);
		stack.set(DataComponents.ITEM_NAME, mode.displayName());
		return stack;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
			if (player.isShiftKeyDown()) {
				stack.remove(GLItems.INTERIOR_WAND_DATA.get());
				sp.sendSystemMessage(GLLang.ItemDebug.INTERIOR_CLEARED.get());
			} else if (getMode(stack) == InteriorWandMode.EXPORT) {
				export(server, sp, stack);
			} else {
				print(server, sp, stack);
			}
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		// Consume the click on the client so it never falls through to a second `use` (air action).
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		if (!(context.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.PASS;
		ItemStack stack = context.getItemInHand();
		InteriorWandData data = GLItems.INTERIOR_WAND_DATA.get(stack);
		if (data == null) return acquire(level, sp, stack, context.getClickedPos());
		return switch (getMode(stack)) {
			case BOUND -> bound(sp, stack, data, context.getClickedPos());
			case BOUND_EDIT -> edit(sp, stack, data, context.getClickedPos(), context.getClickedFace());
			case WAYPOINT -> waypoint(sp, stack, data, context.getClickedPos(), context.getClickedFace(), false);
			case DUAL_WAYPOINT -> waypoint(sp, stack, data, context.getClickedPos(), context.getClickedFace(), true);
			case EXPORT -> export(level, sp, stack);
		};
	}

	/**
	 * {@code canAttackBlock} is the only left-click hook that runs on the server, and it carries no
	 * clicked face; {@link #faceOf} recovers the face from where the player stands. Returning false
	 * keeps the block intact.
	 */
	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		if (level instanceof ServerLevel && player instanceof ServerPlayer sp && consumeClick(player)) {
			leftClick(sp, player.getMainHandItem(), pos, faceOf(player, pos));
		}
		return false;
	}

	private static boolean consumeClick(Player player) {
		Integer last = LAST_CLICK.put(player, player.tickCount);
		return last == null || player.tickCount < last || player.tickCount - last >= CLICK_COOLDOWN;
	}

	/**
	 * The face of {@code pos} that points at the player: the clicked face, approximately.
	 */
	private static Direction faceOf(Player player, BlockPos pos) {
		Vec3 dir = player.getEyePosition().subtract(Vec3.atCenterOf(pos));
		return Direction.getNearest(dir.x, dir.y, dir.z);
	}

	private static InteractionResult acquire(ServerLevel level, ServerPlayer sp, ItemStack stack, BlockPos pos) {
		InteriorWandData data = InteriorWandHelper.acquire(level, pos);
		if (data == null) return fail(sp, GLLang.ItemDebug.INTERIOR_NO_STRUCTURE.get());
		stack.set(GLItems.INTERIOR_WAND_DATA, data);
		var name = StructureMirrorHelper.nameAt(level, data.structureBlock());
		sp.sendSystemMessage(GLLang.ItemDebug.INTERIOR_ORIGIN.get(
				name == null ? data.structureBlock().toShortString() : name.toString()));
		GensokyoLegacy.LOGGER.info("Interior wand: origin at {} from structure block {}",
				data.origin().toShortString(), data.structureBlock().toShortString());
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult bound(ServerPlayer sp, ItemStack stack, InteriorWandData data, BlockPos pos) {
		if (data.pendingStart() == null) {
			stack.set(GLItems.INTERIOR_WAND_DATA, data.withPendingStart(pos));
			return ok(sp, GLLang.ItemDebug.INTERIOR_BOUND_START.get(pos.toShortString()));
		}
		BoundingBox box = BoundingBox.fromCorners(data.pendingStart(), pos);
		InteriorWandData next = data.addRoom(box).withPendingStart(null);
		stack.set(GLItems.INTERIOR_WAND_DATA, next);
		return ok(sp, GLLang.ItemDebug.INTERIOR_BOUND_ADDED.get(next.rooms().size() - 1));
	}

	private static InteractionResult edit(ServerPlayer sp, ItemStack stack, InteriorWandData data,
	                                      BlockPos pos, Direction face) {
		int selected = data.selectedRoom();
		if (selected < 0) {
			int index = data.roomAt(pos);
			if (index < 0) return fail(sp, GLLang.ItemDebug.INTERIOR_NOT_IN_BOUND.get());
			stack.set(GLItems.INTERIOR_WAND_DATA, data.withSelectedRoom(index));
			return ok(sp, GLLang.ItemDebug.INTERIOR_BOUND_SELECTED.get(index));
		}
		if (!data.rooms().get(selected).isInside(pos)) {
			stack.set(GLItems.INTERIOR_WAND_DATA, data.withSelectedRoom(-1));
			return ok(sp, GLLang.ItemDebug.INTERIOR_DESELECTED.get());
		}
		BoundingBox expanded = expand(data.rooms().get(selected), face);
		stack.set(GLItems.INTERIOR_WAND_DATA, data.setRoom(selected, expanded));
		return ok(sp, GLLang.ItemDebug.INTERIOR_BOUND_EXPANDED.get(selected, face.getName()));
	}

	/**
	 * Left-click: delete a bound, shrink the selected bound, or remove a waypoint. Called from
	 * {@link #canAttackBlock}, which keeps the clicked block intact.
	 */
	public static void leftClick(ServerPlayer sp, ItemStack stack,
	                             BlockPos pos, Direction face) {
		InteriorWandData data = GLItems.INTERIOR_WAND_DATA.get(stack);
		if (data == null) {
			fail(sp, GLLang.ItemDebug.INTERIOR_NO_DATA.get());
			return;
		}
		switch (getMode(stack)) {
			case BOUND -> {
				int index = data.roomAt(pos);
				if (index < 0) {
					fail(sp, GLLang.ItemDebug.INTERIOR_NOT_IN_BOUND.get());
					return;
				}
				stack.set(GLItems.INTERIOR_WAND_DATA, data.removeRoom(index));
				ok(sp, GLLang.ItemDebug.INTERIOR_BOUND_DELETED.get(index));
			}
			case BOUND_EDIT -> {
				int selected = data.selectedRoom();
				if (selected < 0) {
					fail(sp, GLLang.ItemDebug.INTERIOR_FIRST_BOUND.get());
					return;
				}
				if (!data.rooms().get(selected).isInside(pos)) {
					fail(sp, GLLang.ItemDebug.INTERIOR_NOT_IN_BOUND.get());
					return;
				}
				BoundingBox shrunk = shrink(data.rooms().get(selected), face);
				if (shrunk == null) {
					fail(sp, GLLang.ItemDebug.INTERIOR_BOUND_TOO_SMALL.get(selected));
					return;
				}
				stack.set(GLItems.INTERIOR_WAND_DATA, data.setRoom(selected, shrunk));
				ok(sp, GLLang.ItemDebug.INTERIOR_BOUND_SHRUNK.get(selected, face.getName()));
			}
			case WAYPOINT, DUAL_WAYPOINT -> {
				int index = nodeAt(data, pos, face);
				if (index < 0) {
					fail(sp, GLLang.ItemDebug.INTERIOR_NO_NODE.get());
					return;
				}
				stack.set(GLItems.INTERIOR_WAND_DATA, data.removeNode(index));
				ok(sp, GLLang.ItemDebug.INTERIOR_NODE_DELETED.get(index));
			}
		}
	}

	private static int nodeAt(InteriorWandData data, BlockPos pos, Direction face) {
		int index = data.nodeAt(pos.relative(face));
		return index >= 0 ? index : data.nodeAt(pos);
	}

	private static InteractionResult waypoint(ServerPlayer sp, ItemStack stack, InteriorWandData data,
	                                          BlockPos pos, Direction face, boolean dual) {
		if (data.nodeRoomA() == InteriorWandData.NO_ROOM) {
			int index = data.roomAt(pos);
			if (index < 0) return fail(sp, GLLang.ItemDebug.INTERIOR_FIRST_BOUND.get());
			stack.set(GLItems.INTERIOR_WAND_DATA, data.withNodeSelection(index, InteriorWandData.NO_ROOM));
			return ok(sp, GLLang.ItemDebug.INTERIOR_ROOM_A.get(index));
		}
		if (data.nodeRoomB() == InteriorWandData.NO_ROOM) {
			int index = data.roomAt(pos);
			if (index == data.nodeRoomA()) return fail(sp, GLLang.ItemDebug.INTERIOR_SAME_BOUND.get());
			if (dual && index < 0) return fail(sp, GLLang.ItemDebug.INTERIOR_DUAL_EXIT.get());
			int roomB = index < 0 ? InteriorWandData.EXIT : index;
			stack.set(GLItems.INTERIOR_WAND_DATA, data.withNodeSelection(data.nodeRoomA(), roomB));
			return ok(sp, roomB < 0 ?
					GLLang.ItemDebug.INTERIOR_ROOM_EXIT.get() :
					GLLang.ItemDebug.INTERIOR_ROOM_B.get(roomB));
		}
		BlockPos waypoint = pos.relative(face);
		if (!dual) return addNode(sp, stack, data, waypoint, waypoint);
		BlockPos a = data.pendingNodeA();
		if (a == null) {
			stack.set(GLItems.INTERIOR_WAND_DATA, data.withPendingNodeA(waypoint));
			return ok(sp, GLLang.ItemDebug.INTERIOR_WAYPOINT_A.get(waypoint.toShortString()));
		}
		return addNode(sp, stack, data, a, waypoint);
	}

	private static InteractionResult addNode(ServerPlayer sp, ItemStack stack, InteriorWandData data,
	                                         BlockPos a, BlockPos b) {
		InteriorWandData next = data.addNode(new InteriorWandData.Node(a, b, data.nodeRoomA(), data.nodeRoomB()));
		stack.set(GLItems.INTERIOR_WAND_DATA, next);
		return ok(sp, GLLang.ItemDebug.INTERIOR_NODE_ADDED.get(
				next.nodes().size() - 1, data.nodeRoomA(), data.nodeRoomB()));
	}

	private static BoundingBox expand(BoundingBox box, Direction face) {
		return switch (face) {
			case EAST -> new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY(), box.maxZ());
			case WEST -> new BoundingBox(box.minX() - 1, box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
			case UP -> new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY() + 1, box.maxZ());
			case DOWN -> new BoundingBox(box.minX(), box.minY() - 1, box.minZ(), box.maxX(), box.maxY(), box.maxZ());
			case SOUTH -> new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() + 1);
			case NORTH -> new BoundingBox(box.minX(), box.minY(), box.minZ() - 1, box.maxX(), box.maxY(), box.maxZ());
		};
	}

	@Nullable
	private static BoundingBox shrink(BoundingBox box, Direction face) {
		return switch (face) {
			case EAST -> box.maxX() <= box.minX() ? null :
					new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX() - 1, box.maxY(), box.maxZ());
			case WEST -> box.minX() >= box.maxX() ? null :
					new BoundingBox(box.minX() + 1, box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
			case UP -> box.maxY() <= box.minY() ? null :
					new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY() - 1, box.maxZ());
			case DOWN -> box.minY() >= box.maxY() ? null :
					new BoundingBox(box.minX(), box.minY() + 1, box.minZ(), box.maxX(), box.maxY(), box.maxZ());
			case SOUTH -> box.maxZ() <= box.minZ() ? null :
					new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() - 1);
			case NORTH -> box.minZ() >= box.maxZ() ? null :
					new BoundingBox(box.minX(), box.minY(), box.minZ() + 1, box.maxX(), box.maxY(), box.maxZ());
		};
	}

	private static void print(ServerLevel level, ServerPlayer sp, ItemStack stack) {
		InteriorWandData data = GLItems.INTERIOR_WAND_DATA.get(stack);
		if (data == null) {
			fail(sp, GLLang.ItemDebug.INTERIOR_NO_DATA.get());
			return;
		}
		var placement = InteriorWandHelper.placement(level, data);
		if (!placement.present()) {
			sp.sendSystemMessage(GLLang.ItemDebug.INTERIOR_EXPORT_NO_BLOCK.get());
		}
		var name = StructureMirrorHelper.nameAt(level, data.structureBlock());
		sp.sendSystemMessage(GLLang.ItemDebug.INTERIOR_EXPORT_HEADER.get(
				name == null ? data.structureBlock().toShortString() : name.toString(),
				data.rooms().size(), data.nodes().size()));
		List<String> lines = InteriorWandHelper.exportLines(level, data);
		for (String line : lines) sp.sendSystemMessage(Component.literal(line));
		GensokyoLegacy.LOGGER.info("Interior wand export ({} rooms, {} nodes):\n{}",
				data.rooms().size(), data.nodes().size(), String.join("\n", lines));
	}

	private static InteractionResult export(ServerLevel level, ServerPlayer sp, ItemStack stack) {
		InteriorWandData data = GLItems.INTERIOR_WAND_DATA.get(stack);
		if (data == null) return fail(sp, GLLang.ItemDebug.INTERIOR_NO_DATA.get());
		try {
			var path = InteriorWandHelper.write(level, data);
			sp.sendSystemMessage(GLLang.ItemDebug.INTERIOR_EXPORT_OK.get(path.toString()));
			GensokyoLegacy.LOGGER.info("Interior wand: wrote {} rooms and {} nodes to {}",
					data.rooms().size(), data.nodes().size(), path.toAbsolutePath());
			return InteractionResult.SUCCESS;
		} catch (Exception e) {
			sp.sendSystemMessage(GLLang.ItemDebug.INTERIOR_EXPORT_FAILED.get(e.getMessage()));
			GensokyoLegacy.LOGGER.error("Interior wand: could not export", e);
			return InteractionResult.FAIL;
		}
	}

	private static InteractionResult ok(ServerPlayer sp, Component message) {
		sp.sendSystemMessage(message.copy().withStyle(ChatFormatting.GREEN));
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult fail(ServerPlayer sp, Component reason) {
		sp.sendSystemMessage(reason.copy().withStyle(ChatFormatting.RED));
		GensokyoLegacy.LOGGER.info("Interior wand: {}", reason.getString());
		return InteractionResult.FAIL;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
		lines.add(GLLang.ItemDebug.INTERIOR_MODE.get(getMode(stack).displayName()).withStyle(ChatFormatting.GRAY));
		lines.add(GLLang.ItemDebug.INTERIOR_USE_HINT.get());
		lines.add(GLLang.ItemDebug.INTERIOR_BOUND_HINT.get());
		lines.add(GLLang.ItemDebug.INTERIOR_EDIT_HINT.get());
		lines.add(GLLang.ItemDebug.INTERIOR_WAYPOINT_HINT.get());
		lines.add(GLLang.ItemDebug.INTERIOR_DUAL_HINT.get());
		lines.add(GLLang.ItemDebug.INTERIOR_EXPORT_HINT.get());
		lines.add(GLLang.ItemDebug.INTERIOR_AIR_HINT.get());
		InteriorWandData data = GLItems.INTERIOR_WAND_DATA.get(stack);
		if (data != null) {
			lines.add(GLLang.ItemDebug.INTERIOR_COUNTS.get(data.rooms().size(), data.nodes().size()));
		}
	}

}
