package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public enum DollGloveMode {
	SUMMON(new SummonMode()),
	HEAL_MARK(new HealMarkMode()),
	VOLLEY(new VolleyMode()),
	SUPER(new SuperMode()),
	SUICIDE(new SuicideMode()),
	STOP(new StopMode()),
	EDITOR(new EditorMode());

	private final DollGloveHandler handler;

	DollGloveMode(DollGloveHandler handler) {
		this.handler = handler;
	}

	public ItemStack icon() {
		return handler.icon();
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
	 * Glow color for the cached ray-trace target while this mode is held
	 * (vanilla formatting palette): aqua for summon, green for heal-mark, red
	 * for the three attack modes, gray for stop, gold for the doll editor.
	 */
	public int glowColor() {
		return switch (this) {
			case SUMMON -> 0x55FFFF;
			case HEAL_MARK -> 0x55FF55;
			case VOLLEY, SUPER, SUICIDE -> 0xFF5555;
			case STOP -> 0xAAAAAA;
			case EDITOR -> 0xFFAA00;
		};
	}

}
