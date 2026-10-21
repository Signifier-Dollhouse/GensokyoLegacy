package dev.xkmc.gensokyolegacy.content.client.party;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureOutlineRenderer;
import dev.xkmc.gensokyolegacy.content.attachment.party.item.PartyWand;
import dev.xkmc.gensokyolegacy.content.attachment.party.item.PartyWandData;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws the tables, seats and dishes of the scan held on the party wand. Stateless - it re-reads
 * the wand's component each frame, so the picture cannot fall out of step with the overlay counts.
 *
 * @see "doc/design/party/debug.md"
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
			PartyWandData data = GLItems.PARTY_WAND_DATA.get(stack);
			if (data != null && !data.drawn().isEmpty()) return data;
		}
		return null;
	}

	/**
	 * Called every frame. Returns immediately unless a wand is held, so there is nothing to keep
	 * in step and nothing to clean up on a level change.
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