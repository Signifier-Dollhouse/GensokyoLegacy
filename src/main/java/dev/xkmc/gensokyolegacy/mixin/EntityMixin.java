package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageRefactorEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

	@Inject(at = @At("HEAD"), method = "setRemoved")
	public void gensokyolegacy$setRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
		Object self = this;
		if (self instanceof DamageRefactorEntity e) {
			e.onRemove(reason);
		}
	}

}