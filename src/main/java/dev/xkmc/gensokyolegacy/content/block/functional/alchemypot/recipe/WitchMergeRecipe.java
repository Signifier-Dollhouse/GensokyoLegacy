package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage.PotionStageRecipe;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import dev.xkmc.gensokyolegacy.util.PotionData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;

@SerialClass
public class WitchMergeRecipe extends AlchemyRecipe<WitchMergeRecipe> {

	@SerialField
	public int potionCount = 0;

	@SerialField
	public Ingredient potionIngredient = Ingredient.EMPTY;

	@SerialField
	public final ArrayList<Ingredient> extra = new ArrayList<>();

	public WitchMergeRecipe(ResourceLocation id) {
		super(id, GLRecipes.ALCHEMY_WITCH_MERGE.get());
		time = 200;
	}

	@Override
	public List<Ingredient> getInputItems() {
		List<Ingredient> list = new ArrayList<>();
		for (int i = 0; i < potionCount; i++) {
			list.add(potionIngredient);
		}
		list.addAll(extra);
		return list;
	}

	@Override
	public boolean matches(AlchemyInv inv, Level level) {
		if (!super.matches(inv, level)) return false;
		if (inv.fluid().getAmount() < 1000) return false;
		if (inv.size() > potionCount + extra.size()) return false;
		int potions = 0;
		List<Ingredient> remain = new ArrayList<>(extra);
		for (var stack : inv.list()) {
			if (potionIngredient.test(stack)) {
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
		if (!inv.isComplete())
			return potions <= potionCount;
		else return potions == potionCount && remain.isEmpty();
	}

	@Override
	public List<Ingredient> getHints(Level level, AlchemyInv inv) {
		List<Ingredient> remain = new ArrayList<>(extra);
		int availPotion = potionCount;
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			if (availPotion > 0 && potionIngredient.test(stack)) {
				availPotion--;
			}
			var itr = remain.iterator();
			while (itr.hasNext()) {
				var ing = itr.next();
				if (ing.test(stack)) {
					itr.remove();
					break;
				}
			}
		}
		if (availPotion > 0) {
			remain.add(potionIngredient);
		}
		return remain;
	}

	@Override
	public FluidStack getResultFluid(AlchemyInv inv, RegistryAccess access) {
		Fluid fluid = resultFluid.isEmpty() ? inv.fluid().getFluid() : resultFluid.getFluid();
		FluidStack outFluid = new FluidStack(fluid, 250);
		int color = PotionStageRecipe.calculateMixColor(inv);
		Map<Pair<MobEffect, Integer>, Integer> map = new LinkedHashMap<>();
		for (ItemStack s : inv.list()) {
			if (!potionIngredient.test(s)) continue;
			var pc = PotionData.read(s);
			if (pc.isEmpty()) continue;
			for (MobEffectInstance e : pc.getAllEffects()) {
				var holder = e.getEffect();
				var key = Pair.of(holder, e.getAmplifier());
				map.merge(key, e.getDuration(), Integer::sum);
			}
		}
		List<MobEffectInstance> list = new ArrayList<>();
		for (var ent : map.entrySet()) {
			list.add(new MobEffectInstance(ent.getKey().getFirst(), ent.getValue(), ent.getKey().getSecond()));
		}
		PotionData.effects(color, list).write(outFluid);
		return outFluid;
	}

}
