package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.rpg.trade.IClientOffer;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.content.ui.trade.TradeScreen;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
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
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * One {@link TradeOffer} as a JEI recipe: what the character takes in, what it hands back,
 * and the same action line the trade screen shows on the offer's tooltip, so the two never
 * disagree. The stock line is left out — that is per-player state, not part of the offer.
 */
public class TradeRecipeCategory extends BaseRecipeCategory<Holder<TradeOffer>, TradeRecipeCategory> {

	private static final int COLS = 3;
	private static final int ROWS = 2;
	private static final int MAX_INGREDIENTS = COLS * ROWS;

	private static final int IN_X = GLJeiUtil.PAD;
	private static final int ARROW_X = IN_X + COLS * GLJeiUtil.SLOT + GLJeiUtil.GAP;
	private static final int OUT_X = ARROW_X + GLJeiUtil.ARROW + GLJeiUtil.GAP;
	private static final int SLOTS_Y = GLJeiUtil.PAD;
	private static final int ROW_Y = SLOTS_Y + (ROWS - 1) * GLJeiUtil.SLOT / 2;
	private static final int TEXT_Y = SLOTS_Y + ROWS * GLJeiUtil.SLOT + GLJeiUtil.GAP;

	private static final int WIDTH = OUT_X + GLJeiUtil.SLOT + GLJeiUtil.PAD;
	private static final int HEIGHT = TEXT_Y + 3 * GLJeiUtil.LINE + GLJeiUtil.PAD;

	public TradeRecipeCategory() {
		super(GensokyoLegacy.loc("trade"), Wrappers.cast(Holder.class));
	}

	public TradeRecipeCategory init(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
		this.icon = helper.createDrawableItemStack(Items.EMERALD.getDefaultInstance());
		return this;
	}

	@Override
	public Component getTitle() {
		return GLLang.Jei.TRADE.get();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Holder<TradeOffer> recipe, IFocusGroup focuses) {
		var offer = recipe.value();
		int index = 0;
		for (var entry : offer.ingredients()) {
			if (index == MAX_INGREDIENTS) break;
			GLJeiUtil.addEntry(builder, RecipeIngredientRole.INPUT,
					GLJeiUtil.gridX(index, COLS), GLJeiUtil.gridY(index, COLS), entry);
			index++;
		}
		GLJeiUtil.slot(builder, RecipeIngredientRole.OUTPUT, OUT_X, ROW_Y).addItemStack(offer.result().copy());
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, Holder<TradeOffer> recipe, IFocusGroup focuses) {
		var offer = recipe.value();
		int textWidth = WIDTH - 2 * GLJeiUtil.PAD;
		builder.addRecipeArrowWidget().setPosition(ARROW_X, ROW_Y);
		GLJeiUtil.text(builder, TradeScreen.actionText(offer), GLJeiUtil.PAD, TEXT_Y, textWidth, 2 * GLJeiUtil.LINE);
		GLJeiUtil.text(builder, priceText(offer), GLJeiUtil.PAD, TEXT_Y + 2 * GLJeiUtil.LINE, textWidth, GLJeiUtil.LINE);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, Holder<TradeOffer> recipe,
			IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
		var offer = recipe.value();
		if (offer.ingredients().size() > MAX_INGREDIENTS)
			tooltip.add(GLLang.JeiExtra.MORE.get(offer.ingredients().size() - MAX_INGREDIENTS));
	}

	@Override
	public @Nullable ResourceLocation getRegistryName(Holder<TradeOffer> recipe) {
		return recipe.unwrapKey().map(e -> e.location()).orElse(null);
	}

	/**
	 * The currency price for a currency trade, the stock cap otherwise — the same split the
	 * trade screen makes between its price tag and its stock line.
	 */
	private static Component priceText(TradeOffer offer) {
		var currency = IClientOffer.resolve(offer).currency();
		if (!currency.isEmpty()) {
			return GLLang.JeiExtra.PRICE.get(Component.literal("¥" + currency.getCount()).withStyle(ChatFormatting.GOLD)
					.append(" ").append(currency.getHoverName().copy()));
		}
		return GLLang.JeiExtra.MAX_STOCK.get(offer.recurrence().maxStock());
	}

}