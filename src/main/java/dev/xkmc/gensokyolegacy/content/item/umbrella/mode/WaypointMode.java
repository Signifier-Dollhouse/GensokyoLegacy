package dev.xkmc.gensokyolegacy.content.item.umbrella.mode;

import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.UmbrellaUtil;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class WaypointMode extends UmbrellaMode {

	@Override
	public ItemStack icon() {
		return new ItemStack(Items.RESPAWN_ANCHOR);
	}

	@Override
	public Component displayName() {
		return GLLang.ItemUmbrella.MODE_WAYPOINT.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemUmbrella.DESC_WAYPOINT.get();
	}

	@Override
	public boolean isHiddenWhenLocked() {
		return false;
	}

	@Override
	public boolean showsSlot() {
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, BorderUmbrellaItem item) {
		if (player instanceof ServerPlayer sp) {
			UmbrellaUtil.teleportToSlot(sp, stack);
			if (!sp.isCreative()) sp.getCooldowns().addCooldown(item, 10);
		}
		return InteractionResultHolder.success(stack);
	}
}
