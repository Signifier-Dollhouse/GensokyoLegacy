package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * Whether the guest has eaten enough of her own appeal — {@link PartyModule#appealEaten}, not the
 * party's total. Anything but a {@link LootContextParams#THIS_ENTITY} guest reads as zero, so a
 * gated table gives nothing, not a crash.
 *
 * @param min    lowest appeal that passes, inclusive
 * @param max    highest appeal that passes, exclusive
 * @param invert pass when the comparison fails, as in {@code SelfReputationCondition}
 * @see "doc/design/party/send.md"
 */
public record AppealConsumedCondition(int min, int max, boolean invert) implements LootItemCondition {

	public static final Serializer<AppealConsumedCondition> SERIALIZER = new Serializer<>() {
		@Override
		public void serialize(JsonObject json, AppealConsumedCondition cond, JsonSerializationContext ctx) {
			json.addProperty("min", cond.min);
			json.addProperty("max", cond.max);
			json.addProperty("invert", cond.invert);
		}

		@Override
		public AppealConsumedCondition deserialize(JsonObject json, JsonDeserializationContext ctx) {
			return new AppealConsumedCondition(
					GsonHelper.getAsInt(json, "min", 0),
					GsonHelper.getAsInt(json, "max", Integer.MAX_VALUE),
					GsonHelper.getAsBoolean(json, "invert", false));
		}
	};

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
		return guest.getModule(PartyModule.class).map(PartyModule::appealEaten).orElse(0);
	}

}