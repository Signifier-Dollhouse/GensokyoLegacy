package dev.xkmc.gensokyolegacy.content.entity.youkai;

import dev.xkmc.gensokyolegacy.content.spell.card.TouhouSpellCards;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.DefaultCombatManager;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiCombatManager;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.compat.l2lib.SyncedData;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

@SerialClass
public class GeneralYoukaiEntity extends SmartYoukaiEntity {

	public static AttributeSupplier.Builder createAttributes() {
		return YoukaiEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 60)
				.add(Attributes.ATTACK_DAMAGE, 8)
				.add(Attributes.FOLLOW_RANGE, 128);
	}

	public static final ResourceLocation SPELL = GensokyoLegacy.loc("ex_rumia");

	private static <T> EntityDataAccessor<T> defineId(EntityDataSerializer<T> ser) {
		return SynchedEntityData.defineId(GeneralYoukaiEntity.class, ser);
	}

	protected static final SyncedData SPELL_DATA = new SyncedData(GeneralYoukaiEntity::defineId, YOUKAI_DATA);

	public GeneralYoukaiEntity(EntityType<? extends GeneralYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel, 8);
	}

	@Override
	protected SyncedData data() {
		return SPELL_DATA;
	}

	@Override
	protected YoukaiCombatManager createCombatManager() {
		return new DefaultCombatManager(this, SPELL);
	}

	@Override
	public void initSpellCard() {
		TouhouSpellCards.set(this);
	}

}