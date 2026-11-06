package dev.xkmc.gensokyolegacy.content.attachment.party.item;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * The invitation. Right-click a party table to send it out; {@code useOn} rather than {@code use},
 * so it has to be aimed at a table. It names nobody — the spread decides who that ends up being.
 *
 * @see "doc/design/party/send.md"
 */
public class InvitationLetterItem extends Item {

	public InvitationLetterItem(Properties p) {
		super(p);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		var level = ctx.getLevel();
		var pos = ctx.getClickedPos();
		ItemStack stack = ctx.getItemInHand();
		if (!level.getBlockState(pos).is(GLTagGen.PARTY_TABLE)) {
			return InteractionResult.PASS;
		}
		if (!(ctx.getPlayer() instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		var outcome = PartySend.send(sp, pos, stack);
		// every refusal happens before the letter is consumed, so a refused send costs nothing
		PartyMessage.report(sp, outcome);
		return outcome.ok() ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.Party.LETTER_HINT.get().withStyle(ChatFormatting.GRAY));
	}

}
