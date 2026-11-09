package dev.xkmc.gensokyolegacy.content.trigger;

import com.google.gson.JsonObject;
import dev.xkmc.gensokyolegacy.compat.l2lib.Val;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FeedCharacterTrigger extends SimpleCriterionTrigger<FeedCharacterTrigger.TriggerInstance> {

	private final ResourceLocation id;

	public FeedCharacterTrigger(ResourceLocation id) {
		this.id = id;
	}

	public ResourceLocation getId() {
		return id;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
		return new TriggerInstance(id, player,
				EntityPredicate.fromJson(json.get("entity")),
				ItemPredicate.fromJson(json.get("item")));
	}

	public void trigger(ServerPlayer player, LivingEntity le, ItemStack item) {
		trigger(player, e -> e.matches(player, le, item));
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {

		private final @Nullable EntityPredicate entity;
		private final @Nullable ItemPredicate item;

		public TriggerInstance(ResourceLocation id, ContextAwarePredicate player,
							   @Nullable EntityPredicate entity, @Nullable ItemPredicate item) {
			super(id, player);
			this.entity = entity;
			this.item = item;
		}

		/**
		 * A criterion bound to whichever trigger id asked for it, since one class is registered under several.
		 *
		 * @param trigger registered instance of this trigger to build the criterion against
		 */
		public static TriggerInstance usedItem(Val<FeedCharacterTrigger> trigger,
											   @Nullable ContextAwarePredicate player, @Nullable EntityPredicate entity, @Nullable ItemPredicate item) {
			return new TriggerInstance(trigger.get().getId(),
					player == null ? ContextAwarePredicate.ANY : player, entity, item);
		}

		public boolean matches(ServerPlayer player, LivingEntity le, ItemStack stack) {
			return (entity == null || entity.matches(player, le)) &&
					(item == null || item.matches(stack));
		}

		@Override
		public JsonObject serializeToJson(SerializationContext ctx) {
			JsonObject ans = super.serializeToJson(ctx);
			if (entity != null) ans.add("entity", entity.serializeToJson());
			if (item != null) ans.add("item", item.serializeToJson());
			return ans;
		}

	}
}
