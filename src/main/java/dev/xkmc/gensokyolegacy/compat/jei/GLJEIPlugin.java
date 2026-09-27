package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.ui.dialog.FirstDialogScreen;
import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogScreen;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2serial.util.Wrappers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class GLJEIPlugin implements IModPlugin {
	private static final IGuiProperties HIDDEN = new IGuiProperties() {
		@Override public Class<? extends Screen> screenClass() { return Screen.class; }
		@Override public int guiLeft() { return 0; }
		@Override public int guiTop() { return 0; }
		@Override public int guiXSize() { return 0; }   // JEI 要求 >= 1，这里故意非法
		@Override public int guiYSize() { return 0; }
		@Override public int screenWidth() { return 0; }
		@Override public int screenHeight() { return 0; }
	};

	public static final ResourceLocation ID = GensokyoLegacy.loc("main");

	public static final RecipeType<AlchemyRecipe<?>> ALCHEMY =
			RecipeType.create(GensokyoLegacy.MODID, "alchemy", Wrappers.cast(AlchemyRecipe.class));

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new AlchemyRecipeCategory().init(helper));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		var level = Minecraft.getInstance().level;
		if (level == null) return;
		var manager = level.getRecipeManager();
		registration.addRecipes(ALCHEMY, Wrappers.cast(
				manager.getAllRecipesFor(GLRecipes.ALCHEMY_RT.get()).stream()
						.map(RecipeHolder::value)
						.toList()));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(GLBlocks.ALCHEMY_POT.asStack(), ALCHEMY);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiScreenHandler(FirstDialogScreen.class, e -> HIDDEN);
		registration.addGuiScreenHandler(SimpleDialogScreen.class, e -> HIDDEN);
	}

}
