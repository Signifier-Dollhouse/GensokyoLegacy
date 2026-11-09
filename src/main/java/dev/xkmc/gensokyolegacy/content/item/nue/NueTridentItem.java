package dev.xkmc.gensokyolegacy.content.item.nue;

import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.EquipmentSlot;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap;
import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.NueTridentProjectile;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.l2library.util.raytrace.RayTraceUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * Nue's trident: the throw sends a copy and never spends the stack, and the copy turns whatever it
 * hits into a UFO. A throw is charged like the vanilla trident's.
 *
 * @see "doc/design/nue_trident.md"
 */
public class NueTridentItem extends DanmakuItem {

	private static final int THROW_THRESHOLD = 10;

	public NueTridentItem(Properties properties) {
		super(properties, DanmakuItems.Bullet.DAGGER, DyeColor.WHITE, DanmakuItems.Bullet.DAGGER.size);
	}

	private static final Multimap<Attribute, AttributeModifier> DEFAULT_MODIFIERS;

	static {
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
				10.0, AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_UUID, "Weapon modifier",
				-2.8, AttributeModifier.Operation.ADDITION));
		DEFAULT_MODIFIERS = builder.build();
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? DEFAULT_MODIFIERS : super.getDefaultAttributeModifiers(slot);
	}


	@Override
	protected boolean consume() {
		return false;
	}

	@Override
	protected int cooldown() {
		return 10;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.SPEAR;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (GrazeHelper.forbidDanmaku(player)) return InteractionResultHolder.fail(stack);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	/**
	 * The charge is the throw: a click only raises the trident, and only a release past the vanilla
	 * threshold sends the copy. This repeats what {@link DanmakuItem#use} does once its click fires.
	 */
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) return;
		if (getUseDuration(stack) - timeLeft < THROW_THRESHOLD) return;
		if (player.getCooldowns().isOnCooldown(this)) return;
		// a use cut short by our own UFO swap hands over a stack that is no longer held
		if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) return;
		DanmakuUseEvent event = new DanmakuUseEvent(player, stack, cooldown());
		MinecraftForge.EVENT_BUS.post(event);
		if (event.isCanceled()) return;
		playThrowSound(level, player);
		if (!level.isClientSide) {
			spawnBullet(newBullet(player, level), player, level, event);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, event.getCooldown());
	}

	@Override
	protected ItemBulletEntity newBullet(Player player, Level level) {
		return new NueTridentProjectile(GLEntities.NUE_TRIDENT.get(), player, level);
	}

	/**
	 * The copy flies on the vanilla trident arc rather than the shared danmaku line, and leaves
	 * from the thrower's eyes so it starts on the crosshair.
	 */
	@Override
	protected void spawnBullet(ItemBulletEntity danmaku, Player player, Level level, DanmakuUseEvent event) {
		danmaku.setItem(bulletStack(player, event));
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		((NueTridentProjectile) danmaku).setDamage(damage);
		Vec3 look = RayTraceUtil.getRayTerm(Vec3.ZERO, player.getXRot(), player.getYRot(), 1);
		danmaku.setup(damage, 120, false, false, look.scale(2.5));
		danmaku.moveTo(player.getEyePosition());
		level.addFreshEntity(danmaku);
		if (player instanceof ServerPlayer sp) SpellContainer.track(sp, danmaku);
	}

	@Override
	protected void playThrowSound(Level level, Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW,
				SoundSource.PLAYERS, 1F, 1F);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.NUE_TRIDENT_LORE.get());
		super.appendHoverText(stack, level, list, flag);
	}

}
