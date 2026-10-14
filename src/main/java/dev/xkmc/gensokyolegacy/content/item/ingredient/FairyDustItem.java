package dev.xkmc.gensokyolegacy.content.item.ingredient;

import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

/**
 * Fairy dust sprinkled on a flower draws a ray of dust towards a character's home, and is spent.
 * <p>
 * Which home is decided by the flower: cherry blossom or poppy for the Hakurei shrine, dandelion or
 * daisy for Marisa, any tulip for Alice, cornflower for Morichika, and any other vanilla flower for
 * whichever of the four is closest — a nearest-structure search over the {@link GLStructureTagGen}
 * tags within {@link #SEARCH_RADIUS} blocks, so an ungenerated home still counts. Nothing of the
 * right kind within reach: the dust is not spent and no ray is cast, it just scatters outwards,
 * which reads as "there is nothing here".
 */
public class FairyDustItem extends Item {

	/**
	 * How far the cast reaches, in blocks.
	 */
	public static final int SEARCH_RADIUS = 50;
	public static final int COOLDOWN_TICKS = 40;

	/**
	 * Sampled off {@code textures/item/ingredient/fairy_dust.png}: a pale mint averaging {@code #70A397}
	 * and peaking at {@code #C0F5C4}, readable without washing out against bright terrain.
	 */
	public static final int DUST_COLOR = 0x98CCAD;

	/**
	 * The dust itself. Immutable, and every mote on screen is this colour, so build it once.
	 */
	private static final DustParticleOptions DUST =
			new DustParticleOptions(Vec3.fromRGB24(DUST_COLOR).toVector3f(), 1);

	/**
	 * A ray step's length, how far apart the steps start, and how many motes each one carries.
	 */
	private static final double RAY_STEP = 1.5;
	private static final int RAY_PER_STEP = 5;
	private static final double RAY_SPREAD = 0.5;
	/**
	 * Motes drift outwards from the line as they go, so the ray is a spray rather than a wire.
	 */
	private static final double RAY_DRIFT = 0.04;

	/**
	 * The failed cast: one puff per direction, each {@link #SCATTER_PER_PUFF} motes wide.
	 */
	private static final int SCATTER_PUFFS = 24;
	private static final int SCATTER_PER_PUFF = 4;
	private static final double SCATTER_RADIUS = 1.1;
	private static final double SCATTER_SPREAD = 0.3;

	public FairyDustItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		BlockPos clicked = ctx.getClickedPos();
		BlockState state = ctx.getLevel().getBlockState(clicked);
		if (!state.is(BlockTags.FLOWERS)) return InteractionResult.PASS;
		Player player = ctx.getPlayer();
		if (player == null) return InteractionResult.PASS;
		// the cooldown goes on first and on both sides, so a failed cast cannot be re-spammed and
		// the client shows the item cooling down either way
		if (!player.getCooldowns().isOnCooldown(this)) player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!(ctx.getLevel() instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
		Vec3 from = clicked.getCenter();
		BlockPos found = sl.findNearestMapStructure(targetTag(state), clicked, SEARCH_RADIUS, false);
		if (found == null) {
			// nothing of the kind nearby: scatter and keep the dust
			scatter(sl, from, sl.random);
		} else {
			castRay(sl, from, Vec3.atBottomCenterOf(found));
			if (!player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	/** The home a flower points at: every flower in {@link BlockTags#FLOWERS} resolves to one of
	 * our four, and anything unnamed goes to {@link GLStructureTagGen#CHARACTER_HOMES} so the
	 * nearest of the four wins.
	 */
	private static TagKey<Structure> targetTag(BlockState state) {
		if (state.is(Blocks.PINK_PETALS) || state.is(Blocks.POPPY)) return GLStructureTagGen.HAKUREI_SHRINE_STRUCTURE;
		if (state.is(Blocks.DANDELION) || state.is(Blocks.OXEYE_DAISY)) return GLStructureTagGen.MARISA_HOUSE_STRUCTURE;
		if (state.is(Blocks.CORNFLOWER)) return GLStructureTagGen.MORICHIKA_SHOP_STRUCTURE;
		if (state.is(Blocks.RED_TULIP) || state.is(Blocks.ORANGE_TULIP)
				|| state.is(Blocks.WHITE_TULIP) || state.is(Blocks.PINK_TULIP))
			return GLStructureTagGen.ALICE_HOUSE_STRUCTURE;
		return GLStructureTagGen.CHARACTER_HOMES;
	}

	/**
	 * The ray, one packet per {@link #RAY_STEP} blocks rather than one per mote: {@code sendParticles}
	 * scatters its count around the point given, so a handful per step reads as a beam and a full-radius
	 * cast costs a few dozen packets, dropping the rest for players out of earshot of the line.
	 */
	private static void castRay(ServerLevel sl, Vec3 from, Vec3 to) {
		if (to.y() < from.y())
			to = new Vec3(to.x, from.y, to.z);
		Vec3 dir = to.subtract(from);
		double len = dir.length();
		// the home can start under the flower being clicked, in which case there is no line to draw
		if (len < 1.0E-4) return;
		dir = dir.normalize();
		for (int i = 0; i < 20; i++) {
			Vec3 at = from.add(dir.scale(i * 0.2));
			sl.sendParticles(DUST, at.x, at.y, at.z, 1, 0, 0, 0, RAY_DRIFT);
		}
	}

	/**
	 * The failed cast. {@code sendParticles} spreads around a single point, so spraying outwards
	 * every which way is one packet per puff, each carrying its own few motes.
	 */
	private static void scatter(ServerLevel sl, Vec3 at, RandomSource rand) {
		for (int i = 0; i < SCATTER_PUFFS; i++) {
			Vec3 dir = new Vec3(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
			Vec3 puff = at.add(dir.scale(SCATTER_RADIUS));
			sl.sendParticles(DUST, puff.x, puff.y, puff.z, SCATTER_PER_PUFF,
					SCATTER_SPREAD, SCATTER_SPREAD, SCATTER_SPREAD, 0);
		}
	}

}