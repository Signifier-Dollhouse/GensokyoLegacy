package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.util.FluidRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws the tank contents: the fluid box (stage colour over fluid tint) and the items still
 * floating on it.
 *
 * @see doc/design/alchemy_pot.md
 */
public class AlchemyPotRenderer implements BlockEntityRenderer<AlchemyPotBlockEntity> {

	public AlchemyPotRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	@Override
	public void render(AlchemyPotBlockEntity be, float pTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		FluidStack fluid = be.getFluid();
		if (fluid.isEmpty() && be.stage.current.color() == -1 && be.items.isEmpty()) return;
		int color = -1;
		var stageColor = be.stage.current.color();
		if (stageColor != -1) {
			color = stageColor;
		} else if (!fluid.isEmpty()) {
			var ext = IClientFluidTypeExtensions.of(fluid.getFluid());
			color = ext.getTintColor(fluid);
		}
		// client-only: the tank keeps its old amount until finishRecipe snaps it
		float amount = fluid.getAmount();
		if (be.isReacting() && be.getRecipeHolder() != null) {
			int target = be.getRecipeHolder().value().resultFluid.isEmpty() ? 0 : be.getRecipeHolder().value().resultFluid.getAmount();
			float prog = be.inProgress();
			float visualProg = Mth.clamp((be.recipeProgress + pTick) / (float) Math.max(1, be.totalTime), 0, 1);
			amount = Mth.lerp(visualProg, amount, (float) target);
		}
		if (amount <= 0) return;
		// cavity runs from y 3 to y 11
		float h = Mth.clamp(amount / (float) AlchemyPotBlockEntity.FLUID_CAPACITY, 0, 1);
		float yMin = 3 / 16f;
		float yMax = yMin + h * (8 / 16f); // 8 high cavity
		if (yMax <= yMin) return;
		// a stage colour with an empty tank still needs a fluid to tint: water
		FluidStack renderFluid = fluid.isEmpty() ? new FluidStack(Fluids.WATER, 1000) : fluid;
		FluidStack vis = renderFluid.copy();
		int col = color;
		FluidRenderer.renderFluidBox(vis, 2 / 16f, yMin, 2 / 16f, 14 / 16f, yMax, 14 / 16f, buffer, ms, light, false, col == -1 ? 0 : col);

		if (!be.stage.floating.isEmpty()) {
			var level = be.getLevel();
			if (level == null) return;
			float time = level.getGameTime() + pTick;
			int idx = 0;
			for (var entry : be.stage.floating) {
				if (entry.stack().isEmpty()) {
					idx++;
					continue;
				}
				float offset = Mth.sin(time * 0.05f + idx * 0.7f) * 0.02f;
				ms.pushPose();
				double angle = idx * (Math.PI * 2 / Math.max(1, be.stage.floating.size()));
				double r = 0.18;
				ms.translate(0.5 + Math.cos(angle) * r, yMax + 0.05 + offset, 0.5 + Math.sin(angle) * r);
				ms.scale(0.25f, 0.25f, 0.25f);
				Minecraft.getInstance().getItemRenderer().renderStatic(entry.stack(), ItemDisplayContext.FIXED, light, overlay, ms, buffer, level, 0);
				ms.popPose();
				idx++;
			}
		}
	}
}
