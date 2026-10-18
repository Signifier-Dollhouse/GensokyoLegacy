package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws an item worn on the head the way vanilla draws a non-armor helmet-slot item: snap to the
 * head bone, apply {@link CustomHeadLayer#translateToHead}, then render with the HEAD display
 * context so the item's own head perspective applies. Anything drawn this way must therefore get
 * its geometry from an item model rather than a {@code ModelPart} — see
 * {@link TouhouHatItem#isModeledHat()}.
 */
public class HeadItemRenderer {

	/**
	 * @param parent the wearer's model, which supplies the head bone to snap to
	 */
	public static void render(ItemStack stack, LivingEntity entity, EntityModel<?> parent,
	                          PoseStack pose, MultiBufferSource buffer, int light) {
		if (entity.isInvisible() && !Minecraft.getInstance().shouldEntityAppearGlowing(entity)) return;
		if (!(parent instanceof HeadedModel headed)) return;
		pose.pushPose();
		headed.getHead().translateAndRotate(pose);
		boolean flag = entity instanceof Villager || entity instanceof ZombieVillager;
		CustomHeadLayer.translateToHead(pose, flag);
		Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
				.renderItem(entity, stack, ItemDisplayContext.HEAD, false, pose, buffer, light);
		pose.popPose();
	}

}