package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
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

public class SuicideMode extends DollGloveHandler {

	@Override
	public ItemStack icon() {
		return new ItemStack(Items.TNT);
	}

	@Override
	public Component displayName() {
		return GLLang.ItemGlove.MODE_SUICIDE.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemGlove.DESC_SUICIDE.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		if (player instanceof ServerPlayer sp) {
			var commands = attachment(sp).commands;
			LivingEntity target = resolveTarget(sp);
			if (target == null || !isValidAttackTarget(sp, target)) {
				sp.displayClientMessage(GLLang.ItemGlove.NO_TARGET.get(), true);
			} else if (commands.issueOneTimeRandom(sp, target, DollActionType.SUICIDE_ATTACK) instanceof DollEntity doll) {
				sp.displayClientMessage(GLLang.ItemGlove.SUICIDE.get(doll.getDisplayName()), true);
			} else {
				sp.displayClientMessage(GLLang.ItemGlove.NO_DOLL.get(), true);
			}
			cooldown(sp, item);
		}
		return InteractionResultHolder.success(stack);
	}

}
