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
 * Whether this character is visiting someone else's home, and for how much longer. A visitor has no
 * {@link HomeModule} binding, so the sleep/bed subsystem stays unreachable; the visit is not stored as a
 * countdown, only a forced one carries a deadline - a natural one is recomputed from {@link VisitTable} each tick.
 *
 * @see "doc/design/character_visit.md"
 */
@SerialClass
public class VisitModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("visit");

	/**
	 * Ticks a conversation keeps her on site after her window closed.
	 */
	public static final int TALK_HOLD = 400;

	/**
	 * Range for {@link #seenByAPlayer}: she must not blink out mid-sentence in
	 * front of a player, but proximity alone does not count.
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
	 * Begin a visit that follows the host's time table, as {@code VisitScheduler}
	 * spawns it.
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
	 * Refuse to disappear for a while. Keeps the longest pending hold rather than
	 * the latest, so a short conversation cannot cut a long one short.
	 */
	public void hold(int ticks) {
		noDisappear = Math.max(noDisappear, ticks);
	}

	@Override
	public void tickServer() {
		if (!(self.level() instanceof ServerLevel sl)) return;
		if (noDisappear > 0) noDisappear--;
		if (host == null) return;
		// She is at a party, not on her visit: stand down entirely until the party
		// lets go, or the visit fights it for the entity.
		if (self.isPartying()) return;
		if (remaining(sl) > 0) {
			// set on the first tick, not in enter(): the brain may not exist yet
			// at spawn time
			BrainUtils.setMemory(self, GLBrains.MEM_VISIT.get(), new GlobalPos(host.getDim(), host.pos()));
			return;
		}
		if (noDisappear > 0) return;
		if (seenByAPlayer(sl)) return;
		dismiss();
	}

	/**
	 * Whether any nearby player can actually see her. Proximity alone does not
	 * count — she may as well go while the player is on the other side of the house.
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
	 * the timer and by {@code /gensokyo visit end}; there is no farewell.
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