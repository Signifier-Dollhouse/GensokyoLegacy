package dev.xkmc.gensokyolegacy.content.entity.behavior.combat;

import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@SerialClass
public class YoukaiTargetContainer {


	private final YoukaiEntity youkai;
	private final int maxSize;

	@SerialField
	private final LinkedHashSet<UUID> list = new LinkedHashSet<>();

	public YoukaiTargetContainer(YoukaiEntity youkai, int maxSize) {
		this.youkai = youkai;
		this.maxSize = maxSize;
	}

	public void tick() {
		if (youkai.level().isClientSide()) return;
		LivingEntity le = youkai.getLastHurtByMob();
		if (le != null && !(le instanceof Player) && isValidTarget(le) && !list.contains(le.getUUID())) {
			list.add(le.getUUID());
		} else {
			le = youkai.getTarget();
			if (le != null && !(le instanceof Player) && isValidTarget(le)) {
				list.add(le.getUUID());
			}
		}
		list.removeIf(e -> !isValid(e));
		if (list.size() > maxSize) {
			list.removeFirst();
		}
	}

	public boolean isValidTarget(LivingEntity e) {
		if (youkai.invalidTarget(e) || !e.canBeSeenAsEnemy() || e.getType().is(GLTagGen.YOUKAI_IGNORE))
			return false;
		if (e instanceof Player player) {
			int rep = youkai.getData(player).map(h -> h.data().reputation)
					.orElse(ReputationConstants.INITIAL_REPUTATION);
			return rep <= ReputationConstants.COMBAT_SAFE_THRESHOLD;
		}
		return true;
	}

	private boolean isValid(UUID id) {
		Entity e = ((ServerLevel) youkai.level()).getEntity(id);
		if (e instanceof Player) return false;
		if (e instanceof LivingEntity le) {
			return isValidTarget(le);
		}
		return false;
	}

	public boolean contains(LivingEntity e) {
		if (e instanceof Player) return youkai.getTarget() == e;
		return youkai.getTarget() == e || list.contains(e.getUUID());
	}

	@Nullable
	public LivingEntity checkTarget(@Nullable LivingEntity last) {
		if (!(youkai.level() instanceof ServerLevel sl)) return null;
		if ((last == null || !isValidTarget(last)) && !list.isEmpty()) {
			var id = list.getLast();
			Entity e = sl.getEntity(id);
			if (e instanceof LivingEntity le && isValidTarget(le))
				return le;
		}
		return last;
	}

	@Nullable
	public LivingEntity getPrimaryTarget() {
		if (!(youkai.level() instanceof ServerLevel sl)) return null;
		for (var id : list)
			if (sl.getEntity(id) instanceof LivingEntity le && !(le instanceof Player) && isValidTarget(le))
				return le;
		return null;
	}

	public List<LivingEntity> getTargets() {
		List<LivingEntity> ans = new ArrayList<>();
		if (!(youkai.level() instanceof ServerLevel sl)) return ans;
		for (var id : list) {
			if (sl.getEntity(id) instanceof LivingEntity le && !(le instanceof Player) && isValidTarget(le))
				ans.add(le);
		}
		return ans;
	}

}
