package dev.xkmc.gensokyolegacy.content.ui.dialog;

import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.action.ActionContext;
import dev.xkmc.gensokyolegacy.content.rpg.dialog.Dialog;
import dev.xkmc.gensokyolegacy.content.rpg.handle.ClientHandle;
import dev.xkmc.gensokyolegacy.content.rpg.handle.IDialogHandle;
import dev.xkmc.gensokyolegacy.content.rpg.network.SimpleDialogToClient;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A single line of a conversation: one dialog, and the options it offers. The
 * server resolves every click, so a random option needs no client guesswork -
 * the result of the roll is simply the next state pushed back here.
 */
public class SimpleDialogSession extends DialogSession {

    public static void open(ServerPlayer sp, YoukaiEntity ch, IDialogHandle handle, Holder<Dialog> dialog) {
        if (ch instanceof GeoYoukaiAnim anim) {
            anim.broadcastDialogAnim(dialog.value().animations(), sp.getRandom());
        }
        new SimpleDialogSession(sp, ch, handle, dialog).activate();
    }

    /**
     * The quest this conversation is about, which every action it runs is
     * scoped to.
     */
    public final IDialogHandle handle;

    private Holder<Dialog> dialog;

    private SimpleDialogSession(ServerPlayer sp, YoukaiEntity ch, IDialogHandle handle, Holder<Dialog> dialog) {
        super(sp, ch);
        this.handle = handle;
        this.dialog = dialog;
    }

    @Override
    protected void sync() {
        GensokyoLegacy.HANDLER.toClientPlayer(new SimpleDialogToClient(id, character.getId(),
                dialog.unwrapKey().orElseThrow().location(),
                ClientHandle.questId(handle.getQuest()), new ArrayList<>(visibleOptions())), player);
    }

    /**
     * The indices of the options this player passes the conditions of, in
     * display order. The client draws its list from exactly this, so a click
     * index always names the same option on both sides.
     */
    private List<Integer> visibleOptions() {
        var options = dialog.value().options();
        List<Integer> ans = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).match(player, character)) ans.add(i);
        }
        return ans;
    }

    @Override
    public void click(int index) {
        var options = dialog.value().options();
        if (index < 0 || index >= options.size()) return;
        var option = options.get(index);
        // the client only ever shows what was unlocked when this state was
        // sent, but conditions can lapse before the click gets here
        if (!option.match(player, character)) return;
        var result = option.resolve(player.getRandom());
        var context = new ActionContext(player, character, handle.getQuest());
        for (var e : result.actions()) {
            e.execute(context);
        }
        var next = result.next();
        if (next.isEmpty()) {
            end();
            return;
        }
        dialog = next.get();
        sync();
    }

}
