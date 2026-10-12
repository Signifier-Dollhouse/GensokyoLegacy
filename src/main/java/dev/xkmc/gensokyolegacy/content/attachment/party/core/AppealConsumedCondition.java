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
 * Whether the guest in the loot context has been served enough of her own appeal — her
 * {@link PartyModule#appealEaten}, not the party's total. The guest is
 * {@link LootContextParams#THIS_ENTITY}; anything else there reads as zero, so a fully
 * gated table gives nothing instead of crashing.
 *
 * @param min lowest appeal that passes, inclusive
 * @param max highest appeal that passes, inclusive
 * @param invert pass when the comparison fails, as in {@code SelfReputationCondition}
 * @see doc/design/party/send.md
 */
public record AppealConsumedCondition(int min, int max, boolean invert) implements LootItemCondition {

	public static final MapCodec<AppealConsumedCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("min", 0).forGetter(AppealConsumedCondition::min),
			Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(AppealConsumedCondition::max),
			Codec.BOOL.optionalFieldOf("invert", false).forGetter(AppealConsumedCondition::invert)
	).apply(i, AppealConsumedCondition::new));

	public AppealConsumedCondition {
		// a band ending before it starts would silently never match and empty a whole pool
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