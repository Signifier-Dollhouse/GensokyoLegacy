package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class StopMode extends DollGloveHandler {

	@Override
	public ItemStack icon() {
		return new ItemStack(Items.SHIELD);
	}

	@Override
	public Component displayName() {
		return GLLang.ItemGlove.MODE_STOP.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemGlove.DESC_STOP.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		if (player instanceof ServerPlayer sp) {
			int n = attachment(sp).commands.stopAll(sp);
			sp.displayClientMessage(GLLang.ItemGlove.STOPPED.get(n), false);
			cooldown(sp, item);
		}
		return InteractionResultHolder.success(stack);
	}

}
