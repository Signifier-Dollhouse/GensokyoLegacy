package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe;

import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import dev.xkmc.gensokyolegacy.util.PotionData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

@SerialClass
public class WitchEnhanceRecipe extends AlchemyRecipe<WitchEnhanceRecipe> {

	private static final int CAP = 8 * 60 * 20; // 8 minutes; a potion already at the cap cannot be enhanced again

	@SerialField
	public final ArrayList<Ingredient> extra = new ArrayList<>();

	public WitchEnhanceRecipe(ResourceLocation id) {
		super(id, GLRecipes.ALCHEMY_WITCH_ENHANCE.get());
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
		if (!matchItems(inv, level, extra)) return false;
		var pc = PotionData.read(inv.fluid());
		if (pc.isEmpty()) return false;
		var effects = pc.getAllEffects();
		for (var e : effects) {
			if (e.getDuration() >= CAP) return false;
		}
		return true;
	}

	@Override
	public List<Ingredient> getHints(Level level, AlchemyInv inv) {
		List<Ingredient> remain = new ArrayList<>(extra);
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

	@Override
	public FluidStack getResultFluid(AlchemyInv inv, RegistryAccess access) {
		Fluid fluid = resultFluid.isEmpty() ? inv.fluid().getFluid() : resultFluid.getFluid();
		FluidStack outFluid = new FluidStack(fluid, 250);
		var pc = PotionData.read(inv.fluid());
		if (pc.isEmpty()) return outFluid;
		List<MobEffectInstance> out = new ArrayList<>();
		for (MobEffectInstance e : pc.getAllEffects()) {
			int nd = Math.min(e.getDuration() * 2, CAP);
			out.add(new MobEffectInstance(e.getEffect(), nd, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
		}
		PotionData.effects(out).write(outFluid);
		return outFluid;
	}
}
