package dev.xkmc.gensokyolegacy.content.block.functional.barriers;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectVisual;
import dev.xkmc.gensokyolegacy.content.attachment.area.EffectData;
import dev.xkmc.gensokyolegacy.content.item.character.StrangeGlassesItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SerialClass
public class SealingEffectData extends EffectData {

	/**
	 * The tint multiplies into the forcefield texture, so a white one read as a
	 * generic magic shield. The pot is a green ceramic, so the wall takes the pot's
	 * own green: the hue comes from
	 * {@code textures/block/utensil/sealing_pot.png}'s main highlight, RGB(89,
	 * 186, 114), i.e. 0.35 / 0.73 / 0.45. Only the hue is taken - the green
	 * channel is pinned to 1.0, because the texture is already a dim grey and
	 * using the pot's raw luminance would have made the wall barely visible.
	 */
	private static final AreaEffectVisual VISUAL = new AreaEffectVisual(GensokyoLegacy.loc("textures/barriers/sealing_pot.png"),
			0.35F, 1F, 0.45F, 0.35F, -0.5F, true, false, false);

	public SealingEffectData() {
	}

	@Override
	public boolean isOwnerStillValid(ServerLevel level, BlockPos ownerPos, BlockState state) {
		return state.is(GLBlocks.SEALING_POT.get());
	}

	@Override
	public List<AreaEffectVisual> getClientVisual(@Nullable Player viewer) {
		if (viewer != null && (StrangeGlassesItem.isWearing(viewer) || isHoldingSealingPot(viewer)))
			return List.of(VISUAL);
		return List.of();
	}

	private static boolean isHoldingSealingPot(Player player) {
		return player.getMainHandItem().is(GLBlocks.SEALING_POT.asItem()) ||
				player.getOffhandItem().is(GLBlocks.SEALING_POT.asItem());
	}
}