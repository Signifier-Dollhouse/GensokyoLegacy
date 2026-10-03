package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2serial.util.Wrappers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class GLJEIPlugin implements IModPlugin {

	public static final ResourceLocation ID = GensokyoLegacy.loc("main");

	public static final RecipeType<AlchemyRecipe<?>> ALCHEMY =
			RecipeType.create(GensokyoLegacy.MODID, "alchemy", Wrappers.cast(AlchemyRecipe.class));
	public static final RecipeType<Holder<TradeOffer>> TRADE =
			RecipeType.create(GensokyoLegacy.MODID, "trade", Wrappers.cast(Holder.class));
	public static final RecipeType<Holder<Quest>> QUEST =
			RecipeType.create(GensokyoLegacy.MODID, "quest", Wrappers.cast(Holder.class));

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(
				new AlchemyRecipeCategory().init(helper),
				new TradeRecipeCategory().init(helper),
				new QuestRewardCategory().init(helper));
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
		// the rpg registries are datapack registries; they are only complete once the world is
		// loaded, which is what having a level here means
		var access = level.registryAccess();
		registration.addRecipes(TRADE, CodecRegistry.TRADE.getAll(access).toList());
		registration.addRecipes(QUEST, CodecRegistry.QUEST.getAll(access).toList());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(GLBlocks.ALCHEMY_POT.asStack(), ALCHEMY);
		registration.addRecipeCatalyst(Items.EMERALD, TRADE);
		registration.addRecipeCatalyst(Items.BOOK, QUEST);
	}

}