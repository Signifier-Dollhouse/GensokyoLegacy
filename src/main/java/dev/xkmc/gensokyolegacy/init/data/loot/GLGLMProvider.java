package dev.xkmc.gensokyolegacy.init.data.loot;

import dev.xkmc.gensokyolegacy.compat.l2lib.AddItemModifier;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;

public class GLGLMProvider extends GlobalLootModifierProvider {

	public static void register() {

	}

	public GLGLMProvider(PackOutput output) {
		super(output, GensokyoLegacy.MODID);
	}

	@Override
	protected void start() {
		add("cirno_frozen_frog_cold", create(GLItems.FROZEN_FROG_COLD.get(),
				killedByIce(), frog(FrogVariant.COLD)));
		add("cirno_frozen_frog_warm", create(GLItems.FROZEN_FROG_WARM.get(),
				killedByIce(), frog(FrogVariant.WARM)));
		add("cirno_frozen_frog_temperate", create(GLItems.FROZEN_FROG_TEMPERATE.get(),
				killedByIce(), frog(FrogVariant.TEMPERATE)));


	}

	private static LootItemCondition killer(EntityType<?> type) {
		return LootItemEntityPropertyCondition.hasProperties(
				LootContext.EntityTarget.KILLER,
				EntityPredicate.Builder.entity().entityType(
						EntityTypePredicate.of(type))).build();
	}


	private static LootItemCondition entity(EntityType<?> type) {
		return LootItemEntityPropertyCondition.hasProperties(
				LootContext.EntityTarget.THIS,
				EntityPredicate.Builder.entity().entityType(
						EntityTypePredicate.of(type))).build();
	}

	/**
	 * 1.20.1 has no frog-variant sub-predicate, so the variant is read straight out of the frog's saved data.
	 */
	private static LootItemCondition frog(FrogVariant type) {
		CompoundTag tag = new CompoundTag();
		tag.putString("variant", BuiltInRegistries.FROG_VARIANT.getKey(type).toString());
		return LootItemEntityPropertyCondition.hasProperties(
				LootContext.EntityTarget.THIS,
				EntityPredicate.Builder.entity().of(EntityType.FROG).nbt(new NbtPredicate(tag))
		).build();
	}

	private static LootItemCondition entity(TagKey<EntityType<?>> tag) {
		return LootItemEntityPropertyCondition.hasProperties(
				LootContext.EntityTarget.THIS,
				EntityPredicate.Builder.entity().entityType(
						EntityTypePredicate.of(tag))).build();
	}

	private static LootItemCondition killedByIce() {
		var cirno = LootItemEntityPropertyCondition.hasProperties(
				LootContext.EntityTarget.KILLER,
				EntityPredicate.Builder.entity().entityType(
						EntityTypePredicate.of(GLEntities.CIRNO.get())));
		var ice = DamageSourceCondition.hasDamageSource(
				DamageSourcePredicate.Builder.damageType()
						.direct(EntityPredicate.Builder.entity().entityType(
								EntityTypePredicate.of(GLEntities.FAIRY_ICE.get()))));
		return cirno.or(ice).build();
	}

	private static LootItemCondition fire(boolean fire) {
		return LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
				EntityPredicate.Builder.entity().flags(
						EntityFlagsPredicate.Builder.flags().setOnFire(fire).build())).build();
	}

	private static AddItemModifier create(Item item, LootItemCondition... conditions) {
		return new AddItemModifier(item, null, conditions);
	}

}
