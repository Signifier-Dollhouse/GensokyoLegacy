package dev.xkmc.gensokyolegacy.init.data;

import dev.xkmc.gensokyolegacy.compat.l2lib.datamap.DataMapConfigs;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.rpg.QuestDialogData;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItemDataMaps;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2library.serial.config.ConfigDataProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;

/**
 * Emits the 1.20.1 data maps as l2library configs. The file layout is
 * {@code data/<ns>/<modid>_config/<map>/<id>.json}, one file per map so a pack can replace one
 * map without touching the others.
 */
public class GLConfigGen extends ConfigDataProvider {

	private final QuestDialogData[] dialogs;

	public GLConfigGen(DataGenerator generator, QuestDialogData... dialogs) {
		super(generator, "Gensokyo Legacy Config");
		this.dialogs = dialogs;
	}

	@Override
	public void add(Collector collector) {
		GLStructureGen.dataMap(collector);

		var dialog = new DataMapConfigs.Dialogs();
		var giftDialog = new DataMapConfigs.GiftDialogs();
		for (var data : dialogs) {
			data.defaultDialogs().forEach((k, v) -> dialog.values.put(BuiltInRegistries.ENTITY_TYPE.getKey(k), v));
			data.giftDialogs().forEach((k, v) -> giftDialog.values.put(BuiltInRegistries.ENTITY_TYPE.getKey(k), v));
		}
		collector.add(GLMeta.DIALOG_DATA.entry(), GensokyoLegacy.loc("main"), dialog);
		collector.add(GLMeta.GIFT_DIALOG_DATA.entry(), GensokyoLegacy.loc("main"), giftDialog);

		var tastes = new DataMapConfigs.PartyTastes();
		var gifts = new DataMapConfigs.GiftPreferences();
		GLEntities.dataMaps().forEach(e -> {
			var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.entity().get());
			if (e.taste() != null) tastes.values.put(key, e.taste());
			if (e.gift() != null) gifts.values.put(key, e.gift());
		});
		collector.add(GLMeta.PARTY_TASTE.entry(), GensokyoLegacy.loc("main"), tastes);
		collector.add(GLMeta.GIFT_PREFERENCE.entry(), GensokyoLegacy.loc("main"), gifts);

		var giftItems = new DataMapConfigs.GiftItems();
		var offers = new DataMapConfigs.MorichikaOffers();
		GLItemDataMaps.entries().forEach(e -> {
			var key = BuiltInRegistries.ITEM.getKey(e.entry().get().asItem());
			if (e.gift() != null) giftItems.values.put(key, e.gift());
			if (e.offer() != null) offers.values.put(key, e.offer());
		});
		collector.add(GLMeta.GIFT_DATA.entry(), GensokyoLegacy.loc("main"), giftItems);
		collector.add(GLMeta.MORICHIKA_OFFER.entry(), GensokyoLegacy.loc("main"), offers);
	}

}
