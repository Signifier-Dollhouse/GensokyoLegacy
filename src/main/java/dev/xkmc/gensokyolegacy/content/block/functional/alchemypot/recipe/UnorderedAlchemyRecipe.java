package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe;

import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.gensokyolegacy.util.PotionData;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches inputs unordered: each stack claims its own input entry; a partial container may leave inputs unmatched.
 *
 * @see "doc/design/alchemy_pot.md"
 */
@SerialClass
public class UnorderedAlchemyRecipe extends AlchemyRecipe<UnorderedAlchemyRecipe> {

	@SerialField
	public final ArrayList<Ingredient> input = new ArrayList<>();

	public UnorderedAlchemyRecipe(ResourceLocation id) {
		super(id, GLRecipes.ALCHEMY_UNORDERED.get());
	}

	@Override
	public List<Ingredient> getInputItems() {
		return input;
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (!super.matches(inv, level)) return false;
		return matchItems(inv, level, input);
	}

	@Override
	public FluidStack getResultFluid(AlchemyInv inv, RegistryAccess access) {
		var ans = super.getResultFluid(inv, access);
		if (ans.isEmpty()) return ans;
		if (inv.fluid().getFluid() instanceof GLHexFluid in && ans.getFluid() instanceof GLHexFluid out) {
			if (in.brew.handler.copiesPotion() && out.brew.handler.copiesPotion())
				PotionData.read(inv.fluid()).write(ans);
		}
		return ans;
	}

	@Override
	public List<Ingredient> getHints(Level level, AlchemyInv inv) {
		List<Ingredient> remain = new ArrayList<>(input);
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			var itr = remain.iterator();
			while (itr.hasNext()) {
				var ing = itr.next();
				if (ing.test(stack)) {
					itr.remove();
					break;
				}
			}
		}
		return remain;
	}
}
