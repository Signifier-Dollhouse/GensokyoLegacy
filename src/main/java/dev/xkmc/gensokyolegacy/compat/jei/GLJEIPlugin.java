package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.entity.module.EntityInteractions;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.Quest;
import dev.xkmc.gensokyolegacy.content.rpg.trade.TradeOffer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
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
import net.minecraft.world.entity.EntityType;
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
	public static final RecipeType<EntityType<?>> GIFT =
			RecipeType.create(GensokyoLegacy.MODID, "gift", Wrappers.cast(EntityType.class));
	public static final RecipeType<EntityType<?>> INTERACTION =
			RecipeType.create(GensokyoLegacy.MODID, "interaction", Wrappers.cast(EntityType.class));

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
				new QuestRewardCategory().init(helper),
				new PartyGiftCategory().init(helper),
				new EntityInteractionCategory().init(helper));
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
		// a quest paying nothing but experience and reputation has no item to look up, so it would
		// be a page of blank slots; better that it never appears than that it does
		registration.addRecipes(QUEST, CodecRegistry.QUEST.getAll(access)
				.filter(QuestRewardCategory::hasItemReward).toList());
		// a character comes to a party only if she has a taste row, so that is the whole roster of
		// gift givers; one page each, holding her own ladder or the default one she falls back to
		registration.addRecipes(GIFT, GLMeta.PARTY_TASTE.getAll(access)
				.<EntityType<?>>map(pair -> pair.getFirst().value()).toList());
		// what a character answers to is stated in code beside the entity rather than read from
		// here, so only the characters that registered a page have one to draw
		registration.addRecipes(INTERACTION, EntityInteractions.characters());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(GLBlocks.ALCHEMY_POT.asStack(), ALCHEMY);
		registration.addRecipeCatalyst(Items.EMERALD, TRADE);
		registration.addRecipeCatalyst(Items.BOOK, QUEST);
		registration.addRecipeCatalyst(GLItems.INVITATION_LETTER.asStack(), GIFT);
		registration.addRecipeCatalyst(Items.COOKIE.getDefaultInstance(), INTERACTION);
	}

}