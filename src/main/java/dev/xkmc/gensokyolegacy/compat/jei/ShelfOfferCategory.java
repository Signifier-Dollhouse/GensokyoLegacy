package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.block.deco.shelf.MorichikaOfferData;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import dev.xkmc.l2library.serial.recipe.BaseRecipeCategory;
import dev.xkmc.l2serial.util.Wrappers;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One item a Morichika shelf can be stocked with: what it costs and how many the shelf holds.
 * Both are ranges because the shelf rolls them each time it is restocked.
 */
public class ShelfOfferCategory extends BaseRecipeCategory<Holder<Item>, ShelfOfferCategory> {

	private static final int IN_X = GLJeiUtil.PAD;
	private static final int ARROW_X = IN_X + GLJeiUtil.SLOT + GLJeiUtil.GAP;
	private static final int OUT_X = ARROW_X + GLJeiUtil.ARROW + GLJeiUtil.GAP;
	private static final int SLOT_Y = GLJeiUtil.PAD;
	private static final int TEXT_Y = SLOT_Y + GLJeiUtil.SLOT + GLJeiUtil.GAP;

	private static final int WIDTH = OUT_X + GLJeiUtil.SLOT + GLJeiUtil.PAD;
	private static final int HEIGHT = TEXT_Y + 2 * GLJeiUtil.LINE + GLJeiUtil.PAD;

	public ShelfOfferCategory() {
		super(GensokyoLegacy.loc("shelf_offer"), Wrappers.cast(Holder.class));
	}

	public ShelfOfferCategory init(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
		this.icon = helper.createDrawableItemStack(GLFurniture.SHELF.asStack());
		return this;
	}

	@Override
	public Component getTitle() {
		return GLLang.Jei.SHELF_OFFER.get();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Holder<Item> recipe, IFocusGroup focuses) {
		var data = data(recipe);
		GLJeiUtil.slot(builder, RecipeIngredientRole.INPUT, IN_X, SLOT_Y)
				.addItemStack(new ItemStack(GLItems.COIN.get(), data.minPrice()));
		GLJeiUtil.slot(builder, RecipeIngredientRole.OUTPUT, OUT_X, SLOT_Y)
				.addItemStack(new ItemStack(recipe.value()));
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, Holder<Item> recipe, IFocusGroup focuses) {
		var data = data(recipe);
		var price = range(data.minPrice(), data.maxPrice());
		var stock = range(data.minStock(), data.maxStock());
		int width = WIDTH - 2 * GLJeiUtil.PAD;
		builder.addRecipeArrow().setPosition(ARROW_X, SLOT_Y);
		GLJeiUtil.text(builder, GLLang.JeiExtra.SHELF_PRICE.get(price),
				GLJeiUtil.PAD, TEXT_Y, width, GLJeiUtil.LINE);
		GLJeiUtil.text(builder, GLLang.JeiExtra.SHELF_STOCK.get(stock),
				GLJeiUtil.PAD, TEXT_Y + GLJeiUtil.LINE, width, GLJeiUtil.LINE);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, Holder<Item> recipe,
						   IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
		tooltip.add(GLLang.JeiExtra.SHELF_NOTE.get());
	}

	@Override
	public @Nullable ResourceLocation getRegistryName(Holder<Item> recipe) {
		return recipe.unwrapKey().map(e -> e.location()).orElse(null);
	}

	private static MorichikaOfferData data(Holder<Item> item) {
		var level = Minecraft.getInstance().level;
		if (level == null) return MorichikaOfferData.DEFAULT;
		var data = GLMeta.MORICHIKA_OFFER.get(level.registryAccess(), item);
		return data == null ? MorichikaOfferData.DEFAULT : data;
	}

	private static Component range(int min, int max) {
		return min == max ? Component.literal(String.valueOf(min)) : GLLang.JeiExtra.COUNT_RANGE.get(min, max);
	}

}
