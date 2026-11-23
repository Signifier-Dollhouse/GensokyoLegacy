package dev.xkmc.gensokyolegacy.content.spell.item;

import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.spell.item.ItemSpell;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.Ticker;
import dev.xkmc.gensokyolegacy.content.item.devil.DevilScrollItem;
import dev.xkmc.gensokyolegacy.event.DevilScrollEventHandlers;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * The superhuman dash: one three-blocks-per-tick impulse and a ticker that sweeps the path, hitting
 * each living entity it passes once.
 *
 * @see "doc/design/item/devils_scroll.md"
 */
@SerialClass
public class DevilScrollDash extends ItemSpell {

	private static final double SPEED = 3;
	private static final double KNOCK_SPEED = 3;
	private static final double SNAP_DIST = 2;
	private static final int DURATION = 10;
	private static final double MIN_SPEED = 0.2;
	private static final double HIT_INFLATE = 0.5;

	@Override
	public void start(LivingEntity entity, @Nullable LivingEntity target) {
		super.start(entity, target);
		if (target != null) {
			Vec3 to = target.getBoundingBox().getCenter().subtract(entity.getEyePosition());
			if (to.lengthSqr() > 1e-6) {
				Vec3 aim = to.normalize();
				if (entity.isShiftKeyDown() || dir.dot(aim) > DevilScrollItem.AIM_COS) dir = aim;
			}
		}
		var ticker = new DashTicker();
		ticker.from = entity.position();
		addTicker(ticker);
		entity.setDeltaMovement(dir.scale(SPEED));
		entity.hasImpulse = true;
		entity.fallDistance = 0;
		if (entity instanceof Player player) player.hurtMarked = true;
	}

	/**
	 * The sweep. It ends when the dash is over or a wall has already stopped the player.
	 */
	@SerialClass
	public static class DashTicker extends Ticker<DevilScrollDash> {

		@SerialField
		private Vec3 from = Vec3.ZERO;
		@SerialField
		private final ArrayList<Integer> hit = new ArrayList<>();

		@Override
		public boolean tick(CardHolder holder, DevilScrollDash spell) {
			super.tick(holder, spell);
			if (!(holder.self() instanceof Player player)) return true;
			Vec3 to = player.position();
			double half = player.getBbWidth() / 2 + HIT_INFLATE;
			AABB box = new AABB(from, to).inflate(half);
			float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			for (var e : player.level().getEntitiesOfClass(LivingEntity.class, box,
					e -> e != player && e.isAlive() && !hit.contains(e.getId())
							&& GrazeHelper.shouldPlayerHurt(player, e))) {
				hit.add(e.getId());
				if (e.hurt(player.damageSources().playerAttack(player), damage)) {
					snap(e, player, spell.dir);
					DevilScrollEventHandlers.applyKnockback(e, spell.dir, KNOCK_SPEED);
				}
			}
			from = to;
			return tick > DURATION || player.getDeltaMovement().length() < MIN_SPEED;
		}

		/**
		 * Moves the target {@link #SNAP_DIST} blocks ahead along the full 3D dash direction. It is an
		 * entity move, so block collisions stop or deflect the snap instead of teleporting through walls.
		 */
		private static void snap(LivingEntity target, Player player, Vec3 dir) {
			double len = dir.length();
			if (len < 1e-5) return;
			Vec3 pos = player.position().add(dir.scale(SNAP_DIST / len));
			target.move(MoverType.SELF, pos.subtract(target.position()));
		}

	}

}
