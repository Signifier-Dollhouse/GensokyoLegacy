package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The one materialization path every ledger shares (doc/design/doll/pairing.md §3.1):
 * create the entity, key {@code data} to its fresh game uuid, hand it the owner,
 * apply the recorded values — and let the <b>caller</b> insert the entry into its
 * ledger <i>before</i> {@code addFreshEntity}, so the entity's join-level inverse
 * check always finds a paired host (§5.2, §8.2).
 * <p>
 * Split out of {@link DollAttachment#doSummon} so a character host conjures her
 * dolls through the same guarantees instead of a second, drifting copy.
 */
public final class DollSpawn {

	private DollSpawn() {
	}

	/**
	 * Materializes {@code data} as a live doll owned by {@code owner} in {@code level}.
	 * On success {@code data} carries the new entity's uuid, final position, and
	 * dimension, and the caller must register it before {@code addFreshEntity} —
	 * see the class note on the join-level inverse check.
	 */
	@Nullable
	public static BaseDollEntity materialize(ServerLevel level, LivingEntity owner, DollData data) {
		if (data.type == null || data.position == null || data.getHealth() <= 0) return null;
		EntityType<?> type = level.registryAccess().registry(Registries.ENTITY_TYPE)
				.flatMap(r -> r.getOptional(ResourceKey.create(Registries.ENTITY_TYPE, data.type))).orElse(null);
		if (type == null) return null;
		Entity ent = type.create(level);
		if (!(ent instanceof BaseDollEntity doll)) {
			if (ent != null) ent.discard();
			return null;
		}
		data.uuid = doll.getUUID();
		data.state = DollState.SUMMONED;
		setPos(level, doll, data.position);
		doll.setOwner(owner);
		// free-space search may have moved the doll; write its final spot back so readValuesFrom
		// (which applies position + facing) puts the entity exactly there.
		data.position = doll.position();
		doll.readValuesFrom(data);
		data.dimension = level.dimension().location();
		data.lastUpdate = level.getGameTime();
		return doll;
	}

	/**
	 * Snaps the doll onto {@code pos}, then nudges it out of anything solid it
	 * overlaps — a doll is small and usually hovers, so a plain spawn can land
	 * inside a wall when the recorded spot is stale.
	 */
	public static void setPos(Level level, BaseDollEntity doll, Vec3 pos) {
		doll.setPos(pos);
		var dim = doll.getDimensions(Pose.STANDING);
		if (dim.width() * dim.width() * dim.height() > 64) return;
		Vec3 center = doll.position().add(0, dim.height() / 2.0, 0);
		double xz = Math.max(0, dim.width() - 1) + 1e-6;
		double y = Math.max(0, dim.height() - 1) + 1e-6;
		VoxelShape shape = Shapes.create(AABB.ofSize(center, xz, y, xz));
		var found = level.findFreePosition(doll, shape, center, dim.width(), dim.height(), dim.width());
		if (found.isPresent()) {
			doll.setPos(found.get().add(0, -dim.height() / 2.0, 0));
		}
	}

	/**
	 * A spread-out free spot around {@code around} for the {@code index}-th of
	 * {@code total} dolls — the ring slot a freshly conjured doll materializes on
	 * so a whole retinue does not stack on one point.
	 */
	@Nullable
	public static Vec3 ringPos(LivingEntity around, int index, int total) {
		int n = Math.max(1, total);
		int i = Math.max(0, Math.min(index, n - 1));
		double a = 2 * Math.PI * i / n;
		return around.position().add(Math.cos(a) * 1.5, 1, Math.sin(a) * 1.5);
	}

}
