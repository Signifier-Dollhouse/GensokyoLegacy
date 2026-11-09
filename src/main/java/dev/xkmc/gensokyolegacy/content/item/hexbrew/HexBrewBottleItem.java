package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.gensokyolegacy.content.entity.misc.HexBrewBottleEntity;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import dev.xkmc.gensokyolegacy.util.PotionData;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Supplier;

public class HexBrewBottleItem extends Item {

	private final HexBrew hexBrew;
	private final Supplier<GLHexFluid> fluidSupplier;

	public HexBrewBottleItem(HexBrew hexBrew, Supplier<GLHexFluid> supplier, Properties properties) {
		super(properties.craftRemainder(Items.GLASS_BOTTLE));
		this.hexBrew = hexBrew;
		this.fluidSupplier = supplier;
	}

	public FluidStack getFluidStack(ItemStack stack) {
		FluidStack base = new FluidStack(fluidSupplier.get(), 250);
		var data = hexBrew.handler instanceof AbstractPotionHandler pot ? pot.potion(stack) : PotionData.read(stack);
		if (!data.isEmpty()) data.write(base);
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
	public int getUseDuration(ItemStack stack) {
		if (hexBrew.handler.isDrinkable()) {
			return hexBrew.handler.getUseDuration(stack);
		}
		return super.getUseDuration(stack);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		// the enchantment glint override every hex brew bottle carried as a data component
		return true;
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
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(stack, level, list, flag);
		var contents = hexBrew.handler instanceof AbstractPotionHandler pot ? pot.potion(stack) : PotionData.read(stack);
		if (!contents.isEmpty()) contents.addPotionTooltip(list::add, 1.0F);
	}

}
