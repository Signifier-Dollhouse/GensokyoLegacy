package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An obscure, hard-to-read magic book (GiftType.BOOK). Can be given to
 * characters, or burned as furnace fuel — roughly two lava buckets.
 */
public class MagicBookItem extends Item {

	public MagicBookItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		list.add(GLLang.ItemCommon.USAGE_MAGIC_BOOK.get().withStyle(ChatFormatting.GRAY));
	}

}
