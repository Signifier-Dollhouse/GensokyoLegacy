package dev.xkmc.gensokyolegacy.content.entity.behavior.module;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * One kind of thing a character answers to, as a heading and the lines under it: what the player
 * offers, what that is worth, and everything she might hand over for it. Stated in code rather than
 * read from a datapack, because a module's own rules are the truth here - a datapack describing a
 * trade the entity does not implement would be a lie, not content.
 *
 * @see EntityInteractions
 */
public record EntityInteraction(Component kind, Component note, List<Line> lines) {

	public EntityInteraction {
		lines = List.copyOf(lines);
	}

	/**
	 * One exchange: what she will take, what it earns, and what she reaches for out of. A die roll
	 * is a whole set rather than one stack, so the page can show what she might hand over instead
	 * of the single outcome one feeding happened to produce.
	 */
	public record Line(Component worth, Ingredient taken, List<ItemStack> given) {
	}
}