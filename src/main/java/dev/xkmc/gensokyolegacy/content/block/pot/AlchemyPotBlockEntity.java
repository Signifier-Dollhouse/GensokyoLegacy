package dev.xkmc.gensokyolegacy.content.block.pot;

import dev.xkmc.gensokyolegacy.content.block.pot.overlay.AlchemyHintOverlay;
import dev.xkmc.gensokyolegacy.content.block.pot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.block.pot.stage.AlchemyStageHolder;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.l2core.base.tile.BaseBlockEntity;
import dev.xkmc.l2core.base.tile.BaseContainerListener;
import dev.xkmc.l2core.base.tile.BaseTank;
import dev.xkmc.l2modularblock.tile_api.BlockContainer;
import dev.xkmc.l2modularblock.tile_api.TickableBlockEntity;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SerialClass
public class AlchemyPotBlockEntity extends BaseBlockEntity implements TickableBlockEntity, BaseContainerListener, BlockContainer, AlchemyHintOverlay.IHintable {

	public static final int MAX_SLOTS = 12;
	public static final int FLUID_CAPACITY = 1000;

	@SerialField
	public final AlchemyItemContainer items = new AlchemyItemContainer(this, MAX_SLOTS).setMax(1);

	@SerialField
	public final BaseTank tank = new BaseTank(1, FLUID_CAPACITY);

	@SerialField
	protected int totalTime = 0, recipeProgress = 0;
	@SerialField
	protected ResourceLocation recipeId = null;

	private boolean doRecipeSearch = true;

	private RecipeHolder<AlchemyRecipe<?>> recipe = null;

	@Nullable
	public RecipeHolder<AlchemyRecipe<?>> getRecipeHolder() {
		return recipe;
	}

	// client side stage
	public final AlchemyStageHolder stage = new AlchemyStageHolder();
	private boolean recheckStage = true;
	private final IItemHandler itemHandler = new InvWrapper(items);

	public AlchemyPotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		tank.add(this);
		tank.setPredicate(stack -> !isReacting() && (tank.isEmpty() || FluidStack.isSameFluidSameComponents(tank.getFluidInTank(0), stack)));
		tank.setExtract(() -> !isReacting());
	}

	public FluidStack getFluid() {
		return tank.getFluidInTank(0);
	}

	public float inProgress() {
		return totalTime == 0 ? 0 : Mth.clamp(1f * recipeProgress / totalTime, 0, 1);
	}

	public boolean isReacting() {
		return totalTime > 0;
	}

	public AlchemyInv createContainer(boolean isComplete) {
		FluidStack fluid = getFluid().copy();
		List<ItemStack> list = new ArrayList<>();
		for (var e : items.getAsList()) if (!e.isEmpty()) list.add(e.copy());
		return new AlchemyInv(fluid, list, isComplete);
	}

	public AlchemyInv createContainer() {
		return createContainer(true);
	}

	public boolean tryAddItem(ItemStack stack, boolean simulate) {
		if (level == null) return false;
		if (inProgress() > 0) return false;
		if (getFluid().getAmount() < FLUID_CAPACITY) return false;
		if (stack.isEmpty()) return false;
		// one by one: we test with count 1
		ItemStack test = stack.copyWithCount(1);
		// check capacity
		boolean hasSpace = false;
		for (var e : items.getAsList())
			if (e.isEmpty()) {
				hasSpace = true;
				break;
			}
		if (!hasSpace) return false;
		List<ItemStack> list = new ArrayList<>();
		for (var e : items.getAsList()) if (!e.isEmpty()) list.add(e.copy());
		list.add(test);
		var inv = new AlchemyInv(getFluid().copy(), list, false);
		var opt = level.getRecipeManager().getRecipeFor(GLRecipes.ALCHEMY_RT.get(), inv, level);
		if (opt.isEmpty()) return false;
		if (!simulate) {
			ItemStack toInsert = test.copy();
			items.addItem(toInsert);
		}
		return true;
	}

	public boolean addItemWithContainer(Player player, ItemStack stack) {
		if (!tryAddItem(stack, true)) return false;
		ItemStack remainder = stack.getCraftingRemainingItem();
		items.addItem(stack.copyWithCount(1));
		if (!player.isCreative()) {
			stack.shrink(1);
			if (!remainder.isEmpty()) {
				if (!player.getInventory().add(remainder)) {
					player.drop(remainder, false);
				}
			}
		}
		notifyTile();
		recheckStage = true;
		return true;
	}

	public void clearContents() {
		List<ItemStack> toDrop = new ArrayList<>();
		for (var e : stage.floating) {
			if (!e.stack().isEmpty() && e.life() < 0) {
				toDrop.add(e.stack().copy());
			}
		}
		// server drop
		if (level != null && !level.isClientSide) {
			for (var s : toDrop) {
				Block.popResource(level, worldPosition.above(), s);
			}
		}
		items.clear();
		tank.clear();
		totalTime = 0;
		recipeProgress = 0;
		recipeId = null;
		recipe = null;
		doRecipeSearch = true;
		recheckStage = true;
		notifyTile();
	}

	public void popLastItem(Player player) {
		if (inProgress() > 0) return;
		// find last non-empty slot
		for (int i = items.getContainerSize() - 1; i >= 0; i--) {
			ItemStack s = items.getItem(i);
			if (!s.isEmpty()) {
				ItemStack out = s.copy();
				items.setItem(i, ItemStack.EMPTY);
				notifyTile();
				recheckStage = true;
				if (!player.getInventory().add(out)) {
					player.drop(out, false);
				}
				return;
			}
		}
	}

	protected RecipeType<AlchemyRecipe<?>> getRecipeType() {
		return GLRecipes.ALCHEMY_RT.get();
	}

	protected boolean isEmpty() {
		return getFluid().isEmpty() && items.isEmpty();
	}

	public void notifyTile() {
		setChanged();
		sync();
		doRecipeSearch = true;
	}

	@Override
	public void tick() {
		if (level == null) return;
		if (level.isClientSide()) {
			if (recheckStage) {
				recheckStage = false;
				stage.recheck(this, level);
			}
			stage.tickStage(this, totalTime == 0 ? 0 : recipeProgress);
			if (totalTime > 0) {
				recipeProgress++;
				// client bubble particles handled in renderer tick? Do here as well
				if (inProgress() > 0 && level.random.nextFloat() < 0.25f) {
					double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.36;
					double y = worldPosition.getY() + 0.78;
					double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.36;
					level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0, 0.04, 0);
					if (level.random.nextFloat() < 0.08f)
						level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0.02, 0);
				}
			}
			return;
		}
		// server
		if (doRecipeSearch) {
			if (!isEmpty() && getFluid().getAmount() >= FLUID_CAPACITY) {
				var opt = level.getRecipeManager().getRecipeFor(getRecipeType(), createContainer(), level);
				if (opt.isPresent()) {
					recipe = opt.get();
					totalTime = recipe.value().getProcessTime();
					if (!recipe.id().equals(recipeId)) {
						recipeProgress = 0;
						recipeId = recipe.id();
					} else if (recipeProgress > totalTime) {
						recipeProgress = totalTime - 1;
					}
				} else {
					recipeId = null;
					recipe = null;
					totalTime = 0;
					recipeProgress = 0;
				}
				sync();
			} else if (isEmpty() || getFluid().getAmount() < FLUID_CAPACITY) {
				// fluid-first: no recipe if not full
				recipeId = null;
				recipe = null;
				totalTime = 0;
				recipeProgress = 0;
				sync();
			}
			doRecipeSearch = false;
			recheckStage = true;
		}
		if (totalTime > 0) {
			recipeProgress++;
			if (recipeProgress >= totalTime) {
				if (recipe != null) {
					finishRecipe(level, recipe.value());
				}
				recipeProgress = 0;
				totalTime = 0;
				recipeId = null;
				recipe = null;
				doRecipeSearch = true;
				recheckStage = true;
				sync();
			}
		}
		// tick stage on server for consistency? not needed but update floating for drop logic
		if (recheckStage) {
			recheckStage = false;
			stage.recheck(this, level);
		}
		stage.tickStage(this, recipeProgress);
	}

	protected void finishRecipe(Level level, AlchemyRecipe<?> recipe) {
		AlchemyInv inv = createContainer(true);
		FluidStack outFluid = recipe.getResultFluid(inv, level.registryAccess());
		ItemStack outItem = recipe.resultItem.copy();
		items.clear();
		tank.clear();
		if (!outFluid.isEmpty()) {
			tank.fill(outFluid, IFluidHandler.FluidAction.EXECUTE);
		} else if (!outItem.isEmpty()) {
			// item-only: fluid already cleared
		} else {
			// both empty should not happen, validated
		}
		if (!outItem.isEmpty()) {
			// distribute one by one up to MAX_SLOTS
			int count = outItem.getCount();
			ItemStack base = outItem.copyWithCount(1);
			for (int i = 0; i < count; i++) {
				if (i >= MAX_SLOTS) {
					// overflow drop
					Block.popResource(level, worldPosition.above(), base.copy());
				} else {
					items.addItem(base.copy());
				}
			}
		}
		// note: keep both inside as per spec, already done
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider pvd) {
		super.loadAdditional(tag, pvd);
		recheckStage = true;
		doRecipeSearch = true;
	}

	@Override
	public List<Container> getContainers() {
		SimpleContainer c = new SimpleContainer(items.getContainerSize());
		for (int i = 0; i < items.getContainerSize(); i++) {
			var s = items.getItem(i);
			if (s.isEmpty() || s.hasCraftingRemainingItem()) continue;
			c.addItem(s.copy());
		}
		return List.of(c);
	}

	public List<Component> getHintLines(boolean shift, BlockHitResult hit) {
		if (inProgress() > 0) return List.of(Component.literal("Progress: " + Math.round(inProgress() * 100) + "%"));
		return List.of();
	}

	public List<Ingredient> getHints(Level lvl, BlockPos pos) {
		if (inProgress() > 0) return List.of();
		if (getFluid().getAmount() < FLUID_CAPACITY) return List.of();
		var cont = createContainer(false);
		var recs = lvl.getRecipeManager().getRecipesFor(getRecipeType(), cont, lvl);
		List<Ingredient> ans = new ArrayList<>();
		for (var e : recs) ans.addAll(e.value().getHints(lvl, cont));
		return ans;
	}

	@Nullable
	public IItemHandler getItemCap(@Nullable Direction dir) {
		return itemHandler;
	}

	public BaseTank getTankCap(@Nullable Direction dir) {
		return tank;
	}
}
