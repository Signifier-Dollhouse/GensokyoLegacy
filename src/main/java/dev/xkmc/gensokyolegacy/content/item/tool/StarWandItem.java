package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.StarDanmakuItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Star wand: a reusable star-danmaku shooter with a fixed 1s cooldown.
 * Never consumed. Extends {@link StarDanmakuItem} (a {@code DanmakuItem}),
 * so dolls pick it up for their danmaku attack automatically.
 * <p>
 * The throw is the inherited one, and only the three steps that make this a wand rather than a
 * plain danmaku item are spelled out: it is free, it is paced by its own fixed cooldown rather
 * than the server's danmaku cooldown, and it throws a loose star instead of itself, so the wand
 * stays in hand while the star is in the air.
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

}
