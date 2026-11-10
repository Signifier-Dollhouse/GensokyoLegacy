package dev.xkmc.gensokyolegacy.content.spell.card;

import dev.xkmc.danmakuapi.content.spell.spellcard.SpellCard;
import dev.xkmc.danmakuapi.content.spell.spellcard.SpellCardWrapper;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TouhouSpellCards {

	private static final Map<String, Supplier<SpellCard>> MAP = new ConcurrentHashMap<>();

	public static void registerSpell(String id, Supplier<SpellCard> card) {
		MAP.put(id, card);
	}

	public static void registerSpells() {
		registerSpell("hakurei_reimu", ReimuSpell::new);
		registerSpell("yukari_yakumo", YukariSpell::new);
		registerSpell("cirno", CirnoSpell::new);
		registerSpell("kochiya_sanae", SanaeSpell::new);
		registerSpell("komeiji_koishi", KoishiSpell::new);
		registerSpell("kirisame_marisa", MarisaSpell::new);
		registerSpell("mystia_lorelei", MystiaSpell::new);
		registerSpell("sunny_milk", SunnySpell::new);
		registerSpell("luna_child", LunaSpell::new);
		registerSpell("star_sapphire", StarSpell::new);
		registerSpell("izayoi_sakuya", SakuyaSpell::new);
		registerSpell("ibuki_suika", SuikaSpell::new);
	}

	public static void setSpell(GeneralYoukaiEntity e, String id) {
		e.spellCard = new SpellCardWrapper();
		e.spellCard.modelId = id;
		var sup = MAP.get(id);
		if (sup != null) e.spellCard.card = sup.get();
	}

	public static void set(GeneralYoukaiEntity e) {
		setSpell(e, BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
	}

}
