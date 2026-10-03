package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ITextWidget;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/**
 * Shared pieces for the rpg JEI categories: slot placement, ingredient-entry slots, text
 * widgets, and stack counts a slot cannot draw for itself.
 */
public class GLJeiUtil {

	public static final int SLOT = 18;
	public static final int ARROW = 24;
	public static final int GAP = 4;
	public static final int PAD = 2;
	/** distance between two text lines */
	public static final int LINE = 10;

	private static final int TEXT = 0xFF404040;
	/** a vanilla stack count is drawn white, with a shadow */
	private static final int COUNT = 0xFFFFFF;

	/**
	 * Where a vanilla stack count sits inside its slot: 1px past the item's right edge, 9px below
	 * its top ({@code GuiGraphics#renderItemDecorations}, which JEI hands the slot's own
	 * coordinates rather than the 16px item box's).
	 */
	private static final int COUNT_X = SLOT - 1;
	private static final int COUNT_Y = 9;

	private GLJeiUtil() {}

	/**
	 * Slot grid position of the {@code index}th slot of a {@code cols}-wide grid.
	 */
	public static int gridX(int index, int cols) {
		return PAD + index % cols * SLOT;
	}

	public static int gridY(int index, int cols) {
		return PAD + index / cols * SLOT;
	}

	public static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
		var slot = builder.addSlot(role, x, y);
		if (role == RecipeIngredientRole.OUTPUT) slot.setOutputSlotBackground();
		else slot.setStandardSlotBackground();
		return slot;
	}

	/**
	 * A slot framed the ordinary way whatever its role. A recipe that lists what it produces as
	 * a plain row of items — a list of rewards, not one product — has no use for the frame that
	 * marks a single output.
	 */
	public static IRecipeSlotBuilder plainSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
		return builder.addSlot(role, x, y).setStandardSlotBackground();
	}

	/**
	 * One slot for an {@link IngredientEntry}: every ingredient variant, each carrying the
	 * required count so the slot shows the price. An entry's optional text (an abstract
	 * ingredient with no item of its own) is added to the slot tooltip.
	 */
	public static void addEntry(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, IngredientEntry entry) {
		var slot = slot(builder, role, x, y);
		var stacks = new ArrayList<ItemStack>();
		for (var stack : entry.ingredient().getItems())
			stacks.add(stack.copyWithCount(entry.count()));
		if (!stacks.isEmpty()) slot.addItemStacks(stacks);
		entry.text().ifPresent(text -> slot.addRichTooltipCallback(
				(view, tooltip) -> tooltip.add(Component.literal(text))));
	}

	public static ITextWidget text(IRecipeExtrasBuilder builder, Component text, int x, int y, int width, int height) {
		return builder.addText(text, width, height).setPosition(x, y).setColor(TEXT);
	}

	/**
	 * A stack size drawn by hand over a slot, for the one thing a stack count cannot say: that
	 * the count is a range. Sits exactly where a real count would, measured rather than guessed
	 * so that a wide range is never truncated to fit the slot.
	 */
	public static ITextWidget countText(IRecipeExtrasBuilder builder, Component text, int slotX, int slotY) {
		int width = Minecraft.getInstance().font.width(text);
		return builder.addText(text, width, SLOT)
				.setPosition(slotX + COUNT_X - width, slotY + COUNT_Y)
				.setColor(COUNT)
				.setShadow(true);
	}

}
