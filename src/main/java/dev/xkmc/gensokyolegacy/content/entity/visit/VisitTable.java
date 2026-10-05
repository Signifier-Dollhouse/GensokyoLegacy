package dev.xkmc.gensokyolegacy.content.entity.visit;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

/**
 * When a given character visits a given home, as a pure function of the host
 * position, the character's type and the game day.
 * <p>
 * Nothing is persisted. {@link VisitScheduler} asks "is she due?" every second
 * and the visitor asks "am I still due?" every tick; both get the same answer
 * because both run this. That is what makes day rollover, chunk unload/reload
 * and {@code /reload} need no bookkeeping - and what makes {@code /gensokyo
 * visit info} able to print today's window directly.
 */
public class VisitTable {

	/**
	 * Visitors only ever show up in the first half of the day. The window closes
	 * exactly where the default schedule switches to {@code Activity.REST}, so a
	 * guest is never on site while her host is asleep.
	 */
	public static final int WINDOW = 12000;

	/**
	 * Ticks left in the current window, or 0 when the character should not be
	 * here. Answers for the level's current day time, so {@code /time set} just
	 * moves the window rather than leaving anything stale behind.
	 */
	public static int remaining(ServerLevel sl, StructureKey host, EntityType<?> guest) {
		if (!sl.dimension().location().equals(host.dim())) return 0;
		var visit = StructureConfig.visitOf(sl.registryAccess(), host, guest);
		if (visit == null) return 0;
		long dayTime = sl.getDayTime();
		int t = (int) (dayTime % 24000L);
		if (t >= WINDOW) return 0;
		double multiplier = GLModConfig.SERVER.visitStayMultiplier.get();
		var rand = RandomSource.create(seed(host, guest, dayTime / 24000L));
		// draw order is part of the schedule: changing it moves every window
		if (multiplier <= 0 || !visit.roll(rand)) return 0;
		int stay = Math.clamp(visit.rollStay(rand, multiplier), 1, WINDOW);
		int start = rand.nextInt(WINDOW - stay + 1);
		return t < start ? 0 : start + stay - t;
	}

	/**
	 * Per-day seed. The host's instance position is what keeps two shrines in the
	 * world from sharing a schedule, and the guest's type is what keeps two
	 * guests of one house from sharing a window. Pre-mixed rather than left to
	 * {@code BlockPos.asLong} alone so the three inputs stay separable.
	 */
	private static long seed(StructureKey host, EntityType<?> guest, long day) {
		long s = host.pos().asLong() * 0x9E3779B97F4A7C15L;
		s = s * 31 + BuiltInRegistries.ENTITY_TYPE.getKey(guest).hashCode();
		s = s * 31 + day;
		return s ^ (s >>> 33);
	}
}