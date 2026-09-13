package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class HealMarkMode extends DollGloveHandler {

	@Override
	public ItemStack icon() {
		return new ItemStack(Items.GOLDEN_CARROT);
	}

	@Override
	public Component displayName() {
		return GLLang.ItemGlove.MODE_HEAL_MARK.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemGlove.DESC_HEAL_MARK.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		if (player instanceof ServerPlayer sp) {
			var commands = attachment(sp).commands;
			LivingEntity target = resolveTarget(sp);
			if (target == null) {
				sp.displayClientMessage(GLLang.ItemGlove.NO_TARGET.get(), true);
			} else if (commands.healTargets.remove(target.getUUID())) {
				sp.displayClientMessage(GLLang.ItemGlove.UNMARKED.get(target.getDisplayName()), false);
			} else {
				commands.healTargets.add(target.getUUID());
				sp.displayClientMessage(GLLang.ItemGlove.MARKED.get(target.getDisplayName()), false);
			}
			cooldown(sp, item);
		}
		return InteractionResultHolder.success(stack);
	}

}
