package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import dev.xkmc.gensokyolegacy.util.PotionData;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.common.util.Lazy;

import java.util.List;

public record SimplePotionHandler(boolean throwable, Lazy<PotionData> potion) implements AbstractPotionHandler {

	public SimplePotionHandler(boolean throwable, Holder<Potion> potion) {
		this(throwable, Lazy.of(() -> PotionData.of(potion)));
	}

	public SimplePotionHandler(boolean throwable, Holder<MobEffect> eff, int dur, int amp) {
		this(throwable, Lazy.of(() -> PotionData.effects(List.of(new MobEffectInstance(eff.value(), dur, amp)))));
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
	public PotionData defaultPotion() {
		return potion().get();
	}

}
