package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.action.GiveItemAction;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.ConditionalDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.DialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.GroupDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.OptionResult;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.RandomDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.SimpleDialogOption;
import dev.xkmc.gensokyolegacy.content.rpg.network.QuestLootToClient;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.reward.LootDrop;
import dev.xkmc.gensokyolegacy.content.rpg.reward.LootTableReward;
import dev.xkmc.gensokyolegacy.content.rpg.reward.QuestLoot;
import dev.xkmc.gensokyolegacy.content.rpg.reward.RewardRule;
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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * One {@link Quest} as a JEI recipe: the character and quest name, the one line stating how the
 * reward list is handed out, then the flat item list itself. A loot table is never synced, so the
 * server reads it and sends the drops over ({@link QuestLootToClient}); a reward picked from the
 * completion dialog is read from the quest itself, one slot per {@link GiveItemAction} option.
 *
 * @see "doc/design/quest/rewards.md"
 */
public class QuestRewardCategory extends BaseRecipeCategory<Holder<Quest>, QuestRewardCategory> {

	private static final int COLS = 3;
	private static final int ROWS = 2;
	private static final int MAX_SLOTS = COLS * ROWS;

	/**
	 * The page is sized for the rule line, which is the longest text on it; the item grid is
	 * narrower and leaves the rest to the hover.
	 */
	private static final int TEXT_WIDTH = 160;

	private static final int CHARACTER_Y = GLJeiUtil.PAD;
	private static final int TITLE_Y = CHARACTER_Y + GLJeiUtil.LINE;
	private static final int RULE_Y = TITLE_Y + 2 * GLJeiUtil.LINE;
	private static final int SLOTS_Y = RULE_Y + 2 * GLJeiUtil.LINE;

	/**
	 * {@link GLJeiUtil#gridY} measures its rows from the page padding, while the grid now sits
	 * under the text.
	 */
	private static final int SLOTS_DY = SLOTS_Y - GLJeiUtil.PAD;

	private static final int WIDTH = TEXT_WIDTH + 2 * GLJeiUtil.PAD;
	private static final int HEIGHT = SLOTS_Y + ROWS * GLJeiUtil.SLOT + GLJeiUtil.PAD;

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
		for (var slot : reward(recipe).slots()) {
			if (index == MAX_SLOTS) break;
			var displayed = GLJeiUtil.plainSlot(builder, RecipeIngredientRole.OUTPUT,
					GLJeiUtil.gridX(index, COLS), GLJeiUtil.gridY(index, COLS) + SLOTS_DY)
					.addItemStack(slot.stack());
			if (slot.note() != null || slot.count() != null)
				displayed.addRichTooltipCallback((view, tooltip) -> {
					if (slot.note() != null) tooltip.add(slot.note());
					// the slot cannot show a range, so the tooltip says what the page cannot
					if (slot.count() != null) tooltip.add(slot.count());
				});
			index++;
		}
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, Holder<Quest> recipe, IFocusGroup focuses) {
		var quest = recipe.value();
		var reward = reward(recipe);
		GLJeiUtil.text(builder, quest.character().getDescription(), GLJeiUtil.PAD, CHARACTER_Y,
				TEXT_WIDTH, GLJeiUtil.LINE);
		GLJeiUtil.text(builder, Component.translatable(quest.title()).withStyle(ChatFormatting.UNDERLINE),
				GLJeiUtil.PAD, TITLE_Y, TEXT_WIDTH, 2 * GLJeiUtil.LINE);
		if (!reward.slots().isEmpty())
			GLJeiUtil.text(builder, ruleText(reward.rule()), GLJeiUtil.PAD, RULE_Y, TEXT_WIDTH, 2 * GLJeiUtil.LINE);
		// only the counts a stack could not carry get drawn by hand, right where it would be
		int index = 0;
		for (var slot : reward.slots()) {
			if (index == MAX_SLOTS) break;
			if (slot.count() != null)
				GLJeiUtil.countText(builder, slot.count(),
						GLJeiUtil.gridX(index, COLS), GLJeiUtil.gridY(index, COLS) + SLOTS_DY);
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
		var slots = reward(recipe).slots();
		if (slots.size() > MAX_SLOTS)
			tooltip.add(GLLang.JeiExtra.MORE.get(slots.size() - MAX_SLOTS));
	}

	@Override
	public @Nullable ResourceLocation getRegistryName(Holder<Quest> recipe) {
		return recipe.unwrapKey().map(e -> e.location()).orElse(null);
	}

	/**
	 * Whether this quest has anything a slot could hold. A quest paying out <i>only</i> experience
	 * and reputation has no item to make a page out of and is not registered here at all.
	 */
	public static boolean hasItemReward(Holder<Quest> quest) {
		for (var reward : quest.value().rewards())
			if (reward instanceof LootTableReward) return true;
		return !dialogRewards(quest.value()).isEmpty();
	}

	/**
	 * Every item the quest hands out, plus the one rule that covers all of them. A single source
	 * keeps its own rule; several sources of different rules are a mixed drop.
	 */
	private static Reward reward(Holder<Quest> holder) {
		List<Group> groups = new ArrayList<>();
		var loot = holder.unwrapKey()
				.map(e -> QuestLootToClient.ClientHandler.get(e.location()))
				.orElseGet(() -> new QuestLoot(RewardRule.MIXED, new ArrayList<>()));
		if (!loot.drops().isEmpty()) groups.add(lootGroup(loot));
		groups.addAll(dialogRewards(holder.value()));
		List<Slot> slots = new ArrayList<>();
		for (var group : groups) slots.addAll(group.slots());
		RewardRule rule = groups.isEmpty() ? RewardRule.MIXED
				: RewardRule.combine(groups.stream().map(Group::rule).toList());
		return new Reward(rule, slots);
	}

	private static Group lootGroup(QuestLoot loot) {
		List<Slot> slots = new ArrayList<>();
		for (LootDrop drop : loot.drops())
			slots.add(new Slot(drop.slotStack(), drop.certain() ? null : drop.countText(), null));
		return new Group(loot.rule(), slots);
	}

	/**
	 * The completion dialog is the only place a quest hands out a chosen item; its options are the
	 * alternatives, so sibling options that each grant one item are a choice, while several grants
	 * on one option are all handed over together.
	 */
	private static List<Group> dialogRewards(Quest quest) {
		List<Group> groups = new ArrayList<>();
		Set<Holder<Dialog>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		collectRoot(quest.completionDialog(), groups, seen);
		return groups;
	}

	private static void collectRoot(DialogOption<?> option, List<Group> groups, Set<Holder<Dialog>> seen) {
		List<Slot> slots = new ArrayList<>();
		int granting = 0;
		for (var result : results(option)) {
			int before = slots.size();
			addActions(result.actions(), option.display(), slots);
			if (slots.size() > before) granting++;
		}
		if (!slots.isEmpty())
			groups.add(new Group(granting > 1 ? RewardRule.MIXED : RewardRule.ALL, slots));
		for (var result : results(option))
			result.next().ifPresent(next -> {
				if (seen.add(next)) collectDialog(next.value(), groups, seen);
			});
	}

	private static void collectDialog(Dialog dialog, List<Group> groups, Set<Holder<Dialog>> seen) {
		List<Slot> slots = new ArrayList<>();
		int branches = 0;
		boolean bundle = false;
		boolean conditional = false;
		for (var option : dialog.options()) {
			var results = results(option);
			int granting = 0;
			for (var result : results) {
				int before = slots.size();
				addActions(result.actions(), option.display(), slots);
				if (slots.size() > before) {
					granting++;
					if (slots.size() - before > 1) bundle = true;
				}
			}
			if (granting > 0) branches++;
			if (granting > 1) conditional = true;
		}
		if (!slots.isEmpty()) {
			RewardRule rule = conditional ? RewardRule.MIXED
					: branches <= 1 ? RewardRule.ALL
					: bundle ? RewardRule.MIXED
					: RewardRule.CHOICE;
			groups.add(new Group(rule, slots));
		}
		for (var option : dialog.options())
			for (var result : results(option))
				result.next().ifPresent(next -> {
					if (seen.add(next)) collectDialog(next.value(), groups, seen);
				});
	}

	private static void addActions(List<DialogAction<?>> actions, Component label, List<Slot> out) {
		for (var action : actions)
			if (action instanceof GiveItemAction give)
				out.add(new Slot(new ItemStack(give.item().value(), give.count()), null, label));
	}

	private static List<? extends OptionResult> results(DialogOption<?> option) {
		return switch (option) {
			case SimpleDialogOption simple -> List.of(simple);
			case GroupDialogOption group -> List.of(group);
			case ConditionalDialogOption conditional -> conditional.entries();
			case RandomDialogOption random -> random.entries();
			default -> List.of();
		};
	}

	private static Component ruleText(RewardRule rule) {
		return switch (rule) {
			case ALL -> GLLang.JeiExtra.REWARD_ALL.get();
			case RANDOM -> GLLang.JeiExtra.REWARD_RANDOM.get();
			case CHOICE -> GLLang.JeiExtra.REWARD_CHOICE.get();
			case MIXED -> GLLang.JeiExtra.REWARD_MIXED.get();
		};
	}

	private record Group(RewardRule rule, List<Slot> slots) {
	}

	/**
	 * @param count the count drawn beside the stack when the stack itself cannot carry it, or null
	 * @param note  the option's own line, for an item the player picks from a menu
	 */
	private record Slot(ItemStack stack, @Nullable Component count, @Nullable Component note) {
	}

	private record Reward(RewardRule rule, List<Slot> slots) {
	}

}
