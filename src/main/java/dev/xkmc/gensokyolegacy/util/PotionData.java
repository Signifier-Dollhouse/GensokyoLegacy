package dev.xkmc.gensokyolegacy.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 1.21's {@code PotionContents} over the 1.20.1 potion NBT shape, shared by the hex brews and the
 * alchemy pot so an item and a fluid stack carry the same effects.
 */
public final class PotionData {

	public static final PotionData EMPTY = new PotionData(Potions.EMPTY, null, List.of());

	private final Potion potion;
	private final @Nullable Integer customColor;
	private final List<MobEffectInstance> customEffects;

	public PotionData(Potion potion) {
		this(potion, null, List.of());
	}

	public PotionData(Holder<Potion> potion) {
		this(potion.value());
	}

	public PotionData(Potion potion, @Nullable Integer customColor, List<MobEffectInstance> customEffects) {
		this.potion = potion;
		this.customColor = customColor;
		this.customEffects = List.copyOf(customEffects);
	}

	public PotionData(Optional<? extends Holder<Potion>> potion, Optional<Integer> customColor,
					  List<MobEffectInstance> customEffects) {
		this(potion.map(Holder::value).orElse(Potions.EMPTY), customColor.orElse(null), customEffects);
	}

	public static PotionData of(Potion potion) {
		return new PotionData(potion);
	}

	public static PotionData of(Holder<Potion> potion) {
		return new PotionData(potion);
	}

	public static PotionData effects(Collection<MobEffectInstance> effects) {
		return new PotionData(Potions.EMPTY, null, new ArrayList<>(effects));
	}

	public static PotionData effects(int color, Collection<MobEffectInstance> effects) {
		return new PotionData(Potions.EMPTY, color, new ArrayList<>(effects));
	}

	public static PotionData read(ItemStack stack) {
		return read(stack.getTag());
	}

	public static PotionData read(FluidStack stack) {
		return read(stack.getTag());
	}

	public static PotionData read(@Nullable CompoundTag tag) {
		if (tag == null || tag.isEmpty()) return EMPTY;
		Potion potion = Potion.byName(tag.getString(PotionUtils.TAG_POTION));
		List<MobEffectInstance> custom = PotionUtils.getCustomEffects(tag);
		Integer color = tag.contains(PotionUtils.TAG_CUSTOM_POTION_COLOR, Tag.TAG_ANY_NUMERIC)
				? tag.getInt(PotionUtils.TAG_CUSTOM_POTION_COLOR) : null;
		return new PotionData(potion, color, custom);
	}

	public void write(ItemStack stack) {
		write(stack.getOrCreateTag());
	}

	public void write(FluidStack stack) {
		write(stack.getOrCreateTag());
	}

	public void write(CompoundTag tag) {
		if (potion == Potions.EMPTY) {
			tag.remove(PotionUtils.TAG_POTION);
		} else {
			tag.putString(PotionUtils.TAG_POTION, BuiltInRegistries.POTION.getKey(potion).toString());
		}
		if (customColor == null) {
			tag.remove(PotionUtils.TAG_CUSTOM_POTION_COLOR);
		} else {
			tag.putInt(PotionUtils.TAG_CUSTOM_POTION_COLOR, customColor);
		}
		if (customEffects.isEmpty()) {
			tag.remove(PotionUtils.TAG_CUSTOM_POTION_EFFECTS);
		} else {
			ListTag list = new ListTag();
			for (MobEffectInstance e : customEffects) list.add(e.save(new CompoundTag()));
			tag.put(PotionUtils.TAG_CUSTOM_POTION_EFFECTS, list);
		}
	}

	public List<MobEffectInstance> getAllEffects() {
		return PotionUtils.getAllEffects(potion, customEffects);
	}

	public int getColor() {
		if (customColor != null) return customColor;
		if (potion != Potions.EMPTY) return PotionUtils.getColor(potion);
		return PotionUtils.getColor(customEffects);
	}

	public boolean isEmpty() {
		return potion == Potions.EMPTY && customEffects.isEmpty();
	}

	public void addPotionTooltip(Consumer<Component> consumer, float durationFactor) {
		List<Component> list = new ArrayList<>();
		PotionUtils.addPotionTooltip(getAllEffects(), list, durationFactor);
		list.forEach(consumer);
	}

}
