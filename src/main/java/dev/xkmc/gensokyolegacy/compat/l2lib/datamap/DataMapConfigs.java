package dev.xkmc.gensokyolegacy.compat.l2lib.datamap;

import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterConfig;
import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.BedData;
import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFood;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.MorichikaOfferData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftItemData;
import dev.xkmc.gensokyolegacy.content.item.gift.GiftPreference;
import dev.xkmc.gensokyolegacy.content.rpg.core.DialogConfig;
import dev.xkmc.gensokyolegacy.content.rpg.core.GiftDialogData;
import dev.xkmc.l2library.serial.config.CollectType;
import dev.xkmc.l2library.serial.config.ConfigCollect;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;

/**
 * One config type per data map. All files of a type merge by overwriting the same key, which is
 * how a data map lets a later pack replace an entry.
 */
public final class DataMapConfigs {

	private DataMapConfigs() {}

	@SerialClass
	public static final class Beds extends DataMapConfig<BedData> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, BedData> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, BedData> values() {
			return values;
		}
	}

	@SerialClass
	public static final class PartyFoods extends DataMapConfig<PartyFood> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, PartyFood> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, PartyFood> values() {
			return values;
		}
	}

	@SerialClass
	public static final class PartyTastes extends DataMapConfig<PartyTaste> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, PartyTaste> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, PartyTaste> values() {
			return values;
		}
	}

	@SerialClass
	public static final class Characters extends DataMapConfig<CharacterConfig> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, CharacterConfig> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, CharacterConfig> values() {
			return values;
		}
	}

	@SerialClass
	public static final class Dialogs extends DataMapConfig<DialogConfig> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, DialogConfig> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, DialogConfig> values() {
			return values;
		}
	}

	@SerialClass
	public static final class Structures extends DataMapConfig<StructureConfig> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, StructureConfig> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, StructureConfig> values() {
			return values;
		}
	}

	@SerialClass
	public static final class GiftPreferences extends DataMapConfig<GiftPreference> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, GiftPreference> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, GiftPreference> values() {
			return values;
		}
	}

	@SerialClass
	public static final class GiftDialogs extends DataMapConfig<GiftDialogData> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, GiftDialogData> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, GiftDialogData> values() {
			return values;
		}
	}

	@SerialClass
	public static final class GiftItems extends DataMapConfig<GiftItemData> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, GiftItemData> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, GiftItemData> values() {
			return values;
		}
	}

	@SerialClass
	public static final class MorichikaOffers extends DataMapConfig<MorichikaOfferData> {
		@SerialClass.SerialField
		@ConfigCollect(CollectType.MAP_OVERWRITE)
		public LinkedHashMap<ResourceLocation, MorichikaOfferData> values = new LinkedHashMap<>();

		@Override
		public LinkedHashMap<ResourceLocation, MorichikaOfferData> values() {
			return values;
		}
	}

}
