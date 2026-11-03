package dev.xkmc.gensokyolegacy.content.item.anchor;

import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorBulletEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorHookEntity;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualDanmakuItem;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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
 * Minamitsu's anchor: a pickaxe that also throws itself. The mode picks what a throw does, and the
 * two storm throws pay spiritual power from the pool the item carries.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorItem extends SpiritualDanmakuItem {

	public static final int MAX_POWER = 1000;
	private static final int COOLDOWN = 10;
	private static final int CHARGE_TIME = 20;
	private static final double FLY_SPEED = 1.5;
	private static final int LIFE = 200;
	private static final double FAN_STEP = 12.5;

	public AnchorItem(Properties properties) {
		super(properties, DanmakuItems.Bullet.DAGGER, DyeColor.WHITE, 1.0F, MAX_POWER);
	}

	public static AnchorMode getMode(ItemStack stack) {
		return GLItems.ANCHOR_MODE.getOrDefault(stack, AnchorMode.DESTROY);
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
	 * Model-override value for {@code gensokyolegacy:anchor_display}: the mode's ordinal + 1 selects
	 * its flat GUI icon, so the hotbar icon shows which mode the anchor is in.
	 */
	public static float displayPredicate(ItemStack stack, Level level, LivingEntity entity, int seed) {
		return getMode(stack).ordinal() + 1;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (isLinked(stack)) return retrieve(level, player, stack);
		if (GrazeHelper.forbidDanmaku(player)) return InteractionResultHolder.fail(stack);
		if (getMode(stack) == AnchorMode.STORM) {
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
		AnchorThrow type = getMode(stack) == AnchorMode.DESTROY ? AnchorThrow.DESTROY : AnchorThrow.PULL;
		return throwAnchors(sp, hand, type)
				? InteractionResultHolder.sidedSuccess(stack, false)
				: InteractionResultHolder.fail(stack);
	}

	/**
	 * A stuck hook is freed by the right click that also frees the hand: the stack is hidden while
	 * the link is set, so a retrieve never competes with a throw.
	 */
	private static InteractionResultHolder<ItemStack> retrieve(Level level, Player player, ItemStack stack) {
		if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
		UUID id = getLink(stack);
		if (id != null && sp.serverLevel().getEntity(id) instanceof AnchorHookEntity hook) hook.retrieve();
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
	 * The storm volley is charged: a release before one second does nothing, so a right click alone
	 * cannot dump five anchors.
	 */
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) return;
		if (getUseDuration(stack, entity) - timeLeft < CHARGE_TIME) return;
		if (level.isClientSide()) return;
		if (!(player instanceof ServerPlayer sp)) return;
		if (getMode(stack) != AnchorMode.STORM) return;
		throwAnchors(sp, player.getUsedItemHand(), AnchorThrow.STORM_UNBOUND);
	}

	/**
	 * The single entry point for every throw, right click or left. Decides nothing about the mode:
	 * the caller has already turned the click into an {@link AnchorThrow}.
	 */
	public static boolean throwAnchors(ServerPlayer sp, InteractionHand hand, AnchorThrow type) {
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
		boolean fan = type == AnchorThrow.STORM_UNBOUND;
		int count = fan ? 5 : 1;
		float damage = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
		var orientation = DanmakuHelper.getOrientation(look);
		for (int i = 0; i < count; i++) {
			Vec3 dir = fan ? orientation.rotateDegrees((i - (count - 1) / 2.0) * FAN_STEP) : look;
			if (fan) {
				AnchorBulletEntity proj = new AnchorBulletEntity(GLEntities.ANCHOR_BULLET.get(), sp, level);
				proj.setItem(new ItemStack(GLItems.MINAMITSU_ANCHOR.get()));
				proj.configure(from);
				proj.setup(0, LIFE, false, false, dir.scale(FLY_SPEED));
				proj.moveTo(from);
				level.addFreshEntity(proj);
				SpellContainer.track(sp, proj);
			} else {
				AnchorHookEntity proj = new AnchorHookEntity(GLEntities.ANCHOR_HOOK.get(), sp, level);
				proj.launch(type, damage, from, dir);
				if (level.addFreshEntity(proj)) setLink(stack, proj.getUUID());
			}
		}
		sp.getCooldowns().addCooldown(item, COOLDOWN);
		sp.swing(hand, true);
		sp.awardStat(Stats.ITEM_USED.get(item));
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.TRIDENT_THROW.value(),
				SoundSource.PLAYERS, 1F, 1F);
		return true;
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
		list.add(GLLang.ItemAnchor.COST.get());
		list.add(GLLang.ItemAnchor.CHARGE.get());
		list.add(GLLang.ItemAnchor.RETRIEVE.get());
		list.add(GLLang.ItemGlove.WHEEL.get(L2Keys.WHEEL.map.getKey().getDisplayName())
				.withStyle(ChatFormatting.GRAY));
	}

}
