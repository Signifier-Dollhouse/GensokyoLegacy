package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2library.serial.recipe.BaseRecipeBuilder;
import net.minecraft.resources.ResourceLocation;

public class PotionStageBuilder extends BaseRecipeBuilder<PotionStageBuilder, PotionStageRecipe, AlchemyStageRecipe<?>, AlchemyInv> {

	private final ResourceLocation id;

	public PotionStageBuilder(ResourceLocation id) {
		super(GLRecipes.ALCHEMY_STAGE_POTION.get());
		this.id = id;
	}

	public void save(RegistrateRecipeProvider pvd) {
		super.save(pvd, pvd.safeId(id));
	}

	public void save(RegistrateRecipeProvider pvd, ResourceLocation id) {
		super.save(pvd, pvd.safeId(id));
	}
}
