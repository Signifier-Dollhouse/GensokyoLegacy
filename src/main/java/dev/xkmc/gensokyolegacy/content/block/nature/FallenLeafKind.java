package dev.xkmc.gensokyolegacy.content.block.nature;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The species a carpet of fallen leaves belongs to. The kind is read off the registry name of the block, so
 * {@code <id>_leaves} and {@code <id>_fallen_leaves} are the same species.
 *
 * @see "doc/design/fallen_leaves.md"
 */
public enum FallenLeafKind {

	CEDAR("cedar", Family.OTHER),
	ORANGE_MAPLE("orange_maple", Family.MAPLE),
	RED_MAPLE("red_maple", Family.MAPLE),
	YELLOW_MAPLE("yellow_maple", Family.MAPLE),
	MIXED_MAPLE("mixed_maple", Family.MAPLE),
	PINK_CHERRY("pink_cherry", Family.CHERRY),
	PURPLE_CHERRY("purple_cherry", Family.CHERRY);

	/**
	 * Maples of different colours share one carpet, cherries of different colours do not.
	 */
	public enum Family {
		MAPLE, CHERRY, OTHER
	}

	private static final String LEAVES_SUFFIX = "_leaves";
	private static final String FALLEN_SUFFIX = "_fallen_leaves";
	private static final Map<String, FallenLeafKind> BY_ID = new HashMap<>();

	static {
		for (var kind : values()) {
			BY_ID.put(kind.id, kind);
		}
	}

	private final String id;
	private final Family family;

	FallenLeafKind(String id, Family family) {
		this.id = id;
		this.family = family;
	}

	public Family family() {
		return family;
	}

	/**
	 * Whether two species share one carpet: the maples do, the cherries do not.
	 */
	public static boolean mixes(@Nullable FallenLeafKind a, @Nullable FallenLeafKind b) {
		return a != null && b != null && a.family() == Family.MAPLE && b.family() == Family.MAPLE;
	}

	/**
	 * The carpet of fallen leaves of this species.
	 */
	public Block fallenLeaves() {
		var block = BuiltInRegistries.BLOCK.get(GensokyoLegacy.loc(id + FALLEN_SUFFIX));
		return block == null ? Blocks.AIR : block;
	}

	/**
	 * The kind of a leaf block or of a carpet of fallen leaves, read off its registry name.
	 */
	public static @Nullable FallenLeafKind of(Block block) {
		var key = BuiltInRegistries.BLOCK.getKey(block);
		if (key == null) return null;
		String path = key.getPath();
		if (path.endsWith(FALLEN_SUFFIX)) {
			path = path.substring(0, path.length() - FALLEN_SUFFIX.length());
		} else if (path.endsWith(LEAVES_SUFFIX)) {
			path = path.substring(0, path.length() - LEAVES_SUFFIX.length());
		}
		return BY_ID.get(path);
	}

}
