package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rally, the glove's one toggle mode: with nothing summoned it materializes
 * everything parked plus every doll item in the inventory, otherwise it recalls
 * — itemizing the summoned dolls (overflow parked to data) and any stray dolls
 * within {@link DollAttachment#STRAY_RECALL_RANGE} blocks, which are as loose
 * as a summoned one as far as the holder is concerned (control.md §5.4).
 */
public class SummonMode extends DollGloveHandler {

	@Override
	public ItemStack icon() {
		return new ItemStack(GLItems.DOLL_GLOVE.get());
	}

	@Override
	public Component displayName() {
		return GLLang.ItemGlove.MODE_SUMMON.get();
	}

	@Override
	public Component description() {
		return GLLang.ItemGlove.DESC_SUMMON.get();
	}

	@Override
	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		if (player instanceof ServerPlayer sp) {
			if (tryOpenEditor(sp, item)) return InteractionResultHolder.success(stack);
			var att = attachment(sp);
			if (!att.hasSummoned()) {
				int n = att.summonAll(sp);
				sp.displayClientMessage(GLLang.ItemGlove.SUMMONED.get(n), true);
			} else {
				int[] r = att.recallAll(sp);
				sp.displayClientMessage(GLLang.ItemGlove.RECALLED.get(r[0], r[1]), true);
			}
			cooldown(sp, item);
		}
		return InteractionResultHolder.success(stack);
	}

}
