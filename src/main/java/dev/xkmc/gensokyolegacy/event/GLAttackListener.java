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
		if (TalismanCurioItem.testAny(cache.getTarget(), ctx -> ctx.paper().onAttacked(ctx, cache))) {
			return true;
		}
		return AttackListener.super.onAttack(cache);
	}

	/**
	 * Doll melee hits ignore the target's 10-tick invulnerability window.
	 * <p>
	 * Vanilla leaves a struck entity invulnerable for half a second, and a melee doll
	 * is built to trade inside a volley — {@code DollMeleeBehavior} releases its ticket
	 * on impact so the next doll charges while this one flies home, which puts several
	 * swings in the same window. Left vanilla, all but the first would be swallowed by
	 * the timer and a volley would land roughly one hit instead of one per doll.
	 * <p>
	 * Matched on the attacker rather than on the behavior, since melee is the only
	 * damage a doll deals through {@code mob_attack} — any doll swinging with anything
	 * gets this, including attacks from add-ons that do not route through
	 * {@code DollMeleeBehavior}.
	 * <p>
	 * {@code sourceIs} also matches tags when prefixed with {@code #}, so this stays
	 * true if the root behind {@code mob_attack} changes. The state resolves to
	 * l2damagetracker's pre-generated {@code l2damagetracker:mob_attack-bypass_cooldown}
	 * — identical to {@code mob_attack} apart from carrying
	 * {@code #minecraft:bypasses_cooldown}, so the death message and scaling are
	 * untouched. A no-op when l2damagetracker has no root for the type, which
	 * {@code enable} reports rather than throws.
	 */
	@Override
	public void onCreateSource(CreateSourceEvent event) {
		if (!(event.getAttacker() instanceof BaseDollEntity)) return;
		if (!event.sourceIs("minecraft:mob_attack")) return;
		event.enable(DefaultDamageState.BYPASS_COOLDOWN);
	}

	@Override
	public void onDamage(DamageData.Defence data) {
		TalismanCurioItem.iterate(data.getTarget(), ctx -> ctx.paper().onDamaged(ctx, data));
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
