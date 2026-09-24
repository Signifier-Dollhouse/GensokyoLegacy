package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.content.client.ClientGlowManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Single entry point for all client-side entity glow effects: delegates to
 * {@link ClientGlowManager}. Client-side only — the server never sees this.
 */
@Mixin(Entity.class)
public abstract class ClientGlowMixin {

	@Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$clientGlow(CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (self.level().isClientSide() && ClientGlowManager.shouldGlow(self)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$clientGlowColor(CallbackInfoReturnable<Integer> cir) {
		Entity self = (Entity) (Object) this;
		if (!self.level().isClientSide()) return;
		Integer color = ClientGlowManager.glowColor(self);
		if (color != null) cir.setReturnValue(color);
	}

}
