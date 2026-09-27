package dev.xkmc.gensokyolegacy.content.ui.util;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Copies of {@link net.minecraft.client.gui.screens.inventory.InventoryScreen}'s entity preview helpers,
 * without the {@code enableScissor} / {@code disableScissor} calls.
 */
public class EntityPreviewRenderer {

	public static void renderEntityInInventoryFollowsAngle(GuiGraphics g, int x1, int y1, int x2, int y2,
														   int scale, float yOffset, float angleXComponent, float angleYComponent, LivingEntity entity) {
		float cx = (float) (x1 + x2) / 2.0F;
		float cy = (float) (y1 + y2) / 2.0F;
		float ax = angleXComponent;
		float ay = angleYComponent;
		Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf tilt = new Quaternionf().rotateX(ay * 20.0F * (float) (Math.PI / 180.0));
		rot.mul(tilt);
		float bodyRot = entity.yBodyRot;
		float yRot = entity.getYRot();
		float xRot = entity.getXRot();
		float headRotO = entity.yHeadRotO;
		float headRot = entity.yHeadRot;
		entity.yBodyRot = 180.0F + ax * 20.0F;
		entity.setYRot(180.0F + ax * 40.0F);
		entity.setXRot(-ay * 20.0F);
		entity.yHeadRot = entity.getYRot();
		entity.yHeadRotO = entity.getYRot();
		float entityScale = entity.getScale();
		Vector3f translate = new Vector3f(0.0F, entity.getBbHeight() / 2.0F + yOffset * entityScale, 0.0F);
		float size = (float) scale / entityScale;
		renderEntityInInventory(g, cx, cy, size, translate, rot, tilt, entity);
		entity.yBodyRot = bodyRot;
		entity.setYRot(yRot);
		entity.setXRot(xRot);
		entity.yHeadRotO = headRotO;
		entity.yHeadRot = headRot;
	}

	public static void renderEntityInInventory(GuiGraphics g, float x, float y, float scale, Vector3f translate,
											   Quaternionf pose, @Nullable Quaternionf cameraOrientation, LivingEntity entity) {
		g.pose().pushPose();
		g.pose().translate((double) x, (double) y, 50.0);
		g.pose().scale(scale, scale, -scale);
		g.pose().translate(translate.x, translate.y, translate.z);
		g.pose().mulPose(pose);
		Lighting.setupForEntityInInventory();
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		if (cameraOrientation != null)
			dispatcher.overrideCameraOrientation(cameraOrientation.conjugate(new Quaternionf()).rotateY((float) Math.PI));
		dispatcher.setRenderShadow(false);
		RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, g.pose(), g.bufferSource(), 15728880));
		g.flush();
		dispatcher.setRenderShadow(true);
		g.pose().popPose();
		Lighting.setupFor3DItems();
	}

}
