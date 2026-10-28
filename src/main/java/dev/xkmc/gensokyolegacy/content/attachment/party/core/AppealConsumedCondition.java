package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * Whether the guest has eaten enough of her own appeal — {@link PartyModule#finalAppealEaten},
 * the tally with the party-size bonus, not the party's total. Anything but a
 * {@link LootContextParams#THIS_ENTITY} guest reads as zero, so a gated table gives nothing,
 * not a crash.
 *
 * @param min    lowest appeal that passes, inclusive
 * @param max    highest appeal that passes, exclusive
 * @param invert pass when the comparison fails, as in {@code SelfReputationCondition}
 * @see "doc/design/party/send.md"
 */
public record AppealConsumedCondition(int min, int max, boolean invert) implements LootItemCondition {

	public static final MapCodec<AppealConsumedCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("min", 0).forGetter(AppealConsumedCondition::min),
			Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(AppealConsumedCondition::max),
			Codec.BOOL.optionalFieldOf("invert", false).forGetter(AppealConsumedCondition::invert)
	).apply(i, AppealConsumedCondition::new));

	/**
	 * At least {@code min} appeal.
	 */
	public AppealConsumedCondition(int min) {
		this(min, 0, false);
	}

	/**
	 * Between {@code min} and {@code max} appeal, both inclusive.
	 */
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
		return invert != (appeal >= min && (max <= 0 || appeal < max));
	}

	@Override
	public Set<LootContextParam<?>> getReferencedContextParams() {
		return Set.of(LootContextParams.THIS_ENTITY);
	}

	/**
	 * Appeal the guest in this context has consumed, or zero if there is no guest.
	 */
	private static int appealOf(LootContext context) {
		if (!(context.getParam(LootContextParams.THIS_ENTITY) instanceof YoukaiEntity guest)) return 0;
		return guest.getModule(PartyModule.class).map(PartyModule::finalAppealEaten).orElse(0);
	}

}