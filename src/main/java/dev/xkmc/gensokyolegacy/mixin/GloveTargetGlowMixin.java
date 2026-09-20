package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.content.item.glove.client.GloveDollHover;
import dev.xkmc.gensokyolegacy.content.item.glove.client.GloveTargetCache;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Glove target highlight (glove.md §2–2b): the doll under the vanilla
 * crosshair glows gold via {@code GloveDollHover}, while the client-cached
 * ray-trace target glows in the held mode's color ({@code LevelRenderer} reads
 * {@code getTeamColor} for the outline). Client-side only — the server never
 * sees this and targeting authority stays server-side.
 */
@Mixin(Entity.class)
public abstract class GloveTargetGlowMixin {

	@Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$gloveTargetGlow(CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (self.level().isClientSide() &&
				(GloveDollHover.isHovered(self) || GloveTargetCache.isMarked(self))) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$gloveTargetColor(CallbackInfoReturnable<Integer> cir) {
		Entity self = (Entity) (Object) this;
		if (!self.level().isClientSide()) return;
		Integer doll = GloveDollHover.hoverColor(self);
		if (doll != null) {
			cir.setReturnValue(doll);
			return;
		}
		Integer color = GloveTargetCache.hoverColor(self);
		if (color != null) cir.setReturnValue(color);
	}

}
