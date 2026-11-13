package dev.xkmc.gensokyolegacy.content.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureOutlineRenderer;
import dev.xkmc.gensokyolegacy.content.item.debug.StructureMirrorData;
import dev.xkmc.gensokyolegacy.content.item.debug.StructureMirrorStick;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws every recorded piece bound in red while the stick is held.
 *
 * @see "doc/design/structure/mirror_stick.md"
 */
public final class StructureMirrorOutlineRenderer {

	/**
	 * A hair outside the true bound, so the red line stays clear of the structure block's own bound.
	 */
	private static final float ENLARGE = 0.05F;

	private StructureMirrorOutlineRenderer() {
	}

	private static StructureMirrorData held() {
		var player = Minecraft.getInstance().player;
		if (player == null) return null;
		for (ItemStack stack : player.getHandSlots()) {
			if (!(stack.getItem() instanceof StructureMirrorStick)) continue;
			StructureMirrorData data = GLItems.STRUCTURE_MIRROR.get(stack);
			if (data != null && !data.isEmpty()) return data;
		}
		return null;
	}

	public static void render(PoseStack pose, Vec3 camera) {
		StructureMirrorData data = held();
		if (data == null) return;
		var line = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());
		var vec = camera.toVector3f();
		for (var entry : data.entries()) {
			var b = entry.bound();
			StructureOutlineRenderer.renderCube(pose, line,
					b.minX() - ENLARGE, b.minY() - ENLARGE, b.minZ() - ENLARGE,
					b.maxX() + 1 + ENLARGE, b.maxY() + 1 + ENLARGE, b.maxZ() + 1 + ENLARGE,
					-vec.x, -vec.y, -vec.z, 1, 0.2F, 0.2F, 1);
		}
	}

}