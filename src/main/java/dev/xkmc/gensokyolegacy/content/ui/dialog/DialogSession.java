package dev.xkmc.gensokyolegacy.content.ui.dialog;

import dev.xkmc.gensokyolegacy.content.entity.module.TalkModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.network.DialogCloseToClient;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-side state of one step of a conversation.
 *
 * <p>This is what the dialog screen used to be an {@code AbstractContainerMenu}
 * for. The menu only ever existed to sync the screen's contents, but a menu
 * also advertises itself to the rest of the game: inventory tabs, JEI overlays
 * and anything else that special-cases container screens attach themselves to
 * it, whether they are meant to or not. The screen is now a plain
 * {@code Screen}, so a conversation is invisible to all of that, and its
 * contents travel over the {@code content.rpg.network} dialog packets instead -
 * the open handshake, the per-step state, and the clicks that used to be
 * inventory button clicks.
 */
public abstract class DialogSession {

	private static final AtomicInteger COUNTER = new AtomicInteger();

	/**
	 * Identifies this step for as long as the client may still be talking
	 * about it. Every client packet names the session it belongs to, so one
	 * that raced a screen switch is dropped instead of being applied to
	 * whatever dialog happens to be on screen now.
	 */
	public final int id = COUNTER.incrementAndGet();

	public final ServerPlayer player;
	public final YoukaiEntity character;

	protected DialogSession(ServerPlayer player, YoukaiEntity character) {
		this.player = player;
		this.character = character;
	}

	/**
	 * Hand the conversation UI to this session and open it on the client.
	 * Refused when this player is no longer the one talking to the character,
	 * so a callback that arrives after the conversation ended cannot restart it.
	 */
	protected void activate() {
		if (!character.getModule(TalkModule.class).map(e -> e.setSession(this)).orElse(false)) return;
		sync();
	}

	/**
	 * Push the current state to the client, opening its screen or re-skinning
	 * the one it has.
	 */
	protected abstract void sync();

	/**
	 * Apply a click on the option at the given index. The index is the one
	 * this session used when it built the list the client is looking at.
	 */
	public abstract void click(int index);

	/**
	 * Give the character back to its AI and take the conversation UI down.
	 * Options that lead nowhere land here.
	 */
	public void end() {
		character.getModule(TalkModule.class).ifPresent(TalkModule::stopTalking);
	}

	/**
	 * Ask the client to drop the dialog screen.
	 */
	public void close() {
		GensokyoLegacy.HANDLER.toClientPlayer(new DialogCloseToClient(id), player);
	}

	/**
	 * Give the UI up without ending the conversation, for when the client
	 * closed the screen but something else - the trade menu - is about to
	 * take over.
	 */
	public void release() {
		character.getModule(TalkModule.class).ifPresent(e -> e.clearSession(this));
	}

	/**
	 * The live session a client packet refers to, or null when it is stale.
	 */
	public static @Nullable DialogSession resolve(Player player, int session, int character) {
		if (player.level().getEntity(character) instanceof YoukaiEntity ch) {
			var module = ch.getModule(TalkModule.class).orElse(null);
			if (module != null) return module.session(player, session);
		}
		return null;
	}

}
