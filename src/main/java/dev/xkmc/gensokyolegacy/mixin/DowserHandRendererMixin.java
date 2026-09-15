package dev.xkmc.gensokyolegacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the player holds Nazrin's Dowser in the main hand and the offhand is empty,
 * render the two halves instead: left half in the main hand, right half in the offhand.
 * Purely visual - no inventory or offhand slot is touched.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class DowserHandRendererMixin {

	@Unique
	private static final ItemStack gensokyolegacy$DOWSER_LEFT_VIEW = new ItemStack(GLItems.DOWSER_LEFT.get());

	@Unique
	private static final ItemStack gensokyolegacy$DOWSER_RIGHT_VIEW = new ItemStack(GLItems.DOWSER_RIGHT.get());

	@Shadow
	private void renderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch,
	                               InteractionHand hand, float swingProgress, ItemStack stack,
	                               float equippedProgress, PoseStack poseStack,
	                               MultiBufferSource buffer, int combinedLight) {
		throw new AssertionError();
	}

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$dowserSplitHands(AbstractClientPlayer player, float partialTicks, float pitch,
	                                             InteractionHand hand, float swingProgress, ItemStack stack,
	                                             float equippedProgress, PoseStack poseStack,
	                                             MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
		if (hand != InteractionHand.MAIN_HAND) return;
		if (!stack.is(GLItems.DOWSER.get())) return;
		if (!player.getOffhandItem().isEmpty()) return;
		ci.cancel();
		this.renderArmWithItem(player, partialTicks, pitch, InteractionHand.MAIN_HAND, swingProgress,
				gensokyolegacy$DOWSER_LEFT_VIEW, equippedProgress, poseStack, buffer, combinedLight);
		this.renderArmWithItem(player, partialTicks, pitch, InteractionHand.OFF_HAND, 0,
				gensokyolegacy$DOWSER_RIGHT_VIEW, equippedProgress, poseStack, buffer, combinedLight);
	}

}
