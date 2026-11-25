package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.event.TreasureBasketEventHandlers;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 has no post-entity-tick event, so the basket ageing runs at the tail of {@link ItemEntity#tick()}.
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void gensokyolegacy$basketAge(CallbackInfo ci) {
		TreasureBasketEventHandlers.onItemTick((ItemEntity) (Object) this);
	}

}
