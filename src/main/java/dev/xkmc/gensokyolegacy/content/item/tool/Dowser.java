package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.gensokyolegacy.content.attachment.index.IndexStorage;
import dev.xkmc.gensokyolegacy.content.attachment.index.MissingTowerManager;
import dev.xkmc.gensokyolegacy.content.client.deco.DowserRenderer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Dowser extends Item {

	public static final int RANGE = 4;
	public static final double TOWER_RAY_STEP = 1.0;

	public static boolean isValid(@Nullable BlockEntity be) {
		if (!(be instanceof RandomizableContainerBlockEntity e)) return false;
		var lv = be.getLevel();
		return !(lv instanceof ServerLevel) || e.lootTable != null;
	}

	public static boolean isValid(Entity entity) {
		if (!(entity instanceof AbstractMinecartContainer e)) return false;
		return e.getLootTable() != null;
	}

	public static LinkedHashSet<BlockPos> searchBlock(ServerLevel level, BlockPos pos, int r) {
		var cpos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
		LinkedHashSet<BlockPos> ans = new LinkedHashSet<>();
		for (int ix = -r; ix <= r; ix++) {
			for (int iz = -r; iz <= r; iz++) {
				var chunk = level.getChunk(cpos.x + ix, cpos.z + iz, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				Map<BlockPos, BlockEntity> bss = chunk instanceof ProtoChunk proto ? proto.getBlockEntities() :
						chunk instanceof LevelChunk lc ? lc.getBlockEntities() : Map.of();
				for (var ent : bss.entrySet()) {
					if (isValid(ent.getValue())) {
						ans.add(ent.getKey());
					}
				}
			}
		}
		return ans;
	}

	public static LinkedHashSet<BlockPos> searchEntity(ServerLevel level, BlockPos pos, int r) {
		LinkedHashSet<BlockPos> ans = new LinkedHashSet<>();
		var cpos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
		level.getAllEntities().forEach(e -> {
			ChunkPos chunkPos = e.chunkPosition();
			if (Math.abs(chunkPos.x - cpos.x) > r || Math.abs(chunkPos.z - cpos.z) > r) return;
			if (isValid(e)) {
				ans.add(e.blockPosition());
			}
		});
		return ans;
	}

	/**
	 * A line of composter sparks from the eye along the tower's bearing, so the dowser points at a lost
	 * tower the way it points at a chest. Sent only to the holder with the long-distance flag.
	 */
	private static void castTowerRay(ServerPlayer player) {
		var state = IndexStorage.get(player.serverLevel()).getTower(player.getUUID());
		if (state == null || state.carried()) return;
		if (!state.dim().equals(player.serverLevel().dimension().location())) return;
		Vec3 from = player.position().add(0, player.getBbHeight() / 2, 0);
		Vec3 dir = Vec3.atCenterOf(state.pos()).subtract(from);
		double len = Math.min(dir.length(), MissingTowerManager.MAX_RADIUS);
		if (len < 1.0E-4) return;
		dir = dir.normalize();
		Vec3 hor = DanmakuHelper.getOrientation(dir).rotateDegrees(90);
		for (double d = -2; d < Math.min(len, 20); d += 0.5) {
			Vec3 at;
			at = from.add(hor).add(dir.scale(d));
			player.serverLevel().sendParticles(player, ParticleTypes.FLAME, true,
					at.x, at.y, at.z, 1, 0, 0, 0, 0);
			at = from.subtract(hor).add(dir.scale(d));
			player.serverLevel().sendParticles(player, ParticleTypes.FLAME, true,
					at.x, at.y, at.z, 1, 0, 0, 0, 0);

		}
	}

	public Dowser(Properties properties) {
		super(properties.durability(64));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer sp) {
			var pos = player.blockPosition();
			var ans = searchBlock(sp.serverLevel(), pos, RANGE);
			ans.addAll(searchEntity(sp.serverLevel(), pos, RANGE));
			GensokyoLegacy.HANDLER.toClientPlayer(new DowserToClient(ans), sp);
			castTowerRay(sp);
			if (!player.isCreative()) {
				stack.hurtAndBreak(1, player, e -> e.broadcastBreakEvent(hand));
			}
			player.getCooldowns().addCooldown(this, 20);
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.DOWSER_LORE.get());
	}

	public record DowserToClient(
			LinkedHashSet<BlockPos> pos
	) implements RecordPacketBase {

		@Override
		public void handle(Player player) {
			DowserPacketHandler.handle(player, pos);
		}

	}

	public static class DowserPacketHandler {

		public static void handle(Player player, Set<BlockPos> pos) {
			DowserRenderer.init(player, pos);
		}

	}

}
