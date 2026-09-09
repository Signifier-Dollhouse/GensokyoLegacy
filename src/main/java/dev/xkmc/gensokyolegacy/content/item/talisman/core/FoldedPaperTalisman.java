package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FoldedPaperTalisman extends TalismanCurioItem {

	public FoldedPaperTalisman(Properties p) {
		super(p);
	}

	public static ItemStack fold(ItemStack from) {
		ItemStack ans = new ItemStack(GLTalismans.FOLDED_PAPER_TALISMAN.get());
		GLTalismans.DC_TALISMAN_PAPER.set(ans, BuiltInRegistries.ITEM.wrapAsHolder(from.getItem()));
		if (from.getItem() instanceof TalismanPaperItem paper) {
			GLTalismans.DC_TALISMAN_DURABILITY.set(ans, paper.getDurability());
		}
		return ans;
	}

	@Nullable
	public static TalismanPaperItem paper(ItemStack stack) {
		Holder<net.minecraft.world.item.Item> holder = GLTalismans.DC_TALISMAN_PAPER.get(stack);
		if (holder != null && holder.value() instanceof TalismanPaperItem paper) {
			return paper;
		}
		return null;
	}

	@Override
	public List<ItemStack> getActiveTalismans(ItemStack stack) {
		return List.of(stack);
	}

	@Override
	public Component getName(ItemStack stack) {
		TalismanPaperItem paper = paper(stack);
		if (paper == null) return super.getName(stack);
		return GLLang.Talisman.FOLDED.get(paper.kindName().get());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		TalismanPaperItem paper = paper(stack);
		if (paper == null) {
			list.add(GLLang.Talisman.BLANK.get());
			return;
		}
		paper.appendTalismanDesc(stack, list);
		list.add(GLLang.Talisman.DURABILITY.get(
				GLTalismans.DC_TALISMAN_DURABILITY.getOrDefault(stack, paper.getDurability()),
				paper.getDurability()));
	}

}