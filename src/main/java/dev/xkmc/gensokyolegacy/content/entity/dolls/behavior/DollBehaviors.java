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
 * Built-in item → action bindings, lazily registered on first registry query so no
 * mod-constructor wiring is needed. Each line's last flag is whether the behavior <b>spends</b>
 * what the doll holds, which lets that stack in a loadout hand (loadout.md §4). The one
 * binding with no item is the bare-handed regular attack, which the lance charge also serves
 * (control.md §5.1b).
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
		// below danmaku: a doll holding both keeps shooting, since the shot has no
		// range limit while the charge gives up past 16 blocks
		DollBehaviorRegistry.register("doll_lance",
				stack -> stack.is(GLItems.DOLL_LANCE.get()),
				DollActionType.REGULAR_ATTACK, -1, false, DollMeleeBehavior::new);
		// below every item binding, which makes it a fallback: a regular attack with no
		// weapon is still an attack, and the swing-time swap frees the main hand
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
	 * A folded heal talisman with remaining durability.
	 */
	public static Optional<HealSetup> findHealPaper(ItemStack stack) {
		if (!(stack.getItem() instanceof FoldedPaperTalisman)) return Optional.empty();
		TalismanPaperItem paper = FoldedPaperTalisman.paper(stack);
		if (!(paper instanceof HealTalisman heal)) return Optional.empty();
		int left = GLTalismans.DC_TALISMAN_DURABILITY.getOrDefault(stack, paper.getDurability());
		return left > 0 ? Optional.of(new HealSetup(heal, left)) : Optional.empty();
	}

}
