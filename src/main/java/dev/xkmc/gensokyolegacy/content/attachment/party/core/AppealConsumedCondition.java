package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * Whether the guest standing in the loot context has been served enough appeal.
 * <p>
 * Reads {@link PartyModule#appealEaten} — the appeal <i>she personally consumed</i>, not
 * the party's {@code appealServed} and not whether she was satisfied. Those are three
 * different quantities (§5.2) and only one of them is hers: two guests at one table are
 * owed two different answers.
 * <p>
 * The guest is {@link LootContextParams#THIS_ENTITY}, which is what {@code PartyGift}
 * puts in the context when it rolls a table for her. Anything else in that slot — no
 * entity at all, a guest who somehow has no {@link PartyModule} — reads as zero appeal
 * rather than throwing, so a table that gates its whole pool on a threshold stays a table
 * that gives nothing rather than a crash inside somebody's party.
 * <p>
 * Both bounds are optional, so the three shapes a tiered table needs are all one field
 * away from each other: {@code min} alone for "at least", {@code max} alone for "at most",
 * and both for one disjoint band of a ladder.
 *
 * @param min lowest appeal that passes, inclusive
 * @param max highest appeal that passes, inclusive
 * @param invert pass when the comparison fails, as in {@code SelfReputationCondition}
 */
public record AppealConsumedCondition(int min, int max, boolean invert) implements LootItemCondition {

	public static final MapCodec<AppealConsumedCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("min", 0).forGetter(AppealConsumedCondition::min),
			Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(AppealConsumedCondition::max),
			Codec.BOOL.optionalFieldOf("invert", false).forGetter(AppealConsumedCondition::invert)
	).apply(i, AppealConsumedCondition::new));

	public AppealConsumedCondition {
		// A band that ends before it starts would silently never match, which empties a whole
		// pool and reads in game as "this gift no longer exists". Clamping turns the same typo
		// into a band of one appeal point: wrong, but visibly wrong rather than invisible.
		max = Math.max(min, max);
	}

	/** At least {@code min} appeal. */
	public AppealConsumedCondition(int min) {
		this(min, Integer.MAX_VALUE, false);
	}

	/** Between {@code min} and {@code max} appeal, both inclusive. */
	public AppealConsumedCondition(int min, int max) {
		this(min, max, false);
	}

	@Override
	public LootItemConditionType getType() {
		return GLMisc.APPEAL_CONSUMED.get();
	}

	@Override
	public boolean test(LootContext context) {
		int appeal = appealOf(context);
		return invert != (appeal >= min && appeal <= max);
	}

	@Override
	public Set<LootContextParam<?>> getReferencedContextParams() {
		return Set.of(LootContextParams.THIS_ENTITY);
	}

	/** Appeal the guest in this context has consumed, or zero if there is no guest. */
	private static int appealOf(LootContext context) {
		if (!(context.getParam(LootContextParams.THIS_ENTITY) instanceof YoukaiEntity guest)) return 0;
		return guest.getModule(PartyModule.class).map(PartyModule::appealEaten).orElse(0);
	}

}