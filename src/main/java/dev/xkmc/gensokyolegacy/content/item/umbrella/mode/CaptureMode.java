package dev.xkmc.gensokyolegacy.content.item.umbrella.mode;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.UmbrellaUtil;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class CaptureMode extends UmbrellaMode {

	@Override
	public ItemStack icon() {
		return new ItemStack(Items.IRON_BARS);
	}

	@Override
	public Component displayName() {
		return GLLang.ItemUmbrella.MODE_CAPTURE.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemUmbrella.DESC_CAPTURE.get();
	}

	@Override
	public boolean isHiddenWhenLocked() {
		return true;
	}

	@Override
	public boolean isAvailable(ItemStack stack) {
		var unlock = GLItems.UMBRELLA_UNLOCK.get(stack);
		if (unlock == null) return false;
		return unlock.captureUnlocked();
	}

	@Override
	public boolean showsSlot() {
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, BorderUmbrellaItem item) {
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(stack, dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaUnlock.DEFAULT);
		if (!unlock.captureUnlocked()) {
			if (!level.isClientSide)
				player.displayClientMessage(GLLang.ItemUmbrella.LOCKED_CAPTURE.get(), true);
			return InteractionResultHolder.fail(stack);
		}
		return InteractionResultHolder.pass(stack);
	}

	@Override
	public InteractionResult handleInteractLiving(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, BorderUmbrellaItem item) {
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(stack, dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaUnlock.DEFAULT);
		if (!unlock.captureUnlocked()) {
			if (!player.level().isClientSide)
				player.displayClientMessage(GLLang.ItemUmbrella.LOCKED_CAPTURE.get(), true);
			return InteractionResult.FAIL;
		}
		if (player instanceof ServerPlayer sp) {
			var slot = BorderUmbrellaItem.getSelectedSlotData(stack);
			if (slot.isEmptySlot()) {
				player.displayClientMessage(GLLang.ItemUmbrella.SLOT_EMPTY_ITEM.get(), true);
				return InteractionResult.FAIL;
			}
			if (target instanceof Player || target.isMultipartEntity() ||
					target.getType().is(GLTagGen.UMBRELLA_CAPTURE_BLACKLIST)) {
				sp.displayClientMessage(GLLang.ItemUmbrella.CAPTURE_FAIL.get(), true);
				return InteractionResult.FAIL;
			}
			UmbrellaUtil.teleportEntityToSlot(sp, target, slot, stack);
			if (!player.isCreative()) player.getCooldowns().addCooldown(item, 20);
		}
		return InteractionResult.SUCCESS;
	}
}
