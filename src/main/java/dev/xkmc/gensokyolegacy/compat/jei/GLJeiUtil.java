package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ITextWidget;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared pieces for the rpg JEI categories: slot placement, ingredient-entry slots, text widgets,
 * and a best-effort client-side read of what a loot table can drop.
 */
public class GLJeiUtil {

	public static final int SLOT = 18;
	public static final int ARROW = 24;
	public static final int GAP = 4;
	public static final int PAD = 2;
	/** distance between two text lines */
	public static final int LINE = 10;

	private static final int TEXT = 0xFF404040;

	/** how many times a loot table is rolled to collect the items it can drop */
	private static final int LOOT_ROLLS = 8;

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
	 * Best-effort client-side read of what a loot table can drop, one stack per item.
	 * <p>
	 * Loot tables live in the server's registries and are never synced, so this only
	 * resolves on an integrated server; elsewhere the caller falls back to naming the table.
	 * The table is rolled {@link #LOOT_ROLLS} times to surface every weighted branch, keeping
	 * the first roll of each item — the counts are what one roll yields, not a total.
	 */
	public static List<ItemStack> lootDrops(ResourceLocation table) {
		var mc = Minecraft.getInstance();
		var level = mc.level;
		var server = level == null ? null : level.getServer();
		if (server == null || mc.player == null) return List.of();
		var loot = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table));
		if (loot == LootTable.EMPTY) return List.of();
		var params = new LootParams.Builder(server.overworld())
				.withParameter(LootContextParams.THIS_ENTITY, mc.player)
				.withParameter(LootContextParams.ORIGIN, mc.player.position())
				.create(LootContextParamSets.ADVANCEMENT_REWARD);
		var distinct = new ArrayList<ItemStack>();
		for (int i = 0; i < LOOT_ROLLS; i++) {
			for (var stack : loot.getRandomItems(params, RandomSource.create(i))) {
				if (stack.isEmpty() || contains(distinct, stack)) continue;
				distinct.add(stack.copy());
			}
		}
		return distinct;
	}

	private static boolean contains(List<ItemStack> list, ItemStack stack) {
		for (var e : list)
			if (ItemStack.isSameItemSameComponents(e, stack)) return true;
		return false;
	}

}