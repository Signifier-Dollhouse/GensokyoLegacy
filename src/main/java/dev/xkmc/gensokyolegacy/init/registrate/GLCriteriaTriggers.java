package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.compat.l2lib.Val;
import dev.xkmc.gensokyolegacy.content.trigger.FeedCharacterTrigger;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 1.20.1 has no trigger registry: {@link CriteriaTriggers#getCriterion} reads a static map, so the
 * triggers are put there by hand at construction.
 */
public class GLCriteriaTriggers {

	private static final List<Val<? extends CriterionTrigger<?>>> ALL = new ArrayList<>();

	public static final Val<PlayerTrigger> SUWAKO_WEAR = reg("suwako_wear", PlayerTrigger::new);
	public static final Val<PlayerTrigger> KOISHI_RING = reg("koishi_ring", PlayerTrigger::new);
	public static final Val<PlayerTrigger> KOISHI_FIRST = reg("koishi_first", PlayerTrigger::new);
	public static final Val<PlayerTrigger> KOISHI_HAT = reg("koishi_hat", PlayerTrigger::new);
	public static final Val<FeedCharacterTrigger> FEED_REIMU = reg("feed_reimu", FeedCharacterTrigger::new);
	/**
	 * A fairy was handed a cookie or a cake - the plain one, Cirno, or whoever else carries a
	 * {@code FairyFeedModule}. Separate id off {@link #FEED_REIMU} so an advancement can ask for a fairy
	 * without spelling out which dish counted: the criterion is about the entity, not the food.
	 */
	public static final Val<FeedCharacterTrigger> FEED_FAIRY = reg("feed_fairy", FeedCharacterTrigger::new);
	public static final Val<PlayerTrigger> GRAVESTONE_SUMMON = reg("gravestone_summon", PlayerTrigger::new);
	public static final Val<PlayerTrigger> BIND_GHOST = reg("bind_ghost", PlayerTrigger::new);
	public static final Val<PlayerTrigger> JIZO_RESTORE = reg("jizo_restore", PlayerTrigger::new);

	private static <T extends CriterionTrigger<?>> Val<T> reg(String id, Function<ResourceLocation, T> factory) {
		ResourceLocation rl = GensokyoLegacy.loc(id);
		var ans = new Val.Unregistered<>(rl, () -> factory.apply(rl));
		ALL.add(ans);
		return ans;
	}

	public static void register() {
		for (var e : ALL) CriteriaTriggers.CRITERIA.put(e.id(), e.get());
	}

}
