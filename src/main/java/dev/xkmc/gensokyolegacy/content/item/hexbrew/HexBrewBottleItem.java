package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.gensokyolegacy.content.entity.misc.HexBrewBottleEntity;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Supplier;

public class HexBrewBottleItem extends Item implements ProjectileItem {

	private final HexBrew hexBrew;
	private final Supplier<GLHexFluid> fluidSupplier;

	public HexBrewBottleItem(HexBrew hexBrew, Supplier<GLHexFluid> supplier, Properties properties) {
		super(properties.craftRemainder(Items.GLASS_BOTTLE));
		this.hexBrew = hexBrew;
		this.fluidSupplier = supplier;
	}

	public FluidStack getFluidStack(ItemStack stack) {
		FluidStack base = new FluidStack(fluidSupplier.get(), 250);
		hexBrew.copyToFluid(stack, base);
		return base;
	}

	public HexBrew getHexBrew() {
		return hexBrew;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hexBrew.handler.isDrinkable()) {
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		if (!hexBrew.handler.isThrowable()) {
			return InteractionResultHolder.fail(stack);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!level.isClientSide) {
			HexBrewBottleEntity entity = new HexBrewBottleEntity(level, player);
			entity.setItem(stack);
			entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
			level.addFreshEntity(entity);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		if (hexBrew.handler.isDrinkable()) {
			return hexBrew.handler.getUseDuration(stack);
		}
		return super.getUseDuration(stack, entity);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		if (hexBrew.handler.isDrinkable()) {
			return hexBrew.handler.getUseAnimation(stack);
		}
		return super.getUseAnimation(stack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
		if (!hexBrew.handler.isDrinkable()) {
			return super.finishUsingItem(stack, level, livingEntity);
		}
		if (livingEntity instanceof Player player) {
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		hexBrew.handler.onDrink(livingEntity, stack, level);
		if (livingEntity instanceof Player player && player.getAbilities().instabuild) {
			return stack;
		}
		ItemStack remainder = new ItemStack(Items.GLASS_BOTTLE);
		if (stack.getCount() <= 1) {
			return remainder;
		} else {
			ItemStack copy = stack.copy();
			copy.shrink(1);
			if (livingEntity instanceof Player player && !player.getInventory().add(remainder)) {
				player.drop(remainder, false);
			}
			return copy;
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, context, list, flag);
		if (hexBrew.handler.isDrinkable()) {
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			if (contents != null && !contents.getAllEffects().iterator().hasNext()) {
				contents = null;
			}
			if (contents != null) {
				contents.addPotionTooltip(list::add, 1.0f, context.tickRate());
			}
		}
	}

	@Override
	public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
		HexBrewBottleEntity entity = new HexBrewBottleEntity(level, pos.x(), pos.y(), pos.z());
		entity.setItem(stack);
		return entity;
	}
}
