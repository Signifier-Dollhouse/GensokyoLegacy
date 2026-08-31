package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SerialClass
public class WitchMergeRecipe extends AlchemyRecipe<WitchMergeRecipe> {

	@SerialField
	public int potionCount = 0;

	@SerialField
	public final ArrayList<Ingredient> extra = new ArrayList<>();

	public WitchMergeRecipe() {
		super(GLRecipes.ALCHEMY_WITCH_MERGE.get());
		time = 200;
	}

	@Override
	public List<Ingredient> getInputItems() {
		List<Ingredient> list = new ArrayList<>();
		for (int i = 0; i < potionCount; i++) {
			list.add(Ingredient.of(Items.POTION));
		}
		list.addAll(extra);
		return list;
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (!super.matches(inv, level)) return false;
		if (inv.fluid().getAmount() < 1000) return false;
		if (inv.size() != potionCount + extra.size()) return false;
		int potions = 0;
		List<Ingredient> remain = new ArrayList<>(extra);
		for (var stack : inv.list()) {
			if (stack.is(Items.POTION)) {
				potions++;
			} else {
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
		}
		if (potions != potionCount) return false;
		if (!remain.isEmpty()) return false;
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
		Map<Holder<MobEffect>, MobEffectInstance> map = new LinkedHashMap<>();
		for (ItemStack s : inv.list()) {
			if (!s.is(Items.POTION)) continue;
			PotionContents pc = s.get(DataComponents.POTION_CONTENTS);
			if (pc == null) continue;
			for (MobEffectInstance e : pc.getAllEffects()) {
				var holder = e.getEffect();
				MobEffectInstance existing = map.get(holder);
				if (existing == null) {
					map.put(holder, new MobEffectInstance(e));
				} else {
					int dur = existing.getDuration() + e.getDuration();
					int amp = Math.max(existing.getAmplifier(), e.getAmplifier());
					var n = new MobEffectInstance(holder, dur, amp, existing.isAmbient(), existing.isVisible(), existing.showIcon());
					map.put(holder, n);
				}
			}
		}
		List<MobEffectInstance> list = new ArrayList<>(map.values());
		PotionContents out = new PotionContents(Optional.empty(), Optional.empty(), list);
		outFluid.set(DataComponents.POTION_CONTENTS, out);
		return outFluid;
	}

}
