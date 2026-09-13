package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.menu.DollLoadoutProvider;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EditorMode extends DollGloveHandler {

	@Override
	public ItemStack icon() {
		return DollItem.blank();
	}

	@Override
	public Component displayName() {
		return GLLang.ItemGlove.MODE_EDITOR.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemGlove.DESC_EDITOR.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		if (player instanceof ServerPlayer sp) {
			LivingEntity target = resolveTarget(sp);
			if (target == null) {
				sp.displayClientMessage(GLLang.ItemGlove.NO_TARGET.get(), true);
			} else if (target instanceof DollEntity doll &&
					(doll.isOwner(sp) || sp.getAbilities().instabuild)) {
				DollLoadoutProvider.open(sp, doll);
			} else {
				sp.displayClientMessage(GLLang.ItemGlove.NOT_DOLL.get(), true);
			}
			cooldown(sp, item);
		}
		return InteractionResultHolder.success(stack);
	}

}
