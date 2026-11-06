package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The glove's modes, each holding a {@link DollGloveHandler} so behaviour never needs a switch.
 *
 * @see "doc/design/doll/glove.md"
 */
public enum DollGloveMode {
	SUMMON(new SummonMode()),
	VOLLEY(new VolleyMode()),
	SUPER(new SuperMode()),
	SUICIDE(new SuicideMode());

	private final DollGloveHandler handler;

	DollGloveMode(DollGloveHandler handler) {
		this.handler = handler;
	}

	public ItemStack icon() {
		return handler.icon();
	}

	/**
	 * Sub-model id suffix for this mode's textures ({@code item/doll_glove/glove_<name>} and
	 * {@code item/doll_glove/glove_icon_<name>}): the enum names already match the texture files.
	 */
	public String iconName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component displayName() {
		return handler.displayName();
	}

	public Component description() {
		return handler.description();
	}

	public InteractionResultHolder<ItemStack> handleUse(Level level, Player player, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		return handler.handleUse(level, player, hand, stack, item);
	}

	/**
	 * Whether left-click issues this mode's command. Attack modes only: the editor open is
	 * right-click only, so fighting never risks opening a menu.
	 */
	public boolean isAttackCommand() {
		return handler.isAttackCommand();
	}

	/**
	 * Left-click path (server-side): the attack-mode command at the cached ray-trace target,
	 * without the editor check.
	 */
	public void performAttack(ServerPlayer sp, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		handler.performAttack(sp, hand, stack, item);
	}

	/**
	 * Left-click path (server-side): the attack-mode command at a directly punched entity, without
	 * the editor check.
	 */
	public void performAttackOn(ServerPlayer sp, @Nullable LivingEntity target, InteractionHand hand, ItemStack stack, DollGloveItem item) {
		handler.performAttackOn(sp, target, hand, stack, item);
	}

	/**
	 * Outline color for the cached ray-trace target while this mode is held: aqua for rally, red
	 * for the attack modes. Hovered dolls always glow gold regardless (see {@code GloveDollHover}).
	 */
	public int glowColor() {
		return switch (this) {
			case SUMMON -> 0x55FFFF;
			case VOLLEY, SUPER, SUICIDE -> 0xFF5555;
		};
	}

}
