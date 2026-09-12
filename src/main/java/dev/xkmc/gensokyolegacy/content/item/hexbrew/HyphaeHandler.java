package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.gensokyolegacy.content.block.nature.HyphaeBlock;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class HyphaeHandler implements HexBrewHandler {

	private static final float RADIUS = 4;
	private static final int DURATION = 1200;

	@Override
	public boolean isThrowable() {
		return true;
	}

	@Override
	public void onHit(Level level, Vec3 pos, @Nullable Entity thrower, ItemStack stack) {
		if (level.isClientSide) return;
		level.levelEvent(2002, BlockPos.containing(pos), 0xFF47C0FC);
		AABB box = new AABB(pos, pos).inflate(RADIUS);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> true)) {
			if (e == thrower) continue;
			if (thrower != null && e.isAlliedTo(thrower)) continue;
			if (e.distanceToSqr(pos) > RADIUS * RADIUS) continue;
			e.addEffect(new MobEffectInstance(GLEffects.HYPHAE_INFECTION.holder(), DURATION, 0));
		}
		spawnHyphae(level, pos);
	}

	private void spawnHyphae(Level level, Vec3 pos) {
		BlockPos center = BlockPos.containing(pos);
		BlockPos.betweenClosed(
				center.offset(-(int) RADIUS, -(int) RADIUS, -(int) RADIUS),
				center.offset((int) RADIUS, (int) RADIUS, (int) RADIUS))
				.forEach(p -> {
					if (p.distToCenterSqr(pos) > RADIUS * RADIUS) return;
					if (level.getRandom().nextFloat() < 0.6f) return;
					if (level.getBlockState(p).canBeReplaced()) {
						level.setBlock(p, GLNaturalBlocks.HYPHAE.get().defaultBlockState()
								.setValue(HyphaeBlock.TRANSIENT, true), 3);
					}
				});
	}

}