package dev.xkmc.gensokyolegacy.content.item.devil;

import dev.xkmc.danmakuapi.content.entity.ItemLaserEntity;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.fastprojectileapi.collision.EntityStorageHelper;
import dev.xkmc.gensokyolegacy.content.spell.item.DevilScrollDash;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Devil's Scroll: hand-based attributes in every mode, a superhuman melee package in the main
 * hand, and four orange lasers from either hand in devil mode.
 *
 * @see "doc/design/item/devils_scroll.md"
 */
public class DevilScrollItem extends Item {

	private static final int DASH_COOLDOWN = 10;
	private static final int JUMP_DURATION = 40;

	private static final float LASER_DAMAGE = 6;
	private static final float LASER_LENGTH = 80;
	private static final int LASER_PREPARE = 5, LASER_SETUP = 5, LASER_DURATION = 20, LASER_CLOSE = 5;
	public static final int LASER_COOLDOWN = 5;

	/**
	 * Distinct ids per hand: modifiers are stored by id, so a scroll in each hand would otherwise
	 * collapse into one set.
	 */
	public static final ItemAttributeModifiers ATTRIBUTES = ItemAttributeModifiers.builder()
			.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
							GensokyoLegacy.loc("devils_scroll_main_damage"), 7,
							AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.MAINHAND)
			.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(
							GensokyoLegacy.loc("devils_scroll_main_speed"), 1.0,
							AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
					EquipmentSlotGroup.MAINHAND)
			.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
							GensokyoLegacy.loc("devils_scroll_off_damage"), 0.2,
							AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
					EquipmentSlotGroup.OFFHAND)
			.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(
							GensokyoLegacy.loc("devils_scroll_off_speed"), 0.5,
							AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
					EquipmentSlotGroup.OFFHAND)
			.build();

	public DevilScrollItem(Properties properties) {
		super(properties.attributes(ATTRIBUTES).stacksTo(1));
	}

	public static DevilScrollMode getMode(ItemStack stack) {
		return GLItems.DEVILS_SCROLL_MODE.getOrDefault(stack, DevilScrollMode.PASSIVE);
	}

	public static void setMode(ItemStack stack, DevilScrollMode mode) {
		GLItems.DEVILS_SCROLL_MODE.set(stack, mode);
	}

	/**
	 * Superhuman abilities need the scroll in the main hand, not merely somewhere in the inventory.
	 */
	public static boolean isSuperhuman(Player player) {
		ItemStack stack = player.getMainHandItem();
		return stack.getItem() instanceof DevilScrollItem && getMode(stack) == DevilScrollMode.SUPERHUMAN;
	}

	/**
	 * The devil's lasers answer either hand.
	 */
	public static boolean isDevil(Player player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof DevilScrollItem && getMode(stack) == DevilScrollMode.DEVIL) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Display stack for a mode (client-side only): a fresh scroll with the mode and the mode's name.
	 */
	public static ItemStack displayStack(DevilScrollMode mode) {
		ItemStack stack = GLItems.DEVILS_SCROLL.asStack();
		setMode(stack, mode);
		stack.set(DataComponents.ITEM_NAME, mode.getDisplayName());
		return stack;
	}

	/**
	 * Model-override value for {@code gensokyolegacy:devils_scroll_mode}: the mode ordinal picks the
	 * matching texture, with the passive mode on the base model.
	 */
	public static float displayPredicate(ItemStack stack, Level level, LivingEntity entity, int seed) {
		return getMode(stack).ordinal();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND || !isSuperhuman(player)) {
			return InteractionResultHolder.pass(stack);
		}
		if (player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(stack);
		}
		if (level.isClientSide()) {
			return InteractionResultHolder.sidedSuccess(stack, true);
		}
		if (!(player instanceof ServerPlayer sp)) {
			return InteractionResultHolder.fail(stack);
		}
		SpellContainer.castSpell(sp, DevilScrollDash::new, null);
		sp.getCooldowns().addCooldown(this, DASH_COOLDOWN);
		sp.swing(hand, true);
		return InteractionResultHolder.sidedSuccess(stack, false);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide()) return;
		if (!(entity instanceof Player player)) return;
		if (player.getMainHandItem() != stack) return;
		if (!isSuperhuman(player)) return;
		if (player.tickCount % 20 == 4) {
			player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_DURATION, 1, true, false, false), player);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.DEVILS_SCROLL_LORE.get());
		DevilScrollMode mode = getMode(stack);
		list.add(GLLang.ItemDevilScroll.MODE.get(mode.getDisplayName()).withStyle(ChatFormatting.GRAY));
		list.add(mode.getDescription().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemGlove.WHEEL.get(L2Keys.WHEEL.map.getKey().getDisplayName())
				.withStyle(ChatFormatting.GRAY));
	}

	/**
	 * Four orange lasers from the eye corners, each aimed at the victim's centre so the beams
	 * converge rather than firing parallel.
	 */
	public static void summonLasers(ServerPlayer player, LivingEntity target) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 center = target.getBoundingBox().getCenter();
		double r = 1.5;
		for (int i = 0; i < 4; i++) {
			double dy = (i & 1) == 0 ? -r : r;
			double dx = (i & 2) == 0 ? -r : r;
			Vec3 origin = eye.add(0, dy, 0).add(side.scale(dx));
			Vec3 dir = center.subtract(origin);
			dir = dir.lengthSqr() < 1e-6 ? look : dir.normalize();
			ItemLaserEntity laser = new ItemLaserEntity(DanmakuEntities.ITEM_LASER.get(), player, level);
			laser.setItem(new ItemStack(DanmakuItems.Laser.LASER.get(DyeColor.ORANGE).get()));
			laser.setup(LASER_DAMAGE, LASER_DURATION, LASER_LENGTH, true, dir);
			laser.setupTime(LASER_PREPARE, LASER_SETUP, LASER_DURATION, LASER_CLOSE);
			laser.setPos(origin);
			EntityStorageHelper.fastAdd(level, laser);
		}
	}

}
