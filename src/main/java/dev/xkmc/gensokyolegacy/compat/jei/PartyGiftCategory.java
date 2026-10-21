package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.GiftTier;
import dev.xkmc.gensokyolegacy.content.attachment.party.network.PartyGiftToClient;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One character's gift ladder as a JEI recipe: a row per rung, the appeal that reaches it beside
 * it and what she hands over there in plain frames. A loot table is never synced, so the server
 * reads every ladder and sends it over ({@link PartyGiftToClient}).
 */
public class PartyGiftCategory extends BaseRecipeCategory<EntityType<?>, PartyGiftCategory> {

	/**
	 * generous over the authored ladders, so a datapack's longer one still fits
	 */
	private static final int ROWS = 5;
	/**
	 * the widest rung of those ladders; a wider one says so in the tooltip
	 */
	private static final int COLS = 5;

	/**
	 * width of the appeal band column the rungs are read against
	 */
	private static final int LABEL = 45;
	/**
	 * between that column and the first rung of a row
	 */
	private static final int MARGIN = 5;

	private static final int SLOTS_X = GLJeiUtil.PAD + LABEL + MARGIN;
	private static final int TEXT_Y = GLJeiUtil.PAD + ROWS * GLJeiUtil.SLOT + GLJeiUtil.GAP;

	private static final int WIDTH = SLOTS_X + COLS * GLJeiUtil.SLOT + GLJeiUtil.PAD;
	private static final int HEIGHT = TEXT_Y + GLJeiUtil.LINE + GLJeiUtil.PAD;

	public PartyGiftCategory() {
		super(GensokyoLegacy.loc("gift"), Wrappers.cast(EntityType.class));
	}

	public PartyGiftCategory init(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
		this.icon = helper.createDrawableItemStack(GLItems.INVITATION_LETTER.asStack());
		return this;
	}

	@Override
	public Component getTitle() {
		return GLLang.Jei.GIFT.get();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, EntityType<?> recipe, IFocusGroup focuses) {
		int row = 0;
		for (var tier : tiers(recipe)) {
			if (row == ROWS) break;
			int slot = 0;
			for (var gift : tier.gifts()) {
				if (slot == COLS) break;
				GLJeiUtil.plainSlot(builder, RecipeIngredientRole.OUTPUT,
								SLOTS_X + slot * GLJeiUtil.SLOT, rowY(row))
						.addItemStack(gift.stack())
						// the slot cannot say how often she picks it, so the tooltip does
						.addRichTooltipCallback((view, tooltip) -> tooltip.add(gift.chanceText()));
				slot++;
			}
			row++;
		}
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, EntityType<?> recipe, IFocusGroup focuses) {
		int row = 0;
		for (var tier : tiers(recipe)) {
			if (row == ROWS) break;
			GLJeiUtil.text(builder, tier.bandText(), GLJeiUtil.PAD,
							rowY(row) + (GLJeiUtil.SLOT - GLJeiUtil.LINE) / 2, LABEL, GLJeiUtil.LINE)
					.setTextAlignment(HorizontalAlignment.RIGHT);
			row++;
		}
		GLJeiUtil.text(builder, recipe.getDescription(), GLJeiUtil.PAD, TEXT_Y,
				WIDTH - 2 * GLJeiUtil.PAD, GLJeiUtil.LINE);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, EntityType<?> recipe,
						   IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
		var tiers = tiers(recipe);
		tooltip.add(recipe.getDescription().copy().withStyle(ChatFormatting.UNDERLINE));
		tooltip.add(GLLang.JeiExtra.GIFT_NOTE.get());
		int hidden = 0;
		for (int row = ROWS; row < tiers.size(); row++) hidden += tiers.get(row).gifts().size();
		if (hidden > 0) tooltip.add(GLLang.JeiExtra.MORE.get(hidden));
		if (tiers.size() > ROWS) tooltip.add(GLLang.JeiExtra.MORE_RUNGS.get(tiers.size() - ROWS));
	}

	@Override
	public @Nullable ResourceLocation getRegistryName(EntityType<?> recipe) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(recipe);
	}

	private static List<GiftTier> tiers(EntityType<?> character) {
		var id = BuiltInRegistries.ENTITY_TYPE.getKey(character);
		if (id == null) return List.of();
		return PartyGiftToClient.ClientHandler.get(id);
	}

	private static int rowY(int row) {
		return GLJeiUtil.PAD + row * GLJeiUtil.SLOT;
	}

}