package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.ChairEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.NothingRenderer;
import dev.xkmc.gensokyolegacy.content.entity.broom.BroomEntity;
import dev.xkmc.gensokyolegacy.content.entity.broom.BroomRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.CirnoEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.CirnoRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.FairyEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.PlainFairyEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.PlainFairyRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.*;
import dev.xkmc.gensokyolegacy.content.entity.characters.magician.AliceEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.magician.AliceRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.merchant.MorichikaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.merchant.MorichikaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.oni.SuikaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.oni.SuikaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.rumia.RumiaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.rumia.RumiaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.sakuya.SakuyaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.sakuya.SakuyaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.render.DollRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.render.IronDaggerBulletRenderer;
import dev.xkmc.gensokyolegacy.content.entity.misc.FairyIce;
import dev.xkmc.gensokyolegacy.content.entity.misc.FrozenFrog;
import dev.xkmc.gensokyolegacy.content.entity.misc.HexBrewBottleEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.EntityInteractions;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.GeneralYoukaiRenderer;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftPreference;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftType;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.loot.EntityLootGen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.Map;

public class GLEntities {

	public static final EntityEntry<ChairEntity> CHAIR;
	// summon-only vehicle, no spawn egg or natural spawn: it exists only while a
	// player rides it with a broom item in hand (see BroomItem)
	public static final EntityEntry<BroomEntity> BROOM;

	public static final EntityEntry<RumiaEntity> RUMIA;
	public static final EntityEntry<ReimuEntity> REIMU;
	public static final EntityEntry<CirnoEntity> CIRNO;

	public static final EntityEntry<MaidenEntity> SANAE;
	public static final EntityEntry<MarisaEntity> MARISA;
	public static final EntityEntry<MorichikaEntity> MORICHIKA;
	public static final EntityEntry<AliceEntity> ALICE;
	/**
	 * No home of her own: she only ever appears as a guest, see {@code GLStructureGen}.
	 */
	public static final EntityEntry<SakuyaEntity> IZAYOI_SAKUYA;
	/**
	 * Also a guest only. The mod has no bamboo forest to put an oni's house in, so she is
	 * declared at one of the magical forest houses instead - see {@code GLStructureGen}.
	 */
	public static final EntityEntry<SuikaEntity> IBUKI_SUIKA;
	public static final EntityEntry<GeneralYoukaiEntity> MYSTIA;
	public static final EntityEntry<BossYoukaiEntity> YUKARI, KOISHI;
	public static final EntityEntry<FairyEntity> SUNNY, LUNA, STAR;
	/**
	 * The unnamed fairy that actually wears the shared fairy rig, in four recolours.
	 */
	public static final EntityEntry<PlainFairyEntity> PLAIN_FAIRY;
	public static final EntityEntry<DollEntity> DOLL;

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
			// what every fairy refuses and the one thing she is glad to be handed; Cirno restates
			// these in her own entry rather than inheriting them, which the datamap cannot do
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
					.<BroomEntity>entity("broom", BroomEntity::new, MobCategory.MISC)
					.properties(e -> e.sized(0.6f, 1.8f).clientTrackingRange(10).updateInterval(2))
					.renderer(() -> BroomRenderer::new)
					.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
					.register();

			DOLL = GensokyoLegacy.REGISTRATE
					.entity("doll", DollEntity::new, MobCategory.MISC)
					.properties(e -> e.sized(0.4F, 0.9F).clientTrackingRange(10))
					.attributes(DollEntity::createAttributes)
					.renderer(() -> DollRenderer::new)
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
					.register();

			MARISA = GensokyoLegacy.REGISTRATE
					.entity("kirisame_marisa", MarisaEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> MarisaRenderer::new)
					.loot(EntityLootGen::marisa)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_MARISA)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_MARISA)
					.register();

			MORICHIKA = GensokyoLegacy.REGISTRATE
					.entity("morichika_rinnosuke", MorichikaEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(GeneralYoukaiEntity::createAttributes)
					.renderer(() -> MorichikaRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_MORICHIKA)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_MORICHIKA)
					.register();

			ALICE = GensokyoLegacy.REGISTRATE
					.entity("alice_margatroid", AliceEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(GeneralYoukaiEntity::createAttributes)
					.renderer(() -> AliceRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_ALICE)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_ALICE)
					.register();

			IZAYOI_SAKUYA = GensokyoLegacy.REGISTRATE
					.entity("izayoi_sakuya", SakuyaEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> SakuyaRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_SAKUYA)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_SAKUYA)
					.register();

			IBUKI_SUIKA = GensokyoLegacy.REGISTRATE
					.entity("ibuki_suika", SuikaEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.5F, 1.9f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> SuikaRenderer::new)
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GIFT_SUIKA)
					.dataMap(GLMeta.PARTY_TASTE.reg(), TASTE_SUIKA)
					.register();
		}

		// unused
		{

			RUMIA = GensokyoLegacy.REGISTRATE
					.entity("rumia", RumiaEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.7f).clientTrackingRange(10))
					.attributes(RumiaEntity::createAttributes)
					.renderer(() -> RumiaRenderer::new)
					.loot(EntityLootGen::noLoot)
					.register();

			YUKARI = GensokyoLegacy.REGISTRATE
					.entity("yukari_yakumo", BossYoukaiEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.loot(EntityLootGen::yukari)
					.register();

			SANAE = GensokyoLegacy.REGISTRATE
					.entity("kochiya_sanae", MaidenEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.loot(EntityLootGen::sanae)
					.register();

			KOISHI = GensokyoLegacy.REGISTRATE
					.entity("komeiji_koishi", BossYoukaiEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.loot(EntityLootGen::noLoot)
					.register();

			MYSTIA = GensokyoLegacy.REGISTRATE
					.entity("mystia_lorelei", GeneralYoukaiEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(GeneralYoukaiEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.loot(EntityLootGen::mystia)
					.register();

			SUNNY = GensokyoLegacy.REGISTRATE
					.entity("sunny_milk", FairyEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(FairyEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.loot(EntityLootGen::noLoot)
					.register();

			LUNA = GensokyoLegacy.REGISTRATE
					.entity("luna_child", FairyEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(FairyEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.loot(EntityLootGen::noLoot)
					.register();

			STAR = GensokyoLegacy.REGISTRATE
					.entity("star_sapphire", FairyEntity::new, MobCategory.CREATURE)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(FairyEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.loot(EntityLootGen::noLoot)
					.register();

		}

	}

	public static void register() {

	}

}
