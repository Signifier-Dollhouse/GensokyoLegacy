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
 * Transient host for stray dolls: holds the entry freed by {@link #detach} so
 * entity code needs no stray branches — pairing queries just work. Persists the
 * doll by delegating chunk save/load through l2serial.
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
		if (doll.level().isClientSide() || data == null) return;
		DollData drop = data;
		data = null;
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
