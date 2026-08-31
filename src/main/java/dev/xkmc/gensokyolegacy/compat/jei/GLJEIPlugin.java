package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.block.pot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrewBottleItem;
import dev.xkmc.gensokyolegacy.content.ui.dialog.FirstDialogScreen;
import dev.xkmc.gensokyolegacy.content.ui.dialog.SimpleDialogScreen;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.util.Wrappers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

@JeiPlugin
public class GLJEIPlugin implements IModPlugin {

	public static final ResourceLocation ID = GensokyoLegacy.loc("main");

	public static final RecipeType<AlchemyRecipe<?>> ALCHEMY =
			RecipeType.create(GensokyoLegacy.MODID, "alchemy", Wrappers.cast(AlchemyRecipe.class));

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		for (var brew : HexBrew.values()) {
			var item = brew.bottle.get();
			registration.registerSubtypeInterpreter(item, GLJEIPlugin::bottleSubtype);
		}
	}

	private static String bottleSubtype(ItemStack stack, UidContext ctx) {
		if (stack.getItem() instanceof HexBrewBottleItem bottle) {
			FluidStack fs = bottle.getFluidStack(stack);
			if (fs.isEmpty()) return "";
			String key = fs.getFluid().builtInRegistryHolder().unwrapKey().orElseThrow().location().toString();
			var patch = fs.getComponentsPatch();
			if (!patch.isEmpty()) key += patch.hashCode();
			var itemPatch = stack.getComponentsPatch();
			if (!itemPatch.isEmpty()) key += itemPatch.hashCode();
			return key;
		}
		return "";
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
		registration.addGuiScreenHandler(FirstDialogScreen.class, e -> null);
		registration.addGuiScreenHandler(SimpleDialogScreen.class, e -> null);
	}

}
