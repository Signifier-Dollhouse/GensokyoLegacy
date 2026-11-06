package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorBulletEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorHookEntity;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Mirrors Cataclysm's stun: slower movement here, with attacking, jumping and item use blocked
 * by {@code StunEventHandlers}.
 */
public class StunEffect extends MobEffect {

	private static final int STUN_TICKS = 20;
	private static final int STUN_CHARGED_TICKS = 40;

	public StunEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF8C00);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, GensokyoLegacy.loc("stun_speed"),
				-0.5, AttributeModifier.Operation.ADD_VALUE);
	}

	/**
	 * The plain anchor stuns for one second and the charged throw for two; the water anchor and
	 * the anchor's own melee stun for one.
	 */
	public static void onHit(LivingEntity target, DamageSource source) {
		if (source.getDirectEntity() instanceof AnchorHookEntity hook) {
			switch (hook.throwType()) {
				case DESTROY -> apply(target, STUN_TICKS);
				case DESTROY_CHARGED -> apply(target, STUN_CHARGED_TICKS);
				default -> {
				}
			}
		} else if (source.getDirectEntity() instanceof AnchorBulletEntity) {
			apply(target, STUN_TICKS);
		} else if (source.is(DamageTypes.PLAYER_ATTACK) &&
				source.getDirectEntity() instanceof Player player &&
				player.getMainHandItem().getItem() instanceof AnchorItem) {
			apply(target, STUN_TICKS);
		}
	}

	public static void apply(LivingEntity target, int ticks) {
		target.addEffect(new MobEffectInstance(GLEffects.STUN, ticks));
	}

}
