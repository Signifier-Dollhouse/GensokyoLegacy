package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.content.entity.broom.BroomEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

	/**
	 * Sneak is the vanilla dismount, but the broom reads it as "descend"
	 * (see {@link BroomEntity}). Getting off is done by right-clicking instead,
	 * either the item or the broom itself.
	 */
	@Inject(at = @At("HEAD"), method = "wantsToStopRiding", cancellable = true)
	private void gensokyolegacy$noSneakDismountOnBroom(CallbackInfoReturnable<Boolean> cir) {
		if (((Player) (Object) this).getVehicle() instanceof BroomEntity) cir.setReturnValue(false);
	}

}