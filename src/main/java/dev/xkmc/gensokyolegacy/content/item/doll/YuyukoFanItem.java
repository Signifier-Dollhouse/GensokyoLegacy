package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollSpawn;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPower;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPowerHolder;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostVariant;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostVariantHolder;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Yuyuko's fan stores ghost power by type. Right-click a ghost to absorb it; right-click air to
 * summon the selected type at full power or top up an existing one. Absorbing an owned ghost
 * with a full fan releases it instead.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class YuyukoFanItem extends Item {

	private static final int COOLDOWN = 10;

	public YuyukoFanItem(Properties properties) {
		super(properties);
	}

	public static GhostVariant getType(ItemStack stack) {
		return GLItems.YUYUKO_FAN_TYPE.getOrDefault(stack, GhostVariant.CURIOS);
	}

	public static void setType(ItemStack stack, GhostVariant type) {
		GLItems.YUYUKO_FAN_TYPE.set(stack, type);
	}

	public static int getPower(ItemStack stack, GhostVariant type) {
		return switch (type) {
			case CURIOS -> GLItems.YUYUKO_FAN_CURIOS.getOrDefault(stack, 0);
			case FOODIE -> GLItems.YUYUKO_FAN_FOODIE.getOrDefault(stack, 0);
			case LIVELY -> GLItems.YUYUKO_FAN_LIVELY.getOrDefault(stack, 0);
		};
	}

	public static void setPower(ItemStack stack, GhostVariant type, int power) {
		int value = Mth.clamp(power, 0, GhostPower.FAN_CAPACITY);
		switch (type) {
			case CURIOS -> GLItems.YUYUKO_FAN_CURIOS.set(stack, value);
			case FOODIE -> GLItems.YUYUKO_FAN_FOODIE.set(stack, value);
			case LIVELY -> GLItems.YUYUKO_FAN_LIVELY.set(stack, value);
		}
	}

	/**
	 * A selector icon for one ghost type: the stray ghost egg, named after the type, because every
	 * type shares the same fan item model.
	 */
	public static ItemStack iconStack(GhostVariant type) {
		SpawnEggItem egg = SpawnEggItem.byId(GLEntities.STRAY_GHOST.get());
		ItemStack stack = egg == null ? new ItemStack(GLItems.YUYUKO_FAN.get()) : new ItemStack(egg);
		stack.set(DataComponents.ITEM_NAME, type.getDisplayName());
		return stack;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player,
												   LivingEntity target, InteractionHand hand) {
		boolean stray = target instanceof StrayGhostEntity;
		boolean owned = target instanceof GhostEntity;
		if (!stray && !owned) return InteractionResult.PASS;
		if (player.level().isClientSide()) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
		if (sp.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
		GhostVariantHolder holder = (GhostVariantHolder) target;
		GhostVariant type = holder.getVariant();
		int stored = getPower(stack, type);
		if (owned) {
			GhostEntity ghost = (GhostEntity) target;
			if (!ghost.isOwner(sp)) {
				sp.displayClientMessage(GLLang.Ghost.NOT_YOURS.get(), true);
				return InteractionResult.FAIL;
			}
			if (stored >= GhostPower.FAN_CAPACITY) {
				return release(sp, ghost, hand);
			}
		}
		if (stored >= GhostPower.FAN_CAPACITY) {
			sp.displayClientMessage(GLLang.Ghost.FAN_FULL.get(), true);
			return InteractionResult.FAIL;
		}
		int power = ((GhostPowerHolder) target).getPower();
		if (power <= 0) {
			sp.displayClientMessage(GLLang.Ghost.NO_POWER.get(), true);
			return InteractionResult.FAIL;
		}
		int take = Math.min(power, GhostPower.FAN_CAPACITY - stored);
		if (owned) {
			GhostEntity ghost = (GhostEntity) target;
			if (!GLMeta.DOLL.type().getOrCreate(sp).absorb(sp, ghost)) {
				sp.displayClientMessage(GLLang.Ghost.ABSORB_FAIL.get(), true);
				return InteractionResult.FAIL;
			}
		} else {
			target.discard();
		}
		setPower(stack, type, stored + take);
		sp.displayClientMessage(GLLang.Ghost.ABSORBED.get(take), true);
		play(sp, 1.2F);
		sp.getCooldowns().addCooldown(this, COOLDOWN);
		sp.swing(hand, true);
		return InteractionResult.SUCCESS;
	}

	/**
	 * The full-fan escape hatch: an owned ghost that cannot be stored is set free rather than
	 * destroyed with its power still in it.
	 */
	private InteractionResult release(ServerPlayer sp, GhostEntity ghost, InteractionHand hand) {
		if (!GLMeta.DOLL.type().getOrCreate(sp).release(sp, ghost)) {
			sp.displayClientMessage(GLLang.Ghost.RELEASE_FAIL.get(), true);
			return InteractionResult.FAIL;
		}
		sp.displayClientMessage(GLLang.Ghost.RELEASED.get(), true);
		play(sp, 0.8F);
		sp.getCooldowns().addCooldown(this, COOLDOWN);
		sp.swing(hand, true);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
		if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
		GhostVariant type = getType(stack);
		int stored = getPower(stack, type);
		DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
		GhostEntity existing = att.findSummonedGhost(sp, type);
		if (existing != null) {
			int need = GhostPower.MAX - existing.getPower();
			if (need <= 0) {
				sp.displayClientMessage(GLLang.Ghost.ALREADY_FULL.get(), true);
				return InteractionResultHolder.fail(stack);
			}
			if (stored <= 0) {
				sp.displayClientMessage(GLLang.Ghost.FAN_EMPTY.get(), true);
				return InteractionResultHolder.fail(stack);
			}
			int spend = Math.min(need, stored);
			existing.addPower(spend);
			setPower(stack, type, stored - spend);
			sp.displayClientMessage(GLLang.Ghost.REFILLED.get(spend), true);
			return used(sp, stack, hand);
		}
		if (stored < GhostPower.SUMMON_COST) {
			sp.displayClientMessage(GLLang.Ghost.FAN_EMPTY.get(), true);
			return InteractionResultHolder.fail(stack);
		}
		if (att.summonCapped()) {
			sp.displayClientMessage(GLLang.Doll.TOO_MANY.get(DollAttachment.MAX_SUMMONED), true);
			return InteractionResultHolder.fail(stack);
		}
		var pos = DollSpawn.ringPos(sp, att.summonedCount(), DollAttachment.MAX_SUMMONED);
		if (!att.summonGhost(sp, type, pos)) {
			sp.displayClientMessage(GLLang.Ghost.SUMMON_FAIL.get(), true);
			return InteractionResultHolder.fail(stack);
		}
		setPower(stack, type, stored - GhostPower.SUMMON_COST);
		sp.displayClientMessage(GLLang.Ghost.SUMMONED.get(type.getDisplayName()), true);
		return used(sp, stack, hand);
	}

	private InteractionResultHolder<ItemStack> used(ServerPlayer sp, ItemStack stack, InteractionHand hand) {
		play(sp, 1.0F);
		sp.awardStat(Stats.ITEM_USED.get(this));
		sp.getCooldowns().addCooldown(this, COOLDOWN);
		sp.swing(hand, true);
		return InteractionResultHolder.sidedSuccess(stack, false);
	}

	private static void play(ServerPlayer player, float pitch) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, pitch);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.YUYUKO_FAN_LORE.get());
		list.add(GLLang.ItemYuyukoFan.ABSORB.get());
		list.add(GLLang.ItemYuyukoFan.SUMMON.get());
		list.add(GLLang.ItemYuyukoFan.RELEASE.get());
		GhostVariant selected = getType(stack);
		list.add(GLLang.ItemYuyukoFan.SELECTED.get(selected.getDisplayName()));
		list.add(GLLang.ItemGlove.WHEEL.get(L2Keys.WHEEL.map.getKey().getDisplayName())
				.withStyle(ChatFormatting.GRAY));
		for (GhostVariant type : GhostVariant.values()) {
			list.add(GLLang.ItemYuyukoFan.STORED.get(type.getDisplayName(),
					getPower(stack, type), GhostPower.FAN_CAPACITY));
		}
	}

}
