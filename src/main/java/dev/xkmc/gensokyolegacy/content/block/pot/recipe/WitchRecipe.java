package dev.xkmc.gensokyolegacy.content.block.pot.recipe;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyInv;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
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
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SerialClass
public class WitchRecipe extends AlchemyRecipe<WitchRecipe> {

	private static final int CAP = 8 * 60 * 20;

	@SerialField
	public int potionCount = 0;

	@SerialField
	public final ArrayList<Ingredient> extra = new ArrayList<>();

	public WitchRecipe() {
		super(GLRecipes.ALCHEMY_WITCH.get());
		// defaults, overwritten by builder/json
		time = 200;
		resultFluid = new FluidStack((Fluid) HexBrew.WITCH.fluid.getSource(), 250);
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
				if (!isRegularPotion(stack)) return false;
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
		// additional validation for witch-extend case
		if (isWitchFluid(inv)) {
			PotionContents pc = inv.fluid().get(DataComponents.POTION_CONTENTS);
			if (pc == null) return false;
			var effects = pc.getAllEffects();
			boolean empty = true;
			for (var ignored : effects) { empty = false; break; }
			if (empty) return false;
			for (var e : effects) {
				if (e.getDuration() >= CAP) return false;
			}
		} else {
			// mundane case: ensure potion effects not empty is not required? but if potions have no effects, result would be empty; allow but will produce empty?
			// still valid
		}
		return true;
	}

	@Override
	public List<Ingredient> getHints(Level level, AlchemyInv inv) {
		return List.of();
	}

	@Override
	public FluidStack getResultFluid(AlchemyInv inv, HolderLookup.Provider access) {
		FluidStack outFluid = new FluidStack((Fluid) HexBrew.WITCH.fluid.getSource(), 250);
		if (isWitchFluid(inv)) {
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
		} else {
			// mundane + potions: merge additive
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

	private static boolean isWitchFluid(AlchemyInv inv) {
		return inv.fluid().getFluid() == HexBrew.WITCH.fluid.getSource();
	}

	private static boolean isRegularPotion(ItemStack stack) {
		return stack.is(Items.POTION);
	}
}
