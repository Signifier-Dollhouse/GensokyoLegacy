package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WitchHandler implements HexBrewHandler {

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
		if (level.isClientSide) return;
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null) return;
		for (MobEffectInstance e : contents.getAllEffects()) {
			user.addEffect(new MobEffectInstance(e));
		}
	}

	@Override
	public List<DataComponentType<?>> getComponentsToCopy() {
		return List.of(DataComponents.POTION_CONTENTS);
	}
}
