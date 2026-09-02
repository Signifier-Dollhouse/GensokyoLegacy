package dev.xkmc.gensokyolegacy.mixin;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectEntry;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.block.functional.barriers.SealingEffectData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerSealingMixin {

	@Inject(method = "spawnCategoryForChunk", at = @At("HEAD"), cancellable = true)
	private static void gensokyolegacy$sealHostileSpawns(
			MobCategory category, ServerLevel level, LevelChunk chunk,
			NaturalSpawner.SpawnPredicate filter, NaturalSpawner.AfterSpawnCallback callback, CallbackInfo ci
	) {
		if (category.isFriendly()) return;
		for (AreaEffectEntry e : AreaEffectManager.getAffecting(level, chunk.getPos())) {
			if (e.data instanceof SealingEffectData) {
				ci.cancel();
				return;
			}
		}
	}

}