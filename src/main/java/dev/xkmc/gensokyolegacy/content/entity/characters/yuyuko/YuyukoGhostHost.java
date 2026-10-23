package dev.xkmc.gensokyolegacy.content.entity.characters.yuyuko;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollCommander;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollLedger;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollSpawn;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollState;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.AbstractYoukaiModule;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPower;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostVariant;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity.CombatData;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Yuyuko's three attendant ghosts: one of each {@link GhostVariant}, kept at full power and circling
 * her. She is a {@link dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost} in her own right, so
 * the ghosts pair and tick exactly like a player's summons without an item ledger.
 *
 * @see "doc/design/doll/host.md"
 */
@SerialClass
public class YuyukoGhostHost extends AbstractYoukaiModule implements DollLedger {

	private static final ResourceLocation ID = GensokyoLegacy.loc("yuyuko_ghosts");

	/**
	 * The ring radius and hover height around her feet.
	 */
	private static final double ORBIT_RADIUS = 1.6;
	private static final double ORBIT_HEIGHT = 1.35;

	/**
	 * Reconjure at most this often, so a dispelled ghost is never a spawn loop.
	 */
	private static final int CONJURE_INTERVAL = 20;

	@SerialField
	private final List<DollData> ghosts = new ArrayList<>();

	private final DollCommander commander = new DollCommander(this);

	private final YuyukoEntity owner;

	public YuyukoGhostHost(YuyukoEntity owner) {
		super(ID, owner);
		this.owner = owner;
	}

	// ---------- host ----------

	@Override
	public Collection<DollData> dolls() {
		return ghosts;
	}

	@Override
	public DollCommander commands() {
		return commander;
	}

	@Override
	@Nullable
	public DollData findSummoned(UUID uuid) {
		if (uuid == null) return null;
		for (DollData data : ghosts) {
			if (data != null && data.isSummoned() && uuid.equals(data.uuid)) return data;
		}
		return null;
	}

	@Override
	public void update(BaseDollEntity doll) {
		DollData data = findSummoned(doll.getUUID());
		if (data != null) doll.writeValuesTo(data);
	}

	@Override
	@Nullable
	public DollData detach(UUID uuid) {
		return null;
	}

	@Override
	@Nullable
	public DollData drop(UUID uuid) {
		if (uuid == null) return null;
		for (var it = ghosts.iterator(); it.hasNext(); ) {
			DollData data = it.next();
			if (data != null && uuid.equals(data.uuid)) {
				it.remove();
				return data;
			}
		}
		return null;
	}

	// ---------- tick ----------

	@Override
	public void tickServer() {
		if (!(owner.level() instanceof ServerLevel sl)) return;
		if (owner.isDeadOrDying() || owner.isRemoved()) return;
		boolean repaired = reconcile(sl);
		if (repaired || ghosts.size() < GhostVariant.values().length || sl.getGameTime() % CONJURE_INTERVAL == 0)
			conjure(sl);
		orbit(sl);
	}

	/**
	 * Drops entries whose ghost is gone or dead; the next conjure makes a fresh one for that variant.
	 */
	private boolean reconcile(ServerLevel sl) {
		boolean repaired = false;
		for (var it = ghosts.iterator(); it.hasNext(); ) {
			DollData data = it.next();
			if (data == null || !data.isSummoned()) continue;
			if (live(sl, data) != null) continue;
			it.remove();
			repaired = true;
		}
		return repaired;
	}

	/**
	 * Brings one ghost of each variant back, materializing a parked entry before minting a new one.
	 */
	private void conjure(ServerLevel sl) {
		for (GhostVariant variant : GhostVariant.values()) {
			DollData data = byVariant(variant);
			if (data == null) {
				data = entry(variant);
				ghosts.add(data);
			}
			if (live(sl, data) != null) continue;
			data.state = DollState.TEMP;
			data.uuid = null;
			data.position = orbitPos(variant.ordinal());
			data.yRot = owner.getYRot();
			data.combat = new CombatData(BaseDollEntity.DEFAULT_MAX_HEALTH, 0);
			data.power = GhostPower.MAX;
			BaseDollEntity doll = DollSpawn.materialize(sl, owner, data);
			if (doll == null) {
				data.state = DollState.TEMP;
				continue;
			}
			sl.addFreshEntity(doll);
		}
	}

	/**
	 * Pins the living ghosts to a slowly turning ring and tops them back up — the infinite supply.
	 */
	private void orbit(ServerLevel sl) {
		for (DollData data : ghosts) {
			if (data == null || !data.isSummoned()) continue;
			GhostEntity ghost = live(sl, data);
			if (ghost == null) continue;
			ghost.setNoAi(true);
			ghost.setNoGravity(true);
			ghost.noPhysics = true;
			ghost.setInvulnerable(true);
			ghost.setPower(GhostPower.MAX);
			Vec3 pos = orbitPos(data.variant);
			ghost.setDeltaMovement(Vec3.ZERO);
			ghost.setPos(pos.x, pos.y, pos.z);
		}
	}

	@Override
	public void onKilled() {
		if (owner.level() instanceof ServerLevel sl) {
			for (DollData data : ghosts) {
				GhostEntity ghost = data == null ? null : live(sl, data);
				if (ghost != null) ghost.discard();
			}
		}
		ghosts.clear();
	}

	// ---------- helpers ----------

	@Nullable
	private GhostEntity live(ServerLevel sl, DollData data) {
		if (data.uuid == null) return null;
		return sl.getEntity(data.uuid) instanceof GhostEntity ghost && ghost.isAlive() ? ghost : null;
	}

	@Nullable
	private DollData byVariant(GhostVariant variant) {
		for (DollData data : ghosts) {
			if (data != null && data.variant == variant.ordinal()) return data;
		}
		return null;
	}

	private DollData entry(GhostVariant variant) {
		DollData data = new DollData();
		data.type = GhostEntity.TYPE;
		data.state = DollState.TEMP;
		data.variant = variant.ordinal();
		data.power = GhostPower.MAX;
		data.combat = new CombatData(BaseDollEntity.DEFAULT_MAX_HEALTH, 0);
		return data;
	}

	private Vec3 orbitPos(int index) {
		double time = owner.tickCount * 0.04;
		double angle = time + index * Math.PI * 2 / GhostVariant.values().length;
		double y = ORBIT_HEIGHT + Math.sin(time * 1.7 + index) * 0.2;
		return owner.position().add(Math.cos(angle) * ORBIT_RADIUS, y, Math.sin(angle) * ORBIT_RADIUS);
	}

}
