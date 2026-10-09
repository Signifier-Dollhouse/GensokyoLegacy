package dev.xkmc.gensokyolegacy.content.entity.module;

import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationState;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.ui.dialog.DialogSession;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SerialClass
public class TalkModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("talk");

	private ServerPlayer talkTarget;

	/**
	 * The conversation step this player is currently looking at, if it is a
	 * dialog. Cleared as soon as the client drops the screen; whether the
	 * character keeps talking is decided by {@link #tickServer()}, which also
	 * accepts a container menu - the trade screen - as a stand-in.
	 */
	private @Nullable DialogSession session;

	public TalkModule(YoukaiEntity self) {
		super(ID, self);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!self.mayInteract(player, GLBrains.TALK.get())) return InteractionResult.PASS;
		if (self.getReputation(player) == ReputationState.ENEMY) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.isEmpty()) return InteractionResult.PASS;
		if (player instanceof ServerPlayer sp) {
			if (talkTarget != null) return InteractionResult.FAIL;
			beginTalking(sp);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void tickServer() {
		if (talkTarget == null) return;
		if (talkTarget.isRemoved() || !talkTarget.isAlive() || talkTarget.level() != self.level() ||
				talkTarget.distanceTo(self) > 5 || !hasTalkUi()) {
			stopTalking();
		}
	}

	/**
	 * Whether the player still has something on screen for this conversation:
	 * either a live dialog session, or a container menu that took over from one.
	 */
	private boolean hasTalkUi() {
		if (session != null) return session.player == talkTarget;
		return talkTarget.containerMenu instanceof ITalkMenu menu && menu.getCharacter() == self;
	}

	/**
	 * Begin a conversation with a player, opening the topic list. Every path
	 * that starts one goes through here - talking, feeding, gifting - so this
	 * module stays the single record of who the character is talking to.
	 */
	public void beginTalking(ServerPlayer player) {
		if (talkTarget != null) return;
		// A visitor's window can close mid-conversation; the hold buys her enough
		// time to finish handing something over or close a trade.
		self.getModule(VisitModule.class).ifPresent(e -> e.hold(VisitModule.TALK_HOLD));
		talkTarget = player;
		self.setTalkTo(player, -1);
	}

	@Override
	public void onKilled() {
		stopTalking();
	}

	public void stopTalking() {
		if (talkTarget == null) return;
		var closed = session;
		session = null;
		talkTarget = null;
		self.setTalkTo(null, -1);
		if (closed != null) closed.close();
	}

	/**
	 * Make a session the conversation's UI, replacing whatever dialog was on
	 * screen. Refuses players that are not the one talking, so a callback that
	 * arrives after the conversation ended cannot restart it.
	 */
	public boolean setSession(DialogSession session) {
		if (session.player != talkTarget) return false;
		this.session = session;
		return true;
	}

	/**
	 * Drop a session the client is no longer showing. Leaves the conversation
	 * alone: another screen may be about to take it over.
	 */
	public void clearSession(DialogSession session) {
		if (this.session == session) this.session = null;
	}

	/**
	 * The live session a client packet refers to, or null when it is stale.
	 * Client packets name the session they were sent for, so a click or a close
	 * that raced a screen switch is dropped rather than applied to whatever
	 * dialog is current now.
	 */
	public @Nullable DialogSession session(Player player, int session) {
		if (this.session == null) return null;
		if (this.session.id != session || this.session.player != player) return null;
		return this.session;
	}

	public boolean isTalking() {
		return talkTarget != null;
	}

	public interface ITalkMenu {

		@Nullable YoukaiEntity getCharacter();

	}

}
