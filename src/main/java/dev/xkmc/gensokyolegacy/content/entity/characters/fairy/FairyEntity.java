package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiCombatManager;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.*;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The shared fairy base. A fairy has no {@link TalkModule}, unlike
 * {@link YoukaiEntity}'s default: a fairy has no dialog of her own to offer, so a
 * conversation with one only ever opened an empty screen. She is fed instead.
 * <p>
 * Subclasses that override {@link #createModules()} - the plain fairy and Cirno -
 * have to leave the talk module out for the same reason.
 * <p>
 * What a fairy does have to say is said without words: a bubble over her head every ten
 * to twenty seconds with one item in it, out of {@link FairyFeedModule#wants}.
 * <p>
 * The gift module is the one favor a fairy never loses: a hand-out is what a player has
 * before they have fed her anything, so it stays on her default list.
 * <p>
 * What she will and will not be handed is data, not this class: every fairy entity type carries the
 * same {@code gift_preference} datamap entry (spirits and strong magic refused, a frozen frog taken
 * at twice worth), and Cirno layers her own on top of it.
 *
 * @see "doc/design/want_bubble.md"
 * @see "doc/design/gift.md"
 */
@SerialClass
public class FairyEntity extends GeneralYoukaiEntity {

	public static AttributeSupplier.Builder createAttributes() {
		return GeneralYoukaiEntity.createAttributes()
				.add(Attributes.MAX_HEALTH, 20)
				.add(Attributes.ATTACK_DAMAGE, 6);
	}

	@Override
	protected List<AbstractYoukaiModule> createModules() {
		return List.of(new HomeModule(this), new VisitModule(this), new PartyModule(this),
				new GiftModule(this));
	}

	public FairyEntity(EntityType<? extends GeneralYoukaiEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	protected YoukaiCombatManager createCombatManager() {
		return new FairyCombatManager(this);
	}

}