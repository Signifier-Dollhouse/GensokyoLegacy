package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Renders strange glasses from the curios head slot exactly as vanilla renders
 * non-armor items worn in the helmet slot ({@link CustomHeadLayer}): snap to
 * the head bone, apply the vanilla head offset, then render the item with the
 * HEAD display context so the {@code strange_glasses_head} sub-model applies.
 */
public class StrangeGlassesRenderer implements ICurioRenderer {

	public StrangeGlassesRenderer() {
	}

	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
	                                                                      PoseStack pose, RenderLayerParent<T, M> parent,
	                                                                      MultiBufferSource buffer, int light,
	                                                                      float limbSwing, float limbSwingAmount, float partialTicks,
	                                                                      float ageInTicks, float netHeadYaw, float headPitch) {
		LivingEntity entity = slotContext.entity();
		if (entity.isInvisible() && !Minecraft.getInstance().shouldEntityAppearGlowing(entity)) return;
		if (!(parent.getModel() instanceof HeadedModel headed)) return;
		pose.pushPose();
		headed.getHead().translateAndRotate(pose);
		boolean flag = entity instanceof Villager || entity instanceof ZombieVillager;
		CustomHeadLayer.translateToHead(pose, flag);
		Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
				entity, stack, ItemDisplayContext.HEAD, false, pose, buffer, light);
		pose.popPose();
	}

}
