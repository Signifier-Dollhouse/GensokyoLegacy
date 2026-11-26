package dev.xkmc.gensokyolegacy.content.item.nue;

import dev.xkmc.gensokyolegacy.compat.l2lib.DCStack;
import dev.xkmc.gensokyolegacy.content.item.common.HoldToOpenItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The wrapper a trident hit leaves behind: it holds one stolen stack and hands it back after a
 * two-second hold, with no deadline and no way to open it early. A UFO carrying loot instead rolls
 * one item out of it when the hold finishes, which is what Nue leaves at a party. The three colours
 * are cosmetic and picked at random by {@link #wrap}.
 *
 * @see "doc/design/item/nue_trident.md"
 */
public class UfoItem extends HoldToOpenItem {

	private static final int HOLD_TICKS = 40;

	public UfoItem(Properties properties) {
		super(properties, HOLD_TICKS);
	}

	public static ItemStack getContent(ItemStack stack) {
		DCStack content = GLItems.UFO_CONTENT.get(stack);
		return content == null ? ItemStack.EMPTY : content.stack();
	}

	@Nullable
	public static ResourceLocation getLootTable(ItemStack stack) {
		return GLItems.UFO_LOOT.get(stack);
	}

	@Override
	protected ItemStack contentOf(ItemStack stack) {
		return getContent(stack);
	}

	@Override
	protected ResourceLocation lootTableOf(ItemStack stack) {
		return getLootTable(stack);
	}

	public static ItemStack wrap(ItemStack held, RandomSource random) {
		ItemStack ufo = new ItemStack(randomUfo(random));
		GLItems.UFO_CONTENT.set(ufo, new DCStack(held.copy()));
		return ufo;
	}

	/**
	 * One of the three colours, equally likely. The colours are cosmetic, so this is only flavour.
	 */
	private static Item randomUfo(RandomSource random) {
		return switch (random.nextInt(3)) {
			case 1 -> GLItems.RED_UFO.get();
			case 2 -> GLItems.GREEN_UFO.get();
			default -> GLItems.BLUE_UFO.get();
		};
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.UFO_LORE.get());
		ItemStack content = getContent(stack);
		if (!content.isEmpty()) {
			list.add(GLLang.ItemUfo.CONTAINS.get(content.getHoverName()));
		}
		boolean opens = content.isEmpty() && getLootTable(stack) != null;
		list.add(opens ? GLLang.ItemUfo.OPEN.get() : GLLang.ItemUfo.HOLD.get());
	}

}
