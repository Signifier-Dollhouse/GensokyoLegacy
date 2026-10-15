package dev.xkmc.gensokyolegacy.content.entity.module;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * A character with something to say without saying it: a bubble over her head with one item in it,
 * put up every ten to twenty seconds and gone three seconds later. All of it the client's - nothing
 * here is a promise to anybody, she is only thinking out loud.
 * <p>
 * What a renderer asks for. The clock and the state behind these are {@link AbstractBubbleModule}'s.
 *
 * @see "doc/design/want_bubble.md"
 */
public interface IBubbleModule {

	/**
	 * What she is hoping to be handed. Built per call rather than held in a list, because this is where a
	 * character's own taste goes - a Cirno who wants ice says so by returning a different one.
	 */
	List<Supplier<ItemStack>> wants();

	/**
	 * Whether a bubble may be drawn over her head right now. Asked at the draw and not at the roll, so a
	 * want that comes up while she is busy is dropped rather than held back until she is free again.
	 */
	default boolean mayShowWant() {
		return true;
	}

	/**
	 * What the bubble is showing, empty when there is nothing to show - the display of a moment rather than
	 * a state of hers, so a player who starts tracking her late has nothing to see.
	 */
	ItemStack getWantBubble();

	/**
	 * Where the current want was put: {@link AbstractBubbleModule#WANT_LEFT},
	 * {@link AbstractBubbleModule#WANT_CENTER} or {@link AbstractBubbleModule#WANT_RIGHT}.
	 */
	int getWantPlacement();

	/**
	 * Ticks since the current want went up, partial tick included: what the renderer ages the bubble by.
	 * Meaningless before the first want, which is fine - an empty stack is skipped before it is read.
	 */
	float getWantBubbleAge(float partialTick);

}