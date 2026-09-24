package dev.xkmc.gensokyolegacy.content.entity.behavior.task.marisa;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.attachment.index.BedRefData;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyInv;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.AlchemyPotBlockEntity;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.AlchemyRecipe;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.WitchEnhanceRecipe;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.recipe.WitchMergeRecipe;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.home.AbstractHomeHolderTask;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.fluid.GLHexFluid;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.init.registrate.GLRecipes;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Marisa's indoor hobby: play with the alchemy pot during at-home hours.
 * <ul>
 * <li>Empty pot: fill water and add a full ingredient set completing a recipe.</li>
 * <li>Pot with liquid that can still be processed: add the missing ingredients.</li>
 * <li>Pot with liquid that leads nowhere: drain it. Hexbrew is bottled and
 * stored into a random home chest, with any leftover pot items.</li>
 * </ul>
 * Witch hexbrew recipes are never considered. Draining happens at most once per 5 minutes.
 * Ingredients are conjured by Marisa herself; only finished brews go into chests.
 */
public class MarisaBrewTask<E extends SmartYoukaiEntity> extends AbstractHomeHolderTask<E> {

	private static final int DRAIN_COOLDOWN = 6000;
	private static final int ADD_WORK_TIME = 60;
	private static final int DRAIN_WORK_TIME = 100;

	private enum Plan {
		ADD, DRAIN
	}

	private record BrewOption(RecipeHolder<?> holder, List<ItemStack> missing) {
	}

	private BlockPos pot;
	private BlockPos bedPos;
	private Plan plan;
	private List<ItemStack> toAdd = List.of();
	private boolean needFill;
	private boolean rescueFirst;
	private long walkEnd, workEnd;
	private long nextDrain;

	public MarisaBrewTask() {
		super(Map.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
		));
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
		if (!super.checkExtraStartConditions(level, entity)) return false;
		var bed = BedRefData.of(level, entity);
		if (bed.isEmpty() || bed.get().getBedPos() == null) return false;
		bedPos = bed.get().getBedPos();
		pot = home.getBlockAround(HomeBlockKind.POT, bedPos);
		if (pot == null) return false;
		if (!(level.getBlockEntity(pot) instanceof AlchemyPotBlockEntity be)) return false;
		if (be.isReacting() || be.inProgress() > 0) return false;
		FluidStack fluid = be.getFluid();
		List<ItemStack> items = potItems(be);
		boolean full = !fluid.isEmpty() && fluid.getAmount() >= AlchemyPotBlockEntity.FLUID_CAPACITY;
		boolean fillable = fluid.isEmpty() || fluid.is(Fluids.WATER) && !full;
		if (full || fillable) {
			FluidStack matchFluid = full ? fluid.copy() :
					new FluidStack(Fluids.WATER, AlchemyPotBlockEntity.FLUID_CAPACITY);
			var options = findOptions(level, entity, matchFluid, items);
			if (!options.isEmpty()) {
				var pick = options.get(entity.getRandom().nextInt(options.size()));
				plan = Plan.ADD;
				toAdd = pick.missing();
				needFill = !full;
				rescueFirst = false;
				return true;
			}
		}
		if (fluid.isEmpty()) {
			// only junk items are blocking the pot: rescue them, then brew fresh
			if (items.isEmpty()) return false;
			plan = Plan.ADD;
			toAdd = List.of();
			needFill = true;
			rescueFirst = true;
			return true;
		}
		if (isCompleteMatch(level, fluid, items)) return false;
		if (level.getGameTime() < nextDrain) return false;
		plan = Plan.DRAIN;
		return true;
	}

	@Override
	protected void start(ServerLevel level, E entity, long gameTime) {
		BrainUtils.setMemory(entity, MemoryModuleType.WALK_TARGET, new WalkTarget(pot, 1, 1));
		BrainUtils.setMemory(entity, MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pot));
		walkEnd = gameTime + 200;
		workEnd = 0;
	}

	@Override
	protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
		if (!home.isValid()) return false;
		if (!(level.getBlockEntity(pot) instanceof AlchemyPotBlockEntity be)) return false;
		if (workEnd == 0) {
			if (entity.distanceToSqr(pot.getCenter()) < 4) {
				workEnd = gameTime + (plan == Plan.DRAIN ? DRAIN_WORK_TIME : ADD_WORK_TIME);
				return true;
			}
			return gameTime < walkEnd;
		}
		if (gameTime >= workEnd) {
			if (plan == Plan.ADD) doAdd(level, entity, be);
			else doDrain(level, entity, be, gameTime);
			BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
			BrainUtils.clearMemory(entity, MemoryModuleType.LOOK_TARGET);
			return false;
		}
		return true;
	}

	@Override
	protected void stop(ServerLevel level, E entity, long gameTime) {
		pot = null;
		bedPos = null;
		plan = null;
		toAdd = List.of();
		needFill = false;
		rescueFirst = false;
		walkEnd = 0;
		workEnd = 0;
		super.stop(level, entity, gameTime);
	}

	private void doAdd(ServerLevel level, E entity, AlchemyPotBlockEntity be) {
		if (be.isReacting()) return;
		entity.swing(InteractionHand.MAIN_HAND);
		if (entity instanceof GeoYoukaiAnim anim) anim.broadcastUseMainhandAnim();
		if (rescueFirst) {
			rescueSlotItems(level, be);
			be.clearContents();
		}
		FluidStack fluid = be.getFluid();
		if (needFill && (fluid.isEmpty() || fluid.is(Fluids.WATER))) {
			be.tank.fill(new FluidStack(Fluids.WATER, AlchemyPotBlockEntity.FLUID_CAPACITY),
					IFluidHandler.FluidAction.EXECUTE);
			be.notifyTile();
			level.playSound(null, pot, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
		}
		List<ItemStack> missing = toAdd;
		if (rescueFirst || missing.isEmpty()) {
			var options = findOptions(level, entity,
					new FluidStack(Fluids.WATER, AlchemyPotBlockEntity.FLUID_CAPACITY), List.of());
			if (options.isEmpty()) return;
			missing = options.get(entity.getRandom().nextInt(options.size())).missing();
		}
		for (var stack : missing) {
			if (!be.tryAddItem(stack, false)) break;
		}
		be.notifyTile();
	}

	private void doDrain(ServerLevel level, E entity, AlchemyPotBlockEntity be, long gameTime) {
		if (be.isReacting()) return;
		FluidStack fluid = be.getFluid();
		if (fluid.isEmpty()) return;
		// make sure nobody completed the pot while Marisa was walking over
		if (isCompleteMatch(level, fluid, potItems(be))) return;
		if (!findOptions(level, entity, fluid.copy(), potItems(be)).isEmpty()) return;
		entity.swing(InteractionHand.MAIN_HAND);
		if (entity instanceof GeoYoukaiAnim anim) anim.broadcastUseMainhandAnim();
		BlockPos chest = home.getBlockAround(HomeBlockKind.CONTAINER, bedPos);
		BlockPos drop = pot.above();
		if (fluid.getFluid() instanceof GLHexFluid hex) {
			int bottles = fluid.getAmount() / 250;
			if (bottles > 0) {
				ItemStack stack = new ItemStack(hex.brew.bottle.get(), bottles);
				hex.brew.copyToItem(fluid, stack);
				MarisaTaskUtil.insertIntoChest(level, chest, stack, drop);
			}
		}
		rescueSlotItems(level, be);
		be.clearContents();
		level.playSound(null, pot, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 0.5f);
		nextDrain = gameTime + DRAIN_COOLDOWN;
	}

	private void rescueSlotItems(ServerLevel level, AlchemyPotBlockEntity be) {
		BlockPos chest = home.getBlockAround(HomeBlockKind.CONTAINER, bedPos);
		BlockPos drop = pot.above();
		for (var s : be.items.getAsList()) {
			if (s.isEmpty()) continue;
			MarisaTaskUtil.insertIntoChest(level, chest, s.copy(), drop);
		}
	}

	private static List<ItemStack> potItems(AlchemyPotBlockEntity be) {
		List<ItemStack> ans = new ArrayList<>();
		for (var s : be.items.getAsList()) {
			if (!s.isEmpty()) ans.add(s.copy());
		}
		return ans;
	}

	private List<BrewOption> findOptions(ServerLevel level, E entity, FluidStack fluid, List<ItemStack> items) {
		List<BrewOption> ans = new ArrayList<>();
		List<ItemStack> copies = new ArrayList<>();
		for (var s : items) copies.add(s.copy());
		var prefix = new AlchemyInv(fluid.copy(), copies, false);
		for (var holder : level.getRecipeManager().getRecipes()) {
			if (!(holder.value() instanceof AlchemyRecipe<?> recipe)) continue;
			if (involvesWitch(recipe)) continue;
			if (!recipe.inputFluid.test(fluid)) continue;
			if (!recipe.matches(prefix, level)) continue;
			List<Ingredient> hints = recipe.getHints(level, prefix);
			if (hints.isEmpty()) continue;
			if (items.size() + hints.size() > AlchemyPotBlockEntity.MAX_SLOTS) continue;
			List<ItemStack> missing = new ArrayList<>();
			boolean ok = true;
			for (var ing : hints) {
				var arr = ing.getItems();
				if (arr.length == 0) {
					ok = false;
					break;
				}
				missing.add(arr[entity.getRandom().nextInt(arr.length)].copyWithCount(1));
			}
			if (ok) ans.add(new BrewOption(holder, missing));
		}
		return ans;
	}

	private static boolean isCompleteMatch(ServerLevel level, FluidStack fluid, List<ItemStack> items) {
		List<ItemStack> copies = new ArrayList<>();
		for (var s : items) copies.add(s.copy());
		var inv = new AlchemyInv(fluid.copy(), copies, true);
		return level.getRecipeManager().getRecipeFor(GLRecipes.ALCHEMY_RT.get(), inv, level).isPresent();
	}

	private static boolean involvesWitch(AlchemyRecipe<?> recipe) {
		if (recipe instanceof WitchMergeRecipe || recipe instanceof WitchEnhanceRecipe) return true;
		if (isWitchFluid(recipe.resultFluid)) return true;
		for (var stack : recipe.inputFluid.getStacks()) {
			if (isWitchFluid(stack)) return true;
		}
		return false;
	}

	private static boolean isWitchFluid(FluidStack stack) {
		return stack.getFluid() instanceof GLHexFluid hex &&
				(hex.brew == HexBrew.WITCH_HEXBREW ||
						hex.brew == HexBrew.WITCH_SPLASH);
	}

}
