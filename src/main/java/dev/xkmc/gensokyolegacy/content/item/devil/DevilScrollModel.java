package dev.xkmc.gensokyolegacy.content.item.devil;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.world.item.Item;

import java.util.Locale;

/**
 * Item model generation for the scroll: one flat icon per mode, selected by
 * {@link DevilScrollItem#displayPredicate}.
 *
 * @see "doc/design/item/devils_scroll.md"
 */
public class DevilScrollModel {

	/**
	 * Icons by mode, in {@link DevilScrollMode} order: the plain book is the passive base model.
	 */
	private static final String[] ICONS = {
			"item/tool/devils_scroll",
			"item/tool/devils_scroll_superhuman",
			"item/tool/devils_scroll_devil"
	};

	public static void model(DataGenContext<Item, DevilScrollItem> ctx, RegistrateItemModelProvider pvd) {
		var base = pvd.generated(ctx, pvd.modLoc(ICONS[0]));
		var modes = DevilScrollMode.values();
		for (int i = 1; i < modes.length; i++) {
			String name = "item/" + ctx.getName() + "_" + modes[i].name().toLowerCase(Locale.ROOT);
			var model = pvd.withExistingParent(name, "item/generated")
					.texture("layer0", pvd.modLoc(ICONS[i]));
			// vanilla picks the first override whose predicate is >= its value, so emit ascending
			base.override()
					.predicate(GensokyoLegacy.loc("devils_scroll_mode"), i)
					.model(model)
					.end();
		}
	}

}
