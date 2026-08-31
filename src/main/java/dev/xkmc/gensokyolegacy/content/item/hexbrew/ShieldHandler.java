package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ShieldHandler implements HexBrewHandler {

	@Override
	public boolean isThrowable() {
		return false;
	}

	@Override
	public void onHit(Level level, Vec3 pos, @Nullable Entity thrower) {
	}

	@Override
	public boolean isDrinkable() {
		return true;
	}

	@Override
	public void onDrink(LivingEntity user, ItemStack stack, Level level) {
		if (!level.isClientSide) {
			user.addEffect(new MobEffectInstance(GLEffects.STARLIGHT_SHIELD.holder(), 1200, 0));
		}
	}
}
