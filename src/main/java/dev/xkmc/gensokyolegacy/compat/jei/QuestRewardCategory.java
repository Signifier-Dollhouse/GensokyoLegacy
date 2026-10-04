package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.rpg.network.QuestLootToClient;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.reward.LootDrop;
import dev.xkmc.gensokyolegacy.content.rpg.reward.LootTableReward;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.compat.jei.BaseRecipeCategory;
import dev.xkmc.l2serial.util.Wrappers;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One {@link Quest} as a JEI recipe, and only a reward list: what the quest hands back, never
 * what it asks for. A requirement is the player's problem, not this page's — and a quest whose
 * reward is a loot table has no better answer than the items that table can drop, which the
 * server reads off the table and sends over ({@link QuestLootToClient}) because a loot table is
 * never synced to a client.
 *
 * <p>The page says only what comes back and who is handing it over: a grid of items in plain
 * frames — a reward is a list, not one product — under the quest's name and its asker's.
 * Everything else, the description and the experience and reputation and which table it all came
 * out of, waits for a hover, where there is room to say it.
 *
 * <p>No slot carries a count it cannot vouch for. A loot table says nothing about how much of
 * anything it will drop, so a count that came out of a constant is shown as a plain stack and
 * one that did not is labelled by hand, {@code 6-8}, in the spot a stack count would go.
 */
public class QuestRewardCategory extends BaseRecipeCategory<Holder<Quest>, QuestRewardCategory> {

	private static final int COLS = 3;
	private static final int ROWS = 2;
	private static final int MAX_SLOTS = COLS * ROWS;

	/**
	 * The page is sized for the two names on it rather than for the grid, which is wider than
	 * either and leaves the rest to the hover.
	 */
	private static final int TEXT_WIDTH = 120;

	private static final int SLOTS_Y = GLJeiUtil.PAD;
	private static final int TEXT_Y = SLOTS_Y + ROWS * GLJeiUtil.SLOT + GLJeiUtil.GAP;

	private static final int WIDTH = TEXT_WIDTH + 2 * GLJeiUtil.PAD;
	private static final int HEIGHT = TEXT_Y + 3 * GLJeiUtil.LINE + GLJeiUtil.PAD;

	public QuestRewardCategory() {
		super(GensokyoLegacy.loc("quest"), Wrappers.cast(Holder.class));
	}

	public QuestRewardCategory init(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
		this.icon = helper.createDrawableItemStack(GLItems.MAGIC_BOOK.asStack());
		return this;
	}

	@Override
	public Component getTitle() {
		return GLLang.Jei.QUEST.get();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Holder<Quest> recipe, IFocusGroup focuses) {
		int index = 0;
		for (var drop : rewardDrops(recipe.value())) {
			if (index == MAX_SLOTS) break;
			GLJeiUtil.plainSlot(builder, RecipeIngredientRole.OUTPUT,
					GLJeiUtil.gridX(index, COLS), GLJeiUtil.gridY(index, COLS))
					.addItemStack(drop.slotStack())
					// the slot cannot show a range, so the tooltip says what the page cannot
					.addRichTooltipCallback((view, tooltip) -> tooltip.add(drop.countText()));
			index++;
		}
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, Holder<Quest> recipe, IFocusGroup focuses) {
		var quest = recipe.value();
		GLJeiUtil.text(builder, Component.translatable(quest.title()).withStyle(ChatFormatting.UNDERLINE),
				GLJeiUtil.PAD, TEXT_Y, TEXT_WIDTH, 2 * GLJeiUtil.LINE);
		GLJeiUtil.text(builder, quest.character().getDescription(), GLJeiUtil.PAD, TEXT_Y + 2 * GLJeiUtil.LINE,
				TEXT_WIDTH, GLJeiUtil.LINE);
		// only the counts a stack could not carry get drawn by hand, right where it would be
		int index = 0;
		for (var drop : rewardDrops(quest)) {
			if (index == MAX_SLOTS) break;
			if (!drop.certain())
				GLJeiUtil.countText(builder, drop.countText(),
						GLJeiUtil.gridX(index, COLS), GLJeiUtil.gridY(index, COLS));
			index++;
		}
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, Holder<Quest> recipe,
			IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
		var quest = recipe.value();
		tooltip.add(Component.translatable(quest.title()).withStyle(ChatFormatting.UNDERLINE));
		tooltip.add(quest.character().getDescription());
		tooltip.add(Component.translatable(quest.description()));
		for (var reward : quest.rewards())
			tooltip.add(reward.getDesc());
		var drops = rewardDrops(quest);
		if (drops.size() > MAX_SLOTS)
			tooltip.add(GLLang.JeiExtra.MORE.get(drops.size() - MAX_SLOTS));
	}

	@Override
	public @Nullable ResourceLocation getRegistryName(Holder<Quest> recipe) {
		return recipe.unwrapKey().map(e -> e.location()).orElse(null);
	}

	private static List<LootDrop> rewardDrops(Quest quest) {
		List<LootDrop> list = new ArrayList<>();
		for (var reward : quest.rewards())
			if (reward instanceof LootTableReward loot) list.addAll(QuestLootToClient.ClientHandler.get(loot.table()));
		return list;
	}

	/**
	 * Whether this quest has anything a slot could hold. Experience and reputation are real
	 * rewards, and they are in the hover, but a quest paying out <i>only</i> those has no item
	 * to make a page out of and is not registered here in the first place.
	 */
	public static boolean hasItemReward(Holder<Quest> quest) {
		for (var reward : quest.value().rewards())
			if (reward instanceof LootTableReward) return true;
		return false;
	}

}
