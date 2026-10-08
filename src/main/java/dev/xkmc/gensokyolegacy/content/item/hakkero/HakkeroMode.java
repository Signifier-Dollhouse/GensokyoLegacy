package dev.xkmc.gensokyolegacy.content.item.hakkero;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * What a hakkero is set to cook as, shared by both models: the prototype
 * ({@link HakkeroPrototype}) and the finished one ({@link Hakkero}).
 *
 * <p>Each constant pairs the vanilla block whose name the mode borrows with the
 * {@link RecipeType} it cooks by. {@link #OFF} has no recipe type and no block, and only the
 * prototype can reach it — see {@link #lit()}.
 */
public enum HakkeroMode {

	/** The prototype switched off: leaks heat but cooks nothing itself. */
	OFF(Blocks.AIR, null),
	SMOKE(Blocks.SMOKER, RecipeType.SMOKING),
	FURNACE(Blocks.FURNACE, RecipeType.SMELTING),
	BLAST(Blocks.BLAST_FURNACE, RecipeType.BLASTING);

	/**
	 * Every mode except {@link #OFF}. A sealed firebox has nothing to switch off — there are
	 * no leaks left to stop — so the finished hakkero only ever cycles these, and its data
	 * component only ever holds these.
	 */
	private static final HakkeroMode[] LIT = {SMOKE, FURNACE, BLAST};

	private final Block block;
	@Nullable
	private final RecipeType<? extends AbstractCookingRecipe> type;

	HakkeroMode(Block block, @Nullable RecipeType<? extends AbstractCookingRecipe> type) {
		this.block = block;
		this.type = type;
	}

	/**
	 * The modes a sealed hakkero can hold, {@link #OFF} excluded. For its data component's
	 * codec and for its mode cycle.
	 */
	public static HakkeroMode[] lit() {
		return LIT.clone();
	}

	/** The vanilla block this mode is named after, shown in tooltips and under the menu grid. */
	public Block block() {
		return block;
	}

	/** The recipe type cooked in this mode, or {@code null} for {@link #OFF}. */
	@Nullable
	public RecipeType<? extends AbstractCookingRecipe> getType() {
		return type;
	}

	/** The next mode in the full cycle, {@link #OFF} included. What the prototype cycles through. */
	public HakkeroMode next() {
		var vals = values();
		return vals[(ordinal() + 1) % vals.length];
	}

	/** The next mode a sealed hakkero can hold. Never {@link #OFF}. */
	public HakkeroMode nextLit() {
		for (HakkeroMode mode : LIT) {
			if (mode.ordinal() > ordinal()) return mode;
		}
		return LIT[0];
	}

	/**
	 * The recipe this mode would cook {@code stack} with, or {@code null}. Also the test the
	 * input slots apply: a channel only ever accepts something it can actually smelt.
	 */
	@Nullable
	public RecipeHolder<AbstractCookingRecipe> findRecipeHolder(Level level, ItemStack stack) {
		if (type == null || stack.isEmpty()) return null;
		return findRecipeHolder(level, new SingleRecipeInput(stack));
	}

	/** As {@link #findRecipeHolder}, for a caller that has already built the input. */
	@Nullable
	public RecipeHolder<AbstractCookingRecipe> findRecipeHolder(Level level, SingleRecipeInput input) {
		if (type == null) return null;
		// every RecipeType here is a RecipeType<AbstractCookingRecipe>; the wildcard only comes
		// from holding them in one enum, so the cast is what restores the element type
		@SuppressWarnings("unchecked")
		RecipeType<AbstractCookingRecipe> cast = (RecipeType<AbstractCookingRecipe>) type;
		return level.getRecipeManager().getRecipeFor(cast, input, level).orElse(null);
	}

	/** As {@link #findRecipeHolder}, dropping the id — only the recipe itself is wanted. */
	@Nullable
	public AbstractCookingRecipe findRecipe(Level level, ItemStack stack) {
		var holder = findRecipeHolder(level, stack);
		return holder == null ? null : holder.value();
	}

}
