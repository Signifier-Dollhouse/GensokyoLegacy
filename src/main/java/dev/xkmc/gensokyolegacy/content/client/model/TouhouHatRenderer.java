package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Renders a touhou hat in the curios head slot, reusing the armor model from
 * the item's client extension. The curios layer only calls this when the
 * slot's render toggle is enabled.
 */
public class TouhouHatRenderer implements ICurioRenderer {

	private final ResourceLocation texture;

	private HumanoidModel<?> model;

	public TouhouHatRenderer(ResourceLocation texture) {
		this.texture = texture;
	}

	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
																		 PoseStack pose, RenderLayerParent<T, M> parent,
																		 MultiBufferSource buffer, int light,
																		 float swing, float swingAmp, float partial,
																		 float age, float yaw, float pitch) {
		LivingEntity entity = slotContext.entity();
		if (model == null) {
			HumanoidModel<?> base = parent.getModel() instanceof HumanoidModel<?> hm ? hm : null;
			model = IClientItemExtensions.of(stack).getHumanoidArmorModel(entity, stack, EquipmentSlot.HEAD, base);
		}
		ICurioRenderer.followHeadRotations(entity, model.head);
		VertexConsumer vc = buffer.getBuffer(RenderType.armorCutoutNoCull(texture));
		model.renderToBuffer(pose, vc, light, LivingEntityRenderer.getOverlayCoords(entity, 0), -1);
	}

}
