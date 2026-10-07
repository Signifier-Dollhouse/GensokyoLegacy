package dev.xkmc.gensokyolegacy.content.item.umbrella;

import dev.xkmc.gensokyolegacy.content.entity.foundation.IDimensionBoundEntity;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderSlot;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaOpenRenamePacket;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class UmbrellaUtil {

	public static void recordPosition(ServerPlayer sp, ItemStack stack, BlockPos pos) {
		int idx = GLItems.UMBRELLA_SLOT_SELECTED.getOrDefault(stack, 0);
		ResourceLocation dim = sp.level().dimension().location();
		String defaultName = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		var old = slots.get(idx);
		boolean overwrite = !old.isEmptySlot();
		if (overwrite) {
			sp.displayClientMessage(GLLang.ItemUmbrella.RECORD_OVERWRITE.get(idx, old.displayName()), true);
		}
		// defer the actual write until the player confirms a name, so cancelling discards the record
		GensokyoLegacy.HANDLER.toClientPlayer(new BorderUmbrellaOpenRenamePacket(idx, defaultName, pos, dim, overwrite), sp);
	}

	public static void teleportToSlot(ServerPlayer sp, ItemStack stack) {
		var slot = BorderUmbrellaItem.getSelectedSlotData(stack);
		if (slot.isEmptySlot()) {
			sp.displayClientMessage(GLLang.ItemUmbrella.SLOT_EMPTY_ITEM.get(), true);
			return;
		}
		ServerLevel targetLevel = sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, slot.dim()));
		if (targetLevel == null) {
			sp.displayClientMessage(GLLang.ItemUmbrella.DIM_MISSING.get(slot.dim().toString()), true);
			return;
		}
		// teleport to slot position, nudged into a free space
		Vec3 dst = Vec3.atBottomCenterOf(slot.pos());
		dst = TravelModeUtil.adjustToFreeSpace(targetLevel, sp, dst);
		TravelModeUtil.teleportPlayer(sp, targetLevel, dst);
		sp.displayClientMessage(GLLang.ItemUmbrella.WAYPOINT.get(slot.name()), true);
	}

	public static void teleportEntityToSlot(ServerPlayer sp, LivingEntity target, BorderSlot slot, ItemStack stack) {
		ServerLevel targetLevel = sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, slot.dim()));
		if (targetLevel == null) {
			sp.displayClientMessage(GLLang.ItemUmbrella.DIM_MISSING.get(slot.dim().toString()), true);
			return;
		}
		// A doll or a character is dimension-bound, so the capture may only move it
		// around inside its own level; refuse rather than strand it somewhere its
		// ledger / index cannot describe.
		if (target instanceof IDimensionBoundEntity && target.level() != targetLevel) {
			sp.displayClientMessage(GLLang.ItemUmbrella.CAPTURE_FAIL.get(), true);
			return;
		}
		Vec3 dst = Vec3.atBottomCenterOf(slot.pos());
		dst = TravelModeUtil.adjustToFreeSpace(targetLevel, target, dst);
		// if entity is in different dimension, need to teleport across dimensions
		if (target.level() != targetLevel) {
			target.teleportTo(targetLevel, dst.x, dst.y, dst.z, Set.of(), target.getYRot(), target.getXRot());
		} else {
			target.teleportTo(dst.x, dst.y, dst.z);
			if (target instanceof ServerPlayer tp) {
				tp.connection.resetPosition();
			}
		}
		sp.displayClientMessage(GLLang.ItemUmbrella.CAPTURED.get(target.getDisplayName(), slot.name()), true);
	}

	// Called by server when renaming slot
	public static void renameSlot(ItemStack stack, int idx, String name) {
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		var old = slots.get(idx);
		if (old.isEmptySlot()) return;
		BorderSlot ns = new BorderSlot(old.pos(), old.dim(), name, old.icon());
		stack.set(GLItems.UMBRELLA_SLOTS.get(), slots.with(idx, ns));
	}

	public static void deleteSlot(ItemStack stack, int idx) {
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		stack.set(GLItems.UMBRELLA_SLOTS.get(), slots.with(idx, BorderSlot.empty()));
	}

	public static void swapSlots(ItemStack stack, int a, int b) {
		var slots = GLItems.UMBRELLA_SLOTS.getOrDefault(stack, BorderUmbrellaSlots.defaultSlots());
		var sa = slots.get(a);
		var sb = slots.get(b);
		var ns = slots.with(a, sb).with(b, sa);
		stack.set(GLItems.UMBRELLA_SLOTS.get(), ns);
	}
}
