package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.item.LaserItem;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrewBottleItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.FoldedPaperTalisman;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.kinds.HealTalisman;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Built-in item → action bindings. Lazily registered on first
 * {@link DollBehaviorRegistry#findHand} so no mod-constructor wiring is needed.
 * <p>
 * Each line's last flag is whether the behavior <b>spends</b> what the doll holds
 * (one per use), which is also what lets that item stack in a loadout hand
 * ({@link DollBehaviorRegistry#spendsOnUse}, loadout.md §4): the throwable
 * hexbrew, the laser and the TNT are ammunition, while a danmaku item, the lance
 * and a folded talisman are one item per hand however big their own stack is.
 * <p>
 * The one binding with no item is the bare-handed regular attack
 * ({@link DollBehaviorRegistry#registerBareHand}), which the lance charge also
 * serves: a doll with nothing to swing a lance with still answers a volley.
 */
public final class DollBehaviors {

	public record HealSetup(HealTalisman paper, int durabilityLeft) {
	}

	private static boolean ready;

	private DollBehaviors() {
	}

	public static void register() {
		if (ready) return;
		ready = true;
		DollBehaviorRegistry.register("danmaku",
				stack -> stack.getItem() instanceof DanmakuItem,
				DollActionType.REGULAR_ATTACK, 0, false, DollDanmakuBehavior::new);
		// Below danmaku: a doll holding both keeps shooting, since the shot has no
		// range limit while the charge gives up past 16 blocks. So the lance is what
		// a doll with no danmaku item does instead.
		DollBehaviorRegistry.register("doll_lance",
				stack -> stack.is(GLItems.DOLL_LANCE.get()),
				DollActionType.REGULAR_ATTACK, -1, false, DollMeleeBehavior::new);
		// Below every item binding (which is what makes it a fallback rather than a
		// competitor): a regular attack with no weapon at all is still an attack. A
		// doll holding something that cannot attack — a laser, a talisman, a shield —
		// and nothing in the other hand slaps instead, and the sticky swap at swing
		// time moves whatever was in the main hand over to the off hand to free it.
		DollBehaviorRegistry.registerBareHand(DollActionType.REGULAR_ATTACK, DollMeleeBehavior::new);
		DollBehaviorRegistry.register("laser",
				stack -> stack.getItem() instanceof LaserItem,
				DollActionType.SUPER_ATTACK, 10, true, DollLaserBehavior::new);
		DollBehaviorRegistry.register("hexbrew",
				stack -> stack.getItem() instanceof HexBrewBottleItem bottle &&
						bottle.getHexBrew().handler.isThrowable(),
				DollActionType.SUPER_ATTACK, 0, true, DollThrowBehavior::new);
		DollBehaviorRegistry.register("tnt",
				stack -> stack.is(Items.TNT),
				DollActionType.SUICIDE_ATTACK, 0, false, DollSuicideBehavior::new);
		DollBehaviorRegistry.register("heal_talisman",
				DollBehaviors::isUsableHealTalisman,
				DollActionType.HEAL, 0, false, DollHealBehavior::new);
	}

	public static boolean isUsableHealTalisman(ItemStack stack) {
		return findHealPaper(stack).isPresent();
	}

	/**
	 * A folded heal talisman with remaining durability. Reads the same components
	 * as {@link FoldedPaperTalisman} tooltips/bars.
	 */
	public static Optional<HealSetup> findHealPaper(ItemStack stack) {
		if (!(stack.getItem() instanceof FoldedPaperTalisman)) return Optional.empty();
		TalismanPaperItem paper = FoldedPaperTalisman.paper(stack);
		if (!(paper instanceof HealTalisman heal)) return Optional.empty();
		int left = GLTalismans.DC_TALISMAN_DURABILITY.getOrDefault(stack, paper.getDurability());
		return left > 0 ? Optional.of(new HealSetup(heal, left)) : Optional.empty();
	}

}
