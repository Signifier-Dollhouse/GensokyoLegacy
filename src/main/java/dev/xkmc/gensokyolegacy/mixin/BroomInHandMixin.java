package dev.xkmc.gensokyolegacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.entity.broom.BroomEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the broom out of the rider's hands: the one they are sitting on is
 * already drawn under them by {@code BroomRenderer}, so the stack shows a
 * second one. Holding it is not a choice the rider can make anyway — the
 * ride only lasts as long as some hand keeps a broom ({@code BroomEntity.tick}
 * discards the entity otherwise) — so the duplicate cannot be avoided by
 * putting the item away.
 *
 * <p>{@code renderItem} is the one point both perspectives pass through, and
 * both hands reach it on the way: first person via {@code GameRenderer}'s
 * {@code renderItemInHand} → {@code renderHandsWithItems} →
 * {@code renderArmWithItem}, once per hand; third person via
 * {@code ItemInHandLayer}, which a player gets from {@code PlayerRenderer} and
 * which {@code LivingEntityRenderer} walks for every player, local or remote.
 * Cancelling here therefore covers either hand in either perspective at once.
 *
 * <p>What it takes is the item. In third person the arm belongs to the player
 * model and is drawn independently of this layer, so it stays and only the
 * broom goes. First person has no separate arm pass for a held item —
 * {@code renderArmWithItem} reaches for {@code renderPlayerArm} only when the
 * stack is empty — so what gets dropped there is the whole hand-held draw.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class BroomInHandMixin {

	@Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$hideRiddenBroom(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
	                                           boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
	                                           int combinedLight, CallbackInfo ci) {
		if (!stack.is(GLItems.BROOM.get())) return;
		if (!(entity.getVehicle() instanceof BroomEntity)) return;
		ci.cancel();
	}

}