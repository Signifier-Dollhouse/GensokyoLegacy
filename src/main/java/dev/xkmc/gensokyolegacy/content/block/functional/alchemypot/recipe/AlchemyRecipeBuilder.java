package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.l2core.serial.recipe.BaseRecipe;
import net.minecraft.resources.ResourceLocation;

public abstract class AlchemyRecipeBuilder<T extends AlchemyRecipe<T>, B extends AlchemyRecipeBuilder<T, B>>
		extends AbstractAlchemyRecipeBuilder<T, B> {

	protected AlchemyRecipeBuilder(BaseRecipe.RecType<T, AlchemyRecipe<?>, AlchemyInv> type) {
		super(type);
	}

	public void save(RegistrateRecipeProvider pvd) {
		ResourceLocation id;
		if (!recipe.resultItem.isEmpty()) {
			var item = recipe.resultItem.getItem();
			var key = item.builtInRegistryHolder().unwrapKey().orElseThrow().location();
			id = pvd.safeId(key);
		} else if (!recipe.resultFluid.isEmpty()) {
			var fluid = recipe.resultFluid.getFluid();
			var key = fluid.builtInRegistryHolder().unwrapKey().orElseThrow().location();
			id = pvd.safeId(key);
		} else {
			throw new IllegalStateException("Alchemy recipe needs result item or fluid");
		}
		super.save(pvd, id);
	}

	public void save(RegistrateRecipeProvider pvd, ResourceLocation id) {
		super.save(pvd, pvd.safeId(id));
	}
}
