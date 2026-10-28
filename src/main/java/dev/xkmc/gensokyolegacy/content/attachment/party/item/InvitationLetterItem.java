package dev.xkmc.gensokyolegacy.content.attachment.party.item;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyInvite;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
												  InteractionHand hand) {
		if (!(target instanceof YoukaiEntity youkai)) return InteractionResult.PASS;
		if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
		var result = PartyInvite.invite(sp, youkai);
		PartyMessage.reportInvite(sp, youkai.getType(), result);
		if (result != PartyInvite.Result.INVITED) return InteractionResult.PASS;
		// a refused invitation costs nothing
		if (!sp.getAbilities().instabuild) stack.shrink(1);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.Party.LETTER_HINT.get().withStyle(ChatFormatting.GRAY));
	}

}
