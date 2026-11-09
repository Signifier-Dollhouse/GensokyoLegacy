package dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.stage;

import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyPotBlockEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks the best matching stage recipe for the pot contents and reports the items it has not
 * consumed yet, so the renderer can bob them and clear can drop them.
 *
 * @see "doc/design/alchemy_pot.md"
 */
public class AlchemyStageHolder {

	private List<AlchemyStageRecipe<?>> recipes = new ArrayList<>();

	public StageData current = StageData.DEF;
	public List<ItemEntry> floating = new ArrayList<>();

	public void recheck(AlchemyPotBlockEntity be, Level level) {
		var cont = be.createContainer(false);
		recipes = level.getRecipeManager().getRecipesFor(GLRecipes.ALCHEMY_STAGE_RT.get(), cont, level);
		update(be);
	}

	public void tickStage(AlchemyPotBlockEntity be, int time) {
		if (floating.isEmpty() && !be.items.isEmpty()) {
			update(be);
		}
	}

	private void update(AlchemyPotBlockEntity be) {
		var r0 = findStage();
		var inv = be.createContainer(false);
		var access = be.getLevel() != null ? be.getLevel().registryAccess() : null;
		if (r0 == null) {
			current = StageData.DEF;
		} else {
			int col = access == null ? -1 : r0.getColor(inv, access);
			// a recipe that leaves the colour unset (-1) falls back to its static field
			if (col == -1 && r0 instanceof SimpleAlchemyStageRecipe s) col = s.color;
			current = new StageData(col);
		}
		var curItems = new ArrayList<>(be.createContainer(false).list());
		if (r0 != null) {
			r0.removeConsumed(curItems);
		}
		floating = new ArrayList<>();
		var original = be.createContainer(false).list();
		for (int i = 0; i < original.size(); i++) {
			ItemStack orig = original.get(i);
			if (orig.isEmpty()) continue;
			if (!curItems.get(i).isEmpty()) {
				floating.add(new ItemEntry(orig, -1));
			}
		}
	}

	@Nullable
	private AlchemyStageRecipe<?> findStage() {
		int max = -1;
		ResourceLocation bestId = null;
		AlchemyStageRecipe<?> best = null;
		for (var e : recipes) {
			int cnt = e.getPriority();
			if (cnt > max) {
				max = cnt;
				bestId = e.getId();
				best = e;
			} else if (cnt == max && bestId != null) {
				if (e.getId().compareTo(bestId) < 0) {
					bestId = e.getId();
					best = e;
				}
			} else if (cnt == max) {
				bestId = e.getId();
				best = e;
			}
		}
		return best;
	}

	public record StageData(int color) {
		public static final StageData DEF = new StageData(-1);
	}

	public record ItemEntry(ItemStack stack, int life) {
	}
}
