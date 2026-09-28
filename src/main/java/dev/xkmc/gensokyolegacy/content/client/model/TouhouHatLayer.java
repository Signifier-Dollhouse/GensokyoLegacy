package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Renders plain (non-armor) {@link TouhouHatItem}s worn in the vanilla head
 * armor slot. Vanilla {@code HumanoidArmorLayer} skips non-{@code ArmorItem}s,
 * so hats would otherwise be invisible there. Delegates to
 * {@link TouhouHatRenderer} so the head slot and the curios slot share one
 * code path.
 */
public class TouhouHatLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

	public TouhouHatLayer(RenderLayerParent<T, M> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffer, int light, T entity,
	                   float swing, float swingAmp, float partial, float age, float yaw, float pitch) {
		ItemStack stack = entity.getItemBySlot(EquipmentSlot.HEAD);
		if (!(stack.getItem() instanceof TouhouHatItem)) return;
		Minecraft minecraft = Minecraft.getInstance();
		boolean glowing = minecraft.shouldEntityAppearGlowing(entity);
		if (entity.isInvisible() && !glowing) return;
		TouhouHatRenderer.renderHat(stack, entity, getParentModel(), pose, buffer, light, false);
	}

}
