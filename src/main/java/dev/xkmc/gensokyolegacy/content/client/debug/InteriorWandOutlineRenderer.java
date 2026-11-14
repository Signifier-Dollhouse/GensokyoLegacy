package dev.xkmc.gensokyolegacy.content.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureOutlineRenderer;
import dev.xkmc.gensokyolegacy.content.item.debug.InteriorWandData;
import dev.xkmc.gensokyolegacy.content.item.debug.InteriorWandItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws the interior wand's recorded bounds and waypoints while it is held: selected bounds green,
 * the rest red, waypoints cyan.
 *
 * @see "doc/design/structure/interior_wand.md"
 */
public final class InteriorWandOutlineRenderer {

	private static final float ENLARGE = 0.05F;

	private InteriorWandOutlineRenderer() {
	}

	private static InteriorWandData held() {
		var player = Minecraft.getInstance().player;
		if (player == null) return null;
		for (ItemStack stack : player.getHandSlots()) {
			if (!(stack.getItem() instanceof InteriorWandItem)) continue;
			InteriorWandData data = GLItems.INTERIOR_WAND_DATA.get(stack);
			if (data != null) return data;
		}
		return null;
	}

	public static void render(PoseStack pose, Vec3 camera) {
		InteriorWandData data = held();
		if (data == null) return;
		var line = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());
		var vec = camera.toVector3f();
		float dx = -vec.x, dy = -vec.y, dz = -vec.z;
		Set<Integer> selected = new HashSet<>();
		if (data.selectedRoom() >= 0) selected.add(data.selectedRoom());
		if (data.nodeRoomA() >= 0) selected.add(data.nodeRoomA());
		if (data.nodeRoomB() >= 0) selected.add(data.nodeRoomB());
		for (int i = 0; i < data.rooms().size(); i++) {
			var b = data.rooms().get(i);
			boolean green = selected.contains(i);
			StructureOutlineRenderer.renderCube(pose, line,
					b.minX() - ENLARGE, b.minY() - ENLARGE, b.minZ() - ENLARGE,
					b.maxX() + 1 + ENLARGE, b.maxY() + 1 + ENLARGE, b.maxZ() + 1 + ENLARGE,
					dx, dy, dz, green ? 0.2F : 1, green ? 1 : 0.2F, 0.2F, 1);
		}
		for (var node : data.nodes()) {
			node(pose, line, node.posA(), dx, dy, dz);
			if (!node.posB().equals(node.posA())) node(pose, line, node.posB(), dx, dy, dz);
		}
		if (data.pendingStart() != null) pending(pose, line, data.pendingStart(), dx, dy, dz);
		if (data.pendingNodeA() != null) pending(pose, line, data.pendingNodeA(), dx, dy, dz);
	}

	private static void node(PoseStack pose, VertexConsumer line, BlockPos pos,
	                         float dx, float dy, float dz) {
		StructureOutlineRenderer.renderCube(pose, line,
				pos.getX() + 0.25F, pos.getY() + 0.25F, pos.getZ() + 0.25F,
				pos.getX() + 0.75F, pos.getY() + 0.75F, pos.getZ() + 0.75F,
				dx, dy, dz, 0.2F, 0.8F, 1, 1);
	}

	private static void pending(PoseStack pose, VertexConsumer line, BlockPos pos,
	                            float dx, float dy, float dz) {
		StructureOutlineRenderer.renderCube(pose, line,
				pos.getX() - ENLARGE, pos.getY() - ENLARGE, pos.getZ() - ENLARGE,
				pos.getX() + 1 + ENLARGE, pos.getY() + 1 + ENLARGE, pos.getZ() + 1 + ENLARGE,
				dx, dy, dz, 1, 1, 0.2F, 1);
	}

}
