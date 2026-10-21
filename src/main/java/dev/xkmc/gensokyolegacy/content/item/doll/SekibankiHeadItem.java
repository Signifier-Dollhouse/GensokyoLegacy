package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Sekibanki head's item: a {@link DollGadgetItem} that also carries the gadget kind's own
 * instructions, since what the head can be done with is not the doll half's business. Summoning and
 * collecting are inherited unchanged (gadget.md §2); the last two lines are the possession the kind
 * adds (gadget.md §5).
 *
 * @see "doc/design/doll/gadget.md"
 */
public class SekibankiHeadItem extends DollGadgetItem {

	public SekibankiHeadItem(Properties props, ResourceLocation type) {
		super(props, type);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, ctx, list, flag);
		list.add(GLLang.ItemSekibankiHead.SUMMON.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemSekibankiHead.COLLECT.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemSekibankiHead.POSSESS.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemSekibankiHead.EXIT.get().withStyle(ChatFormatting.GRAY));
	}

}