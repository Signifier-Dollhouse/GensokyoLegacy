package dev.xkmc.gensokyolegacy.content.ui.dialog;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.ServerCharacterDialogManager;
import dev.xkmc.gensokyolegacy.content.rpg.handle.ClientHandle;
import dev.xkmc.gensokyolegacy.content.rpg.handle.GroupHandle;
import dev.xkmc.gensokyolegacy.content.rpg.handle.IDialogHandle;
import dev.xkmc.gensokyolegacy.content.rpg.network.FirstDialogToClient;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2menustacker.init.L2MenuStacker;
import dev.xkmc.l2menustacker.screen.packets.CacheMouseToClient;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The topic list a character offers at the start of a conversation: whatever
 * chats, quests and trades it has unlocked right now.
 */
public class FirstDialogSession extends DialogSession {

	public static void open(ServerPlayer sp, YoukaiEntity ch) {
		var handles = ServerCharacterDialogManager.get(sp.serverLevel(), ch.getType()).getInitialConversation(sp, ch);
		new FirstDialogSession(sp, ch, handles, toClient(handles, IDialogHandle::display), null).activate();
	}

	public static void openGroup(ServerPlayer sp, YoukaiEntity ch, GroupHandle group) {
		var members = group.members();
		new FirstDialogSession(sp, ch, members, toClient(members, IDialogHandle::groupLabel), group.text()).activate();
	}

	private static List<ClientHandle> toClient(List<IDialogHandle> handles, Function<IDialogHandle, Component> label) {
		List<ClientHandle> ans = new ArrayList<>(handles.size());
		for (var e : handles) {
			ans.add(new ClientHandle(label.apply(e), ClientHandle.questId(e.getQuest())));
		}
		return ans;
	}

	/**
	 * The server-side handles the client labels point at. Never leaves this
	 * side; the client only ever gets {@link #options}.
	 */
	private final List<IDialogHandle> handles;

	private final List<ClientHandle> options;

	/**
	 * Text to show above the list, or null to fall back to the character's
	 * configured greeting.
	 */
	private final @Nullable Component body;

	private FirstDialogSession(ServerPlayer sp, YoukaiEntity ch, List<IDialogHandle> handles,
			List<ClientHandle> options, @Nullable Component body) {
		super(sp, ch);
		this.handles = handles;
		this.options = options;
		this.body = body;
	}

	@Override
	protected void sync() {
		GensokyoLegacy.HANDLER.toClientPlayer(new FirstDialogToClient(id, character.getId(), body, new ArrayList<>(options)), player);
	}

	@Override
	public void click(int index) {
		if (index < 0 || index >= handles.size()) return;
		// every option here hands off to another screen, so seed that screen's
		// hover state from the mouse position this one is closing over
		L2MenuStacker.PACKET_HANDLER.toClientPlayer(new CacheMouseToClient(), player);
		handles.get(index).open(player, character);
	}

}
