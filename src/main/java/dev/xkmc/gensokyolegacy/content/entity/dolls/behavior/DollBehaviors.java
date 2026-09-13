package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.item.LaserItem;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrewBottleItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.FoldedPaperTalisman;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.content.item.talisman.kinds.HealTalisman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Built-in item → action bindings. Lazily registered on first
 * {@link DollBehaviorRegistry#findHand} so no mod-constructor wiring is needed.
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
				DollActionType.REGULAR_ATTACK, 0, DollDanmakuBehavior::new);
		DollBehaviorRegistry.register("laser",
				stack -> stack.getItem() instanceof LaserItem,
				DollActionType.SUPER_ATTACK, 10, DollLaserBehavior::new);
		DollBehaviorRegistry.register("hexbrew",
				stack -> stack.getItem() instanceof HexBrewBottleItem bottle &&
						bottle.getHexBrew().handler.isThrowable(),
				DollActionType.SUPER_ATTACK, 0, DollThrowBehavior::new);
		DollBehaviorRegistry.register("tnt",
				stack -> stack.is(Items.TNT),
				DollActionType.SUICIDE_ATTACK, 0, DollSuicideBehavior::new);
		DollBehaviorRegistry.register("heal_talisman",
				DollBehaviors::isUsableHealTalisman,
				DollActionType.HEAL, 0, DollHealBehavior::new);
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
