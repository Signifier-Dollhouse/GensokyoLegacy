package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SuperMode extends DollGloveHandler {

	@Override
	public ItemStack icon() {
		return new ItemStack(HexBrew.EXPLOSIVE_HEXBREW.bottle.get());
	}

	@Override
	public Component displayName() {
		return GLLang.ItemGlove.MODE_SUPER.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemGlove.DESC_SUPER.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		if (player instanceof ServerPlayer sp) {
			if (tryOpenEditor(sp, item)) return InteractionResultHolder.success(stack);
			performAttack(sp, hand, stack, item);
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public boolean isAttackCommand() {
		return true;
	}

	@Override
	public void performAttackOn(ServerPlayer sp, @Nullable LivingEntity target, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		var commands = attachment(sp).commands;
		if (target == null || !isValidAttackTarget(sp, target)) {
			sp.displayClientMessage(GLLang.ItemGlove.NO_TARGET.get(), true);
		} else if (commands.issueOneTimeRandom(sp, target, DollActionType.SUPER_ATTACK) instanceof DollEntity doll) {
			sp.displayClientMessage(GLLang.ItemGlove.SUPER.get(doll.getDisplayName()), true);
		} else {
			sp.displayClientMessage(GLLang.ItemGlove.NO_DOLL.get(), true);
		}
		cooldown(sp, item);
	}

}
