package dev.xkmc.gensokyolegacy.content.client.deco;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureOutlineRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DowserRenderer {

    public static final int DURATION = 20 * 30;

    private static Level level;
    private static final Map<BlockPos, Long> POS = new LinkedHashMap<>();

    public static void init(Player player, Set<BlockPos> set) {
        if (level != player.level()) {
            level = player.level();
            POS.clear();
        }
        long expire = player.level().getGameTime() + DURATION;
        for (var pos : set) POS.put(pos, expire);
    }

    public static void tickClient() {
        var cl = Minecraft.getInstance().level;
        if (cl != level) {
            level = null;
            POS.clear();
            return;
        }
        if (cl == null) return;
        long time = cl.getGameTime();
        POS.entrySet().removeIf(e -> {
            if (time >= e.getValue()) return true;
            return !cl.isLoaded(e.getKey());
        });
    }

    public static void renderOutline(PoseStack pose, Vec3 camera) {
        var cl = Minecraft.getInstance().level;
        if (cl != level) {
            level = null;
            POS.clear();
            return;
        }
        if (POS.isEmpty()) return;
        StructureOutlineRenderer.renderPosSet(pose, camera, POS.keySet());
    }
}
