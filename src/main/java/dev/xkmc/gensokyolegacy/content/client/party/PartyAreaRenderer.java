package dev.xkmc.gensokyolegacy.content.client.party;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureOutlineRenderer;
import dev.xkmc.gensokyolegacy.content.attachment.party.item.PartyWand;
import dev.xkmc.gensokyolegacy.content.attachment.party.item.PartyWandComponent;
import dev.xkmc.gensokyolegacy.content.attachment.party.item.PartyWandData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws the party the wand is holding: the tables, seats and dishes of the last scan,
 * inside a box over the extent of all of them.
 * <p>
 * The wand's readout is otherwise a line of text, which cannot say <em>where</em> the
 * food it counted is. A spread across two rooms, or a chair the walk never reached, is
 * only visible in the world.
 * <p>
 * There is no state here at all: the scan is read off the held wand's data component
 * every frame. Vanilla keeps that component in sync, so the picture cannot fall out of
 * step with the counts in the overlay, and there is no second copy to expire, clear or
 * push. Staleness is the same staleness the overlay already reports — the food is being
 * eaten while the wand is held.
 * <p>
 * Only drawn while the wand is in hand, which is the point: the render is a view of one
 * scan and the scan is the wand's, so it comes and goes with the wand rather than
 * following the player around all level.
 */
public class PartyAreaRenderer {

	/** Green for a dish, blue for a seat, gold for a table: three colours, three meanings. */
	private static final float[] FOOD_COLOUR = {0.3f, 1f, 0.3f, 0.9f};
	private static final float[] SEAT_COLOUR = {0.4f, 0.8f, 1f, 0.9f};
	private static final float[] TABLE_COLOUR = {1f, 0.8f, 0.2f, 0.9f};

	/** The scan on the wand in hand, or null when there is none to draw. */
	private static PartyWandData held() {
		var player = Minecraft.getInstance().player;
		if (player == null) return null;
		for (ItemStack stack : player.getHandSlots()) {
			if (!(stack.getItem() instanceof PartyWand)) continue;
			PartyWandData data = stack.get(PartyWandComponent.TYPE);
			if (data != null && !data.drawn().isEmpty()) return data;
		}
		return null;
	}

	/**
	 * Called on every frame. Walks away immediately unless a wand is held, so there is
	 * nothing to keep in step and nothing to clean up on a level change.
	 */
	public static void renderOutline(PoseStack pose, Vec3 camera) {
		PartyWandData data = held();
		if (data == null) return;
		List<BlockPos> bound = data.drawn();
		StructureOutlineRenderer.renderPosSet(pose, camera, data.tables(), TABLE_COLOUR);
		StructureOutlineRenderer.renderPosSet(pose, camera, data.seats(), SEAT_COLOUR);
		StructureOutlineRenderer.renderPosSet(pose, camera, data.foods(), FOOD_COLOUR);
		StructureOutlineRenderer.renderBound(pose, camera, bound);
	}

}