package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.content.item.debug.StructureMirrorData.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * World access, connector naming, placement, validation and repair for the structure mirror stick. Never
 * force-loads a chunk.
 *
 * @see "doc/design/structure/mirror_stick.md"
 */
public final class StructureMirrorHelper {

	private static final int CHUNK_RADIUS = 7;
	private static final String FINAL_STATE = "minecraft:air";
	private static final ResourceLocation EMPTY_POOL = ResourceLocation.withDefaultNamespace("empty");

	private StructureMirrorHelper() {
	}

	public record FoundPiece(BlockPos block, ResourceLocation name, BoundingBox bound) {
	}

	/**
	 * Smallest save-mode piece bound containing {@code pos}, or null.
	 */
	@Nullable
	public static FoundPiece findPiece(ServerLevel level, BlockPos pos) {
		FoundPiece best = null;
		long bestVolume = Long.MAX_VALUE;
		int cx = SectionPos.blockToSectionCoord(pos.getX());
		int cz = SectionPos.blockToSectionCoord(pos.getZ());
		for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
			for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (!(be instanceof StructureBlockEntity structure)) continue;
					if (structure.getMode() != StructureMode.SAVE) continue;
					ResourceLocation name = name(structure);
					if (name == null) continue;
					if (structure.getStructureSize().getX() <= 0
							|| structure.getStructureSize().getY() <= 0
							|| structure.getStructureSize().getZ() <= 0) continue;
					BoundingBox bound = bound(structure);
					if (!bound.isInside(pos)) continue;
					long volume = volume(bound);
					if (volume < bestVolume) {
						bestVolume = volume;
						best = new FoundPiece(structure.getBlockPos(), name, bound);
					}
				}
			}
		}
		return best;
	}

	@Nullable
	public static ResourceLocation nameAt(ServerLevel level, BlockPos structureBlock) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(
				SectionPos.blockToSectionCoord(structureBlock.getX()),
				SectionPos.blockToSectionCoord(structureBlock.getZ()));
		if (chunk == null) return null;
		if (!(chunk.getBlockEntity(structureBlock) instanceof StructureBlockEntity structure)) return null;
		return structure.getMode() == StructureMode.SAVE ? name(structure) : null;
	}

	@Nullable
	private static ResourceLocation name(StructureBlockEntity structure) {
		return structure.hasStructureName() ? ResourceLocation.tryParse(structure.getStructureName()) : null;
	}

	private static BoundingBox bound(StructureBlockEntity structure) {
		BlockPos origin = structure.getBlockPos().offset(structure.getStructurePos());
		Vec3i far = structure.getStructureSize().offset(-1, -1, -1);
		// Vanilla rotates a template about its local origin, then offsets by the world origin.
		BlockPos first = StructureTemplate.transform(BlockPos.ZERO, structure.getMirror(), structure.getRotation(), BlockPos.ZERO);
		BlockPos second = StructureTemplate.transform(BlockPos.ZERO.offset(far), structure.getMirror(), structure.getRotation(), BlockPos.ZERO);
		return BoundingBox.fromCorners(first, second).moved(origin.getX(), origin.getY(), origin.getZ());
	}

	private static long volume(BoundingBox bound) {
		return (long) (bound.maxX() - bound.minX() + 1)
				* (bound.maxY() - bound.minY() + 1)
				* (bound.maxZ() - bound.minZ() + 1);
	}

	/**
	 * Parent-side jigsaw name: {@code a:c2d} for parent {@code a:b/c} and child {@code a:b/d}.
	 */
	public static ResourceLocation exitName(ResourceLocation parent, ResourceLocation child) {
		return ResourceLocation.fromNamespaceAndPath(parent.getNamespace(),
				shortName(parent) + "2" + shortName(child));
	}

	/**
	 * Child-side jigsaw name: {@code a:d2c} for parent {@code a:b/c} and child {@code a:b/d}.
	 */
	public static ResourceLocation entryName(ResourceLocation parent, ResourceLocation child) {
		return ResourceLocation.fromNamespaceAndPath(child.getNamespace(),
				shortName(child) + "2" + shortName(parent));
	}

	private static String shortName(ResourceLocation id) {
		String path = id.getPath();
		int slash = path.lastIndexOf('/');
		return slash < 0 ? path : path.substring(slash + 1);
	}

	/**
	 * Places and configures both jigsaws of a pair.
	 */
	public static boolean applyPair(ServerLevel level, BlockPos parentJig, BlockPos childJig,
	                                ResourceLocation parentName, ResourceLocation childName) {
		Direction direction = direction(parentJig, childJig);
		if (direction == null) return false;
		boolean vertical = direction.getAxis() == Direction.Axis.Y;
		Direction top = vertical ? Direction.NORTH : Direction.UP;
		ResourceLocation exit = exitName(parentName, childName);
		ResourceLocation entry = entryName(parentName, childName);
		return place(level, parentJig, direction, top, vertical, exit, entry, childName)
				&& place(level, childJig, direction.getOpposite(), top, vertical, entry, exit, EMPTY_POOL);
	}

	private static boolean place(ServerLevel level, BlockPos pos, Direction front, Direction top, boolean vertical,
	                             ResourceLocation name, ResourceLocation target, ResourceLocation pool) {
		FrontAndTop orientation = FrontAndTop.fromFrontAndTop(front, top);
		if (orientation == null) return false;
		BlockState state = Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, orientation);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		if (!(level.getBlockEntity(pos) instanceof JigsawBlockEntity jigsaw)) return false;
		jigsaw.setName(name);
		jigsaw.setTarget(target);
		jigsaw.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, pool));
		jigsaw.setJoint(vertical ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE);
		jigsaw.setFinalState(FINAL_STATE);
		jigsaw.setChanged();
		level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
		return true;
	}

	/**
	 * Null when the pair is valid, otherwise the first mismatch.
	 */
	@Nullable
	public static String problem(ServerLevel level, BlockPos parentJig, BlockPos childJig,
	                             ResourceLocation parentName, ResourceLocation childName) {
		Direction direction = direction(parentJig, childJig);
		if (direction == null) return "connector cells are not adjacent";
		boolean vertical = direction.getAxis() == Direction.Axis.Y;
		Direction top = vertical ? Direction.NORTH : Direction.UP;
		ResourceLocation exit = exitName(parentName, childName);
		ResourceLocation entry = entryName(parentName, childName);
		String parent = check(level, parentJig, direction, top, vertical, exit, entry, childName);
		if (parent != null) return "parent " + parent;
		String child = check(level, childJig, direction.getOpposite(), top, vertical, entry, exit, EMPTY_POOL);
		return child == null ? null : "child " + child;
	}

	@Nullable
	private static String check(ServerLevel level, BlockPos pos, Direction front, Direction top, boolean vertical,
	                            ResourceLocation name, ResourceLocation target, ResourceLocation pool) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(Blocks.JIGSAW)) return pos.toShortString() + " is not a jigsaw";
		if (JigsawBlock.getFrontFacing(state) != front)
			return pos.toShortString() + " does not face " + front.getName();
		if (JigsawBlock.getTopFacing(state) != top)
			return pos.toShortString() + " does not have its top toward " + top.getName();
		if (!(level.getBlockEntity(pos) instanceof JigsawBlockEntity jigsaw))
			return pos.toShortString() + " has no jigsaw data";
		if (!jigsaw.getName().equals(name)) return pos.toShortString() + " has name " + jigsaw.getName();
		if (!jigsaw.getTarget().equals(target)) return pos.toShortString() + " targets " + jigsaw.getTarget();
		if (!jigsaw.getPool().location().equals(pool)) return pos.toShortString() + " pool is " + jigsaw.getPool().location();
		if (!jigsaw.getFinalState().equals(FINAL_STATE)) return pos.toShortString() + " turns into " + jigsaw.getFinalState();
		JigsawBlockEntity.JointType joint = vertical
				? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE;
		if (jigsaw.getJoint() != joint) return pos.toShortString() + " joint is " + jigsaw.getJoint().getSerializedName();
		return null;
	}

	@Nullable
	private static Direction direction(BlockPos from, BlockPos to) {
		return Direction.fromDelta(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
	}

	public record RepairReport(int total, int valid, int fixed, int failed, List<String> log) {
	}

	/**
	 * Validates every recorded pair and re-places the invalid ones.
	 */
	public static RepairReport repair(ServerLevel level, StructureMirrorData data) {
		List<String> log = new ArrayList<>();
		int total = 0, valid = 0, fixed = 0, failed = 0;
		for (int i = 0; i < data.entries().size(); i++) {
			Entry entry = data.entries().get(i);
			if (entry.parent() < 0) continue;
			total++;
			String tag = "entry #" + i + " (parent #" + entry.parent() + ")";
			BlockPos childJig = entry.childJigsaw();
			BlockPos parentJig = data.parentJigsaw(i);
			if (childJig == null || parentJig == null || entry.parent() >= data.entries().size()) {
				log.add(tag + ": failed (cannot derive the parent connector)");
				failed++;
				continue;
			}
			ResourceLocation parentName = nameAt(level, data.entries().get(entry.parent()).block());
			ResourceLocation childName = nameAt(level, entry.block());
			if (parentName == null || childName == null) {
				log.add(tag + ": failed (structure block missing, unloaded or unnamed)");
				failed++;
				continue;
			}
			String problem = problem(level, parentJig, childJig, parentName, childName);
			if (problem == null) {
				log.add(tag + ": ok");
				valid++;
			} else if (applyPair(level, parentJig, childJig, parentName, childName)) {
				log.add(tag + ": fixed (" + problem + ")");
				fixed++;
			} else {
				log.add(tag + ": failed (" + problem + ")");
				failed++;
			}
		}
		return new RepairReport(total, valid, fixed, failed, log);
	}

	/**
	 * One line per entry, depth-first from every root, then any orphaned entries.
	 */
	public static List<String> describe(ServerLevel level, StructureMirrorData data) {
		List<String> lines = new ArrayList<>();
		boolean[] seen = new boolean[data.entries().size()];
		for (int i = 0; i < data.entries().size(); i++) {
			if (data.entries().get(i).parent() < 0) describe(level, data, i, 0, lines, seen);
		}
		for (int i = 0; i < data.entries().size(); i++) {
			if (!seen[i]) lines.add("orphan " + line(level, data, i));
		}
		return lines;
	}

	private static void describe(ServerLevel level, StructureMirrorData data, int index, int depth,
	                             List<String> lines, boolean[] seen) {
		if (index < 0 || index >= data.entries().size() || seen[index]) {
			lines.add("  ".repeat(depth) + "cycle/invalid entry #" + index);
			return;
		}
		seen[index] = true;
		lines.add("  ".repeat(depth) + line(level, data, index));
		for (int child : data.children(index)) {
			describe(level, data, child, depth + 1, lines, seen);
		}
	}

	private static String line(ServerLevel level, StructureMirrorData data, int index) {
		Entry entry = data.entries().get(index);
		ResourceLocation name = nameAt(level, entry.block());
		StringBuilder ans = new StringBuilder();
		ans.append('#').append(index).append(' ')
				.append(name == null ? "<unloaded>" : name)
				.append(" bound ").append(box(entry.bound()))
				.append(" structure_block ").append(entry.block().toShortString());
		if (entry.parent() < 0) {
			ans.append(" root");
		} else {
			ans.append(" parent #").append(entry.parent())
					.append(" child_jigsaw ").append(entry.childJigsaw() == null
							? "<none>" : entry.childJigsaw().toShortString())
					.append(" parent_jigsaw ").append(
							data.parentJigsaw(index) == null ? "<none>" : data.parentJigsaw(index).toShortString());
		}
		return ans.toString();
	}

	private static String box(BoundingBox bound) {
		return "(" + bound.minX() + "," + bound.minY() + "," + bound.minZ() + ")-("
				+ bound.maxX() + "," + bound.maxY() + "," + bound.maxZ() + ")";
	}

}