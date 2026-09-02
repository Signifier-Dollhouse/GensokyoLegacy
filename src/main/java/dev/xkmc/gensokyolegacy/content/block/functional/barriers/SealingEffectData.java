package dev.xkmc.gensokyolegacy.content.block.functional.barriers;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectVisual;
import dev.xkmc.gensokyolegacy.content.attachment.area.EffectData;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

@SerialClass
public class SealingEffectData extends EffectData {

	private static final AreaEffectVisual VISUAL = new AreaEffectVisual(GensokyoLegacy.loc("textures/barriers/sealing_pot.png"),
			1, 1, 1, 0.35F, -0.5F, true, false, false);

	public SealingEffectData() {
	}

	@Override
	public boolean isOwnerStillValid(ServerLevel level, BlockPos ownerPos, BlockState state) {
		return state.is(GLBlocks.SEALING_POT.get());
	}

	@Override
	public List<AreaEffectVisual> getClientVisual() {
		return List.of(VISUAL);
	}
}