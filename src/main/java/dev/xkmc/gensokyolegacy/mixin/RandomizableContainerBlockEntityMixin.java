package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.event.MobEffectEventHandlers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public class RandomizableContainerBlockEntityMixin {

	@Inject(at = @At("HEAD"), method = "unpackLootTable")
	public void gensokyolegacy$trayLoad(Player player, CallbackInfo ci) {
		if (player != null) {
			MobEffectEventHandlers.removeKoishi(player);
		}
	}

}
