package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollGeo;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollOneShotController;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DollRenderer extends GeoEntityRenderer<DollEntity> {
	public DollRenderer(EntityRendererProvider.Context context) {
		super(context, new DollModel());
		addRenderLayer(new DollHeldItemLayer(this));
	}

	boolean consumeSpearParticle(DollEntity doll) {
		var controller = doll.getAnimatableInstanceCache().<DollEntity>getManagerForId(getInstanceId(doll))
				.getAnimationControllers().get(DollGeo.ONE_SHOT_CONTROLLER);
		return controller instanceof DollOneShotController oneShot && oneShot.consumeSpearParticle();
	}

	Vec3 itemPointToWorld(DollEntity doll, PoseStack poseStack, Vector3f point, float partialTick) {
		Vector3f offset = new Matrix4f(entityRenderTranslations).invert().mul(poseStack.last().pose())
				.transformPosition(point);
		return doll.getPosition(partialTick).add(offset.x, offset.y, offset.z);
	}
}
