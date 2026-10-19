package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A shield sized for a doll: too small for a player's arm, so a player cannot raise it at all,
 * but a doll holding it in the off hand blocks projectiles with it. It has no durability, so the
 * doll's wear path leaves it alone (item.md §10).
 *
 * @see "doc/design/doll/item.md"
 */
public class MiniShieldItem extends Item implements DollShieldItem {

	public MiniShieldItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.MINI_SHIELD_LORE.get());
	}

}
