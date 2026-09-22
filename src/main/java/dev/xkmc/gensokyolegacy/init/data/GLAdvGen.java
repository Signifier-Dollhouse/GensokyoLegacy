package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLCriteriaTriggers;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.serial.advancements.AdvancementGenerator;
import dev.xkmc.l2core.serial.advancements.CriterionBuilder;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

public class GLAdvGen {
	public static final ResourceLocation KOISHI_FIRST = GensokyoLegacy.loc("koishi_first");
	public static final ResourceLocation KOISHI_HAT = GensokyoLegacy.loc("koishi_hat");

	public static final ResourceLocation ENTER_HAKUREI_SHRINE = GensokyoLegacy.loc("main/enter_hakurei_shrine");
	public static final ResourceLocation ENTER_MARISA_HOUSE = GensokyoLegacy.loc("main/enter_marisa_house");
	public static final ResourceLocation ENTER_MORICHIKA_SHOP = GensokyoLegacy.loc("main/enter_morichika_shop");

	public static void genAdv(RegistrateAdvancementProvider pvd) {
		pvd.accept(Advancement.Builder.advancement().addCriterion("koishi_first",
				GLCriteriaTriggers.KOISHI_FIRST.get().createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()))
		).build(KOISHI_FIRST));
		pvd.accept(Advancement.Builder.advancement().addCriterion("koishi_hat",
				GLCriteriaTriggers.KOISHI_HAT.get().createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()))
		).build(KOISHI_HAT));

		var gen = new AdvancementGenerator(pvd, GensokyoLegacy.MODID);
		var tab = gen.new TabBuilder("main");
		var root = tab.root("welcome", GLItems.BORDER_UMBRELLA.get(),
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.tick()),
				"Welcome to Gensokyo", "Find your way in this new world");
		root.create("enter_hakurei_shrine", Items.CHERRY_SAPLING,
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.located(
						LocationPredicate.Builder.inStructure(resolve(pvd, "hakurei_shrine")))),
				"Hakurei Shrine", "Enter the Hakurei Shrine");
		root.create("enter_marisa_house", Items.RED_MUSHROOM_BLOCK,
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.located(
						LocationPredicate.Builder.inStructure(resolve(pvd, "marisa_house")))),
				"Kirisame House", "Enter Marisa's house in the Magical Forest");
		root.create("enter_morichika_shop", Items.EMERALD,
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.located(
						LocationPredicate.Builder.inStructure(resolve(pvd, "morichika_shop")))),
				"Kourindou", "Enter Morichika's shop in the Magical Forest");
		root.finish();
	}

	private static Holder<Structure> resolve(RegistrateAdvancementProvider pvd, String id) {
		return pvd.resolve(ResourceKey.create(Registries.STRUCTURE, GensokyoLegacy.loc(id)));
	}

}
