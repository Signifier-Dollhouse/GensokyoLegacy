package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.gensokyolegacy.content.entity.misc.HexBrewBottleEntity;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Supplier;

public class HexBrewBottleItem extends Item implements ProjectileItem {

	private final HexBrew hexBrew;
	private final Supplier<GLHexFluid> fluidSupplier;

	public HexBrewBottleItem(HexBrew hexBrew, Supplier<GLHexFluid> supplier, Properties properties) {
		super(properties.craftRemainder(Items.GLASS_BOTTLE));
		this.hexBrew = hexBrew;
		this.fluidSupplier = supplier;
	}

	public GLHexFluid getFluid() {
		return fluidSupplier.get();
	}

	public FluidStack getFluidStack() {
		return new FluidStack(fluidSupplier.get(), 250);
	}

	public HexBrew getHexBrew() {
		return hexBrew;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!hexBrew.isThrowable()) {
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
	public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
		HexBrewBottleEntity entity = new HexBrewBottleEntity(level, pos.x(), pos.y(), pos.z());
		entity.setItem(stack);
		return entity;
	}
}
