package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.init.data.DanmakuDamageTypes;
import dev.xkmc.gensokyolegacy.content.effect.MiasmaEffect;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.SparklingEventHandler;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanCurioItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.l2damagetracker.contents.attack.AttackListener;
import dev.xkmc.l2damagetracker.contents.attack.CreateSourceEvent;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import dev.xkmc.l2damagetracker.contents.damage.DefaultDamageState;
import net.minecraft.world.damagesource.DamageTypes;
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
		var target = cache.getTarget();
		boolean blocked = TalismanCurioItem.testAny(target, ctx -> ctx.paper().onAttacked(ctx, cache));
		TalismanCurioItem.syncSpentTalismans(target);
		return blocked || AttackListener.super.onAttack(cache);
	}

	@Override
	public void onCreateSource(CreateSourceEvent event) {
		if (!(event.getAttacker() instanceof BaseDollEntity)) return;
		if (!event.getOriginal().equals(DamageTypes.MOB_ATTACK)) return;
		event.enable(DefaultDamageState.BYPASS_COOLDOWN);
	}

	@Override
	public void onDamage(DamageData.Defence data) {
		TalismanCurioItem.iterate(data.getTarget(), ctx -> ctx.paper().onDamaged(ctx, data));
		TalismanCurioItem.syncSpentTalismans(data.getTarget());
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
		MiasmaEffect.onHurt(data.getTarget());

		var attacker = data.getAttacker();
		if (attacker == null) return;
		for (ItemStack head : TouhouHatItem.getEquippedHats(attacker)) {
			if (head.getItem() instanceof TouhouHatItem hat) {
				hat.onHurtTarget(head, data.getSource(), data.getTarget());
			}
		}
	}

}
