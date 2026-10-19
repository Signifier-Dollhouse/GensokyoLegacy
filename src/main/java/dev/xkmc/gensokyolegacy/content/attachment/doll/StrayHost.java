package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity.CombatData;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Transient host for stray dolls, holding the entry freed by {@link #detach} so pairing
 * queries need no stray branches. Persists by delegating chunk save/load through l2serial.
 *
 * @see "doc/design/doll/control.md"
 */
public class StrayHost implements DollHost {

	@Nullable
	private DollData data;

	public StrayHost(DollData data) {
		this.data = data;
	}

	@Nullable
	public DollData data() {
		return data;
	}

	/**
	 * Hands the detached entry to a rejoin or an itemize — the death drop's one-shot
	 * consumption, but for a doll still alive. The entry never comes back, so a caller that
	 * fails partway must drop it rather than leave a spent host behind.
	 */
	@Nullable
	public DollData take() {
		DollData taken = data;
		data = null;
		return taken;
	}

	@Override
	@Nullable
	public DollData findSummoned(UUID uuid) {
		return data != null && data.isSummoned() && uuid.equals(data.uuid) ? data : null;
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
	public void onDeath(BaseDollEntity doll) {
		if (doll.level().isClientSide()) return;
		DollData drop = take();
		if (drop == null) return;
		// the one place outside a ledger that can mint an item, and the only way a timed summon
		// could reach it: its death ends it outright, the same as a lapse (timed_summon.md §3)
		if (!drop.getNature().itemizable()) return;
		doll.writeValuesTo(drop);
		drop.combat = new CombatData(0, drop.combat.baseline());
		ItemStack stack = DollItem.makeItem(drop);
		if (stack.isEmpty()) return;
		ItemEntity item = new ItemEntity(doll.level(), doll.getX(), doll.getY(), doll.getZ(), stack);
		item.setDefaultPickUpDelay();
		doll.level().addFreshEntity(item);
	}

	public CompoundTag save(HolderLookup.Provider access) {
		CompoundTag tag = new CompoundTag();
		if (data != null) new TagCodec(access).toTag(tag, DollData.class, data);
		return tag;
	}

	@Nullable
	public static StrayHost load(HolderLookup.Provider access, CompoundTag tag) {
		DollData data = new TagCodec(access).fromTag(tag, DollData.class);
		if (data == null || !data.isSummoned()) return null;
		return new StrayHost(data);
	}

}
