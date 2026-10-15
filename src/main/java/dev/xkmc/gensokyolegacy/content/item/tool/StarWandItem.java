package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.StarDanmakuItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Star wand: a reusable star-danmaku shooter with a fixed 1s cooldown, thrown by dolls that pick it up. Against the
 * inherited {@link DanmakuItem} throw it is free, uses its own cooldown instead of the server's, and throws a loose
 * star so the wand stays in hand.
 */
public class StarWandItem extends StarDanmakuItem {

	public StarWandItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	protected int cooldown() {
		return 20;
	}

	@Override
	protected boolean consume() {
		return false;
	}

	@Override
	protected ItemStack bulletStack(Player player, DanmakuUseEvent event) {
		return GLItems.STAR.asStack();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.STAR_WAND_LORE.get());
	}

}
