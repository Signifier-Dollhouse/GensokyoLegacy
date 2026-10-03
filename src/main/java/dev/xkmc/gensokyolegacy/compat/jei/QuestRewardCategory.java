package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientList;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.RollItemRequirement;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One {@link Quest} as a JEI recipe: the items the quest asks for on the left, the items it
 * can hand back on the right, and every other reward (experience, reputation, a loot table
 * the client cannot read) on the tooltip. Only {@link IngredientList} requirements and loot
 * tables become slots — a kill or raid requirement has no item to look up.
 */
public class QuestRewardCategory extends BaseRecipeCategory<Holder<Quest>, QuestRewardCategory> {

	private static final int COLS = 3;
	private static final int ROWS = 2;
	private static final int MAX_SLOTS = COLS * ROWS;

	private static final int IN_X = GLJeiUtil.PAD;
	private static final int ARROW_X = IN_X + COLS * GLJeiUtil.SLOT + GLJeiUtil.GAP;
	private static final int OUT_X = ARROW_X + GLJeiUtil.ARROW + GLJeiUtil.GAP;
	private static final int SLOTS_Y = GLJeiUtil.PAD;
	private static final int ROW_Y = SLOTS_Y + (ROWS - 1) * GLJeiUtil.SLOT / 2;
	private static final int TEXT_Y = SLOTS_Y + ROWS * GLJeiUtil.SLOT + GLJeiUtil.GAP;

	private static final int WIDTH = OUT_X + COLS * GLJeiUtil.SLOT + GLJeiUtil.PAD;
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
		var quest = recipe.value();
		int index = 0;
		for (var stack : requiredItems(quest)) {
			if (index == MAX_SLOTS) break;
			GLJeiUtil.slot(builder, RecipeIngredientRole.INPUT,
					GLJeiUtil.gridX(index, COLS), GLJeiUtil.gridY(index, COLS)).addItemStacks(List.of(stack));
			index++;
		}
		index = 0;
		for (var stack : rewardItems(quest)) {
			if (index == MAX_SLOTS) break;
			GLJeiUtil.slot(builder, RecipeIngredientRole.OUTPUT,
					OUT_X + index % COLS * GLJeiUtil.SLOT, GLJeiUtil.gridY(index, COLS)).addItemStacks(List.of(stack));
			index++;
		}
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, Holder<Quest> recipe, IFocusGroup focuses) {
		var quest = recipe.value();
		int textWidth = WIDTH - 2 * GLJeiUtil.PAD;
		builder.addRecipeArrowWidget().setPosition(ARROW_X, ROW_Y);
		GLJeiUtil.text(builder, Component.translatable(quest.title()).withStyle(ChatFormatting.UNDERLINE),
				GLJeiUtil.PAD, TEXT_Y, textWidth, 2 * GLJeiUtil.LINE);
		GLJeiUtil.text(builder, quest.character().getDescription(), GLJeiUtil.PAD, TEXT_Y + 2 * GLJeiUtil.LINE,
				textWidth, GLJeiUtil.LINE);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, Holder<Quest> recipe,
			IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
		var quest = recipe.value();
		var player = Minecraft.getInstance().player;
		tooltip.add(Component.translatable(quest.title()).withStyle(ChatFormatting.UNDERLINE));
		tooltip.add(Component.translatable(quest.description()));
		for (var req : quest.requirements().values()) {
			if (req instanceof IngredientList ingredients) {
				for (var entry : ingredients.ingredients())
					tooltip.add(entry.getDesc(player));
			} else if (req instanceof RollItemRequirement roll) {
				for (var stack : GLJeiUtil.lootDrops(roll.table()))
					tooltip.add(stack.getHoverName().copy());
			}
		}
		for (var reward : quest.rewards())
			tooltip.add(reward.getDesc());
	}

	@Override
	public @Nullable ResourceLocation getRegistryName(Holder<Quest> recipe) {
		return recipe.unwrapKey().map(e -> e.location()).orElse(null);
	}

	private static List<ItemStack> requiredItems(Quest quest) {
		List<ItemStack> list = new ArrayList<>();
		for (var req : quest.requirements().values()) {
			if (req instanceof IngredientList ingredients) {
				for (var entry : ingredients.ingredients())
					list.addAll(stacks(entry));
			} else if (req instanceof RollItemRequirement roll) {
				list.addAll(GLJeiUtil.lootDrops(roll.table()));
			}
		}
		return list;
	}

	private static List<ItemStack> rewardItems(Quest quest) {
		List<ItemStack> list = new ArrayList<>();
		for (var reward : quest.rewards())
			if (reward instanceof LootTableReward loot) list.addAll(GLJeiUtil.lootDrops(loot.table()));
		return list;
	}

	private static List<ItemStack> stacks(IngredientEntry entry) {
		List<ItemStack> list = new ArrayList<>();
		for (var stack : entry.ingredient().getItems())
			list.add(stack.copyWithCount(entry.count()));
		return list;
	}

}