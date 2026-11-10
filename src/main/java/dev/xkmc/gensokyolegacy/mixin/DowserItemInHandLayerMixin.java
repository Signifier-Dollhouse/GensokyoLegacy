package dev.xkmc.gensokyolegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Third-person counterpart of {@link ItemInHandRendererMixin}: with the dowser in the main hand and an empty
 * offhand, the layer sees the left half as the main-hand item and the right half as the offhand item, so both
 * halves land in the correct hands, respecting the entity's handedness.
 */
@Mixin(ItemInHandLayer.class)
public abstract class DowserItemInHandLayerMixin {

	@Unique
	private static boolean gensokyolegacy$isSplitDowser(LivingEntity entity) {
		return entity.getMainHandItem().is(GLItems.DOWSER.get()) && entity.getOffhandItem().isEmpty();
	}

	@WrapOperation(method = "render*", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack gensokyolegacy$dowserMainHand(LivingEntity entity, Operation<ItemStack> original) {
		if (gensokyolegacy$isSplitDowser(entity)) return GLItems.DOWSER_LEFT.asStack();
		return original.call(entity);
	}

	@WrapOperation(method = "render*", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack gensokyolegacy$dowserOffHand(LivingEntity entity, Operation<ItemStack> original) {
		if (gensokyolegacy$isSplitDowser(entity)) return GLItems.DOWSER_RIGHT.asStack();
		return original.call(entity);
	}

}
