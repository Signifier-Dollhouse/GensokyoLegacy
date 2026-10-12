package dev.xkmc.gensokyolegacy.content.entity.behavior.task.party;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * What a guest is going after, and how close she has to get to each half of it.
 *
 * <p>
 * <b>Two blocks and two distances, because they are never the same.</b> The thing she acts on
 * and the block she stands on to act on it are different blocks: a chair is furniture she sits
 * down from beside, and a dish on a laid table has air beside it rather than floor.
 *
 * <p>
 * Both distances are measured to the block's <b>box</b> rather than its centre, so that
 * standing on the block beside a chair counts as zero blocks from it.
 *
 * @param interact      the chair or the dish — the thing she does something to
 * @param walkTo        the standable block she goes to in order to do it
 * @param interactReach how close to {@code interact} she must be to act on it
 * @param walkReach     how close to {@code walkTo} she must be to count as having arrived
 *
 * @see doc/design/party/guest.md
 */
public record PartyTarget(@Nullable BlockPos interact, @Nullable BlockPos walkTo,
		double interactReach, double walkReach) {

	/**
	 * How close she must get to the block she walked to before she counts as there. One block,
	 * not zero: a pathfinder that stops a block short is ordinary, and the interaction reach is
	 * what decides whether she can act.
	 */
	public static final double WALK_REACH = 1;

	/** A chair, and the ground she stands on to sit in it. */
	public static PartyTarget forSeat(BlockPos seat, @Nullable BlockPos ground) {
		return new PartyTarget(seat, ground, 2, WALK_REACH);
	}

	/**
	 * A dish, and the ground she stands on to take it from. Three blocks from the chair and two
	 * from her feet, so a dish she could have eaten from her seat is never worth standing up for.
	 */
	public static PartyTarget forDish(BlockPos dish, @Nullable BlockPos ground, boolean seated) {
		return new PartyTarget(dish, ground, seated ? 3 : 2, WALK_REACH);
	}

	/** The same target, arriving somewhere else instead — used after a teleport. */
	public PartyTarget walkedTo(BlockPos at) {
		return new PartyTarget(interact, at, interactReach, walkReach);
	}

	/** Whether she is where she was sent. */
	public boolean arrived(PartyContext ctx) {
		return within(ctx, walkTo, walkReach);
	}

	/** Whether she is close enough to the chair or the dish to act on it. */
	public boolean inReach(PartyContext ctx) {
		return within(ctx, interact, interactReach);
	}

	private static boolean within(PartyContext ctx, @Nullable BlockPos pos, double blocks) {
		if (pos == null) return false;
		return new AABB(pos).distanceToSqr(ctx.entity().position()) <= blocks * blocks;
	}
}