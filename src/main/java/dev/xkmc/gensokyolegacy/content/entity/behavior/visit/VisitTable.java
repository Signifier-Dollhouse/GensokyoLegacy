package dev.xkmc.gensokyolegacy.content.entity.behavior.visit;

import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.index.IndexStorage;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

/**
 * When a character visits a given home, a pure function of the host position, the guest type and the
 * game day. The only stored state is the day a guest gave up on, read by the spawner and the visitor
 * alike, so asking "is she due?" never depends on who is asking.
 *
 * @see "doc/design/structure/character_visit.md"
 */
public class VisitTable {

	/**
	 * Visits happen in the first half of the day only. The window closes where the default schedule
	 * switches to {@code Activity.REST}, so a guest is never on site while her host is asleep.
	 */
	public static final int WINDOW = 12000;

	/**
	 * Ticks left in the current window, or 0 when she should not be here. Answers for the level's
	 * current day time, so {@code /time set} moves the window rather than leaving anything stale behind.
	 */
	public static int remaining(ServerLevel sl, StructureKey host, EntityType<?> guest) {
		if (!sl.dimension().location().equals(host.dim())) return 0;
		var visit = StructureConfig.visitOf(sl.registryAccess(), host, guest);
		if (visit == null) return 0;
		long dayTime = sl.getDayTime();
		long day = dayTime / 24000L;
		int t = (int) (dayTime % 24000L);
		if (t >= WINDOW) return 0;
		if (IndexStorage.get(sl).getOrCreate(host).visitDismissed(guest, day)) return 0;
		double multiplier = GLModConfig.SERVER.visitStayMultiplier.get();
		var rand = RandomSource.create(seed(host, guest, day));
		// draw order is part of the schedule: changing it moves every window
		if (multiplier <= 0 || !visit.roll(rand)) return 0;
		int stay = Math.clamp(visit.rollStay(rand, multiplier), 1, WINDOW);
		int start = rand.nextInt(WINDOW - stay + 1);
		return t < start ? 0 : start + stay - t;
	}

	/**
	 * Write off the rest of today's visit for this guest, so a visitor who gave up
	 * is not put back on site by the scheduler in the same window.
	 */
	public static void dismiss(ServerLevel sl, StructureKey host, EntityType<?> guest) {
		IndexStorage.get(sl).getOrCreate(host).dismissVisit(guest, sl.getDayTime() / 24000L);
	}

	/**
	 * Per-day seed, pre-mixed so the host position, the guest type and the day
	 * stay separable inputs rather than one hashed lump.
	 */
	private static long seed(StructureKey host, EntityType<?> guest, long day) {
		long s = host.pos().asLong() * 0x9E3779B97F4A7C15L;
		s = s * 31 + BuiltInRegistries.ENTITY_TYPE.getKey(guest).hashCode();
		s = s * 31 + day;
		return s ^ (s >>> 33);
	}
}