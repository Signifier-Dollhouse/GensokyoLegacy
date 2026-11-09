package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Legacy stand-in for the 1.21 {@code net.neoforged.neoforge.fluids.crafting.FluidIngredient};
 * matches a fluid by id or tag. JSON shape follows the generated 1.21 recipes:
 * {@code {"fluid": "..."}} or {@code {"tag": "..."}}.
 */
public record FluidIngredient(Optional<ResourceLocation> fluid, Optional<TagKey<Fluid>> tag) {

	public static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(i -> i.group(
			ResourceLocation.CODEC.optionalFieldOf("fluid").forGetter(FluidIngredient::fluid),
			TagKey.codec(Registries.FLUID).optionalFieldOf("tag").forGetter(FluidIngredient::tag)
	).apply(i, FluidIngredient::new));

	public static FluidIngredient empty() {
		return new FluidIngredient(Optional.empty(), Optional.empty());
	}

	public static FluidIngredient of(Fluid fluid) {
		return new FluidIngredient(Optional.of(BuiltInRegistries.FLUID.getKey(fluid)), Optional.empty());
	}

	public static FluidIngredient of(FluidStack stack) {
		return of(stack.getFluid());
	}

	public static FluidIngredient tag(TagKey<Fluid> tag) {
		return new FluidIngredient(Optional.empty(), Optional.of(tag));
	}

	public boolean isEmpty() {
		return fluid.isEmpty() && tag.isEmpty();
	}

	public List<FluidStack> getStacks() {
		if (fluid.isPresent()) return List.of(new FluidStack(
				BuiltInRegistries.FLUID.get(fluid.get()), 1000));
		if (tag.isPresent()) {
			List<FluidStack> list = new ArrayList<>();
			for (Fluid f : BuiltInRegistries.FLUID) {
				if (f.is(tag.get())) list.add(new FluidStack(f, 1000));
			}
			return list;
		}
		return List.of();
	}

	public boolean test(FluidStack stack) {
		if (fluid.isPresent()) return BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(fluid.get());
		if (tag.isPresent()) return stack.getFluid().is(tag.get());
		return false;
	}

}
