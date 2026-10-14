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
 * Server-side state of one step of a conversation, in place of the {@code AbstractContainerMenu} the screen ran
 * on: a menu only synced the screen's contents, but anything special-casing a container screen attached itself.
 */
public abstract class DialogSession {

	private static final AtomicInteger COUNTER = new AtomicInteger();

	/**
	 * Identifies this step for as long as the client may still be talking about it. Every client packet names its
	 * session, so one that raced a screen switch is dropped rather than applied to whatever dialog is on screen now.
	 */
	public final int id = COUNTER.incrementAndGet();

	public final ServerPlayer player;
	public final YoukaiEntity character;

	protected DialogSession(ServerPlayer player, YoukaiEntity character) {
		this.player = player;
		this.character = character;
	}

	/**
	 * Hand the conversation UI to this session and open it on the client. Refused when this player is no longer the
	 * one talking to the character, so a callback arriving after the conversation ended cannot restart it.
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
	 * Give the UI up without ending the conversation, for when the client closed the screen but the trade menu
	 * is about to take over.
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
