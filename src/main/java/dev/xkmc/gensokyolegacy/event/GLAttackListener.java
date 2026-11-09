package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.danmakuapi.init.data.DanmakuDamageTypes;
import dev.xkmc.gensokyolegacy.content.effect.DanmakuRetaliateEffect;
import dev.xkmc.gensokyolegacy.content.effect.MiasmaEffect;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanCurioItem;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.AttackListener;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GLAttackListener implements AttackListener {

	@Override
	public void onAttack(AttackCache cache, ItemStack weapon) {
		if (cache.getAttackTarget() instanceof Cat cat) {
			if (cat.isPassenger() && cat.getVehicle() instanceof Player) {
				if (cat.getTags().contains("CatBell")) {
					if (cache.getLivingAttackEvent() != null) cache.getLivingAttackEvent().setCanceled(true);
					return;
				}
			}
		}
		var target = cache.getAttackTarget();
		boolean blocked = TalismanCurioItem.testAny(target, ctx -> ctx.paper().onAttacked(ctx, cache));
		TalismanCurioItem.syncSpentTalismans(target);
		if (blocked && cache.getLivingAttackEvent() != null) cache.getLivingAttackEvent().setCanceled(true);
	}

	@Override
	public void onDamage(AttackCache cache, ItemStack weapon) {
		var target = cache.getAttackTarget();
		TalismanCurioItem.iterate(target, ctx -> ctx.paper().onDamaged(ctx, cache));
		TalismanCurioItem.syncSpentTalismans(target);
		var source = cache.getLivingDamageEvent().getSource();
		if (source.is(DanmakuDamageTypes.DANMAKU) && source.getEntity() instanceof YoukaiEntity) {
			double min = target instanceof Player ?
					GLModConfig.SERVER.danmakuPlayerPHPDamage.get() :
					GLModConfig.SERVER.danmakuMinPHPDamage.get();
			cache.addDealtModifier(DamageModifier.nonlinearMiddle(460,
					f -> Math.max(f, target.getMaxHealth() * (float) min)));
		}
	}

	@Override
	public void onDamageFinalized(AttackCache cache, ItemStack weapon) {
		var target = cache.getAttackTarget();
		DanmakuRetaliateEffect.onLivingHurt(target, GLEffects.SPARKLING);
		DanmakuRetaliateEffect.onLivingHurt(target, GLEffects.HAUNTING);
		MiasmaEffect.onHurt(target);

		var attacker = cache.getAttacker();
		if (attacker == null) return;
		var source = cache.getLivingDamageEvent().getSource();
		for (ItemStack head : TouhouHatItem.getEquippedHats(attacker)) {
			if (head.getItem() instanceof TouhouHatItem hat) {
				hat.onHurtTarget(head, source, target);
			}
		}
	}

}
