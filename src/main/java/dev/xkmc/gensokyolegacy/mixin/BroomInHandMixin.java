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

/** Drops the broom from the rider's hands: the one they sit on is drawn under them by {@code BroomRenderer},
 *  and the ride ends anyway if no hand keeps a broom ({@code BroomEntity.tick}). <p>{@code renderItem} is the
 *  one point both perspectives reach: the arm stays in third person, the whole hand draw goes in first. */
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