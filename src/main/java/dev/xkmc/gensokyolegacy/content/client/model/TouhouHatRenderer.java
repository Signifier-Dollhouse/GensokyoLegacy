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
 * Renders a touhou hat, shared by the vanilla head-slot layer
 * ({@link TouhouHatLayer}) and the curios head-slot renderer.
 *
 * Vanilla {@code HumanoidArmorLayer} only renders {@code ArmorItem}, so plain
 * {@code TouhouHatItem}s in the head armor slot need a custom layer. Both
 * paths resolve the model from the item's client extension and the texture
 * from the item's armor texture, so a hat renders identically in either slot.
 */
public class TouhouHatRenderer implements ICurioRenderer {

	public TouhouHatRenderer() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
	                                                                      PoseStack pose, RenderLayerParent<T, M> parent,
	                                                                      MultiBufferSource buffer, int light,
	                                                                      float swing, float swingAmp, float partial,
	                                                                      float age, float yaw, float pitch) {
		if (!(parent.getModel() instanceof HumanoidModel hm)) return;
		renderHat(stack, slotContext.entity(), hm, pose, buffer, light, true);
	}

	/**
	 * Render one hat stack on an entity. The parent model must already have
	 * {@code setupAnim} applied; its pose is copied to the hat model.
	 *
	 * @param followHead whether to apply curios head-following rotations
	 *                   (curios path only; the armor layer path already inherits head pose)
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void renderHat(ItemStack stack, LivingEntity entity, HumanoidModel<?> parent,
	                             PoseStack pose, MultiBufferSource buffer, int light, boolean followHead) {
		HumanoidModel<?> model = IClientItemExtensions.of(stack)
				.getHumanoidArmorModel(entity, stack, EquipmentSlot.HEAD, parent);
		if (model == null) return;
		copyProperties(parent, model);
		if (followHead) {
			ICurioRenderer.followHeadRotations(entity, model.head);
		}
		ResourceLocation texture = stack.getItem().getArmorTexture(stack, entity, EquipmentSlot.HEAD, null, false);
		if (texture == null) return;
		VertexConsumer vc = buffer.getBuffer(RenderType.armorCutoutNoCull(texture));
		model.renderToBuffer(pose, vc, light, LivingEntityRenderer.getOverlayCoords(entity, 0), -1);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void copyProperties(HumanoidModel<?> from, HumanoidModel<?> to) {
		((HumanoidModel) from).copyPropertiesTo((HumanoidModel) to);
	}

}
