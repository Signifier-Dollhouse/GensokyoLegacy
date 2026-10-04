package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

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
 * Cirno's spell circle. Vanilla's {@code SpellCircleLayer} is a plain
 * {@code RenderLayer}, which a {@code GeoEntityRenderer} cannot host, so this is the
 * same draw against {@link GeoRenderLayer}'s hooks — otherwise she would lose the
 * circle the moment she became geo-rendered.
 */
public class CirnoSpellCircleLayer extends GeoRenderLayer<CirnoEntity> {

	private static final ResourceLocation SPELL = FastProjectileAPI.loc("textures/entities/spell_circle.png");

	public CirnoSpellCircleLayer(GeoRenderer<CirnoEntity> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack poseStack, CirnoEntity cirno, BakedGeoModel bakedModel, @Nullable RenderType renderType,
					   MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
					   int packedLight, int packedOverlay) {
		ResourceLocation circle = cirno.getSpellCircle();
		if (circle == null) return;
		SpellComponent component = SpellCircleConfig.getFromConfig(circle);
		if (component == null) return;
		var handle = new SpellComponent.RenderHandle(poseStack,
				bufferSource.getBuffer(SpellRenderState.getSpell(SPELL)),
				cirno.tickCount + partialTick, packedLight);
		poseStack.pushPose();
		poseStack.translate(0, cirno.getBbHeight() / 2, cirno.getBbWidth());
		float scale = cirno.getCircleSize(partialTick);
		poseStack.scale(scale / 16f, scale / 16f, scale / 16f);
		component.render(() -> handle);
		poseStack.popPose();
	}

}
