package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectEntry;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.block.functional.barriers.SealingEffectData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelSealingMixin {

	@Inject(method = "isNaturalSpawningAllowed(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"), cancellable = true)
	private void gensokyolegacy$sealHostileSpawnsAtPos(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;
		for (AreaEffectEntry e : AreaEffectManager.getAffecting((ServerLevel) (Object) this, pos)) {
			if (e.data instanceof SealingEffectData) {
				cir.setReturnValue(false);
				return;
			}
		}
	}

}