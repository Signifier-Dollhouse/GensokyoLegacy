package dev.xkmc.gensokyolegacy.content.item.anchor;

import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorBulletEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorHookEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorStorm;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualDanmakuItem;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Minamitsu's anchor: a pickaxe that also throws itself. The mode picks what a throw does, and every
 * storm throw pays spiritual power from the pool the item carries.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorItem extends SpiritualDanmakuItem {

	public static final int MAX_POWER = 1000;
	private static final int COOLDOWN = 10;
	private static final int CHARGE_TIME = 20;
	private static final int DESTROY_CHARGE_MIN = 20;
	private static final int DESTROY_CHARGE_MAX = 40;
	private static final int DESTROY_RADIUS_BASE = 2;
	private static final int DESTROY_RADIUS_MIN = 3;
	private static final int DESTROY_RADIUS_MAX = 4;
	private static final float DESTROY_DAMAGE_MIN = 1.5F;
	private static final float DESTROY_DAMAGE_MAX = 2.5F;
	private static final double MELEE_RADIUS = 3;
	private static final double FLY_SPEED = 1.5;
	private static final int LIFE = 200;

	public AnchorItem(Properties properties) {
		super(properties, DanmakuItems.Bullet.DAGGER, DyeColor.WHITE, 1.0F, MAX_POWER);
	}

	public static AnchorMode getMode(ItemStack stack) {
		return GLItems.ANCHOR_MODE.getOrDefault(stack, AnchorMode.VOYAGE);
	}

	public static void setMode(ItemStack stack, AnchorMode mode) {
		GLItems.ANCHOR_MODE.set(stack, mode);
	}

	/**
	 * The bounded hook this stack is tied to while it is out, or null. The UUID is written on the
	 * throw and cleared when the hook leaves the world, so the client can trust its presence alone.
	 */
	@Nullable
	public static UUID getLink(ItemStack stack) {
		return GLItems.ANCHOR_LINK.get(stack);
	}

	public static boolean isLinked(ItemStack stack) {
		return stack.has(GLItems.ANCHOR_LINK);
	}

	public static void setLink(ItemStack stack, UUID id) {
		GLItems.ANCHOR_LINK.set(stack, id);
	}

	public static void clearLink(ItemStack stack) {
		stack.remove(GLItems.ANCHOR_LINK.get());
	}

	/**
	 * The left click on an entity in any mode: the vanilla attack is cancelled and everything within
	 * {@link #MELEE_RADIUS} of the clicked target takes the swing instead.
	 */
	public static void melee(ServerPlayer sp, Entity target) {
		AnchorStorm.melee(sp, target.position(), MELEE_RADIUS);
	}

	/**
	 * Display stack for a mode (client-side only): a fresh anchor with the given mode and a name.
	 */
	public static ItemStack displayStack(AnchorMode mode) {
		ItemStack stack = GLItems.MINAMITSU_ANCHOR.asStack();
		setMode(stack, mode);
		stack.set(DataComponents.ITEM_NAME, mode.getDisplayName());
		return stack;
	}

	/**
	 * As {@link #displayStack}, but marked as an icon display so the wheel and the selector list draw
	 * this mode's wheel icon instead of its GUI icon.
	 */
	public static ItemStack iconStack(AnchorMode mode) {
		ItemStack stack = displayStack(mode);
		stack.set(GLItems.ICON.get(), Unit.INSTANCE);
		return stack;
	}

	/**
	 * Model-override value for {@code gensokyolegacy:anchor_display}: the mode's ordinal + 1 selects
	 * its flat GUI icon, and an icon stack uses the range behind it for the wheel icons.
	 */
	public static float displayPredicate(ItemStack stack, Level level, LivingEntity entity, int seed) {
		int ordinal = getMode(stack).ordinal();
		return stack.has(GLItems.ICON.get())
				? AnchorMode.values().length + 1 + ordinal
				: ordinal + 1;
	}

	/**
	 * Bow-shaped charge ramp over {@link #CHARGE_TIME}, squared so the first ticks barely pull the
	 * camera in. The client FOV handler reads it while the anchor is raised.
	 */
	public static float chargeProgress(LivingEntity user) {
		if (!user.isUsingItem()) return 0;
		float t = Math.min(user.getTicksUsingItem() / (float) CHARGE_TIME, 1F);
		return t * t;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (isLinked(stack)) return retrieve(level, player, stack);
		if (GrazeHelper.forbidDanmaku(player)) return InteractionResultHolder.fail(stack);
		if (getMode(stack) != AnchorMode.VOYAGE) {     // Destroy and Storm charge
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
		return throwAnchors(sp, hand, AnchorThrow.PULL)
				? InteractionResultHolder.sidedSuccess(stack, false)
				: InteractionResultHolder.fail(stack);
	}

	/**
	 * The link branch: a retained hook pulls the player again from beyond 3 blocks and is recalled
	 * from closer, and the mode on the stack does not matter.
	 */
	private static InteractionResultHolder<ItemStack> retrieve(Level level, Player player, ItemStack stack) {
		if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
		UUID id = getLink(stack);
		if (id != null && sp.serverLevel().getEntity(id) instanceof AnchorHookEntity hook) {
			hook.onRetrieve(sp);
		}
		return InteractionResultHolder.sidedSuccess(stack, false);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.SPEAR;
	}

	/**
	 * Storm releases itself once the charge completes; Destroy waits for the release and scales its
	 * radius with the time held.
	 */
	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
		if (getMode(stack) != AnchorMode.STORM) return;
		if (level.isClientSide() && user instanceof Player player)
			AnchorRain.chargeParticles(player);
		if (getUseDuration(stack, user) - remaining < CHARGE_TIME) return;
		if (user instanceof Player player) player.releaseUsingItem();
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) return;
		if (level.isClientSide()) return;
		if (!(player instanceof ServerPlayer sp)) return;
		int used = getUseDuration(stack, entity) - timeLeft;
		switch (getMode(stack)) {
			case STORM -> {
				if (used < CHARGE_TIME) return;
				throwAnchors(sp, player.getUsedItemHand(), AnchorThrow.STORM_RAIN);
			}
			case DESTROY -> {
				if (used < DESTROY_CHARGE_MIN) return;
				int held = Math.min(used, DESTROY_CHARGE_MAX) / CHARGE_TIME;
				int radius = Mth.clamp(DESTROY_RADIUS_BASE + held,
						DESTROY_RADIUS_MIN, DESTROY_RADIUS_MAX);
				float t = (float) (Mth.clamp(used, DESTROY_CHARGE_MIN, DESTROY_CHARGE_MAX)
						- DESTROY_CHARGE_MIN) / (DESTROY_CHARGE_MAX - DESTROY_CHARGE_MIN);
				float scale = Mth.lerp(t, DESTROY_DAMAGE_MIN, DESTROY_DAMAGE_MAX);
				throwAnchors(sp, player.getUsedItemHand(), AnchorThrow.DESTROY_CHARGED, radius, scale);
			}
			case VOYAGE -> {
			}
		}
	}

	/**
	 * The single entry point for every throw, right click or left. Decides nothing about the mode:
	 * the caller has already turned the click into an {@link AnchorThrow}.
	 */
	public static boolean throwAnchors(ServerPlayer sp, InteractionHand hand, AnchorThrow type) {
		return throwAnchors(sp, hand, type, 0, 1);
	}

	/**
	 * @param radius the blast radius of a charged destroy throw; ignored by every other type
	 * @param scale  the charged throw's damage multiplier on the thrower's attack damage
	 */
	public static boolean throwAnchors(ServerPlayer sp, InteractionHand hand, AnchorThrow type,
			int radius, float scale) {
		ItemStack stack = sp.getItemInHand(hand);
		if (!(stack.getItem() instanceof AnchorItem item)) return false;
		if (isLinked(stack)) return false;
		if (sp.getCooldowns().isOnCooldown(item)) return false;
		int cost = type.cost();
		if (cost > 0 && SpiritualPower.get(stack) < cost) {
			sp.displayClientMessage(GLLang.ItemAnchor.NEED_POWER.get(cost), true);
			return false;
		}
		if (cost > 0) SpiritualPower.add(stack, -cost);

		ServerLevel level = sp.serverLevel();
		Vec3 look = RayTraceUtil.getRayTerm(Vec3.ZERO, sp.getXRot(), sp.getYRot(), 1);
		Vec3 from = RayTraceUtil.getRayTerm(sp.getEyePosition(), sp.getXRot(), sp.getYRot(), 2);
		if (type.isRain()) {
			AnchorRain.rain(sp);
		} else if (type.isStorm()) {
			throwUnbound(sp, level, from, look);
		} else {
			AnchorHookEntity proj = new AnchorHookEntity(GLEntities.ANCHOR_HOOK.get(), sp, level);
			float damage = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE) * scale;
			proj.launch(type, damage, from, look);
			proj.setRadius(radius);
			if (level.addFreshEntity(proj)) setLink(stack, proj.getUUID());
		}
		sp.getCooldowns().addCooldown(item, COOLDOWN);
		sp.swing(hand, true);
		sp.awardStat(Stats.ITEM_USED.get(item));
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.TRIDENT_THROW.value(),
				SoundSource.PLAYERS, 1F, 1F);
		return true;
	}

	/**
	 * The single unbound anchor of a left-click storm throw; the right-click rain is
	 * {@link AnchorRain}.
	 */
	private static void throwUnbound(ServerPlayer sp, ServerLevel level, Vec3 from, Vec3 look) {
		AnchorBulletEntity proj = new AnchorBulletEntity(GLEntities.ANCHOR_BULLET.get(), sp, level);
		proj.setItem(new ItemStack(GLItems.MINAMITSU_ANCHOR.get()));
		proj.configure(from);
		proj.setup(0, LIFE, false, false, look.scale(FLY_SPEED));
		proj.moveTo(from);
		level.addFreshEntity(proj);
		SpellContainer.track(sp, proj);
	}

	/**
	 * Only the server can see the hook leave the world, so the link is cleared here rather than on
	 * removal: the stack may have moved to another slot or a container by then.
	 */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (!(level instanceof ServerLevel sl)) return;
		UUID id = getLink(stack);
		if (id == null) return;
		if (sl.getEntity(id) instanceof AnchorHookEntity hook && !hook.isRemoved()) return;
		clearLink(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.MINAMITSU_ANCHOR_LORE.get());
		super.appendHoverText(stack, context, list, flag);
		list.add(GLLang.ItemGlove.WHEEL.get(L2Keys.WHEEL.map.getKey().getDisplayName())
				.withStyle(ChatFormatting.GRAY));
	}

}
