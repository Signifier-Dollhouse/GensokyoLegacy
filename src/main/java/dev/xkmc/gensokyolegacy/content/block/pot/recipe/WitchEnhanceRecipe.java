package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SerialClass
public class WitchEnhanceRecipe extends AlchemyRecipe<WitchEnhanceRecipe> {

	private static final int CAP = 8 * 60 * 20;

	@SerialField
	public final ArrayList<Ingredient> extra = new ArrayList<>();

	public WitchEnhanceRecipe() {
		super(GLRecipes.ALCHEMY_WITCH_ENHANCE.get());
		time = 200;
	}

	@Override
	public List<Ingredient> getInputItems() {
		return new ArrayList<>(extra);
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (!super.matches(inv, level)) return false;
		if (inv.fluid().getAmount() < 1000) return false;
		if (inv.size() != extra.size()) return false;
		List<Ingredient> remain = new ArrayList<>(extra);
		for (var stack : inv.list()) {
			boolean matched = false;
			var itr = remain.iterator();
			while (itr.hasNext()) {
				Ingredient ing = itr.next();
				if (ing.test(stack)) {
					itr.remove();
					matched = true;
					break;
				}
			}
			if (!matched) return false;
		}
		if (!remain.isEmpty()) return false;
		PotionContents pc = inv.fluid().get(DataComponents.POTION_CONTENTS);
		if (pc == null) return false;
		var effects = pc.getAllEffects();
		boolean empty = true;
		for (var ignored : effects) {
			empty = false;
			break;
		}
		if (empty) return false;
		for (var e : effects) {
			if (e.getDuration() >= CAP) return false;
		}
		return true;
	}

	@Override
	public List<Ingredient> getHints(Level level, AlchemyInv inv) {
		return List.of();
	}

	@Override
	public FluidStack getResultFluid(AlchemyInv inv, HolderLookup.Provider access) {
		Fluid fluid = resultFluid.isEmpty() ? inv.fluid().getFluid() : resultFluid.getFluid();
		FluidStack outFluid = new FluidStack(fluid, 250);
		PotionContents pc = inv.fluid().get(DataComponents.POTION_CONTENTS);
		if (pc == null) return outFluid;
		List<MobEffectInstance> out = new ArrayList<>();
		for (MobEffectInstance e : pc.getAllEffects()) {
			int nd = Math.min(e.getDuration() * 2, CAP);
			out.add(new MobEffectInstance(e.getEffect(), nd, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
		}
		PotionContents ncont = new PotionContents(Optional.empty(), Optional.empty(), out);
		outFluid.set(DataComponents.POTION_CONTENTS, ncont);
		return outFluid;
	}
}
