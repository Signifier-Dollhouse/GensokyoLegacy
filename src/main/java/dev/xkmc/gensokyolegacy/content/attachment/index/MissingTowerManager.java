package dev.xkmc.gensokyolegacy.content.attachment.index;

import dev.xkmc.gensokyolegacy.content.entity.misc.MissingTowerEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.network.QuestStatusToClient;
import dev.xkmc.gensokyolegacy.content.rpg.requirement.MissingTowerRequirement;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Placement, the spawn pass, cross-pickup and completion for missing towers.
 *
 * @see "doc/design/quest/missing_tower.md"
 */
public class MissingTowerManager {

	public static final int MIN_RADIUS = 32;
	public static final int MAX_RADIUS = 128;
	public static final int FOLLOW_RANGE = 32;
	public static final double FOLLOW_DISTANCE = 5;
	public static final double FOLLOW_SPEED = 0.6;
	public static final double FOLLOW_ACCEL = 0.2;
	public static final double FOLLOW_GAIN = 0.25;
	public static final int CHUNK_ATTEMPTS = 4;
	public static final int COLUMN_ATTEMPTS = 8;

	private static final Map<ResourceKey<Level>, Set<ChunkPos>> PENDING = new HashMap<>();

	static MissingTowerState create(ResourceLocation quest, ServerLevel level, BlockPos origin) {
		BlockPos pos = roll(level, origin);
		return new MissingTowerState(quest, level.dimension().location(), pos, origin);
	}

	/**
	 * A uniform point in the 32..128 annulus around the origin, in at most four chunks.
	 */
	public static BlockPos roll(ServerLevel level, BlockPos origin) {
		RandomSource random = level.getRandom();
		Set<Long> chunks = new HashSet<>();
		for (int i = 0; i < CHUNK_ATTEMPTS * COLUMN_ATTEMPTS; i++) {
			double d = Math.sqrt(Mth.lerp(random.nextDouble(),
					(double) MIN_RADIUS * MIN_RADIUS, (double) MAX_RADIUS * MAX_RADIUS));
			double angle = random.nextDouble() * Math.PI * 2;
			int x = origin.getX() + (int) Math.round(Math.cos(angle) * d);
			int z = origin.getZ() + (int) Math.round(Math.sin(angle) * d);
			int cx = x >> 4, cz = z >> 4;
			long key = ChunkPos.asLong(cx, cz);
			if (!chunks.contains(key)) {
				if (chunks.size() >= CHUNK_ATTEMPTS) continue;
				chunks.add(key);
			}
			// getHeight returns minBuildHeight for an unloaded chunk
			level.getChunk(cx, cz);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);
			if (validSpot(level, pos)) return pos;
		}
		return origin.above(2);
	}

	private static boolean validSpot(ServerLevel level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return level.getBlockState(pos).isAir()
				&& level.getBlockState(pos.above()).isAir()
				&& below.isSolid()
				&& below.getFluidState().isEmpty();
	}

	/**
	 * Drops the tower on the first free spot near {@code pos}, scanning down before up so a tower
	 * carried over a cliff lands at the ground rather than floating over the player's head.
	 */
	@Nullable
	private static BlockPos snap(ServerLevel level, BlockPos pos) {
		for (int i = 0; i <= 4; i++) if (validSpot(level, pos.below(i))) return pos.below(i);
		for (int i = 1; i <= 8; i++) if (validSpot(level, pos.above(i))) return pos.above(i);
		return null;
	}

	public static void setGround(MissingTowerState state, ServerLevel level, Vec3 at) {
		BlockPos pos = snap(level, BlockPos.containing(at));
		if (pos == null) pos = snap(level, state.origin());
		if (pos == null) pos = state.origin().above(2);
		state.setGround(pos, level.dimension().location(), level.getGameTime());
		discard(level, state);
	}

	/**
	 * Makes the synced quest progress agree with the record: 1 while carried, 0 otherwise.
	 */
	public static void syncProgress(ServerPlayer player) {
		MissingTowerState state = IndexStorage.get(player.serverLevel()).getTower(player.getUUID());
		var attachment = GLMeta.QUEST.type().getOrCreate(player);
		for (var entry : attachment.data.entrySet()) {
			var data = entry.getValue();
			if (!data.started) continue;
			var holder = player.serverLevel().registryAccess()
					.registryOrThrow(CodecRegistry.Keys.QUEST)
					.getHolder(ResourceKey.create(CodecRegistry.Keys.QUEST, entry.getKey()));
			if (holder.isEmpty()) continue;
			boolean carried = state != null && entry.getKey().equals(state.quest()) && state.carried();
			boolean changed = false;
			for (var req : holder.get().value().requirements().entrySet()) {
				if (!(req.getValue() instanceof MissingTowerRequirement)) continue;
				int want = carried ? 1 : 0;
				if (data.progress.getOrDefault(req.getKey(), 0) != want) {
					data.progress.put(req.getKey(), want);
					changed = true;
				}
			}
			if (changed) GensokyoLegacy.HANDLER.toClientPlayer(
					new QuestStatusToClient(entry.getKey(), data, QuestStatusToClient.Reason.UPDATE), player);
		}
	}

	public static void syncPos(MissingTowerState state, BlockPos pos) {
		state.setPos(pos);
	}

	public static void discard(ServerLevel level, MissingTowerState state) {
		if (state.entity() == null) return;
		Entity entity = level.getEntity(state.entity());
		if (entity != null) entity.discard();
		state.setEntity(null);
	}

	/**
	 * Once a second, over the overworld's record map: ground towers are kept alive with their
	 * chunk, carried towers are grounded when their owner is gone or too far.
	 */
	public static void tick(ServerLevel overworld) {
		IndexStorage storage = IndexStorage.get(overworld);
		MinecraftServer server = overworld.getServer();
		for (var entry : storage.towers().entrySet()) {
			UUID owner = entry.getKey();
			MissingTowerState state = entry.getValue();
			ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, state.dim()));
			if (level == null) continue;
			if (!state.carried()) {
				ensureEntity(level, owner, state);
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(owner);
			if (player == null) continue;
			if (player.level() != level || player.blockPosition().distSqr(state.pos()) > FOLLOW_RANGE * FOLLOW_RANGE) {
				setGround(state, level, Vec3.atCenterOf(state.pos()));
				syncProgress(player);
			} else {
				ensureEntity(level, owner, state);
			}
		}
	}

	public static void onChunkLoad(ServerLevel level, ChunkPos pos) {
		PENDING.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos);
	}

	/**
	 * Runs after the level tick, when chunks queued by {@link #onChunkLoad} are fully loaded.
	 */
	public static void postTick(ServerLevel level) {
		Set<ChunkPos> pending = PENDING.remove(level.dimension());
		if (pending == null || pending.isEmpty()) return;
		IndexStorage storage = IndexStorage.get(level);
		for (var entry : storage.towers().entrySet()) {
			MissingTowerState state = entry.getValue();
			if (state.carried() || !state.dim().equals(level.dimension().location())) continue;
			if (!pending.contains(new ChunkPos(state.pos()))) continue;
			ensureEntity(level, entry.getKey(), state);
		}
	}

	public static void ensureEntity(ServerLevel level, UUID owner, MissingTowerState state) {
		if (state.entity() != null) {
			Entity existing = level.getEntity(state.entity());
			if (existing instanceof MissingTowerEntity tower && owner.equals(tower.getOwnerId())) return;
			if (existing != null) existing.discard();
			state.setEntity(null);
		}
		if (!level.isLoaded(state.pos())) return;
		MissingTowerEntity tower = new MissingTowerEntity(GLEntities.MISSING_TOWER.get(), level);
		tower.setOwner(owner);
		Vec3 at = Vec3.atBottomCenterOf(state.pos());
		tower.setPos(at.x, at.y + 0.35, at.z);
		if (level.addFreshEntity(tower)) state.setEntity(tower.getUUID());
	}

	/**
	 * The only pickup path: the owner takes their own tower, another quest holder adopts it.
	 */
	public static boolean pickup(ServerPlayer player, ServerLevel level, MissingTowerEntity tower) {
		UUID owner = tower.getOwnerId();
		if (owner == null) return false;
		IndexStorage storage = IndexStorage.get(level);
		MissingTowerState state = storage.getTower(owner);
		if (state == null || state.carried() || !tower.getUUID().equals(state.entity())) return false;
		if (!GLMeta.QUEST.type().getOrCreate(player).hasStarted(state.quest())) {
			player.displayClientMessage(GLLang.MissingTower.NOT_STARTED.get(), true);
			return false;
		}
		long now = level.getGameTime();
		MissingTowerState mine = storage.getTower(player.getUUID());
		if (player.getUUID().equals(owner)) {
			state.setCarried(state.dim(), tower.blockPosition(), now);
		} else {
			if (mine != null && mine.carried()) {
				player.displayClientMessage(GLLang.MissingTower.CARRIED.get(), true);
				return false;
			}
			BlockPos pos = tower.blockPosition();
			ResourceLocation dim = level.dimension().location();
			// the clicked entity becomes the picker's: reroll must not discard it
			state.setEntity(null);
			reroll(state, level.getServer());
			if (mine == null) {
				mine = new MissingTowerState(state.quest(), dim, pos, pos);
				storage.putTower(player.getUUID(), mine);
			} else {
				discardRecord(level.getServer(), mine);
			}
			mine.setQuest(state.quest());
			mine.setCarried(dim, pos, now);
		}
		tower.setOwner(player.getUUID());
		(mine != null ? mine : state).setEntity(tower.getUUID());
		syncProgress(player);
		if (!player.getUUID().equals(owner)) {
			ServerPlayer old = level.getServer().getPlayerList().getPlayer(owner);
			if (old != null) syncProgress(old);
		}
		player.displayClientMessage(GLLang.MissingTower.PICKUP.get(), true);
		return true;
	}

	private static void reroll(MissingTowerState state, MinecraftServer server) {
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, state.dim()));
		if (level == null) {
			state.setGround(state.origin().above(2), state.dim(), server.overworld().getGameTime());
			return;
		}
		discard(level, state);
		state.setGround(roll(level, state.origin()), state.dim(), level.getGameTime());
	}

	private static void discardRecord(MinecraftServer server, MissingTowerState state) {
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, state.dim()));
		if (level == null) state.setEntity(null);
		else discard(level, state);
	}

	public static void complete(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return;
		IndexStorage storage = IndexStorage.get(level);
		MissingTowerState state = storage.getTower(player.getUUID());
		if (state == null) return;
		discardRecord(level.getServer(), state);
		storage.removeTower(player.getUUID());
	}

}
