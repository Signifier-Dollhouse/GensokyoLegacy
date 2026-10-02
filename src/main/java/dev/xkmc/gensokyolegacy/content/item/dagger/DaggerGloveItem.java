package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
 * The <b>Dagger Glove</b>: a three-mode launcher for {@link
 * dev.xkmc.gensokyolegacy.content.item.tool.IronDaggerItem}, so that a stack of daggers can be
 * thrown as a pattern instead of one at a time.
 * <p>
 * The glove owns three decisions that {@link DaggerGloveMode} deliberately leaves to it, because
 * they are the same decision in all three modes and belong in one place:
 * <ul>
 * <li><b>Whether the shot happens at all</b> — {@link DaggerGloveMode#HOMING} needs something
 * under the crosshair, and every mode needs its full dagger count in the holder's inventory. Either
 * check failing spends nothing: no daggers, no cooldown, and a message saying why.</li>
 * <li><b>Where the daggers come from</b> — the holder's whole inventory rather than the held
 * stack, since the glove is the weapon and the daggers are its ammunition.</li>
 * <li><b>How long the shot then takes to come back</b> — {@link #cooldown}, which is where a
 * {@link DaggerGloveRune} adds its price.</li>
 * </ul>
 * All of it is server-side; the client only reports the use and swings. The cooldown is checked
 * here rather than relied upon from the client, because a client will not send the use while it
 * shows the glove as cooling down but nothing stops one from sending it anyway. Glove shots also
 * post no {@code DanmakuUseEvent}: that event prices a danmaku item's own use, and the glove has
 * already priced its shot two ways over — by daggers and by cooldown.
 */
public class DaggerGloveItem extends Item {

	/** How far {@link DaggerGloveMode#HOMING} will look for something to home on. */
	public static final double TARGET_RANGE = 64;

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
	 * The id of the glove's rune, or null when it carries none. This is the form the glove stores
	 * and hands to a flying dagger; resolve it with {@link DaggerGloveRunes#get}.
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
	 * Ticks before this stack can fire again: its mode's own cooldown plus whatever its rune
	 * charges.
	 * <p>
	 * This one line is the whole of the rune system's effect on the glove
	 * ({@code doc/design/dagger_glove.md} §5): a rune that wanted to be free, or ruinous, only has
	 * to return a different number from {@link DaggerGloveRune#cooldownCost()}.
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
		DaggerGloveMode mode = getMode(stack);
		Entity target = null;
		if (mode.needsTarget()) {
			target = target(player);
			if (target == null) {
				player.displayClientMessage(GLLang.ItemDaggerGlove.NO_TARGET.get(), true);
				return InteractionResultHolder.fail(stack);
			}
		}
		if (!takeDaggers(player, mode.count())) {
			player.displayClientMessage(GLLang.ItemDaggerGlove.NO_DAGGER.get(), true);
			return InteractionResultHolder.fail(stack);
		}
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResultHolder.fail(stack);
		mode.fire(serverLevel, player, DaggerGloveMode.dagger(), getRuneId(stack), target);
		playThrowSound(level, player);
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, cooldown(stack));
		return InteractionResultHolder.sidedSuccess(stack, false);
	}

	/**
	 * What a homing shot would aim at: whatever the holder is looking at, traced fresh on this use
	 * rather than cached by a client, and accepted only if the holder's own danmaku could actually
	 * hit it — so allies and spectators are never homed on.
	 * <p>
	 * Walls do not block the trace: the daggers turn, so "something behind that wall" is a target
	 * this mode is for rather than one to refuse ({@code doc/design/dagger_glove.md} §2).
	 */
	@Nullable
	private static Entity target(Player player) {
		var hit = RayTraceUtil.rayTraceEntity(player, TARGET_RANGE, e -> e != player
				&& e instanceof LivingEntity living && !living.isSpectator()
				&& IDanmakuEntity.canHurt(player, living));
		return hit == null ? null : hit.getEntity();
	}

	/**
	 * Takes exactly {@code count} iron daggers out of the holder's inventory — every slot, the
	 * off hand and armour included — and reports whether the whole count was there.
	 * <p>
	 * Partial is failure. A homing volley that could only afford four of its ten daggers would be
	 * a lopsided spread aimed at a target sixty blocks away, which is worse than not firing; the
	 * fewest daggers are spent (none) and the shot is refused outright instead. Everything taken
	 * here is handed to a {@link
	 * dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity}, which brings it back.
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
		// no rune is applied by anything yet; this line is how one will announce itself
		if (getRuneId(stack) != null) {
			list.add(GLLang.ItemDaggerGlove.RUNE.get(rune.cooldownCost()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	/**
	 * A fresh glove stack in the given mode, for the selector wheel to draw: the wheel shows the
	 * glove's own texture per mode rather than a separate icon, so all it has to carry is the mode
	 * component ({@code doc/design/dagger_glove.md} §6).
	 */
	public static ItemStack displayStack(DaggerGloveMode mode) {
		ItemStack stack = new ItemStack(GLItems.DAGGER_GLOVE.get());
		setMode(stack, mode);
		return stack;
	}

}