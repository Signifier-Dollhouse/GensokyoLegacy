package dev.xkmc.gensokyolegacy.content.entity.youkai;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.fastprojectileapi.FastProjectileAPI;
import dev.xkmc.fastprojectileapi.spellcircle.SpellCircleConfig;
import dev.xkmc.fastprojectileapi.spellcircle.SpellComponent;
import dev.xkmc.fastprojectileapi.spellcircle.SpellRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * A youkai's spell circle. Vanilla's {@code SpellCircleLayer} is a plain
 * {@code RenderLayer}, which a {@code GeoEntityRenderer} cannot host, so this is the
 * same draw against {@link GeoRenderLayer}'s hooks — otherwise a character would lose
 * her circle the moment she became geo-rendered.
 */
public class YoukaiSpellCircleLayer<T extends GeneralYoukaiEntity & GeoYoukaiAnim> extends GeoRenderLayer<T> {

	private static final ResourceLocation SPELL = FastProjectileAPI.loc("textures/entities/spell_circle.png");

	public YoukaiSpellCircleLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, @Nullable RenderType renderType,
					   MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
					   int packedLight, int packedOverlay) {
		ResourceLocation circle = entity.getSpellCircle();
		if (circle == null) return;
		SpellComponent component = SpellCircleConfig.getFromConfig(circle);
		if (component == null) return;
		var handle = new SpellComponent.RenderHandle(poseStack,
				bufferSource.getBuffer(SpellRenderState.getSpell(SPELL)),
				entity.tickCount + partialTick, packedLight);
		poseStack.pushPose();
		poseStack.translate(0, entity.getBbHeight() / 2, entity.getBbWidth());
		float scale = entity.getCircleSize(partialTick);
		poseStack.scale(scale / 16f, scale / 16f, scale / 16f);
		component.render(() -> handle);
		poseStack.popPose();
	}

}