package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Renders strange glasses from the curios head slot exactly as vanilla renders non-armor helmet-slot items
 * ({@link HeadItemRenderer}), so the {@code strange_glasses_head} perspective applies. The vanilla head
 * slot needs no renderer of its own: {@code CustomHeadLayer} already draws them.
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
		HeadItemRenderer.render(stack, slotContext.entity(), parent.getModel(), pose, buffer, light);
	}

}