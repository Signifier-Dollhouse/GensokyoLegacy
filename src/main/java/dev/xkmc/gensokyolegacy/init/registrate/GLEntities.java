package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.ChairEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.NothingRenderer;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.EntityInteractions;
import dev.xkmc.gensokyolegacy.content.entity.broom.BroomEntity;
import dev.xkmc.gensokyolegacy.content.entity.broom.BroomRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.*;
import dev.xkmc.gensokyolegacy.content.entity.characters.magician.AliceEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.magician.AliceRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.MarisaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.MarisaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.ReimuEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.ReimuRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.merchant.MorichikaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.merchant.MorichikaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.oni.SuikaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.oni.SuikaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.sakuya.SakuyaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.sakuya.SakuyaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.SekibankiHeadEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.render.DollRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.render.GhostRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.render.IronDaggerBulletRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.render.SekibankiHeadRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.render.StrayDollRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.render.StrayGhostRenderer;
import dev.xkmc.gensokyolegacy.content.entity.misc.FairyIce;
import dev.xkmc.gensokyolegacy.content.entity.misc.FrozenFrog;
import dev.xkmc.gensokyolegacy.content.entity.misc.HexBrewBottleEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftPreference;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftType;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.data.loot.EntityLootGen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.Map;

public class GLEntities {

	public static final EntityEntry<ReimuEntity> REIMU;
	public static final EntityEntry<CirnoEntity> CIRNO;
	public static final EntityEntry<MarisaEntity> MARISA;
	public static final EntityEntry<MorichikaEntity> MORICHIKA;
	public static final EntityEntry<AliceEntity> ALICE;
	public static final EntityEntry<SakuyaEntity> IZAYOI_SAKUYA;
	public static final EntityEntry<SuikaEntity> IBUKI_SUIKA;

	public static final EntityEntry<PlainFairyEntity> PLAIN_FAIRY;

	public static final EntityEntry<DollEntity> DOLL;
	public static final EntityEntry<SekibankiHeadEntity> SEKIBANKI_HEAD;
	public static final EntityEntry<StrayDollEntity> STRAY_DOLL;
	public static final EntityEntry<StrayGhostEntity> STRAY_GHOST;
	public static final EntityEntry<GhostEntity> GHOST;

	public static final EntityEntry<ChairEntity> CHAIR;
	public static final EntityEntry<BroomEntity> BROOM;
	public static final EntityEntry<FrozenFrog> FROZEN_FROG;
	public static final EntityEntry<FairyIce> FAIRY_ICE;
	public static final EntityEntry<HexBrewBottleEntity> HEXBREW_BOTTLE;
	public static final EntityEntry<IronDaggerBulletEntity> IRON_DAGGER;


	// No character has a spawn egg: every mob is met in the world or summoned in-game, never placed.
	static {

		PartyTaste TASTE_CIRNO, TASTE_REIMU, TASTE_MARISA, TASTE_MORICHIKA,
				TASTE_ALICE, TASTE_SAKUYA, TASTE_SUIKA;

		GiftPreference GIFT_CIRNO, GIFT_REIMU, GIFT_MARISA, GIFT_MORICHIKA,
				GIFT_ALICE, GIFT_SAKUYA, GIFT_SUIKA, GIFT_FAIRY;

		// taste
		{

			// weight, minRep, maxRep, repBonus, minAppeal, maxAppeal, fulfillment,
			// hungrySeat, fullSeat, leaveChance, repGain.
			TASTE_CIRNO = new PartyTaste(100, 0, 150, 1, 4, 20, 6, 60, 160, 0.05, 0.05);
			TASTE_REIMU = new PartyTaste(100, 0, 200, 1, 50, 300, 20, 60, 160, 0.05, 0.05);
			TASTE_MARISA = new PartyTaste(100, 0, 200, 1, 100, 400, 20, 60, 160, 0.05, 0.05);
			TASTE_MORICHIKA = new PartyTaste(100, 0, 200, 1, 100, 400, 20, 60, 160, 0.05, 0.05);
			TASTE_ALICE = new PartyTaste(100, 0, 250, 1, 150, 500, 12, 60, 160, 0.05, 0.05);
			TASTE_SAKUYA = new PartyTaste(100, 0, 250, 1, 200, 500, 20, 60, 160, 0.05, 0.05);
			// they eat a lot
			TASTE_SUIKA = new PartyTaste(10, 0, 250, 1, 500, 1000, 400, 60, 160, 0.05, 0.01);
		}

		//gift
		{
			// what every fairy refuses and what she takes gladly; Cirno raises only her frog off this
			// rather than restating it, which the datamap would not merge
			GIFT_FAIRY = GiftPreference.of(Map.of(
					GiftType.TOY, 2.0,
					GiftType.FOOD, 1.5,
					GiftType.BOOK, 0.0,
					GiftType.ALCOHOL, 0.0,
					GiftType.MAGIC, 0.0,
					GiftType.COLLECTION, 0.5,
					GiftType.TOOL, 0.0,
					GiftType.FROG, 1.5
			));
			GIFT_CIRNO = GIFT_FAIRY.and(GiftType.FROG, 3.0);
			GIFT_REIMU = GiftPreference.of(Map.of(
					GiftType.ALCOHOL, 2.0,
					GiftType.DRINK, 2.0,
					GiftType.BOOK, 1.5,
					GiftType.FOOD, 1.5,
					GiftType.COLLECTION, 1.5
			));
			GIFT_MARISA = GiftPreference.of(Map.of(
					GiftType.ALCOHOL, 2.0,
					GiftType.MAGIC, 2.0,
					GiftType.BOOK, 2.0,
					GiftType.COLLECTION, 1.5
			));
			GIFT_ALICE = GiftPreference.of(Map.of(
					GiftType.BOOK, 2.0,
					GiftType.TOY, 1.5,
					GiftType.DRINK, 1.5,
					GiftType.MAGIC, 1.5,
					GiftType.COLLECTION, 1.5
			));
			GIFT_SAKUYA = GiftPreference.of(Map.of(
					GiftType.DRINK, 2.0,
					GiftType.TOOL, 1.5
			));
			GIFT_MORICHIKA = GiftPreference.of(Map.of(
					GiftType.ALCOHOL, 1.5,
					GiftType.DRINK, 1.5,
					GiftType.TOOL, 1.5,
					GiftType.BOOK, 1.5,
					GiftType.MAGIC, 0.5
			));
			GIFT_SUIKA = GiftPreference.of(Map.of(
					GiftType.ALCOHOL, 3.0,
					GiftType.FOOD, 1.5,
					GiftType.COLLECTION, 0.5,
					GiftType.TOY, 0.0,
					GiftType.MAGIC, 0.0,
					GiftType.BOOK, 0.0
			));
		}

		// misc
		{
			CHAIR = GensokyoLegacy.REGISTRATE
					.<ChairEntity>entity("dining_chair", ChairEntity::new, MobCategory.MISC)
					.properties(e -> e.sized(0f, 0f))
					.renderer(() -> NothingRenderer::new)
					.register();

			BROOM = GensokyoLegacy.REGISTRATE
					.entity("broom", BroomEntity::new, MobCategory.MISC)
					.properties(e -> e.sized(0.6f, 1.8f).clientTrackingRange(10).updateInterval(2))
					.renderer(() -> BroomRenderer::new)
					.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE, GLTagGen.CARRYON_BLACKLIST)
					.register();

			DOLL = GensokyoLegacy.REGISTRATE
					.entity("doll", DollEntity::new, MobCategory.MISC)
					.properties(e -> e.sized(0.4F, 0.9F).clientTrackingRange(10))
					.attributes(DollEntity::createAttributes)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.renderer(() -> DollRenderer::new)
					.register();

			SEKIBANKI_HEAD = GensokyoLegacy.REGISTRATE
					.entity("sekibanki_head", SekibankiHeadEntity::new, MobCategory.MISC)
					.properties(e -> e.sized(0.4F, 0.4F).clientTrackingRange(10))
					.attributes(DollEntity::createAttributes)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.renderer(() -> SekibankiHeadRenderer::new)
					.register();

			STRAY_DOLL = GensokyoLegacy.REGISTRATE
					.entity("stray_doll", StrayDollEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 0.9F).clientTrackingRange(10))
					.attributes(StrayDollEntity::createAttributes)
					.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
					.loot(EntityLootGen::noLoot)
					.spawnEgg(0xE6D5B8, 0x8B6B4A).build()
					.renderer(() -> StrayDollRenderer::new)
					.register();

			STRAY_GHOST = GensokyoLegacy.REGISTRATE
					.entity("stray_ghost", StrayGhostEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 0.9F).clientTrackingRange(10))
					.attributes(StrayGhostEntity::createAttributes)
					.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
					.loot(EntityLootGen::noLoot)
					.spawnEgg(0xBFD8E6, 0x5A7D8C).build()
					.renderer(() -> StrayGhostRenderer::new)
					.register();

			GHOST = GensokyoLegacy.REGISTRATE
					.entity("ghost", GhostEntity::new, MobCategory.MISC)
					.properties(e -> e.sized(0.4F, 0.9F).clientTrackingRange(10))
					.attributes(DollEntity::createAttributes)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.renderer(() -> GhostRenderer::new)
					.register();

			FROZEN_FROG = GensokyoLegacy.REGISTRATE
					.<FrozenFrog>entity("frozen_frog", FrozenFrog::new, MobCategory.MISC)
					.properties(p -> p.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10))
					.renderer(() -> ThrownItemRenderer::new)
					.register();

			FAIRY_ICE = GensokyoLegacy.REGISTRATE
					.<FairyIce>entity("fairy_ice_crystal", FairyIce::new, MobCategory.MISC)
					.properties(p -> p.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10))
					.renderer(() -> ThrownItemRenderer::new)
					.register();

			HEXBREW_BOTTLE = GensokyoLegacy.REGISTRATE
					.<HexBrewBottleEntity>entity("hexbrew_bottle", HexBrewBottleEntity::new, MobCategory.MISC)
					.properties(p -> p.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10))
					.renderer(() -> ThrownItemRenderer::new)
					.register();

			// the thrown iron dagger: same danmaku bullet, but it returns to its owner
			// instead of vanishing, so it needs its own entity type
			IRON_DAGGER = GensokyoLegacy.REGISTRATE
					.<IronDaggerBulletEntity>entity("iron_dagger", IronDaggerBulletEntity::new, MobCategory.MISC)
					.properties(p -> p.sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(1 << 16))
					.renderer(() -> IronDaggerBulletRenderer::new)
					.register();
		}

		// characters
		{

			PLAIN_FAIRY = GensokyoLegacy.REGISTRATE
					.entity("plain_fairy", PlainFairyEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(FairyEntity::createAttributes)
					.renderer(() -> PlainFairyRenderer::new)
					.loot(EntityLootGen::noLoot)
					.spawnPlacement(SpawnPlacementTypes.ON_GROUND,
							Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PlainFairyEntity::checkFairySpawnRules,
							RegisterSpawnPlacementsEvent.Operation.REPLACE)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_FAIRY)
					.register();
			EntityInteractions.register(PLAIN_FAIRY.getId(), PlainFairyEntity::interactions);

			CIRNO = GensokyoLegacy.REGISTRATE
					.entity("cirno", CirnoEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(CirnoEntity::createAttributes)
					.renderer(() -> CirnoRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_CIRNO)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_CIRNO)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.register();

			EntityInteractions.register(CIRNO.getId(), CirnoEntity::interactions);

			REIMU = GensokyoLegacy.REGISTRATE
					.entity("hakurei_reimu", ReimuEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> ReimuRenderer::new)
					.loot(EntityLootGen::reimu)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_REIMU)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_REIMU)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.register();

			MARISA = GensokyoLegacy.REGISTRATE
					.entity("kirisame_marisa", MarisaEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> MarisaRenderer::new)
					.loot(EntityLootGen::marisa)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_MARISA)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_MARISA)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.register();

			MORICHIKA = GensokyoLegacy.REGISTRATE
					.entity("morichika_rinnosuke", MorichikaEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(GeneralYoukaiEntity::createAttributes)
					.renderer(() -> MorichikaRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_MORICHIKA)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_MORICHIKA)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.register();

			ALICE = GensokyoLegacy.REGISTRATE
					.entity("alice_margatroid", AliceEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(GeneralYoukaiEntity::createAttributes)
					.renderer(() -> AliceRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_ALICE)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_ALICE)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.register();

			IZAYOI_SAKUYA = GensokyoLegacy.REGISTRATE
					.entity("izayoi_sakuya", SakuyaEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> SakuyaRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_SAKUYA)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_SAKUYA)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.register();

			IBUKI_SUIKA = GensokyoLegacy.REGISTRATE
					.entity("ibuki_suika", SuikaEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.5F, 1.9f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> SuikaRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_SUIKA)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_SUIKA)
					.tag(GLTagGen.CARRYON_BLACKLIST)
					.register();
		}

	}

	public static void register() {

	}

}
