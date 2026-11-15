package dev.xkmc.gensokyolegacy.content.item.debug;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Interior rooms and routing nodes recorded on the interior wand, in world coords. Rooms get
 * {@code room_<index>} names on export; node room indices point into {@link #rooms()}.
 *
 * @see "doc/design/structure/interior_wand.md"
 */
public record InteriorWandData(
		BlockPos structureBlock,
		BlockPos origin,
		List<BoundingBox> rooms,
		List<Node> nodes,
		int selectedRoom,
		@Nullable BlockPos pendingStart,
		int nodeRoomA,
		int nodeRoomB,
		@Nullable BlockPos pendingNodeA
) {

	public static final int NO_ROOM = -2;
	public static final int EXIT = -1;

	public static final Codec<InteriorWandData> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("structure_block").forGetter(InteriorWandData::structureBlock),
			BlockPos.CODEC.fieldOf("origin").forGetter(InteriorWandData::origin),
			BoundingBox.CODEC.listOf().fieldOf("rooms").forGetter(InteriorWandData::rooms),
			Node.CODEC.listOf().fieldOf("nodes").forGetter(InteriorWandData::nodes),
			Codec.INT.optionalFieldOf("selected_room", -1).forGetter(InteriorWandData::selectedRoom),
			BlockPos.CODEC.optionalFieldOf("pending_start")
					.forGetter(d -> Optional.ofNullable(d.pendingStart())),
			Codec.INT.optionalFieldOf("node_room_a", NO_ROOM).forGetter(InteriorWandData::nodeRoomA),
			Codec.INT.optionalFieldOf("node_room_b", NO_ROOM).forGetter(InteriorWandData::nodeRoomB),
			BlockPos.CODEC.optionalFieldOf("pending_node_a")
					.forGetter(d -> Optional.ofNullable(d.pendingNodeA()))
	).apply(i, (structureBlock, origin, rooms, nodes, selectedRoom, pendingStart,
	           nodeRoomA, nodeRoomB, pendingNodeA) -> new InteriorWandData(
			structureBlock, origin, rooms, nodes, selectedRoom,
			pendingStart.orElse(null), nodeRoomA, nodeRoomB, pendingNodeA.orElse(null)
	)));

	public InteriorWandData {
		rooms = List.copyOf(rooms);
		nodes = List.copyOf(nodes);
	}

	public static InteriorWandData empty(BlockPos structureBlock, BlockPos origin) {
		return new InteriorWandData(structureBlock, origin, List.of(), List.of(),
				-1, null, NO_ROOM, NO_ROOM, null);
	}

	/**
	 * Index of the first recorded bound containing {@code pos}, or -1.
	 */
	public int roomAt(BlockPos pos) {
		for (int i = 0; i < rooms.size(); i++) {
			if (rooms.get(i).isInside(pos)) return i;
		}
		return -1;
	}

	/**
	 * Index of the first node whose either waypoint is {@code pos}, or -1.
	 */
	public int nodeAt(BlockPos pos) {
		for (int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			if (node.posA().equals(pos) || node.posB().equals(pos)) return i;
		}
		return -1;
	}

	public InteriorWandData addRoom(BoundingBox bound) {
		List<BoundingBox> list = new ArrayList<>(rooms);
		list.add(bound);
		return new InteriorWandData(structureBlock, origin, list, nodes, selectedRoom,
				pendingStart, nodeRoomA, nodeRoomB, pendingNodeA);
	}

	public InteriorWandData setRoom(int index, BoundingBox bound) {
		if (index < 0 || index >= rooms.size()) return this;
		List<BoundingBox> list = new ArrayList<>(rooms);
		list.set(index, bound);
		return new InteriorWandData(structureBlock, origin, list, nodes, selectedRoom,
				pendingStart, nodeRoomA, nodeRoomB, pendingNodeA);
	}

	/**
	 * Drops a bound, the nodes touching it, and renumbers the remaining node room indices.
	 */
	public InteriorWandData removeRoom(int index) {
		if (index < 0 || index >= rooms.size()) return this;
		List<BoundingBox> list = new ArrayList<>(rooms.size() - 1);
		for (int i = 0; i < rooms.size(); i++) {
			if (i != index) list.add(rooms.get(i));
		}
		List<Node> kept = new ArrayList<>(nodes.size());
		for (Node node : nodes) {
			if (node.roomA() == index || node.roomB() == index) continue;
			kept.add(node.shiftRoom(index));
		}
		int selected = selectedRoom;
		if (selected == index) selected = -1;
		else if (selected > index) selected--;
		return new InteriorWandData(structureBlock, origin, list, kept, selected,
				pendingStart, NO_ROOM, NO_ROOM, pendingNodeA);
	}

	public InteriorWandData addNode(Node node) {
		List<Node> list = new ArrayList<>(nodes);
		list.add(node);
		return new InteriorWandData(structureBlock, origin, rooms, list, selectedRoom,
				pendingStart, NO_ROOM, NO_ROOM, null);
	}

	public InteriorWandData removeNode(int index) {
		if (index < 0 || index >= nodes.size()) return this;
		List<Node> list = new ArrayList<>(nodes.size() - 1);
		for (int i = 0; i < nodes.size(); i++) {
			if (i != index) list.add(nodes.get(i));
		}
		return new InteriorWandData(structureBlock, origin, rooms, list, selectedRoom,
				pendingStart, nodeRoomA, nodeRoomB, pendingNodeA);
	}

	public InteriorWandData withSelectedRoom(int index) {
		return new InteriorWandData(structureBlock, origin, rooms, nodes, index,
				pendingStart, nodeRoomA, nodeRoomB, pendingNodeA);
	}

	public InteriorWandData withPendingStart(@Nullable BlockPos pos) {
		return new InteriorWandData(structureBlock, origin, rooms, nodes, selectedRoom,
				pos, nodeRoomA, nodeRoomB, pendingNodeA);
	}

	public InteriorWandData withNodeSelection(int roomA, int roomB) {
		return new InteriorWandData(structureBlock, origin, rooms, nodes, selectedRoom,
				pendingStart, roomA, roomB, null);
	}

	public InteriorWandData withPendingNodeA(@Nullable BlockPos pos) {
		return new InteriorWandData(structureBlock, origin, rooms, nodes, selectedRoom,
				pendingStart, nodeRoomA, nodeRoomB, pos);
	}

	/**
	 * Drops the in-progress click state, keeping the recorded rooms and nodes.
	 */
	public InteriorWandData resetTransient() {
		return new InteriorWandData(structureBlock, origin, rooms, nodes, -1,
				null, NO_ROOM, NO_ROOM, null);
	}

	public record Node(BlockPos posA, BlockPos posB, int roomA, int roomB) {

		public static final Codec<Node> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("posA").forGetter(Node::posA),
				BlockPos.CODEC.fieldOf("posB").forGetter(Node::posB),
				Codec.INT.fieldOf("roomA").forGetter(Node::roomA),
				Codec.INT.fieldOf("roomB").forGetter(Node::roomB)
		).apply(i, Node::new));

		private Node shiftRoom(int removed) {
			int a = roomA > removed ? roomA - 1 : roomA;
			int b = roomB > removed ? roomB - 1 : roomB;
			return new Node(posA, posB, a, b);
		}

	}

}
