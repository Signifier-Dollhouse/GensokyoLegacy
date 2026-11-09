package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * One of the rockets the table puts up while it fills. A real {@link FireworkRocketEntity}, built
 * the way the game builds one: a rocket item carrying the {@code Fireworks} NBT tag.
 * <p>
 * <b>Where it goes up is the whole of the difficulty.</b> A burst hurts every living thing within
 * five blocks of it, and a guest knocked off her chair drops her seat claim — which holds the table
 * in GATHERING until {@link PartyTicker#PARTY_TIMEOUT} calls it off. So a rocket is fired from a
 * ring well outside {@link PartyArea#GUEST_RADIUS}, and only from a spot with nobody under it.
 *
 * @see "doc/design/party/state.md"
 */
public class PartyFirework {

	/**
	 * Nearest the rockets go up from the table: outside every guest's reach, let alone a burst's.
	 */
	private static final int RING_MIN = 12;

	/**
	 * Furthest. A ring, not a spot, so the rockets do not stack over one corner of the room.
	 */
	private static final int RING_MAX = 18;

	/**
	 * How far a burst reaches, in blocks — vanilla's firework damage radius.
	 */
	private static final int BLAST = 5;

	/**
	 * How high a burst is assumed to be. Deliberately under the dozen-odd blocks an unobstructed
	 * rocket climbs, and in the safe direction: indoors one stops at the ceiling, which is lower,
	 * and a burst higher than assumed is further from anything on the floor.
	 */
	private static final int RISE = 8;

	/**
	 * How many places are rolled before the round is given up on.
	 */
	private static final int TRIES = 6;

	/**
	 * Put one rocket up over the table, if the room has room for one. A round that finds nowhere
	 * fires nothing and is simply tried again: the next one is only ten ticks away.
	 *
	 * @param anchor the table, as the party holds it
	 */
	public static void launch(ServerLevel level, BlockPos anchor) {
		var random = level.getRandom();
		for (int i = 0; i < TRIES; i++) {
			var spot = ring(anchor, random);
			if (!clear(level, spot)) continue;
			var stack = rocket(random);
			level.addFreshEntity(new FireworkRocketEntity(level,
					spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, stack));
			return;
		}
	}

	/**
	 * A block on the ground {@link #RING_MIN}–{@link #RING_MAX} out from the table.
	 */
	private static BlockPos ring(BlockPos anchor, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2;
		int dist = RING_MIN + random.nextInt(RING_MAX - RING_MIN + 1);
		return anchor.offset((int) Math.round(Math.cos(angle) * dist), 0,
				(int) Math.round(Math.sin(angle) * dist));
	}

	/**
	 * Whether a rocket can go up here without bursting on top of somebody. The box is the whole
	 * column a burst could land in, widened by the damage radius: the ring keeps the seated guests
	 * out of reach on its own, but a guest still walking in is out as far as the scan distance, and
	 * so is the host.
	 */
	private static boolean clear(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return false;
		var box = new AABB(pos.getX() + 0.5, pos.getY() - 1.0, pos.getZ() + 0.5,
				pos.getX() + 0.5, pos.getY() + RISE, pos.getZ() + 0.5).inflate(BLAST);
		return level.getEntitiesOfClass(LivingEntity.class, box).isEmpty();
	}

	/**
	 * A rocket: two random colours, a random shape, and a trail or a flicker on some of them. The
	 * colours are the raw RGB in the explosion tag — {@link DyeColor#getFireworkColor}, not the
	 * dye's own index.
	 */
	private static ItemStack rocket(RandomSource random) {
		var explosion = new CompoundTag();
		explosion.putByte(FireworkRocketItem.TAG_EXPLOSION_TYPE,
				(byte) (random.nextBoolean() ? FireworkRocketItem.Shape.LARGE_BALL : FireworkRocketItem.Shape.STAR).getId());
		explosion.putIntArray(FireworkRocketItem.TAG_EXPLOSION_COLORS, new int[]{color(random), color(random)});
		explosion.putBoolean(FireworkRocketItem.TAG_EXPLOSION_TRAIL, random.nextInt(4) == 0);
		explosion.putBoolean(FireworkRocketItem.TAG_EXPLOSION_FLICKER, random.nextBoolean());
		var explosions = new ListTag();
		explosions.add(explosion);
		var fireworks = new CompoundTag();
		// no flight duration: it goes up and bursts, rather than climbing for two seconds first
		fireworks.putByte("Flight", (byte) 0);
		fireworks.put(FireworkRocketItem.TAG_EXPLOSIONS, explosions);
		var stack = new ItemStack(Items.FIREWORK_ROCKET);
		stack.getOrCreateTag().put("Fireworks", fireworks);
		return stack;
	}

	/**
	 * One dye colour, as the firework colour an explosion carries it in.
	 */
	private static int color(RandomSource random) {
		var dyes = DyeColor.values();
		return dyes[random.nextInt(dyes.length)].getFireworkColor();
	}

	private PartyFirework() {
	}

}