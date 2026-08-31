package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.block.pot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLBlocks;
import dev.xkmc.l2core.compat.jei.BaseRecipeCategory;
import dev.xkmc.l2serial.util.Wrappers;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class AlchemyRecipeCategory extends BaseRecipeCategory<AlchemyRecipe<?>, AlchemyRecipeCategory> {

	public AlchemyRecipeCategory() {
		super(GensokyoLegacy.loc("alchemy"), Wrappers.cast(AlchemyRecipe.class));
	}

	public AlchemyRecipeCategory init(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(18 * 6 + 30, 54);
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
		builder.addAnimatedRecipeArrow(time).setPosition(68, 18);
		builder.addText(Component.translatable("gui.jei.category.smelting.time.seconds", time / 20), 80, 10)
				.setPosition(0, 0, getWidth(), getHeight(), HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM)
				.setTextAlignment(HorizontalAlignment.RIGHT)
				.setColor(0xFF808080);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, AlchemyRecipe<?> recipe, IFocusGroup focuses) {
		// input fluid as item (water bucket/bottle, hexbrew bottle), or fluid
		var inFluidItems = recipe.getInputFluidItemStacks();
		if (!inFluidItems.isEmpty()) {
			builder.addSlot(RecipeIngredientRole.INPUT, 1, 18)
					.setStandardSlotBackground()
					.addItemStacks(inFluidItems);
			builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
					.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(recipe.inputFluid.getStacks()));
		} else if (!recipe.inputFluid.isEmpty()) {
			builder.addSlot(RecipeIngredientRole.INPUT, 1, 18)
					.setStandardSlotBackground()
					.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(recipe.inputFluid.getStacks()));
		}
		// input items
		var compiled = compile(recipe.getInputItems());
		int n = compiled.size();
		int width = n <= 3 ? n : (n + 2) / 3;
		int startX = 20;
		int rows = n == 0 ? 0 : (n + width - 1) / width;
		int startY = rows <= 1 ? 18 : rows == 2 ? 9 : 0;
		int x = 0, y = 0;
		for (var arr : compiled) {
			int px = startX + x * 18;
			int py = startY + y * 18;
			builder.addSlot(RecipeIngredientRole.INPUT, px, py)
					.setStandardSlotBackground()
					.addItemStacks(List.of(arr));
			x++;
			if (x >= width) {
				x = 0;
				y++;
			}
		}
		// output fluid as item
		FluidStack outFluid = recipe.resultFluid;
		if (!outFluid.isEmpty()) {
			var outFluidItems = recipe.getOutputFluidItemStacks();
			if (!outFluidItems.isEmpty()) {
				builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 18)
						.setOutputSlotBackground()
						.addItemStacks(outFluidItems);
				builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
						.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(outFluid));
			} else {
				if (!outFluid.isEmpty()) {
					builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 18)
							.setOutputSlotBackground()
							.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(outFluid));
				}
			}
		} else if (!recipe.resultItem.isEmpty()) {
			builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 18)
					.setOutputSlotBackground()
					.addItemStack(recipe.resultItem);
		}
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
