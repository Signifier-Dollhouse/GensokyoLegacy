package dev.xkmc.gensokyolegacy.content.item.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Structure-origin lookup and world-to-template conversion for the interior wand. Never force-loads
 * a chunk.
 *
 * @see "doc/design/structure/interior_wand.md"
 */
public final class InteriorWandHelper {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private InteriorWandHelper() {
	}

	/**
	 * The recorded origin: the placement corner of the smallest save-mode structure block bound
	 * containing {@code pos}, or null.
	 */
	public static InteriorWandData acquire(ServerLevel level, BlockPos pos) {
		var found = StructureMirrorHelper.findPiece(level, pos);
		if (found == null) return null;
		var structure = StructureMirrorHelper.structureAt(level, found.block());
		if (structure == null) return null;
		return InteriorWandData.empty(found.block(), structure.getBlockPos().offset(structure.getStructurePos()));
	}

	public record Placement(Mirror mirror, Rotation rotation, boolean present) {
	}

	public static Placement placement(ServerLevel level, InteriorWandData data) {
		var structure = StructureMirrorHelper.structureAt(level, data.structureBlock());
		if (structure == null) return new Placement(Mirror.NONE, Rotation.NONE, false);
		return new Placement(structure.getMirror(), structure.getRotation(), true);
	}

	/**
	 * Inverse of {@code StructureTemplate.transform} about a zero pivot: world back to template-local.
	 */
	public static BlockPos toLocal(BlockPos world, BlockPos origin, Mirror mirror, Rotation rotation) {
		int x = world.getX() - origin.getX();
		int y = world.getY() - origin.getY();
		int z = world.getZ() - origin.getZ();
		int i, k;
		switch (rotation) {
			case COUNTERCLOCKWISE_90 -> {
				i = -z;
				k = x;
			}
			case CLOCKWISE_90 -> {
				i = z;
				k = -x;
			}
			case CLOCKWISE_180 -> {
				i = -x;
				k = -z;
			}
			default -> {
				i = x;
				k = z;
			}
		}
		switch (mirror) {
			case LEFT_RIGHT -> k = -k;
			case FRONT_BACK -> i = -i;
			default -> {
			}
		}
		return new BlockPos(i, y, k);
	}

	public static BoundingBox toLocal(BoundingBox world, BlockPos origin, Mirror mirror, Rotation rotation) {
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		for (int x : new int[]{world.minX(), world.maxX()})
			for (int y : new int[]{world.minY(), world.maxY()})
				for (int z : new int[]{world.minZ(), world.maxZ()}) {
					BlockPos p = toLocal(new BlockPos(x, y, z), origin, mirror, rotation);
					x0 = Math.min(x0, p.getX());
					y0 = Math.min(y0, p.getY());
					z0 = Math.min(z0, p.getZ());
					x1 = Math.max(x1, p.getX());
					y1 = Math.max(y1, p.getY());
					z1 = Math.max(z1, p.getZ());
				}
		return new BoundingBox(x0, y0, z0, x1, y1, z1);
	}

	/**
	 * The {@code interior} object in template-local coords, ready to paste into the structure config.
	 */
	public static JsonObject exportJson(ServerLevel level, InteriorWandData data) {
		Placement placement = placement(level, data);
		JsonObject root = new JsonObject();
		JsonArray nodes = new JsonArray();
		for (var node : data.nodes()) {
			JsonObject obj = new JsonObject();
			obj.add("posA", pos(toLocal(node.posA(), data.origin(), placement.mirror(), placement.rotation())));
			obj.add("posB", pos(toLocal(node.posB(), data.origin(), placement.mirror(), placement.rotation())));
			obj.addProperty("roomA", node.roomA());
			obj.addProperty("roomB", node.roomB());
			nodes.add(obj);
		}
		root.add("nodes", nodes);
		JsonArray rooms = new JsonArray();
		for (int i = 0; i < data.rooms().size(); i++) {
			JsonObject obj = new JsonObject();
			obj.add("bound", box(toLocal(data.rooms().get(i), data.origin(),
					placement.mirror(), placement.rotation())));
			obj.addProperty("name", "room_" + i);
			rooms.add(obj);
		}
		root.add("rooms", rooms);
		return root;
	}

	/**
	 * The exported JSON split into chat lines.
	 */
	public static List<String> exportLines(ServerLevel level, InteriorWandData data) {
		return GSON.toJson(exportJson(level, data)).lines().toList();
	}

	/**
	 * Writes the exported JSON to {@code out/interior/<namespace>/<name>.json} under the game
	 * directory.
	 */
	public static Path write(ServerLevel level, InteriorWandData data) throws IOException {
		Path path = Path.of("out", "interior", fileName(level, data) + ".json");
		Files.createDirectories(path.getParent());
		Files.writeString(path, GSON.toJson(exportJson(level, data)));
		return path;
	}

	private static String fileName(ServerLevel level, InteriorWandData data) {
		var name = StructureMirrorHelper.nameAt(level, data.structureBlock());
		if (name != null) return name.getNamespace() + "/" + name.getPath();
		BlockPos pos = data.structureBlock();
		return "unknown/interior_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
	}

	private static JsonArray pos(BlockPos pos) {
		JsonArray arr = new JsonArray();
		arr.add(pos.getX());
		arr.add(pos.getY());
		arr.add(pos.getZ());
		return arr;
	}

	private static JsonArray box(BoundingBox box) {
		JsonArray arr = new JsonArray();
		arr.add(box.minX());
		arr.add(box.minY());
		arr.add(box.minZ());
		arr.add(box.maxX());
		arr.add(box.maxY());
		arr.add(box.maxZ());
		return arr;
	}

}
