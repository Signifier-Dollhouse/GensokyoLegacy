package dev.xkmc.gensokyolegacy.content.entity.visit;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.CharacterConfig;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.entity.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The spawn side of a visit: wakes up once a second per structure, and puts a
 * character on site for every guest the structure's time table currently says
 * should be there.
 * <p>
 * Driven from {@code StructureRefData.blockTick}, which already collapses to one
 * call per game time no matter how many beds a structure has, so a structure that
 * is not loaded costs nothing.
 */
public class VisitScheduler {

	private static final int INTERVAL = 20;

	/**
	 * Wander radius used when the guest has no {@code CharacterConfig} entry -
	 * same default the structure datagen uses.
	 */
	private static final int FALLBACK_WANDER = 12;

	public static void tick(ServerLevel sl, StructureKey key, long structureTick, IHomeHolder home) {
		if (structureTick % INTERVAL != 0) return;
		var config = StructureConfig.of(sl.registryAccess(), key);
		if (config == null || config.visitors().isEmpty()) return;
		for (var guest : config.visitors().keySet()) {
			if (VisitTable.remaining(sl, key, guest) <= 0) continue;
			// guests are independent: one being on site never blocks another
			if (find(sl, key, guest, home) != null) continue;
			spawn(sl, key, home, guest);
		}
	}

	/**
	 * The character already visiting this home, if any. Scans the wander area
	 * rather than the whole structure bound, since the visitor is restricted to
	 * it, and inflates it so a guest that wandered to the edge is still found.
	 */
	@Nullable
	public static YoukaiEntity find(ServerLevel sl, StructureKey key, EntityType<?> guest, IHomeHolder home) {
		var center = home.getWanderCenter();
		if (center == null) return null;
		var area = new AABB(center).inflate(home.getWanderBaseRadius() + FALLBACK_WANDER);
		return sl.getEntities(EntityTypeTest.forClass(YoukaiEntity.class), area,
						y -> y.getType() == guest
								&& y.getModule(VisitModule.class).map(m -> key.equals(m.host())).orElse(false))
				.stream().findFirst().orElse(null);
	}

	/**
	 * Put a guest on site. Deliberately does not touch {@code HomeModule}: with
	 * no home binding the character never gets a {@code HOME} memory, so it can
	 * neither sleep nor claim the bed it is standing next to, and the resident
	 * back home keeps living its own day.
	 *
	 * @return the visitor, or null if there is nowhere for her to stand
	 */
	@Nullable
	public static YoukaiEntity spawn(ServerLevel sl, StructureKey key, IHomeHolder home, EntityType<?> guest) {
		if (!(guest.create(sl) instanceof YoukaiEntity youkai)) return null;
		var visit = youkai.getModule(VisitModule.class);
		if (visit.isEmpty()) return null;
		var pos = home.getRandomPosInBound(youkai);
		var center = home.getWanderCenter();
		if (pos == null || center == null) return null;
		var config = CharacterConfig.of(guest);
		youkai.setPos(pos);
		youkai.initSpellCard();
		youkai.restrictTo(center, home.getWanderBaseRadius()
				+ (config == null ? FALLBACK_WANDER : config.wanderRadius()));
		visit.get().arrive(key);
		sl.addFreshEntity(youkai);
		return youkai;
	}

	/**
	 * Put a guest on site for a fixed stretch of time, ignoring the time table -
	 * the command's forced mode. Returns whoever is already visiting, so the
	 * command cannot stack two copies of the same character.
	 */
	@Nullable
	public static YoukaiEntity force(ServerLevel sl, StructureKey key, IHomeHolder home,
			EntityType<?> guest, int stay) {
		var existing = find(sl, key, guest, home);
		if (existing != null) return existing;
		var youkai = spawn(sl, key, home, guest);
		if (youkai == null) return null;
		youkai.getModule(VisitModule.class).ifPresent(e -> e.force(key, sl.getGameTime() + stay));
		return youkai;
	}
}