package dev.xkmc.gensokyolegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.xkmc.gensokyolegacy.content.entity.dolls.client.HeadPossession;
import dev.xkmc.gensokyolegacy.util.LavaEffectsHelper;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

	@WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canSwimInFluidType(Lnet/neoforged/neoforge/fluids/FluidType;)Z"))
	public boolean gensokyolegacy$canSwim$lavaSwim(LocalPlayer instance, FluidType fluidType, Operation<Boolean> original) {
		return original.call(instance, fluidType) || fluidType == NeoForgeMod.LAVA_TYPE.value() && LavaEffectsHelper.canLavaSwim(instance);
	}

	/**
	 * A body left behind keeps saying where it is. Vanilla stops the movement packets once the
	 * camera is not the player, and the server then holds its copy of the body still while the
	 * client's own copy falls and is knocked about, so the two drift apart into two places.
	 */
	@WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z"))
	public boolean gensokyolegacy$possessed$bodyStillReports(LocalPlayer instance, Operation<Boolean> original) {
		return original.call(instance) || HeadPossession.isPossessedBody(instance);
	}

}
