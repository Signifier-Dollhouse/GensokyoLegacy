package dev.xkmc.gensokyolegacy.init.registrate;

import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.danmakuapi.content.item.SpellItem;
import dev.xkmc.danmakuapi.init.data.DanmakuTagGen;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollInventory;
import dev.xkmc.gensokyolegacy.content.block.deco.shelf.MorichikaOfferData;
import dev.xkmc.gensokyolegacy.content.block.functional.portal.PortalSide;
import dev.xkmc.gensokyolegacy.content.client.model.*;
import dev.xkmc.gensokyolegacy.content.item.tool.BroomItem;
import dev.xkmc.gensokyolegacy.content.item.character.*;
import dev.xkmc.gensokyolegacy.content.item.debug.DebugGlasses;
import dev.xkmc.gensokyolegacy.content.item.debug.DebugWand;
import dev.xkmc.gensokyolegacy.content.item.debug.DoorDebugItem;
import dev.xkmc.gensokyolegacy.content.item.debug.StructureWand;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItemData;
import dev.xkmc.gensokyolegacy.content.item.doll.DollLanceItem;
import dev.xkmc.gensokyolegacy.content.item.gift.*;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveModel;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.StarDanmakuItem;
import dev.xkmc.gensokyolegacy.content.item.ingredient.FairyIceItem;
import dev.xkmc.gensokyolegacy.content.item.ingredient.FrozenFrogItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveItem;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveMode;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveModel;
import dev.xkmc.gensokyolegacy.content.item.tool.*;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaMode;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaSlots;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaTravelData;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaUnlock;
import dev.xkmc.gensokyolegacy.content.spell.item.*;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLDecoBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import dev.xkmc.l2core.init.reg.simple.DCReg;
import dev.xkmc.l2core.init.reg.simple.DCVal;
import dev.xkmc.l2core.init.reg.simple.EnumCodec;
import dev.xkmc.l2itemselector.init.data.L2ISTagGen;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.UUID;

public class GLItems {

	public static final SimpleEntry<CreativeModeTab> TAB;

	public static final ItemEntry<FairyIceItem> FAIRY_ICE_CRYSTAL;
	public static final ItemEntry<FrozenFrogItem> FROZEN_FROG_COLD, FROZEN_FROG_WARM, FROZEN_FROG_TEMPERATE;
	public static final ItemEntry<Item> MYSTICAL_STRAW;

	public static final ItemEntry<Item> HAKUREI_GOHEI;

	public static final ItemEntry<SpellItem> REIMU_SPELL, MARISA_SPELL, SANAE_SPELL, YUKARI_SPELL_BUTTERFLY, YUKARI_SPELL_LASER, MYSTIA_SPELL;

	public static final ItemEntry<StrawHatItem> STRAW_HAT;
	public static final ItemEntry<SuwakoHatItem> SUWAKO_HAT;
	public static final ItemEntry<KoishiHatItem> KOISHI_HAT;

	public static final ItemEntry<MiniFurnace1> MINI_FURNACE_1;
	public static final ItemEntry<CentiPickaxe> CENTIPICKAXE;
	public static final ItemEntry<Dowser> DOWSER;
	public static final ItemEntry<Item> DOWSER_LEFT, DOWSER_RIGHT;
	public static final ItemEntry<MermaidPearl> MERMAID_PEARL;
	public static final ItemEntry<CatBell> CAT_BELL;
	public static final ItemEntry<IronDaggerItem> IRON_DAGGER;
	public static final ItemEntry<DaggerGloveItem> DAGGER_GLOVE;

	public static final ItemEntry<TenguSakeItem> TENGU_SAKE;
	public static final ItemEntry<DrinkGiftItem> GHOST_SAKE;
	public static final ItemEntry<Item> FAIRY_CAKE;
	public static final ItemEntry<MagicBookItem> MAGIC_BOOK;

	public static final ItemEntry<DebugGlasses> DEBUG_GLASSES;
	public static final ItemEntry<DebugWand> DEBUG_WAND;
	public static final ItemEntry<StructureWand> STRUCTURE_WAND;
	public static final ItemEntry<DoorDebugItem> DOOR_DEBUG_WAND;

	public static final ItemEntry<BorderUmbrellaItem> BORDER_UMBRELLA;

	public static final ItemEntry<BroomItem> BROOM;

	public static final ItemEntry<DollItem> DOLL;

	public static final ItemEntry<DollGloveItem> DOLL_GLOVE;

	public static final ItemEntry<DollLanceItem> DOLL_LANCE;

	public static final ItemEntry<StarDanmakuItem> STAR;
	public static final ItemEntry<StarWandItem> STAR_WAND;
	public static final ItemEntry<StrangeGlassesItem> STRANGE_GLASSES;

	private static final DCReg DC = DCReg.of(GensokyoLegacy.REG);
	public static final DCVal<MiniFurnace1.Data> DC_FURNACE_1 = DC.reg("mini_furnace_1_data", MiniFurnace1.Data.class, false);
	public static final DCVal<UUID> DC_UUID = DC.uuid("uuid");
	public static final DCVal<PortalSide> DC_PORTAL_SIDE = DC.enumVal("portal_side", EnumCodec.of(PortalSide.class, PortalSide.values()));
	public static final DCVal<UUID> DC_DEBUG_YOUKAI = DC.uuid("debug_youkai");
	public static final DCVal<ResourceLocation> DC_OFFER = DC.loc("offer");
	public static final DCVal<BorderUmbrellaSlots> UMBRELLA_SLOTS = DC.reg("border_umbrella_slots", BorderUmbrellaSlots.class, false);
	public static final DCVal<Integer> UMBRELLA_SLOT_SELECTED = DC.intVal("border_umbrella_slot_selected");
	public static final DCVal<BorderUmbrellaMode> UMBRELLA_TYPE = DC.enumVal("border_umbrella_type", EnumCodec.of(BorderUmbrellaMode.class, BorderUmbrellaMode.values()));
	public static final DCVal<Unit> UMBRELLA_ICON = DC.unit("border_umbrella_icon");
	public static final DCVal<BorderUmbrellaUnlock> UMBRELLA_UNLOCK = DC.reg("border_umbrella_unlock", BorderUmbrellaUnlock.class, false);
	public static final DCVal<BorderUmbrellaTravelData> UMBRELLA_TRAVEL = DC.reg("border_umbrella_travel", BorderUmbrellaTravelData.class, false);
	public static final DCVal<Integer> UMBRELLA_DISTANCE = DC.intVal("border_umbrella_distance");
	public static final DCVal<DollItemData> DOLL_DATA = DC.reg("doll_item_data", DollItemData.class, false);
	public static final DCVal<DollInventory> DOLL_LOADOUT = DC.reg("doll_loadout", DollInventory.class, false);
	public static final DCVal<DyeColor> DOLL_COLOR = DC.enumVal("doll_color", EnumCodec.of(DyeColor.class, DyeColor.values()));
	public static final DCVal<Integer> DOLL_GLOVE_MODE = DC.intVal("doll_glove_mode");
	public static final DCVal<Unit> DOLL_GLOVE_ICON = DC.unit("doll_glove_icon");
	public static final DCVal<DaggerGloveMode> DAGGER_GLOVE_MODE = DC.enumVal("dagger_glove_mode", EnumCodec.of(DaggerGloveMode.class, DaggerGloveMode.values()));
	public static final DCVal<ResourceLocation> DAGGER_GLOVE_RUNE = DC.loc("dagger_glove_rune");
	public static final DCVal<Unit> DAGGER_GLOVE_ICON = DC.unit("dagger_glove_icon");


	static {
		var reg = GensokyoLegacy.REGISTRATE;

		// hidden
		{
			// gifts
			{
				TENGU_SAKE = reg.item("tengu_sake", TenguSakeItem::new)
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/gift/" + ctx.getName())))
						.dataMap(GLMeta.GIFT_DATA.reg(), new GiftItemData(5, 1000, GiftType.DRINK))
						.lang("Tengu Sake").register();

				// TODO placeholder favor / cooldown
				GHOST_SAKE = reg.item("ghost_sake", DrinkGiftItem::new)
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/gift/" + ctx.getName())))
						.dataMap(GLMeta.GIFT_DATA.reg(), new GiftItemData(5, 1000, GiftType.DRINK))
						.lang("Ghost Sake").register();

				FAIRY_CAKE = reg.item("fairy_cake", p -> new Item(p.stacksTo(1)
								.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build())))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/gift/" + ctx.getName())))
						.dataMap(GLMeta.GIFT_DATA.reg(), new GiftItemData(3, 1000, GiftType.FOOD))

						.lang("Fairy Cake").register();

				MAGIC_BOOK = reg.item("magic_book", MagicBookItem::new)
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/gift/" + ctx.getName())))
						.dataMap(GLMeta.GIFT_DATA.reg(), new GiftItemData(6, 1000, GiftType.BOOK))
						.dataMap(NeoForgeDataMaps.FURNACE_FUELS, new FurnaceFuel(40000))

						.lang("Obscure Magic Book").register();
			}


			STAR = reg.item("star_danmaku", p -> new StarDanmakuItem(p.rarity(Rarity.RARE)))
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/hexbrew/star")))
					.tag(DanmakuItems.Bullet.STAR.tag)
					.register();

			// spell cards
			{
				REIMU_SPELL = reg
						.item("spell_reimu", p -> new SpellItem(
								p.stacksTo(1), ReimuItemSpell::new, true,
								() -> DanmakuItems.Bullet.CIRCLE.get(DyeColor.RED).get()))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/spell/" + ctx.getName())))
						.lang("Reimu's Spellcard \"Innate Dream\"")
						.tag(DanmakuTagGen.PRESET_SPELL)
						.register();

				MARISA_SPELL = reg
						.item("spell_marisa", p -> new SpellItem(
								p.stacksTo(1), MarisaItemSpell::new, false,
								() -> DanmakuItems.Laser.LASER.get(DyeColor.WHITE).get()))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/spell/" + ctx.getName())))
						.lang("Marisa's Spellcard \"Master Spark\"")
						.tag(DanmakuTagGen.PRESET_SPELL)
						.register();

				SANAE_SPELL = reg
						.item("spell_sanae", p -> new SpellItem(
								p.stacksTo(1), SanaeItemSpell::new, false,
								() -> DanmakuItems.Bullet.SPARK.get(DyeColor.GREEN).get()))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/spell/" + ctx.getName())))
						.lang("Sanae's Spellcard \"Inherited Ritual\"")
						.tag(DanmakuTagGen.PRESET_SPELL)
						.register();

				YUKARI_SPELL_BUTTERFLY = reg
						.item("spell_yukari_butterfly", p -> new SpellItem(
								p.stacksTo(1), YukariItemSpellButterfly::new, false,
								() -> DanmakuItems.Bullet.BUTTERFLY.get(DyeColor.MAGENTA).get()))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/spell/spell_yukari")))
						.lang("Barrier \"Double Black Death Butterfly\"")
						.tag(DanmakuTagGen.PRESET_SPELL)
						.register();

				YUKARI_SPELL_LASER = reg
						.item("spell_yukari_laser", p -> new SpellItem(
								p.stacksTo(1), YukariItemSpellLaser::new, false,
								() -> DanmakuItems.Laser.LASER.get(DyeColor.RED).get()))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/spell/spell_yukari")))
						.lang("Barrier \"Mesh of Light & Darkness\"")
						.tag(DanmakuTagGen.PRESET_SPELL)
						.register();

				MYSTIA_SPELL = reg
						.item("spell_mystia", p -> new SpellItem(
								p.stacksTo(1), MystiaItemSpell::new, false,
								() -> DanmakuItems.Bullet.MENTOS.get(DyeColor.GREEN).get()))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/spell/" + ctx.getName())))
						.lang("Night Sparrow \"Midnight Chorus Master\"")
						.tag(DanmakuTagGen.PRESET_SPELL)
						.register();
			}

			// ice
			{
				FAIRY_ICE_CRYSTAL = reg.item("fairy_ice_crystal", FairyIceItem::new)
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/ingredient/" + ctx.getName())))
						.register();
				FROZEN_FROG_COLD = reg.item("frozen_frog_cold",
								p -> new FrozenFrogItem(p.stacksTo(16), FrogVariant.COLD))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/ingredient/" + ctx.getName())))
						.register();
				FROZEN_FROG_WARM = reg.item("frozen_frog_warm",
								p -> new FrozenFrogItem(p.stacksTo(16), FrogVariant.WARM))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/ingredient/" + ctx.getName())))
						.register();
				FROZEN_FROG_TEMPERATE = reg.item("frozen_frog_temperate",
								p -> new FrozenFrogItem(p.stacksTo(16), FrogVariant.TEMPERATE))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/ingredient/" + ctx.getName())))
						.register();
			}

			MYSTICAL_STRAW = reg.item("mystical_straw", Item::new)
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/ingredient/" + ctx.getName())))
					.register();

			// 博丽的御币：装饰物品，手持方式同原版剑
			HAKUREI_GOHEI = reg.item("hakurei_gohei", Item::new)
					.model((ctx, pvd) -> pvd.handheld(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.lang("Hakurei Gohei").register();
		}

		GLEffects.register();
		GLParticles.register();
		GLDecoBlocks.register();
		GLNaturalBlocks.register();

		TAB = reg.buildModCreativeTab("ingredients", "Gensokyo Legacy - Ingredients",
				e -> e.icon(GLItems.MYSTICAL_STRAW::asStack));

		// tools
		{
			var head = ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "head"));
			MINI_FURNACE_1 = reg.item("mini_hakkero_prototype", MiniFurnace1::new)
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.tag(GLTagGen.MORICHIKA_OFFERS)
					.dataMap(GLMeta.MORICHIKA_OFFER.reg(), new MorichikaOfferData(16, 24, 1, 1))
					.lang("Mini Hakkero [Prototype]").register();

			CENTIPICKAXE = reg.item("centipickaxe", CentiPickaxe::new)
					.model((ctx, pvd) -> pvd.handheld(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.tab(TAB.key(), (a, b) -> b.accept(a.get().getDefaultInstance(b.getParameters().holders())))
					.tag(GLTagGen.MORICHIKA_OFFERS)
					.dataMap(GLMeta.MORICHIKA_OFFER.reg(), new MorichikaOfferData(4, 6, 1, 1))
					.lang("Centipeck").register();

			DOWSER = reg.item("dowser", Dowser::new)
					.model((ctx, pvd) -> pvd.handheld(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.tag(GLTagGen.MORICHIKA_OFFERS)
					.dataMap(GLMeta.MORICHIKA_OFFER.reg(), new MorichikaOfferData(12, 18, 1, 1))
					.lang("Nazrin's Dowser").register();

			DOWSER_LEFT = reg.item("dowser_left", Item::new)
					.model((ctx, pvd) -> pvd.handheld(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.removeTab(TAB.key())
					.lang("Nazrin's Dowser (Left Half)").register();

			DOWSER_RIGHT = reg.item("dowser_right", Item::new)
					.model((ctx, pvd) -> pvd.handheld(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.removeTab(TAB.key())
					.lang("Nazrin's Dowser (Right Half)").register();

			MERMAID_PEARL = reg.item("mermaid_pearl", MermaidPearl::new)
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.tag(GLTagGen.MORICHIKA_OFFERS)
					.dataMap(GLMeta.MORICHIKA_OFFER.reg(), new MorichikaOfferData(4, 6, 3, 5))
					.lang("Mermaid's Pearl").register();

			CAT_BELL = reg.item("cat_bell", CatBell::new)
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.tag(GLTagGen.MORICHIKA_OFFERS)
					.dataMap(GLMeta.MORICHIKA_OFFER.reg(), new MorichikaOfferData(8, 12, 1, 1))
					.lang("Cat Bell").register();

			BORDER_UMBRELLA = reg.item("border_umbrella", BorderUmbrellaItem::new)
					.model((ctx, pvd) -> {
						var base = pvd.handheld(ctx, pvd.modLoc("item/border_umbrella/" + ctx.getName()));
						base.override().predicate(GensokyoLegacy.loc("umbrella_open"), 1)
								.model(pvd.withExistingParent("item/" + ctx.getName() + "_open", "item/handheld").
										texture("layer0", pvd.modLoc("item/border_umbrella/" + ctx.getName() + "_open")))
								.end();
						// icon variants for the wheel display stacks (cf. glove_display):
						// vanilla reverses the override list at bake time and returns the
						// first match with >= per predicate, so emit ascending values for
						// exact per-mode matching; plain stacks keep value 0 (base model)
						var modes = BorderUmbrellaMode.values();
						for (int i = 0; i < modes.length; i++) {
							base.override()
									.predicate(GensokyoLegacy.loc("umbrella_display"), i + 1)
									.model(pvd.withExistingParent("item/umbrella_icon_" + modes[i].iconName(), "item/generated")
											.texture("layer0", pvd.modLoc("item/border_umbrella/border_umbrella_icon_" + modes[i].iconName())))
									.end();
						}
					})
					.lang("Border Umbrella").tab(TAB.key(), BorderUmbrellaItem::fillCreativeModeTab)
					.tag(L2ISTagGen.SELECTABLE)
					.register();

			BROOM = reg.item("broom", BroomItem::new)
					.model((ctx, pvd) -> pvd.handheld(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.lang("Flying Broom").tab(TAB.key())
					.register();

			DOLL = reg.item("doll", p -> new DollItem(p.stacksTo(1)))
					.model((ctx, pvd) -> genLayeredItemModel(ctx.getName(), pvd))
					.color(() -> () -> DollItem::getColor)
					.lang("Doll")
					.tab(TAB.key(), (a, b) -> b.accept(DollItem.blank()))
					.register();

			DOLL_GLOVE = reg.item("doll_glove", DollGloveItem::new)
					.model(DollGloveModel::model)
					.lang("Seven-Colored Doll Glove").tab(TAB.key())
					.tag(L2ISTagGen.SELECTABLE)
					.register();

			// The lance is a hand-authored Blockbench model with real elements — a 28-unit
			// polearm along +Z, which neither `handheld` nor `generated` can express — so it
			// lives in src/main/resources/models/custom and is wired up as the base, i.e. every
			// display context but GUI. The GUI gets the flat 16x16 icon instead: a 28-unit
			// lance has no transform that lands it inside a 16px slot.
			DOLL_LANCE = reg.item("doll_lance", DollLanceItem::new)
					.model((ctx, pvd) -> {
						var base = pvd.nested()
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/doll_lance")));
						var guiModel = pvd.nested()
								.parent(new ModelFile.UncheckedModelFile("item/generated"))
								.texture("layer0", pvd.modLoc("item/tool/" + ctx.getName() + "_icon"));
						pvd.getBuilder(ctx.getName())
								.customLoader(SeparateTransformsModelBuilder::begin)
								.base(base)
								.perspective(ItemDisplayContext.GUI, guiModel)
								.end().guiLight(BlockModel.GuiLight.FRONT);
					})
					.lang("Doll Lance").tab(TAB.key())
					.register();

			STRAW_HAT = reg
					.item("straw_hat", p -> new StrawHatItem(p.rarity(Rarity.UNCOMMON)))
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/curio/" + ctx.getName())))
					.clientExtension(() -> () -> new HatModel(SuwakoHatModel.STRAW))
					.tag(GLTagGen.MORICHIKA_OFFERS)
					.tag(head, GLTagGen.TOUHOU_HAT)
					.dataMap(GLMeta.MORICHIKA_OFFER.reg(), new MorichikaOfferData(3, 6, 4, 4))
					.register();

			SUWAKO_HAT = reg
					.item("suwako_hat", p -> new SuwakoHatItem(p.rarity(Rarity.EPIC)))
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/curio/" + ctx.getName())))
					.clientExtension(() -> () -> new HatModel(SuwakoHatModel.SUWAKO))
					.tag(head, GLTagGen.TOUHOU_HAT)
					.register();

			KOISHI_HAT = reg
					.item("koishi_hat", p -> new KoishiHatItem(p.rarity(Rarity.EPIC)))
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/curio/" + ctx.getName())))
					.clientExtension(() -> () -> new HatModel(KoishiHatModel.HAT))
					.tag(head, GLTagGen.TOUHOU_HAT)
					.register();

			STAR_WAND = reg.item("star_wand", p -> new StarWandItem(p.rarity(Rarity.RARE)))
					.model((ctx, pvd) -> pvd.handheld(ctx, pvd.modLoc("item/tool/" + ctx.getName())))
					.tag(DanmakuItems.Bullet.STAR.tag)
					.lang("Star Wand").tab(TAB.key())
					.register();

			// 3D model hand-written in models/custom, so only wire up the parent here. Split with
			// SeparateTransformsModelBuilder and gui_light: front (the same shape as DOLL_LANCE),
			// because the two halves want opposite things: the base is the modelled dagger, which
			// is what every display except the gui wants, and the gui gets the flat 16x16 icon
			// instead, since a modelled blade has no transform that lands it inside a 16px slot.
			IRON_DAGGER = reg.item("iron_dagger", IronDaggerItem::new)
					.model((ctx, pvd) -> {
						var base = pvd.nested()
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/" + ctx.getName())));
						var guiModel = pvd.nested()
								.parent(new ModelFile.UncheckedModelFile("item/generated"))
								.texture("layer0", pvd.modLoc("item/tool/" + ctx.getName() + "_icon"));
						pvd.getBuilder(ctx.getName())
								.customLoader(SeparateTransformsModelBuilder::begin)
								.base(base)
								.perspective(ItemDisplayContext.GUI, guiModel)
								.end().guiLight(BlockModel.GuiLight.FRONT);
					})
					.tag(DanmakuItems.Bullet.DAGGER.tag)
					.lang("Iron Dagger").tab(TAB.key())
					.register();

			// The glove's held model carries a predicate override per mode rather than a flat
			// layer0, so per-mode art can be dropped into the four texture paths without
			// touching the wheel, which renders real glove stacks and names the hovered mode
			// (dagger_glove.md §6). Today every mode wears the same texture. The held half is
			// the mitten the doll glove shares (DaggerGloveModel).
			DAGGER_GLOVE = reg.item("dagger_glove", p -> new DaggerGloveItem(p.stacksTo(1)))
					.model(DaggerGloveModel::model)
					.lang("Dagger Glove").tab(TAB.key())
					.tag(L2ISTagGen.SELECTABLE)
					.register();

			STRANGE_GLASSES = reg.item("strange_glasses", p -> new StrangeGlassesItem(p.rarity(Rarity.UNCOMMON)))
					.model((ctx, pvd) -> {
						var base = pvd.nested()
								.parent(new ModelFile.UncheckedModelFile("item/generated"))
								.texture("layer0", pvd.modLoc("item/curio/" + ctx.getName()));
						var headModel = pvd.nested()
								.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/strange_glasses_head")));
						pvd.getBuilder(ctx.getName())
								.customLoader(SeparateTransformsModelBuilder::begin)
								.base(base)
								.perspective(ItemDisplayContext.HEAD, headModel)
								.end().guiLight(BlockModel.GuiLight.FRONT);
					})
					.tag(ItemTags.HEAD_ARMOR, head, GLTagGen.MORICHIKA_OFFERS)
					.dataMap(GLMeta.MORICHIKA_OFFER.reg(), new MorichikaOfferData(6, 10, 1, 1))
					.lang("Strange Glasses").tab(TAB.key())
					.register();
		}

		GLTalismans.register();

		GLFluids.register();

		GLBlocks.register();

		// debug
		{

			DEBUG_GLASSES = reg.item("debug_glasses", p -> new DebugGlasses(p.stacksTo(1)))
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/debug/" + ctx.getName())))
					.defaultLang().register();

			DEBUG_WAND = reg.item("debug_wand", p -> new DebugWand(p.stacksTo(1)))
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/debug/" + ctx.getName())))
					.defaultLang().register();

			STRUCTURE_WAND = reg.item("structure_wand", p -> new StructureWand(p.stacksTo(1)))
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/debug/" + ctx.getName())))
					.defaultLang().register();

			DOOR_DEBUG_WAND = reg.item("door_debug_wand", p -> new DoorDebugItem(p.stacksTo(1)))
					.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/debug/" + ctx.getName())))
					.defaultLang().register();

		}

	}

	/**
	 * Two-layer doll item: layer0 is the base doll, layer1 is the tint overlay colored by the
	 * item's {@code DyeColor} (via {@link DollItem#getColor}).
	 */
	private static void genLayeredItemModel(String name, RegistrateItemModelProvider pvd) {
		pvd.getBuilder(name)
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", pvd.modLoc("item/doll/doll0"))
				.texture("layer1", pvd.modLoc("item/doll/doll1"));
	}

	public static void register() {

	}

}
