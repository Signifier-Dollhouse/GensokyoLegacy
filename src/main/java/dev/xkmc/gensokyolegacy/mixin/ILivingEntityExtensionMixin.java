package dev.xkmc.gensokyolegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.xkmc.gensokyolegacy.util.LavaEffectsHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.extensions.ILivingEntityExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ILivingEntityExtension.class)
public interface ILivingEntityExtensionMixin {

	@WrapOperation(method = "jumpInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
	default Vec3 gensokyolegacy$jumpInFluid$lavaSwim(Vec3 vec, double x, double y, double z, Operation<Vec3> original, @Local(argsOnly = true) FluidType fluid) {
		if (this instanceof LivingEntity le && fluid == NeoForgeMod.LAVA_TYPE.value()) {
			y = LavaEffectsHelper.lavaSwim(le, y);
		}
		return original.call(vec, x, y, z);
	}

	@WrapOperation(method = "sinkInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
	default Vec3 gensokyolegacy$sinkInFluid$lavaSwim(Vec3 vec, double x, double y, double z, Operation<Vec3> original, @Local(argsOnly = true) FluidType fluid) {
		if (this instanceof LivingEntity le && fluid == NeoForgeMod.LAVA_TYPE.value()) {
			y = LavaEffectsHelper.lavaSwim(le, y);
		}
		return original.call(vec, x, y, z);
	}

}
