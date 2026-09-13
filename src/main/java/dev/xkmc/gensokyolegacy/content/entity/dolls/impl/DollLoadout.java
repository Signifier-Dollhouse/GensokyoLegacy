package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.MutableDollInventory;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;

/**
 * Authoritative loadout plus its client mirror. Persistence (accessor definitions and
 * {@code DollData} restore) lives in {@link DollLoadoutModule}, driven as a doll
 * module; tint is split out into {@link DollTint}. Every loadout operation here is a
 * default over the {@code entityData} primitives below.
 *
 * <p>Client mirror of the ledger inventory: authoritative storage is the ledger entry;
 * the entity accessors only carry it to clients. Deliberately not vanilla equipment
 * slots so doll AI bypasses vanilla equip code.
 */
public interface DollLoadout extends DollBaseImpl {

	default ItemStack getLoadoutItem(DollSlot slot) {
		return switch (slot) {
			case MAIN_HAND -> getEntityData().get(DollEntity.DATA_MAIN_HAND).copy();
			case OFF_HAND -> getEntityData().get(DollEntity.DATA_OFF_HAND).copy();
			case CLOTH -> getEntityData().get(DollEntity.DATA_CLOTH).copy();
			case CORE -> getEntityData().get(DollEntity.DATA_CORE).copy();
		};
	}

	default void setLoadoutItem(DollSlot slot, ItemStack stack) {
		ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		switch (slot) {
			case MAIN_HAND -> getEntityData().set(DollEntity.DATA_MAIN_HAND, copy);
			case OFF_HAND -> getEntityData().set(DollEntity.DATA_OFF_HAND, copy);
			case CLOTH -> getEntityData().set(DollEntity.DATA_CLOTH, copy);
			case CORE -> getEntityData().set(DollEntity.DATA_CORE, copy);
		}
	}

	/**
	 * Authoritative loadout. Server logic reads and writes here directly — the
	 * synced accessors above are client mirror only and are never written back.
	 * Never null: without a host entry it returns a detached empty inventory, so
	 * callers need no null checks (writes to a detached inventory simply persist
	 * nowhere — and every read path treats it as empty).
	 */
	default MutableDollInventory loadout() {
		DollHost host = asDoll().getHost();
		if (host == null) return new MutableDollInventory();
		DollData data = host.findSummoned(asDoll().getUUID());
		if (data == null || data.inventory == null) return new MutableDollInventory();
		return data.inventory;
	}

	/**
	 * Live authoritative stack. May be modified in place (that is what the mutable
	 * inventory variant is for); refresh the mirror afterwards.
	 */
	default ItemStack ledgerStack(DollSlot slot) {
		return loadout().get(slot);
	}

	/**
	 * Pushes the authoritative ledger loadout to the client mirror. Call after any
	 * ledger-side mutation. The mirror starts empty and only ledger writes ever
	 * fill it, so syncing a detached (hostless) inventory is a harmless no-op.
	 */
	default void syncLoadoutMirror() {
		MutableDollInventory inv = loadout();
		for (DollSlot slot : DollSlot.values())
			asDoll().setLoadoutItem(slot, inv.get(slot));
	}

	/**
	 * Swaps main/off contents in the ledger, then refreshes the mirror. Called when
	 * the doll is commanded to act with its off-hand item — the swap sticks until a
	 * further command swaps back.
	 */
	default void swapHands() {
		loadout().swapHands();
		syncLoadoutMirror();
	}

	/**
	 * Consumes items from a hand slot in the ledger, then refreshes the mirror.
	 */
	default boolean consumeLoadoutItem(DollSlot slot, int count) {
		MutableDollInventory inv = loadout();
		ItemStack live = inv.get(slot);
		if (live.getCount() < count) return false;
		live.shrink(count);
		if (live.isEmpty()) inv.set(slot, ItemStack.EMPTY);
		syncLoadoutMirror();
		return true;
	}

	/**
	 * Loadout persistence slice: the 4-slot client mirror definitions plus mirror restore
	 * from {@link DollData}. There is nothing to write — the ledger entry is authoritative
	 * and the mirror is derive-only. Runtime loadout logic stays in {@link DollLoadout}.
	 */
	final class DollLoadoutModule implements DollModule {

		public DollLoadoutModule() {
		}

		private static EntityDataAccessor<ItemStack> accessorFor(DollSlot slot) {
			return switch (slot) {
				case MAIN_HAND -> DollEntity.DATA_MAIN_HAND;
				case OFF_HAND -> DollEntity.DATA_OFF_HAND;
				case CLOTH -> DollEntity.DATA_CLOTH;
				case CORE -> DollEntity.DATA_CORE;
			};
		}

		@Override
		public void defineSynchedData(SynchedEntityData.Builder builder) {
			for (DollSlot slot : DollSlot.values())
				builder.define(accessorFor(slot), ItemStack.EMPTY);
		}

		@Override
		public void readValuesFrom(SynchedEntityData sync, DollData data) {
			if (data.inventory == null) return;
			for (DollSlot slot : DollSlot.values()) {
				ItemStack stack = data.inventory.get(slot);
				sync.set(accessorFor(slot), stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
			}
		}

	}
}
