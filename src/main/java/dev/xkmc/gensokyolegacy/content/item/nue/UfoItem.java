package dev.xkmc.gensokyolegacy.content.item.nue;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.compat.l2lib.DCStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The wrapper a trident hit leaves behind: it holds one stolen stack and hands it back after a
 * two-second hold, with no deadline and no way to open it early.
 *
 * @see "doc/design/nue_trident.md"
 */
public class UfoItem extends Item {

	private static final int HOLD_TICKS = 40;

	public UfoItem(Properties properties) {
		super(properties);
	}

	public static ItemStack getContent(ItemStack stack) {
		DCStack content = GLItems.UFO_CONTENT.get(stack);
		return content == null ? ItemStack.EMPTY : content.stack();
	}

	public static ItemStack wrap(ItemStack held) {
		ItemStack ufo = new ItemStack(GLItems.UFO.get());
		GLItems.UFO_CONTENT.set(ufo, new DCStack(held.copy()));
		return ufo;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (getContent(stack).isEmpty()) return InteractionResultHolder.pass(stack);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return HOLD_TICKS;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BRUSH;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack content = getContent(stack);
		return content.isEmpty() ? stack : content;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.UFO_LORE.get());
		ItemStack content = getContent(stack);
		if (!content.isEmpty()) {
			list.add(GLLang.ItemUfo.CONTAINS.get(content.getHoverName()));
		}
		list.add(GLLang.ItemUfo.HOLD.get());
	}

}
