package dev.xkmc.gensokyolegacy.content.block.deco.donation;

import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The structure a donation box belongs to and the effect it rolls every eight coins.
 *
 * @see "doc/design/structure/donation.md"
 */
public record DonationBoxData(ResourceLocation structure, ArrayList<DonationEffect> effects) {

	public DonationBoxData(ResourceLocation structure, List<DonationEffect> effects) {
		this(structure, new ArrayList<>(effects));
	}

	/**
	 * One weighted reward: {@code level} as written, one higher than the stored amplifier.
	 */
	public record DonationEffect(MobEffect effect, int level, int weight) {

		public DonationEffect {
			level = Math.max(1, level);
			weight = Math.max(0, weight);
		}

	}

	public boolean matches(@Nullable StructureKey key) {
		return key != null && structure.equals(key.structure());
	}

	/**
	 * Weighted draw over the entries {@code allowed} accepts, or {@code null} when none are left.
	 */
	@Nullable
	public DonationEffect pick(RandomSource rand, Predicate<DonationEffect> allowed) {
		int total = 0;
		for (var e : effects) if (allowed.test(e)) total += e.weight();
		if (total <= 0) return null;
		int roll = rand.nextInt(total);
		for (var e : effects) {
			if (!allowed.test(e)) continue;
			roll -= e.weight();
			if (roll < 0) return e;
		}
		return null;
	}

	@Nullable
	public static DonationBoxData of(Block block) {
		return GLMeta.DONATION_DATA.get(BuiltInRegistries.BLOCK.wrapAsHolder(block));
	}

}
