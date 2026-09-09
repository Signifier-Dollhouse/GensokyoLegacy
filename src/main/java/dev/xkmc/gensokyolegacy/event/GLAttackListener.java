package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.init.data.DanmakuDamageTypes;
import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.SparklingEventHandler;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.l2damagetracker.contents.attack.AttackListener;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GLAttackListener implements AttackListener {

	@Override
	public boolean onAttack(DamageData.Attack cache) {
		if (cache.getTarget() instanceof Cat cat) {
			if (cat.isPassenger() && cat.getVehicle() instanceof Player sp) {
				if (cat.getTags().contains("CatBell")) {
					return true;
				}
			}
		}
		if (cache.getTarget() instanceof ServerPlayer sp) {
			for (ItemStack paper : CuriosManager.getEquippedTalismans(sp)) {
				if (sp.getCooldowns().isOnCooldown(paper.getItem())) continue;
				if (paper.getItem() instanceof TalismanPaperItem entry) {
					if (entry.onAttacked(paper, sp, cache)) {
						return true;
					}
				}
			}
		}
		return AttackListener.super.onAttack(cache);
	}

	@Override
	public void onDamage(DamageData.Defence data) {
		if (data.getTarget() instanceof ServerPlayer sp) {
			for (ItemStack paper : CuriosManager.getEquippedTalismans(sp)) {
				if (paper.getItem() instanceof TalismanPaperItem entry) {
					entry.onDamaged(paper, sp, data);
				}
			}
		}
		if (data.getSource().is(DanmakuDamageTypes.DANMAKU) && data.getSource().getEntity() instanceof YoukaiEntity) {
			LivingEntity le = data.getTarget();
			double min = le instanceof Player ?
					GLModConfig.SERVER.danmakuPlayerPHPDamage.get() :
					GLModConfig.SERVER.danmakuMinPHPDamage.get();
			data.addDealtModifier(DamageModifier.nonlinearMiddle(460,
					f -> Math.max(f, le.getMaxHealth() * (float) min),
					GensokyoLegacy.loc("youkai_damage")
			));
		}
	}

	@Override
	public void onDamageFinalized(DamageData.DefenceMax data) {
		SparklingEventHandler.onLivingHurt(data.getTarget());
		
		var attacker = data.getAttacker();
		if (attacker == null) return;
		ItemStack head = attacker.getItemBySlot(EquipmentSlot.HEAD);
		if (head.getItem() instanceof TouhouHatItem hat) {
			hat.onHurtTarget(head, data.getSource(), data.getTarget());
		}
	}

}
