package dev.xkmc.gensokyolegacy.content.entity.module;

import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.entity.visit.VisitTable;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Whether this character is currently paying a visit to someone else's home,
 * and for how much longer.
 * <p>
 * A visitor has no {@link HomeModule} binding, so {@code MemoryModuleType.HOME}
 * is never set for it and the whole sleep/bed subsystem stays unreachable - that
 * is what "not bound to a bed" means here, and it needs no extra machinery.
 * <p>
 * The visit is not stored as a countdown. A natural visit's remaining time is
 * recomputed from {@link VisitTable} every tick, which is a pure function of the
 * host position, this character's type and the game day - so day rollover, chunk
 * unload and {@code /reload} all need no bookkeeping. Only a command-forced
 * visit carries an explicit deadline.
 */
@SerialClass
public class VisitModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("visit");

	/**
	 * Ticks a conversation keeps the character on site after her window closed.
	 * Long enough to finish a trade, short enough not to pin her there.
	 */
	public static final int TALK_HOLD = 400;

	/**
	 * How close a player has to be for the character to refuse to disappear.
	 * Without this she blinks out mid-sentence in front of the player - but only
	 * counts players who can actually see her, see {@link #seenByAPlayer}.
	 */
	public static final int VISIBLE_HOLD_RANGE = 32;

	@Nullable
	@SerialField
	private StructureKey host;

	@SerialField
	private int noDisappear = 0;

	@SerialField
	private boolean forced = false;

	@SerialField
	private long forcedUntil = 0;

	public VisitModule(YoukaiEntity self) {
		super(ID, self);
	}

	public boolean visiting() {
		return host != null;
	}

	@Nullable
	public StructureKey host() {
		return host;
	}

	public boolean isForced() {
		return forced;
	}

	/**
	 * Begin a visit that follows the host's time table, as
	 * {@link VisitScheduler} spawns it.
	 */
	public void arrive(StructureKey host) {
		enter(host, false, 0);
	}

	/**
	 * Begin a visit with an explicit deadline, bypassing the time table.
	 * Used by {@code /gensokyo visit}.
	 */
	public void force(StructureKey host, long until) {
		enter(host, true, until);
	}

	private void enter(StructureKey host, boolean forced, long until) {
		this.host = host;
		this.forced = forced;
		this.forcedUntil = until;
		self.setFlag(YoukaiFlags.VISITING, true);
	}

	/**
	 * Ticks this character still owes the visit, 0 when she should not be here.
	 * Read by the {@code home_bound} condition and by {@link #tickServer()}.
	 */
	public int remaining(ServerLevel sl) {
		if (host == null) return 0;
		if (forced) return (int) Math.max(0, forcedUntil - sl.getGameTime());
		return VisitTable.remaining(sl, host, self.getType());
	}

	/**
	 * Refuse to disappear for a while. Does not stack: a second conversation
	 * while the first hold is running neither refreshes nor extends it, so the
	 * longer of the two asks wins.
	 */
	public void hold(int ticks) {
		noDisappear = Math.max(noDisappear, ticks);
	}

	@Override
	public void tickServer() {
		if (!(self.level() instanceof ServerLevel sl)) return;
		if (noDisappear > 0) noDisappear--;
		if (host == null) return;
		if (remaining(sl) > 0) {
			// set on the first tick rather than in enter(): the brain does not
			// necessarily exist yet at spawn time
			BrainUtils.setMemory(self, GLBrains.MEM_VISIT.get(), new GlobalPos(host.getDim(), host.pos()));
			return;
		}
		if (noDisappear > 0) return;
		if (seenByAPlayer(sl)) return;
		dismiss();
	}

	/**
	 * Whether any nearby player can actually see her. Proximity alone is not
	 * enough: she may as well vanish while the player is on the other side of the
	 * house, and staying put for someone who cannot see her just leaves her
	 * standing around for no reason.
	 */
	private boolean seenByAPlayer(ServerLevel sl) {
		var area = self.getBoundingBox().inflate(VISIBLE_HOLD_RANGE);
		for (var player : sl.getEntitiesOfClass(Player.class, area)) {
			if (player.distanceTo(self) > VISIBLE_HOLD_RANGE) continue;
			if (player.hasLineOfSight(self)) return true;
		}
		return false;
	}

	/**
	 * End the visit and drop the character, skipping the expiry checks. Used by
	 * the timer and by {@code /gensokyo visit end}; there is no farewell, she
	 * simply is not here any more.
	 */
	public void dismiss() {
		leave();
		self.discard();
	}

	/**
	 * Clear the visit state but leave the entity standing, so the caller decides
	 * what happens to it.
	 */
	public void leave() {
		BrainUtils.clearMemory(self, GLBrains.MEM_VISIT.get());
		self.setFlag(YoukaiFlags.VISITING, false);
		host = null;
		forced = false;
		forcedUntil = 0;
	}

	@Override
	public void onKilled() {
		leave();
	}
}