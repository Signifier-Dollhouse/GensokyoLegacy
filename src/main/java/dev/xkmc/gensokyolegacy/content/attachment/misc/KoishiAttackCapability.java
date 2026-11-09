package dev.xkmc.gensokyolegacy.content.attachment.misc;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import dev.xkmc.gensokyolegacy.content.rpg.trigger.KoishiHatTrigger;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLAdvGen;
import dev.xkmc.gensokyolegacy.init.data.GLDamageTypes;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.data.rpg.MarisaQDGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLCriteriaTriggers;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.util.RayTraceUtil;
import dev.xkmc.l2library.capability.player.PlayerCapabilityTemplate;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SerialClass
public class KoishiAttackCapability extends PlayerCapabilityTemplate<KoishiAttackCapability> {

	public static final Capability<KoishiAttackCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

	private static final int DELAY = 360, MARK_POS = 300;

	@SerialField
	private int tickRemain = 0;
	@SerialField
	private int attackCooldown = 0;
	@SerialField
	private int blockCount = 0;
	@SerialField
	private Vec3 source = null;

	@Override
	public void onClone(boolean isWasDeath) {
		blockCount = 0;
		tickRemain = 0;
	}

	protected void startParticle(Vec3 pos) {
		tickRemain = DELAY - MARK_POS;
		source = pos;
	}

	private boolean notValid(Player player) {
		if (!player.level().dimension().equals(Level.NETHER)) return true;
		var quest = GLMeta.QUEST.type().getOrCreate(player).getData(MarisaQDGen.QUEST_KOISHI);
		if (!quest.started) return true;
		return quest.progress.getOrDefault(MarisaQDGen.KOISHI_PROOF, 0) > 0;
	}

	@Override
	public void tick() {
		if (!(player instanceof ServerPlayer sp)) {
			if (source != null && tickRemain > 0 && tickRemain <= DELAY - MARK_POS) {
				ClientCapHandler.showParticle(player, source);
				tickRemain--;
				if (tickRemain == 0) source = null;
			}
			return;
		}
		if (tickRemain > 0) {
			tickRemain--;
			if (tickRemain == DELAY - MARK_POS) {
				source = RayTraceUtil.rayTraceBlock(player.level(), player, -2).getLocation();
				GensokyoLegacy.HANDLER.toClientPlayer(new KoishiStartPacket(KoishiStartPacket.Type.PARTICLE, source), sp);
			}
			if (tickRemain == 0 && source != null) {
				attackCooldown = GLModConfig.SERVER.koishiAttackCoolDown.get();
				if (!notValid(player)) {
					float dmg = GLModConfig.SERVER.koishiAttackDamage.get();

					var adv = sp.server.getAdvancements().getAdvancement(GLAdvGen.KOISHI_FIRST);
					if (adv != null && !sp.getAdvancements().getOrStartProgress(adv).isDone()) {
						GLCriteriaTriggers.KOISHI_FIRST.get().trigger(sp);
						dmg = Math.min(dmg, player.getMaxHealth() - 1);
					}

					if (player.hurt(GLDamageTypes.koishi(player, source), dmg)) {
						blockCount = 0;
					}
				}
				source = null;
			}
			return;
		}
		if (attackCooldown > 0) {
			attackCooldown--;
			return;
		}
		if (notValid(player)) {
			return;
		}
		if (player.getRandom().nextDouble() < GLModConfig.SERVER.koishiAttackChance.get()) {
			tickRemain = DELAY;
			GLCriteriaTriggers.KOISHI_RING.get().trigger(sp);
			GensokyoLegacy.HANDLER.toClientPlayer(new KoishiStartPacket(KoishiStartPacket.Type.START, player.position()), sp);
		}

	}

	public void onBlock(Player player) {
		player.getCooldowns().addCooldown(player.getUseItem().getItem(), 100);
		blockCount++;
		if (blockCount >= GLModConfig.SERVER.koishiAttackBlockCount.get()) {
			blockCount = 0;
			player.spawnAtLocation(GLItems.KOISHI_HAT.get());
			if (player instanceof ServerPlayer sp) {
				GLCriteriaTriggers.KOISHI_HAT.get().trigger(sp);
				GLMeta.QUEST.type().getOrCreate(sp).dispatch(sp, new KoishiHatTrigger(sp));
			}
		}
	}

	public static void register() {
	}

}
