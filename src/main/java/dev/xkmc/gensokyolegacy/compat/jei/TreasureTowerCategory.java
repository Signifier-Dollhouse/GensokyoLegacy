package dev.xkmc.gensokyolegacy.compat.jei;

import dev.xkmc.gensokyolegacy.content.item.treasure.OreRecipe;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.compat.jei.BaseRecipeCategory;
import dev.xkmc.l2serial.util.Wrappers;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * One derived recipe: a gem spent on any of the stones in the middle slot turns it into the ore on
 * the right. The stones come from the ore's rule test, expanded here for the page only.
 *
 * @see "doc/design/tower.md"
 */
public class TreasureTowerCategory extends BaseRecipeCategory<OreRecipe, TreasureTowerCategory> {

	private static final int SLOT_Y = GLJeiUtil.PAD;
	private static final int GEM_X = GLJeiUtil.PAD;
	private static final int STONE_X = GEM_X + GLJeiUtil.SLOT + GLJeiUtil.GAP;
	private static final int ARROW_X = STONE_X + GLJeiUtil.SLOT + GLJeiUtil.GAP;
	private static final int OUT_X = ARROW_X + GLJeiUtil.ARROW + GLJeiUtil.GAP;
	private static final int WIDTH = OUT_X + GLJeiUtil.SLOT + GLJeiUtil.PAD;
	private static final int HEIGHT = SLOT_Y + GLJeiUtil.SLOT + GLJeiUtil.PAD;

	public TreasureTowerCategory() {
		super(GensokyoLegacy.loc("treasure_tower"), Wrappers.cast(OreRecipe.class));
	}

	public TreasureTowerCategory init(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
		this.icon = helper.createDrawableItemStack(GLItems.TREASURE_TOWER.asStack());
		return this;
	}

	@Override
	public Component getTitle() {
		return GLLang.Jei.TREASURE_TOWER.get();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, OreRecipe recipe, IFocusGroup focuses) {
		GLJeiUtil.slot(builder, RecipeIngredientRole.INPUT, GEM_X, SLOT_Y)
				.addItemStack(new ItemStack(recipe.gem()));
		List<ItemStack> stacks = new ArrayList<>();
		for (Block block : stones(recipe)) {
			var item = block.asItem();
			if (item != Items.AIR) stacks.add(new ItemStack(item));
		}
		GLJeiUtil.slot(builder, RecipeIngredientRole.INPUT, STONE_X, SLOT_Y)
				.addItemStacks(stacks);
		GLJeiUtil.slot(builder, RecipeIngredientRole.OUTPUT, OUT_X, SLOT_Y)
				.addItemStack(new ItemStack(recipe.ore()));
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, OreRecipe recipe, IFocusGroup focuses) {
		builder.addRecipeArrowWidget().setPosition(ARROW_X, SLOT_Y);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, OreRecipe recipe, IRecipeSlotsView slots, double x, double y) {
		for (var id : recipe.tags())
			tooltip.add(GLLang.JeiExtra.TREASURE_TOWER_TAG.get(id));
		tooltip.add(GLLang.JeiExtra.TREASURE_TOWER_NOTE.get());
	}

	@Override
	public @Nullable ResourceLocation getRegistryName(OreRecipe recipe) {
		return GensokyoLegacy.loc(BuiltInRegistries.ITEM.getKey(recipe.gem()).getPath()
				+ "/" + BuiltInRegistries.BLOCK.getKey(recipe.ore()).getPath());
	}

	/**
	 * The stones the recipe accepts: the loose blocks the build recorded, plus whatever the tag ids
	 * hold on this client. Display only — the item never expands a tag.
	 */
	private static List<Block> stones(OreRecipe recipe) {
		var ans = new LinkedHashSet<>(recipe.stones());
		for (var key : recipe.tagKeys())
			BuiltInRegistries.BLOCK.getTag(key).ifPresent(set -> set.forEach(e -> ans.add(e.value())));
		return new ArrayList<>(ans);
	}

}
