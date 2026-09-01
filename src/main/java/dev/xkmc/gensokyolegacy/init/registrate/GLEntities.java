package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.ChairEntity;
import dev.xkmc.gensokyolegacy.content.block.deco.seat.NothingRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.CirnoEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.CirnoRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.fairy.FairyEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.*;
import dev.xkmc.gensokyolegacy.content.entity.characters.merchant.MorichikaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.merchant.MorichikaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.characters.rumia.RumiaEntity;
import dev.xkmc.gensokyolegacy.content.entity.characters.rumia.RumiaRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollRenderer;
import dev.xkmc.gensokyolegacy.content.entity.misc.FairyIce;
import dev.xkmc.gensokyolegacy.content.entity.misc.FrozenFrog;
import dev.xkmc.gensokyolegacy.content.entity.misc.HexBrewBottleEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.BossYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiRenderer;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftPreference;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftType;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.loot.EntityLootGen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.Map;

public class GLEntities {

	public static final EntityEntry<ChairEntity> CHAIR;

	public static final EntityEntry<RumiaEntity> RUMIA;
	public static final EntityEntry<ReimuEntity> REIMU;
	public static final EntityEntry<CirnoEntity> CIRNO;

	public static final EntityEntry<MaidenEntity> SANAE;
	public static final EntityEntry<MarisaEntity> MARISA;
	public static final EntityEntry<MorichikaEntity> MORICHIKA;
	public static final EntityEntry<GeneralYoukaiEntity> MYSTIA;
	public static final EntityEntry<BossYoukaiEntity> YUKARI, KOISHI;
	public static final EntityEntry<FairyEntity> SUNNY, LUNA, STAR;
	public static final EntityEntry<DollEntity> DOLL;

	public static final EntityEntry<FrozenFrog> FROZEN_FROG;
	public static final EntityEntry<FairyIce> FAIRY_ICE;
	public static final EntityEntry<HexBrewBottleEntity> HEXBREW_BOTTLE;

	static {

		GensokyoLegacy.REGISTRATE.defaultCreativeTab(CreativeModeTabs.OP_BLOCKS);

		CHAIR = GensokyoLegacy.REGISTRATE
				.<ChairEntity>entity("dining_chair", ChairEntity::new, MobCategory.MISC)
				.properties(e -> e.sized(0f, 0f))
				.renderer(() -> NothingRenderer::new)
				.register();

		{

			RUMIA = GensokyoLegacy.REGISTRATE
					.entity("rumia", RumiaEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.7f).clientTrackingRange(10))
					.attributes(RumiaEntity::createAttributes)
					.renderer(() -> RumiaRenderer::new)
					.spawnEgg(0x413734, 0xA55064).build()
					.loot(EntityLootGen::noLoot)
					.register();

			REIMU = GensokyoLegacy.REGISTRATE
					.entity("hakurei_reimu", ReimuEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> ReimuRenderer::new)
					.spawnEgg(0xa93937, 0xfaf5f2).build()
					.loot(EntityLootGen::reimu)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GiftPreference.of(Map.of(
							GiftType.DRINK, 2.0,
							GiftType.BOOK, 1.5
					)))
					.register();

			CIRNO = GensokyoLegacy.REGISTRATE
					.entity("cirno", CirnoEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(CirnoEntity::createAttributes)
					.renderer(() -> CirnoRenderer::new)
					.spawnEgg(0x5676af, 0xb6ecf1).build()
					.loot(EntityLootGen::noLoot)
					.dataMap(GLMeta.GIFT_PREFERENCE.reg(), GiftPreference.of(Map.of(
							GiftType.TOY, 2.0,
							GiftType.FOOD, 1.5,
							GiftType.BOOK, 0.5
					)))
					.register();
		}

		{
			YUKARI = GensokyoLegacy.REGISTRATE
					.entity("yukari_yakumo", BossYoukaiEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.spawnEgg(0x4B1442, 0xFFFFFF).build()
					.loot(EntityLootGen::yukari)
					.register();

			SANAE = GensokyoLegacy.REGISTRATE
					.entity("kochiya_sanae", MaidenEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.spawnEgg(0x4eaff9, 0xFFFFFF).build()
					.loot(EntityLootGen::sanae)
					.register();

			KOISHI = GensokyoLegacy.REGISTRATE
					.entity("komeiji_koishi", BossYoukaiEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.spawnEgg(0x88BA7F, 0x645856).build()
					.loot(EntityLootGen::noLoot)
					.register();

			MARISA = GensokyoLegacy.REGISTRATE
					.entity("kirisame_marisa", MarisaEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(BossYoukaiEntity::createAttributes)
					.renderer(() -> MarisaRenderer::new)
					.spawnEgg(0x52403C, 0xFAF2EF).build()
					.loot(EntityLootGen::marisa)
					.register();

			MORICHIKA = GensokyoLegacy.REGISTRATE
					.entity("morichika_rinnosuke", MorichikaEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(GeneralYoukaiEntity::createAttributes)
					.renderer(() -> MorichikaRenderer::new)
					.spawnEgg(0x52403C, 0xFAF2EF).build()
					.loot(EntityLootGen::noLoot)
					.register();

			MYSTIA = GensokyoLegacy.REGISTRATE
					.entity("mystia_lorelei", GeneralYoukaiEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(GeneralYoukaiEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.spawnEgg(0x9B6D79, 0xF4BDAE).build()
					.loot(EntityLootGen::mystia)
					.register();

			SUNNY = GensokyoLegacy.REGISTRATE
					.entity("sunny_milk", FairyEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(FairyEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.spawnEgg(0xB14435, 0xFCF5D8).build()
					.loot(EntityLootGen::noLoot)
					.register();

			LUNA = GensokyoLegacy.REGISTRATE
					.entity("luna_child", FairyEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(FairyEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.spawnEgg(0xFFF9DA, 0xA26B4F).build()
					.loot(EntityLootGen::noLoot)
					.register();

			STAR = GensokyoLegacy.REGISTRATE
					.entity("star_sapphire", FairyEntity::new, MobCategory.MONSTER)
					.properties(e -> e.sized(0.4F, 1.8f).clientTrackingRange(10))
					.attributes(FairyEntity::createAttributes)
					.renderer(() -> GeneralYoukaiRenderer::new)
					.spawnEgg(0x353D95, 0x482E25).build()
					.loot(EntityLootGen::noLoot)
					.register();
		}

		{
			DOLL = GensokyoLegacy.REGISTRATE
					.entity("doll", DollEntity::new, MobCategory.MISC)
					.properties(e -> e.sized(0.4F, 1.2F).clientTrackingRange(10))
					.attributes(DollEntity::createAttributes)
					.renderer(() -> DollRenderer::new)
					.spawnEgg(0xFFFFFF, 0x000000).build()
					.register();
		}

		{

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
		}

	}

	public static void register() {

	}

}
