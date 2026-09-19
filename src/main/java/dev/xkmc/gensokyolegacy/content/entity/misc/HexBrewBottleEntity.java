package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrewBottleItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class HexBrewBottleEntity extends ThrowableItemProjectile {

	public HexBrewBottleEntity(EntityType<? extends HexBrewBottleEntity> type, Level level) {
		super(type, level);
	}

	public HexBrewBottleEntity(Level level, LivingEntity shooter) {
		super(GLEntities.HEXBREW_BOTTLE.get(), shooter, level);
	}

	public HexBrewBottleEntity(Level level, double x, double y, double z) {
		super(GLEntities.HEXBREW_BOTTLE.get(), x, y, z, level);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EntityEvent.DEATH) {
			double d0 = 0.08D;
			for (int i = 0; i < 8; ++i) {
				level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, getItem()),
						getX(), getY(), getZ(),
						(random.nextFloat() - 0.5D) * d0,
						(random.nextFloat() - 0.5D) * d0,
						(random.nextFloat() - 0.5D) * d0);
			}
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (level().isClientSide) return;
		ItemStack stack = getItem();
		if (!(stack.getItem() instanceof HexBrewBottleItem bottle)) {
			level().broadcastEntityEvent(this, EntityEvent.DEATH);
			discard();
			return;
		}
		HexBrew hex = bottle.getHexBrew();
		Vec3 pos = result.getLocation();
		hex.handler.onHit(level(), pos, getOwner(), stack);
		level().broadcastEntityEvent(this, EntityEvent.DEATH);
		discard();
	}

	@Override
	protected Item getDefaultItem() {
		return HexBrew.MUNDANE_HEXBREW.bottle.asItem();
	}

	/**
	 * Explosive bottles fly through allies of the thrower instead of
	 * detonating on them. Either alliance direction counts: a doll thrower
	 * covers the owner side via its own isAlliedTo, while the victim's
	 * vanilla check would not (and vice versa for teams). Other brews keep
	 * vanilla behavior so helpful splashes still land on allies.
	 */
	@Override
	protected boolean canHitEntity(Entity target) {
		if (!super.canHitEntity(target)) return false;
		Entity owner = getOwner();
		if (owner != null && getItem().getItem() instanceof HexBrewBottleItem bottle &&
				bottle.getHexBrew() == HexBrew.EXPLOSIVE_HEXBREW &&
				(target.isAlliedTo(owner) || owner.isAlliedTo(target))) {
			return false;
		}
		return true;
	}
}
