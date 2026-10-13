package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.common.GloveTargeting;
import dev.xkmc.gensokyolegacy.content.item.tool.IronDaggerItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The <b>Dagger Glove</b>: a three-mode launcher for {@link IronDaggerItem}, so that a stack of
 * daggers can be thrown as a pattern instead of one at a time.
 * <p>
 * The glove owns the three decisions {@link DaggerGloveMode} leaves to it, because they are the
 * same in all three modes: whether the shot happens at all (homing needs a target, every mode
 * needs its full dagger count, and a check that fails spends nothing), where the daggers come
 * from — the holder's whole inventory, since the glove is the weapon and the daggers its
 * ammunition — and how long the shot then takes to come back ({@link #cooldown}, where a
 * {@link DaggerGloveRune} adds its price).
 * <p>
 * All of it is server-side; the client only reports the use and swings. The cooldown is re-checked
 * here rather than trusted from the client, which sends the use whatever it displays.
 *
 * @see doc/design/dagger_glove.md
 */
public class DaggerGloveItem extends Item implements GloveTargeting {

	/** How far {@link DaggerGloveMode#HOMING} will look for something to home on. */
	public static final double TARGET_RANGE = 64;

	/**
	 * How long a traced homing target stays usable: half a second, because a homing shot fires on
	 * the same click that asks for the target (dagger_glove.md §2d).
	 */
	public static final long TARGET_TTL = 60;

	public DaggerGloveItem(Properties props) {
		super(props.stacksTo(1));
	}

	public static DaggerGloveMode getMode(ItemStack stack) {
		return GLItems.DAGGER_GLOVE_MODE.getOrDefault(stack, DaggerGloveMode.SINGLE);
	}

	public static void setMode(ItemStack stack, DaggerGloveMode mode) {
		GLItems.DAGGER_GLOVE_MODE.set(stack, mode);
	}

	/**
	 * Model-override value for {@code gensokyolegacy:dagger_glove_display}: the held mode's
	 * ordinal + 1 selects its held override, each with its own hand skin; an icon display stack
	 * (wheel or sidebar, marked with {@code DAGGER_GLOVE_ICON}) instead uses the icon range behind
	 * it. Values are emitted ascending, so they match exactly (glove.md §3b).
	 */
	public static float displayPredicate(ItemStack stack, Level level, LivingEntity entity, int seed) {
		int ordinal = getMode(stack).ordinal();
		return stack.has(GLItems.DAGGER_GLOVE_ICON.get()) ?
				DaggerGloveMode.values().length + 1 + ordinal : ordinal + 1;
	}

	/**
	 * The id of the glove's rune, or null when it carries none — the form the glove stores and
	 * hands to a flying dagger. Resolve it with {@link DaggerGloveRunes#get}.
	 */
	@Nullable
	public static ResourceLocation getRuneId(ItemStack stack) {
		return GLItems.DAGGER_GLOVE_RUNE.get(stack);
	}

	/** The glove's rune, or {@link DaggerGloveRunes#NONE} when it carries none or an unknown one. */
	public static DaggerGloveRune getRune(ItemStack stack) {
		ResourceLocation id = getRuneId(stack);
		return id == null ? DaggerGloveRunes.NONE : DaggerGloveRunes.get(id);
	}

	public static void setRune(ItemStack stack, ResourceLocation id) {
		GLItems.DAGGER_GLOVE_RUNE.set(stack, id);
	}

	/**
	 * Ticks before this stack can fire again: the mode's own cooldown plus the rune's price. This
	 * one line is the whole of the rune system's effect on the glove (dagger_glove.md §5).
	 */
	public int cooldown(ItemStack stack) {
		return getMode(stack).cooldown() + getRune(stack).cooldownCost();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		// the server owns every decision below; the client only reports the use
		if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
		DaggerGloveMode mode = getMode(stack);
		LivingEntity target = null;
		if (mode.needsTarget()) {
			// the shared glove cache (glove.md §2): a hint from the client's crosshair trace,
			// re-validated here — a hint is not authority, so the glove's own predicate runs again.
			target = GLMeta.GLOVE_TARGET.type().getOrCreate(sp).resolve(sp, stack);
			if (target == null || !acceptsTarget(stack, sp, target)) {
				player.displayClientMessage(GLLang.ItemDaggerGlove.NO_TARGET.get(), true);
				return InteractionResultHolder.fail(stack);
			}
		}
		if (!takeDaggers(player, mode.count())) {
			player.displayClientMessage(GLLang.ItemDaggerGlove.NO_DAGGER.get(), true);
			return InteractionResultHolder.fail(stack);
		}
		mode.fire(sp.serverLevel(), sp, DaggerGloveMode.dagger(), getRuneId(stack), target);
		playThrowSound(level, player);
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, cooldown(stack));
		return InteractionResultHolder.sidedSuccess(stack, false);
	}

	// ---------- shared target cache (glove.md §2) ----------

	@Override
	public double targetRange() {
		return TARGET_RANGE;
	}

	/**
	 * False: the daggers turn, so refusing a target behind a wall would only make the mode less
	 * useful. The trace answers "what is under the crosshair", not "what can I shoot at".
	 */
	@Override
	public boolean blockedByBlocks() {
		return false;
	}

	@Override
	public long targetTtl() {
		return TARGET_TTL;
	}

	/**
	 * What a homing shot will aim at: whatever the holder is looking at, accepted only if the
	 * holder's own danmaku could hit it — so allies and spectators are never homed on.
	 * <p>
	 * The aimed modes answer false here rather than at the call site, which is also what keeps them
	 * from lighting anything up.
	 */
	@Override
	public boolean acceptsTarget(ItemStack stack, Player holder, LivingEntity candidate) {
		if (!getMode(stack).needsTarget()) return false;
		return candidate != holder && !candidate.isSpectator() && IDanmakuEntity.canHurt(holder, candidate);
	}

	/** The homing target glows red, like the doll glove's attack modes; the aimed modes draw nothing. */
	@Nullable
	@Override
	public Integer targetGlow(ItemStack stack) {
		return getMode(stack).needsTarget() ? getMode(stack).glowColor() : null;
	}

	/**
	 * Takes exactly {@code count} iron daggers from the holder's inventory — every slot, the off
	 * hand and armour included — and reports whether the whole count was there.
	 * <p>
	 * Partial is failure: nothing is spent and the shot is refused rather than firing a lopsided
	 * spread. Each dagger is handed to the {@link IronDaggerBulletEntity} that carries it, which is
	 * what brings it back.
	 */
	private static boolean takeDaggers(Player player, int count) {
		Inventory inventory = player.getInventory();
		int taken = 0;
		for (int slot = 0; slot < inventory.getContainerSize() && taken < count; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.is(GLItems.IRON_DAGGER.get())) continue;
			int n = Math.min(count - taken, stack.getCount());
			stack.shrink(n);
			if (stack.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
			taken += n;
		}
		return taken == count;
	}

	private static void playThrowSound(Level level, Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(),
				SoundSource.PLAYERS, 1F, 1F);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		DaggerGloveMode mode = getMode(stack);
		list.add(GLLang.ItemDaggerGlove.MODE.get(mode.displayName()).withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemGlove.WHEEL.get(L2Keys.WHEEL.map.getKey().getDisplayName()).withStyle(ChatFormatting.GRAY));
		list.add(mode.description());
		DaggerGloveRune rune = getRune(stack);
		// nothing sets a rune yet; this line is how one will announce itself
		if (getRuneId(stack) != null) {
			list.add(GLLang.ItemDaggerGlove.RUNE.get(rune.cooldownCost()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	/**
	 * A fresh glove stack carrying only the given mode — the base of an icon display stack.
	 */
	public static ItemStack displayStack(DaggerGloveMode mode) {
		ItemStack stack = new ItemStack(GLItems.DAGGER_GLOVE.get());
		setMode(stack, mode);
		return stack;
	}

	/**
	 * As {@link #displayStack}, but marked as an icon display so the wheel and the sidebar draw
	 * this mode's icon. The four held textures are identical copies of one glove, so neither can
	 * tell the modes apart any other way (dagger_glove.md §6b).
	 */
	public static ItemStack iconStack(DaggerGloveMode mode) {
		ItemStack stack = displayStack(mode);
		stack.set(GLItems.DAGGER_GLOVE_ICON.get(), Unit.INSTANCE);
		return stack;
	}

}