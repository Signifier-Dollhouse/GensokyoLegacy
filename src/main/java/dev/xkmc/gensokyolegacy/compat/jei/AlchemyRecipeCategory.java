package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2core.compat.jei.BaseRecipeCategory;
import dev.xkmc.l2serial.util.Wrappers;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class AlchemyRecipeCategory extends BaseRecipeCategory<AlchemyRecipe<?>, AlchemyRecipeCategory> {

	private static final int WIDTH = 18 * 4 + 30 + 36;

	public AlchemyRecipeCategory() {
		super(GensokyoLegacy.loc("alchemy"), Wrappers.cast(AlchemyRecipe.class));
	}

	public AlchemyRecipeCategory init(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(WIDTH, 36);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, GLBlocks.ALCHEMY_POT.asStack());
		return this;
	}

	@Override
	public Component getTitle() {
		return GLLang.Jei.ALCHEMY.get();
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, AlchemyRecipe<?> recipe, IFocusGroup focuses) {
		int time = recipe.getProcessTime();
		if (time <= 0) time = 200;
		Layout lay = compute(recipe);
		builder.addAnimatedRecipeArrow(time).setPosition(lay.arrowX, lay.arrowY);
		builder.addText(Component.translatable("gui.jei.category.smelting.time.seconds", time / 20), 80, 10)
				.setPosition(lay.arrowX, 26)
				.setColor(0xFF808080);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, AlchemyRecipe<?> recipe, IFocusGroup focuses) {
		var compiled = compile(recipe.getInputItems());
		Layout lay = compute(recipe);
		int idx = 0;
		// input fluid
		if (!recipe.inputFluid.isEmpty()) {
			var slot = builder.addSlot(RecipeIngredientRole.INPUT, lay.inputX, lay.inputY)
					.setStandardSlotBackground();
			var inFluidItems = recipe.getInputFluidItemStacks();
			if (!inFluidItems.isEmpty()) {
				slot.addItemStacks(inFluidItems);
				builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
						.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(recipe.inputFluid.getStacks()));
			} else {
				slot.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(recipe.inputFluid.getStacks()));
			}
			idx++;
		}
		// input items
		for (var arr : compiled) {
			int col = idx % lay.inputCols, row = idx / lay.inputCols;
			builder.addSlot(RecipeIngredientRole.INPUT, lay.inputX + col * SLOT, lay.inputY + row * SLOT)
					.setStandardSlotBackground()
					.addItemStacks(List.of(arr));
			idx++;
		}
		// outputs
		boolean outFluid = !recipe.resultFluid.isEmpty();
		boolean outItem = !recipe.resultItem.isEmpty();
		if (lay.outCount == 1) {
			addOutput(builder, recipe, lay.outX, lay.outY, true, outFluid, outItem);
		} else if (lay.outCount == 2) {
			int oi = 0;
			if (outFluid) addOutput(builder, recipe, lay.outX + SLOT * oi++, lay.outY, false, true, false);
			if (outItem) addOutput(builder, recipe, lay.outX + SLOT * oi, lay.outY, false, false, true);
		}
	}

	private void addOutput(IRecipeLayoutBuilder builder, AlchemyRecipe<?> recipe, int x, int y,
	                       boolean big, boolean useFluid, boolean useItem) {
		var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y);
		if (big) slot.setOutputSlotBackground();
		else slot.setStandardSlotBackground();
		if (useFluid) {
			if (!recipe.getOutputFluidItemStacks().isEmpty()) {
				slot.addItemStacks(recipe.getOutputFluidItemStacks());
				builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
						.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(recipe.resultFluid));
			} else {
				slot.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(recipe.resultFluid));
			}
		}
		if (useItem) {
			slot.addItemStack(recipe.resultItem);
		}
	}

	private static final int SLOT = 18;
	private static final int BIG_OUT = 26;
	private static final int ARROW_W = 30;

	private record Layout(int inputX, int inputY, int inputCols, int arrowX, int arrowY,
	                      int outX, int outY, int outCount) {
	}

	private Layout compute(AlchemyRecipe<?> recipe) {
		boolean outFluid = !recipe.resultFluid.isEmpty();
		boolean outItem = !recipe.resultItem.isEmpty();
		int outCount = (outFluid ? 1 : 0) + (outItem ? 1 : 0);

		int inputCount = compile(recipe.getInputItems()).size() + (!recipe.inputFluid.isEmpty() ? 1 : 0);
		int inputW = inputCount <= 4 ? inputCount * SLOT : ((inputCount + 1) / 2) * SLOT;
		int outW = outCount == 1 ? BIG_OUT : outCount == 2 ? SLOT * 2 : 0;
		int totalW = inputW + ARROW_W + outW;
		int x0 = Math.max(0, (getWidth() - totalW) / 2);

		int inputCols = inputCount <= 4 ? inputCount : (inputCount + 1) / 2;
		boolean twoRows = inputCount > 4;

		return new Layout(
				x0 + 1, twoRows ? 1 : 10, inputCols,
				x0 + inputW + 4, 10,
				x0 + inputW + ARROW_W + 5, 10,
				outCount
		);
	}

	private List<ItemStack[]> compile(List<Ingredient> list) {
		Int2ObjectLinkedOpenHashMap<ItemStack[]> set = new Int2ObjectLinkedOpenHashMap<>();
		for (var e : list) {
			if (e.isEmpty()) {
				set.put(1, new ItemStack[0]);
				continue;
			}
			var stacks = e.getItems();
			int result = 1;
			for (var stack : stacks) {
				int hash;
				if (stack.isEmpty()) {
					hash = 0;
				} else {
					hash = BuiltInRegistries.ITEM.getId(stack.getItem());
					var tag = stack.getComponentsPatch();
					if (!tag.isEmpty()) {
						hash += tag.hashCode() * 15;
					}
				}
				result = 31 * result + hash;
			}
			var old = set.get(result);
			if (old != null) {
				for (var x : old) {
					x.grow(1);
				}
			} else {
				var copy = stacks.clone();
				for (int i = 0; i < copy.length; i++)
					copy[i] = copy[i].copy();
				set.put(result, copy);
			}
		}
		return new ArrayList<>(set.values());
	}
}
