package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Third-person counterpart of {@link DowserHandRendererMixin}:
 * when the main hand holds Nazrin's Dowser and the offhand is empty,
 * the layer sees the left half as the main-hand item and the right half
 * as the offhand item, so both halves render in the correct hands,
 * respecting the entity's handedness (main arm vs opposite arm).
 */
@Mixin(ItemInHandLayer.class)
public abstract class DowserItemInHandLayerMixin {

	@Unique
	private static final ItemStack gensokyolegacy$DOWSER_LEFT_VIEW = new ItemStack(GLItems.DOWSER_LEFT.get());

	@Unique
	private static final ItemStack gensokyolegacy$DOWSER_RIGHT_VIEW = new ItemStack(GLItems.DOWSER_RIGHT.get());

	@Unique
	private static boolean gensokyolegacy$isSplitDowser(LivingEntity entity) {
		return entity.getMainHandItem().is(GLItems.DOWSER.get()) && entity.getOffhandItem().isEmpty();
	}

	@Redirect(method = "render*", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack gensokyolegacy$dowserMainHand(LivingEntity entity) {
		if (gensokyolegacy$isSplitDowser(entity)) return gensokyolegacy$DOWSER_LEFT_VIEW;
		return entity.getMainHandItem();
	}

	@Redirect(method = "render*", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack gensokyolegacy$dowserOffHand(LivingEntity entity) {
		if (gensokyolegacy$isSplitDowser(entity)) return gensokyolegacy$DOWSER_RIGHT_VIEW;
		return entity.getOffhandItem();
	}
}
