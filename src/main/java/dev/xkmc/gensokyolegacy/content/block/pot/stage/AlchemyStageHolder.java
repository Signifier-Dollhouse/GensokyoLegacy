package dev.xkmc.gensokyolegacy.content.block.pot.stage;

import dev.xkmc.gensokyolegacy.content.block.pot.AlchemyPotBlockEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AlchemyStageHolder {

	private List<RecipeHolder<AlchemyStageRecipe<?>>> recipes = new ArrayList<>();

	public StageData current = StageData.DEF;
	public List<ItemEntry> floating = new ArrayList<>();

	public void recheck(AlchemyPotBlockEntity be, Level level) {
		var cont = be.createContainer(false);
		recipes = level.getRecipeManager().getRecipesFor(GLRecipes.ALCHEMY_STAGE_RT.get(), cont, level);
		update(be);
	}

	public void tickStage(AlchemyPotBlockEntity be, int time) {
		// time ignored; stage is immediate based on current contents
		// keep for compatibility but just ensures floating is up to date
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
			int col = access == null ? -1 : r0.value().getColor(inv, access);
			// fallback if subclass didn't override properly
			if (col == -1 && r0.value() instanceof SimpleAlchemyStageRecipe s) col = s.color;
			current = new StageData(col);
		}
		var curItems = new ArrayList<>(be.createContainer(false).list());
		if (r0 != null) {
			r0.value().removeConsumed(curItems);
		}
		floating = new ArrayList<>();
		var original = be.createContainer(false).list();
		for (int i = 0; i < original.size(); i++) {
			ItemStack orig = original.get(i);
			if (orig.isEmpty()) {
				floating.add(new ItemEntry(ItemStack.EMPTY, -1));
				continue;
			}
			boolean curEmpty = curItems.get(i).isEmpty();
			if (curEmpty) {
				// consumed -> dissolved, not floating
				floating.add(new ItemEntry(ItemStack.EMPTY, -1));
			} else {
				floating.add(new ItemEntry(orig, -1));
			}
		}
		while (floating.size() < original.size()) floating.add(new ItemEntry(ItemStack.EMPTY, -1));
	}

	@Nullable
	private RecipeHolder<AlchemyStageRecipe<?>> findStage() {
		int max = -1;
		ResourceLocation bestId = null;
		RecipeHolder<AlchemyStageRecipe<?>> best = null;
		for (var e : recipes) {
			int cnt = e.value().getPriority();
			if (cnt > max) {
				max = cnt;
				bestId = e.id();
				best = e;
			} else if (cnt == max && bestId != null) {
				if (e.id().compareTo(bestId) < 0) {
					bestId = e.id();
					best = e;
				}
			} else if (cnt == max) {
				bestId = e.id();
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
