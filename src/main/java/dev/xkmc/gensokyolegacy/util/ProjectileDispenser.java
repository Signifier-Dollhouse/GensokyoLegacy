package dev.xkmc.gensokyolegacy.util;

import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * 1.20.1 has no {@code ProjectileItem} hook for dispensers, so each throwable item registers the
 * projectile factory it would have declared.
 */
public final class ProjectileDispenser {

	@FunctionalInterface
	public interface Factory {

		ThrowableItemProjectile create(Level level, Position pos);

	}

	@FunctionalInterface
	public interface ThrownFactory {

		ThrowableItemProjectile create(Level level, double x, double y, double z);

	}

	public static Factory thrown(ThrownFactory factory) {
		return (level, pos) -> factory.create(level, pos.x(), pos.y(), pos.z());
	}

	public static void register(ItemLike item, Factory factory) {
		DispenserBlock.registerBehavior(item, new AbstractProjectileDispenseBehavior() {
			@Override
			protected Projectile getProjectile(Level level, Position pos, ItemStack stack) {
				ThrowableItemProjectile projectile = factory.create(level, pos);
				projectile.setItem(stack);
				return projectile;
			}
		});
	}

	private ProjectileDispenser() {
	}

}
