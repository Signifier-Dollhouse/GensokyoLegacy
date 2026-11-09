package dev.xkmc.gensokyolegacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.entity.broom.BroomEntity;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every held-item draw change sits on one class, since all three need the same two callbacks.
 * {@code renderItem} is the one point both perspectives reach: the arm stays in third person, the
 * whole hand draw goes in first.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

	@Shadow
	private void renderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch,
								   InteractionHand hand, float swingProgress, ItemStack stack,
								   float equippedProgress, PoseStack poseStack,
								   MultiBufferSource buffer, int combinedLight) {
		throw new AssertionError();
	}

	/**
	 * Nazrin's Dowser in the main hand with an empty offhand draws as its two halves, one per hand.
	 * Purely visual - no slot is touched.
	 */
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
				new ItemStack(GLItems.DOWSER_LEFT.get()), equippedProgress, poseStack, buffer, combinedLight);
		this.renderArmWithItem(player, partialTicks, pitch, InteractionHand.OFF_HAND, 0,
				new ItemStack(GLItems.DOWSER_RIGHT.get()), equippedProgress, poseStack, buffer, combinedLight);
	}

	/**
	 * Drops the broom from the rider's hands: the one they sit on is drawn under them by
	 * {@code BroomRenderer}, and the ride ends anyway if no hand keeps a broom ({@code BroomEntity.tick}).
	 */
	@Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$hideRiddenBroom(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
												boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
												int combinedLight, CallbackInfo ci) {
		if (!stack.is(GLItems.BROOM.get())) return;
		if (!(entity.getVehicle() instanceof BroomEntity)) return;
		ci.cancel();
	}

	/**
	 * The anchor is in the world, not in the hand: while its stack carries the hook's UUID, skip the
	 * held-item draw. The client trusts the UUID alone; the server clears it when the hook is gone.
	 *
	 * @see "doc/design/minamitsu_anchor.md §3.3"
	 */
	@Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$hideThrownAnchor(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
												 boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
												 int combinedLight, CallbackInfo ci) {
		if (!(stack.getItem() instanceof AnchorItem)) return;
		if (!GLItems.ANCHOR_LINK.has(stack)) return;
		ci.cancel();
	}

}
