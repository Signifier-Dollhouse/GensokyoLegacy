package dev.xkmc.gensokyolegacy.content.entity.behavior.task.core;

import dev.xkmc.gensokyolegacy.content.block.door.SlidingDoor;
import dev.xkmc.gensokyolegacy.content.block.door.SlidingDoorUtils;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class YoukaiSmartDoorTask<E extends SmartYoukaiEntity> extends Behavior<E> {

	private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
	private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = 3.0;
	private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = 2.0;

	@Nullable
	private Node node;
	private int cooldown;

	public YoukaiSmartDoorTask() {
		super(Map.of(
				MemoryModuleType.PATH, MemoryStatus.REGISTERED,
				MemoryModuleType.DOORS_TO_CLOSE, MemoryStatus.REGISTERED,
				MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.REGISTERED), 0, 0);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
		return true;
	}

	@Override
	protected void tick(ServerLevel level, E entity, long gameTime) {
		tryOpenDoors(level, entity);
		closeDoors(level, entity);
	}

	private void tryOpenDoors(ServerLevel level, E entity) {
		Path path = BrainUtils.getMemory(entity, MemoryModuleType.PATH);
		if (path == null || path.notStarted() || path.isDone()) return;
		if (entity.navCtrl.isFlying()) return;
		Node next = path.getNextNode();
		if (Objects.equals(this.node, next)) {
			this.cooldown = COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE;
		} else if (this.cooldown-- > 0) {
			return;
		}
		this.node = next;
		Set<GlobalPos> doors = BrainUtils.getMemory(entity, MemoryModuleType.DOORS_TO_CLOSE);
		doors = tryOpenDoor(level, entity, doors, path.getPreviousNode());
		doors = tryOpenDoor(level, entity, doors, next);
		if (doors != null) {
			BrainUtils.setMemory(entity, MemoryModuleType.DOORS_TO_CLOSE, doors);
		}
	}

	@Nullable
	private Set<GlobalPos> tryOpenDoor(ServerLevel level, E entity, @Nullable Set<GlobalPos> doors, @Nullable Node node) {
		if (node == null) return doors;
		BlockPos pos = node.asBlockPos();
		BlockState state = level.getBlockState(pos);
		if (state.is(BlockTags.MOB_INTERACTABLE_DOORS) && state.getBlock() instanceof DoorBlock door) {
			if (!door.isOpen(state)) {
				door.setOpen(entity, level, state, pos, true);
				doors = rememberDoorToClose(entity, doors, level, pos);
			}
		} else if (state.is(GLTagGen.SLIDING_DOOR)) {
			BlockPos seat = SlidingDoorUtils.tryOpen(level, pos);
			if (seat != null) {
				doors = rememberDoorToClose(entity, doors, level, seat);
			}
		}
		return doors;
	}

	private Set<GlobalPos> rememberDoorToClose(E entity, @Nullable Set<GlobalPos> doors, ServerLevel level, BlockPos pos) {
		GlobalPos gpos = GlobalPos.of(level.dimension(), pos);
		if (doors == null) {
			doors = new HashSet<>();
			doors.add(gpos);
			BrainUtils.setMemory(entity, MemoryModuleType.DOORS_TO_CLOSE, doors);
		} else {
			doors.add(gpos);
		}
		return doors;
	}

	private void closeDoors(ServerLevel level, E entity) {
		Set<GlobalPos> doors = BrainUtils.getMemory(entity, MemoryModuleType.DOORS_TO_CLOSE);
		if (doors == null) return;
		Path path = BrainUtils.getMemory(entity, MemoryModuleType.PATH);
		Node prev = null;
		Node next = null;
		if (path != null && !path.notStarted() && !path.isDone()) {
			prev = path.getPreviousNode();
			next = path.getNextNode();
		}
		List<LivingEntity> nearby = BrainUtils.getMemory(entity, MemoryModuleType.NEAREST_LIVING_ENTITIES);
		for (Iterator<GlobalPos> it = doors.iterator(); it.hasNext(); ) {
			GlobalPos gpos = it.next();
			BlockPos pos = gpos.pos();
			if ((prev != null && prev.asBlockPos().equals(pos)) || (next != null && next.asBlockPos().equals(pos))) {
				continue;
			}
			if (gpos.dimension() != level.dimension() || !pos.closerToCenterThan(entity.position(), SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN)) {
				it.remove();
				continue;
			}
			if (tryCloseDoor(level, entity, pos, nearby)) {
				continue;
			}
			it.remove();
		}
	}

	private boolean tryCloseDoor(ServerLevel level, E entity, BlockPos pos, @Nullable List<LivingEntity> nearby) {
		BlockState state = level.getBlockState(pos);
		if (state.is(BlockTags.MOB_INTERACTABLE_DOORS) && state.getBlock() instanceof DoorBlock door) {
			if (door.isOpen(state)) {
				if (!holdingForOthers(entity, pos, nearby)) {
					door.setOpen(entity, level, state, pos, false);
					return false;
				}
				return true;
			}
			return false;
		}
		if (state.is(GLTagGen.SLIDING_DOOR)) {
			return false;
		}
		if (state.isAir()) {
			BlockPos panel = findPanel(level, pos);
			if (panel == null) return false;
			if (holdingForOthers(entity, pos, nearby)) return true;
			return !SlidingDoorUtils.tryClose(level, panel);
		}
		return false;
	}

	@Nullable
	private BlockPos findPanel(ServerLevel level, BlockPos seat) {
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockPos cand = seat.relative(dir);
			BlockState state = level.getBlockState(cand);
			if (state.is(GLTagGen.SLIDING_DOOR)) {
				BlockPos bottom = SlidingDoor.bottom(level, cand);
				if (SlidingDoorUtils.seatOf(level.getBlockState(bottom), bottom).equals(seat)) {
					return bottom;
				}
			}
		}
		return null;
	}

	private boolean holdingForOthers(E entity, BlockPos pos, @Nullable List<LivingEntity> nearby) {
		if (nearby == null) return false;
		for (LivingEntity other : nearby) {
			if (other == entity || !(other instanceof SmartYoukaiEntity)) continue;
			if (pos.closerToCenterThan(other.position(), MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS) && isMobComingThroughDoor(other.getBrain(), pos)) {
				return true;
			}
		}
		return false;
	}

	private boolean isMobComingThroughDoor(Brain<?> brain, BlockPos pos) {
		Path path = BrainUtils.getMemory(brain, MemoryModuleType.PATH);
		if (path == null || path.isDone()) return false;
		Node prev = path.getPreviousNode();
		Node next = path.getNextNode();
		return (prev != null && pos.equals(prev.asBlockPos())) || (next != null && pos.equals(next.asBlockPos()));
	}

}
