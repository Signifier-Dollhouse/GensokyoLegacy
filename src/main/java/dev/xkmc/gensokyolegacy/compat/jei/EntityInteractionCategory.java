package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.entity.module.EntityInteraction;
import dev.xkmc.gensokyolegacy.content.entity.module.EntityInteractions;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2core.compat.jei.BaseRecipeCategory;
import dev.xkmc.l2serial.util.Wrappers;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What a character takes from a hand and gives back, one page per character: a line per thing she
 * answers to, under a heading per kind of interaction. Feeding is the first kind; gift exchange
 * joins the same page rather than a category of its own, since both are the same shape — something
 * offered, what it earns, something handed over.
 *
 * @see dev.xkmc.gensokyolegacy.content.entity.module.EntityInteractions
 */
public class EntityInteractionCategory extends BaseRecipeCategory<EntityType<?>, EntityInteractionCategory> {

	/** generous over the two dishes a fairy takes, so a third kind of interaction still fits */
	private static final int ROWS = 3;
	/** how many kinds of interaction the page can head */
	private static final int GROUPS = 2;

	/** width of the column the worth of a line is read in; the longest worth that fits it */
	private static final int LABEL = 74;
	/** between that column and the first slot of a line */
	private static final int MARGIN = 5;

	/** a heading and its gap from the lines under it */
	private static final int HEAD = GLJeiUtil.LINE + 2;

	private static final int IN_X = GLJeiUtil.PAD + LABEL + MARGIN;
	private static final int ARROW_X = IN_X + GLJeiUtil.SLOT + GLJeiUtil.GAP;
	private static final int OUT_X = ARROW_X + GLJeiUtil.ARROW + GLJeiUtil.GAP;

	private static final int SLOTS_Y = GLJeiUtil.PAD + HEAD;
	private static final int TEXT_Y = SLOTS_Y + GROUPS * HEAD + ROWS * GLJeiUtil.SLOT + GLJeiUtil.GAP;

	private static final int WIDTH = OUT_X + GLJeiUtil.SLOT + GLJeiUtil.PAD;
	private static final int HEIGHT = TEXT_Y + GLJeiUtil.LINE + GLJeiUtil.PAD;

	public EntityInteractionCategory() {
		super(GensokyoLegacy.loc("interaction"), Wrappers.cast(EntityType.class));
	}

	public EntityInteractionCategory init(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
		this.icon = helper.createDrawableItemStack(Items.COOKIE.getDefaultInstance());
		return this;
	}

	@Override
	public Component getTitle() {
		return GLLang.Jei.INTERACTION.get();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, EntityType<?> recipe, IFocusGroup focuses) {
		for (var part : place(EntityInteractions.pageOf(recipe))) {
			if (part.line() == null) continue;
			GLJeiUtil.slot(builder, RecipeIngredientRole.INPUT, IN_X, part.y())
					.addItemStacks(stacks(part.line().taken().getItems()));
			GLJeiUtil.plainSlot(builder, RecipeIngredientRole.OUTPUT, OUT_X, part.y())
					.addItemStacks(stacks(part.line().given().toArray(ItemStack[]::new)))
					// the page states the worth beside the line; the slot repeats it where a
					// player actually is when they read it
					.addRichTooltipCallback((view, tooltip) -> tooltip.add(part.line().worth()));
		}
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, EntityType<?> recipe, IFocusGroup focuses) {
		for (var part : place(EntityInteractions.pageOf(recipe))) {
			if (part.line() == null) {
				GLJeiUtil.text(builder, part.group().kind(), GLJeiUtil.PAD, part.y(),
						WIDTH - 2 * GLJeiUtil.PAD, GLJeiUtil.LINE);
			} else {
				GLJeiUtil.text(builder, part.line().worth(), GLJeiUtil.PAD,
						part.y() + (GLJeiUtil.SLOT - GLJeiUtil.LINE) / 2, LABEL, GLJeiUtil.LINE)
						.setTextAlignment(HorizontalAlignment.RIGHT);
				builder.addRecipeArrowWidget().setPosition(ARROW_X, part.y());
			}
		}
		GLJeiUtil.text(builder, recipe.getDescription(), GLJeiUtil.PAD, TEXT_Y,
				WIDTH - 2 * GLJeiUtil.PAD, GLJeiUtil.LINE);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, EntityType<?> recipe,
			IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
		tooltip.add(recipe.getDescription().copy().withStyle(ChatFormatting.UNDERLINE));
		var page = EntityInteractions.pageOf(recipe);
		for (var group : page) tooltip.add(group.note());
		int total = page.stream().mapToInt(group -> group.lines().size()).sum();
		int hidden = total - place(page).size();
		if (hidden > 0) tooltip.add(GLLang.JeiExtra.MORE.get(hidden));
	}

	@Override
	public @Nullable ResourceLocation getRegistryName(EntityType<?> recipe) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(recipe);
	}

	/**
	 * One drawn piece of the page: a heading, at the y its first line sits below, or a line of it.
	 * The slots and the labels both ask here, so a line's worth cannot end up beside someone else's
	 * slots; a page is a handful of lines, and redrawing it per widget costs nothing.
	 */
	private record Placed(EntityInteraction group, @Nullable EntityInteraction.Line line, int y) {
	}

	/** Fits the page to its frame: past {@link #GROUPS} kinds or {@link #ROWS} lines the rest is
	 * left to the tooltip, which is where the {@link GLLang.JeiExtra#MORE} count comes from. */
	private static List<Placed> place(List<EntityInteraction> page) {
		var out = new ArrayList<Placed>();
		int y = SLOTS_Y, lines = 0, kinds = 0;
		for (var group : page) {
			if (kinds == GROUPS || lines == ROWS) break;
			out.add(new Placed(group, null, y));
			kinds++;
			y += HEAD;
			for (var line : group.lines()) {
				if (lines == ROWS) break;
				out.add(new Placed(group, line, y));
				y += GLJeiUtil.SLOT;
				lines++;
			}
		}
		return out;
	}

	/** The stated stacks are shared between every page of the same character, and an {@link ItemStack}
	 * is mutable, so each page gets its own copy to hand to JEI. */
	private static List<ItemStack> stacks(ItemStack[] from) {
		var out = new ArrayList<ItemStack>(from.length);
		for (var stack : from) out.add(stack.copy());
		return out;
	}

}