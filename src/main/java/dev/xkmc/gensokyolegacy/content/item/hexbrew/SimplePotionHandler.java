package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record SimplePotionHandler(boolean throwable, PotionContents potion) implements AbstractPotionHandler {

	public SimplePotionHandler(boolean throwable, Holder<MobEffect> eff, int dur, int amp) {
		this(throwable, new PotionContents(Optional.empty(), Optional.empty(),
				List.of(new MobEffectInstance(eff, dur, amp))));
	}

	@Override
	public boolean isThrowable() {
		return throwable;
	}

	@Override
	public boolean isDrinkable() {
		return !throwable;
	}

	@Override
	public void onHit(Level level, Vec3 pos, @Nullable Entity thrower) {

	}

	public PotionContents getPotionContents() {
		return potion;
	}

	@Override
	public Item.Properties modify(Item.Properties p) {
		return AbstractPotionHandler.super.modify(p).component(DataComponents.POTION_CONTENTS, getPotionContents());
	}

}
