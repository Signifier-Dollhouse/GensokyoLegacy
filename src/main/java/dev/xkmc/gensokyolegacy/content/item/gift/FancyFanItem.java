package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A fan that blows. Right-click and it sweeps a cone in front of the wielder, shoving everything
 * caught in it along the wielder's own look vector.
 * <p>
 * The reach, the two cone angles and the cooldown are the whole tuning.
 *
 * @see "doc/design/fancy_fan.md"
 */
public class FancyFanItem extends Item {

	public static final double REACH = 9;
	public static final double HORIZONTAL_ANGLE = 60;
	public static final double VERTICAL_ANGLE = 45;
	public static final int COOLDOWN = 20;

	public FancyFanItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
		if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
		if (!gust(player)) return InteractionResultHolder.fail(stack);
		return InteractionResultHolder.sidedSuccess(stack, false);
	}

	/**
	 * Pointing the fan at a mob blows the gust at it, so the fan is not a thing that has to be swung
	 * at thin air to be of any use. A character who takes the fan answers first and so keeps it.
	 */
	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.PASS;
		if (player.level().isClientSide) return InteractionResult.SUCCESS;
		if (!gust(player)) return InteractionResult.PASS;
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, context, list, flag);
		list.add(GLLang.ItemLores.FANCY_FAN_LORE.get());
		list.add(GLLang.ItemCommon.BLOW.get());
	}

	/**
	 * The gust and the cooldown it starts. Returns whether the gust was actually blown.
	 */
	private boolean gust(Player player) {
		if (player.getCooldowns().isOnCooldown(this)) return false;
		if (!(player instanceof ServerPlayer sp)) return false;
		blow(sp.serverLevel(), player);
		player.getCooldowns().addCooldown(this, COOLDOWN);
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}

	/**
	 * The gust. Everything alive in the cone is shoved along the wielder's look vector; allies and
	 * the wielder themselves are left alone.
	 */
	private static void blow(ServerLevel level, Player wielder) {
		var origin = wielder.getEyePosition();
		var look = wielder.getLookAngle().normalize();
		var box = wielder.getBoundingBox().inflate(REACH);
		for (var target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != wielder
				&& e.isAlive() && !e.isSpectator() && !e.isAlliedTo(wielder))) {
			if (!inCone(wielder, origin, target)) continue;
			push(target, look);
		}
		puffs(level, origin, look);
		level.playSound(null, wielder.getX(), wielder.getY(), wielder.getZ(),
				SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
	}

	/**
	 * Whether the target is inside the cone: within {@link #HORIZONTAL_ANGLE} of the wielder's yaw
	 * and within {@link #VERTICAL_ANGLE} of their pitch, and no further off than {@link #REACH}.
	 * <p>
	 * The reach is measured to the nearest point of the body, so a tall mob standing at the limit
	 * still counts.
	 */
	private static boolean inCone(Player wielder, Vec3 origin, LivingEntity target) {
		if (target.getBoundingBox().distanceToSqr(origin) > REACH * REACH) return false;
		var to = target.getBoundingBox().getCenter().subtract(origin);
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		// straight up or down has no bearing, so only the elevation decides there
		if (flat >= 1e-6) {
			// the bearing off the wielder's yaw, in the degrees Minecraft counts yaw in
			double bearing = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-to.x, to.z)) - wielder.getYRot());
			if (Math.abs(bearing) > HORIZONTAL_ANGLE) return false;
		}
		// the elevation off the wielder's pitch, which is the other way up
		double elevation = Math.toDegrees(Math.atan2(to.y, flat));
		return Math.abs(elevation + wielder.getXRot()) <= VERTICAL_ANGLE;
	}

	/**
	 * The shove. The impulse is added to whatever the target was already doing, so a target already
	 * running away is not slowed by the gust, and the speed is capped so that spamming the fan
	 * cannot launch anything across the map.
	 * <p>
	 * Only the horizontal speed is capped: a mob already falling keeps falling. A target's knockback
	 * resistance scales the impulse down, so the heavy things stay heavy.
	 */
	private static void push(LivingEntity target, Vec3 look) {
		double scale = 3;
		var acc = look.scale(scale);
		if (target.onGround()) acc = acc.with(Direction.Axis.Y, 0.4);
		var motion = target.getDeltaMovement().add(acc);
		target.setDeltaMovement(motion);
		target.hasImpulse = true;
		target.hurtMarked = true;
	}

	/**
	 * The visible gust: wind puffs scattered along the look vector, spreading as they go.
	 */
	private static void puffs(ServerLevel level, Vec3 origin, Vec3 look) {
		var r = level.getRandom();
		for (int i = 3; i < REACH; i++) {
			for (int j = 0; j < i; j++) {
				var dir = DanmakuHelper.getOrientation(look).rotateDegrees(
						60d * ((j + r.nextDouble()) / i * 2 - 1),
						45d * (r.nextDouble() * 2 - 1)
				);
				var at = origin.add(dir.scale(i + r.nextDouble()));
				level.sendParticles(ParticleTypes.GUST, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
		}
	}

}
