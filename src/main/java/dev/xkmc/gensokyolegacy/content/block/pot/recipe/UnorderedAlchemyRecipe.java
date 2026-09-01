package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import com.google.common.collect.Sets;
import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@SerialClass
public class UnorderedAlchemyRecipe extends AlchemyRecipe<UnorderedAlchemyRecipe> {

	@SerialField
	public final ArrayList<Ingredient> input = new ArrayList<>();

	public UnorderedAlchemyRecipe() {
		super(GLRecipes.ALCHEMY_UNORDERED.get());
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
	public FluidStack getResultFluid(AlchemyInv inv, HolderLookup.Provider access) {
		var ans = super.getResultFluid(inv, access);
		if (ans.isEmpty()) return ans;
		if (inv.fluid().getFluid() instanceof GLHexFluid in && ans.getFluid() instanceof GLHexFluid out) {
			var inList = in.brew.handler.getComponentsToCopy();
			var outList = out.brew.handler.getComponentsToCopy();
			for (var e : Sets.intersection(new LinkedHashSet<>(inList), new LinkedHashSet<>(outList))) {
				HexBrew.copyComponent(e, inv.fluid(), ans);
			}
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
