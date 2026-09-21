package dev.xkmc.gensokyolegacy.content.attachment.home.structure;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureInfoUpdateToClient;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SerialClass
public class StructureHomeData {

	private StructureStart start;
	private StructurePiece piece;
	private StructureCache.Builder cacheBuilder;
	private IntegrityVerifier verifier;

	@SerialField
	private final List<BlockPos> containers = new ArrayList<>();
	@SerialField
	private final List<BlockPos> chairs = new ArrayList<>();
	@SerialField
	private final AbnormalCache abnormal = new AbnormalCache();
	@SerialField
	private StructureCache cache;

	public boolean checkInit(StructureHomeHolder holder) {
		if (piece == null) {
			var structure = holder.level().registryAccess().holderOrThrow(holder.key().getStructure()).value();
			var start = holder.level().structureManager().getStructureWithPieceAt(holder.key().pos(), structure);
			if (start.getStructure() == structure && !start.getPieces().isEmpty()) {
				this.start = start;
				piece = start.getPieces().getFirst();
			}
		}
		return true;
	}

	public void tick(StructureHomeHolder holder) {
		if (piece == null) return;
		if (cache == null) {
			if (cacheBuilder == null) {
				cacheBuilder = new StructureCache.Builder(holder.level(), getHouseBound(holder.config()));
			}
			cacheBuilder.tick();
			if (cacheBuilder.isDone()) {
				cache = cacheBuilder.build();
				holder.chunk().setUnsaved(true);
			}
		} else {
			if (verifier == null) {
				verifier = new IntegrityVerifier(holder, getHouseBound(holder.config()),
						getRoomBound(holder.config()), cache, abnormal);
				if (!verifier.isValid()) {
					cache = null;
					cacheBuilder = null;
					verifier = null;
					return;
				}
			}
			if (verifier.tick()) {
				holder.chunk().setUnsaved(true);
			}
		}
	}

	public BlockPos getRoot() {
		return piece.getLocatorPosition();
	}

	public boolean isInRoom(StructureConfig config, BlockPos pos) {
		if (!config.rooms().isEmpty() && piece instanceof TemplateStructurePiece template) {
			var settings = template.placeSettings();
			var origin = template.templatePosition();
			for (var local : config.rooms()) {
				if (worldBox(local, settings, origin).isInside(pos)) return true;
			}
			return false;
		}
		return getRoomBound(config).isInside(pos);
	}

	public BoundingBox getRoomBound(StructureConfig config) {
		if (!config.rooms().isEmpty() && piece instanceof TemplateStructurePiece template) {
			// map precalculated template-local room boxes to world;
			// no scan here, just rotation/mirror/offset coordinate mapping
			var settings = template.placeSettings();
			var origin = template.templatePosition();
			int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
			int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
			for (var local : config.rooms()) {
				var w = worldBox(local, settings, origin);
				x0 = Math.min(x0, w.minX());
				y0 = Math.min(y0, w.minY());
				z0 = Math.min(z0, w.minZ());
				x1 = Math.max(x1, w.maxX());
				y1 = Math.max(y1, w.maxY());
				z1 = Math.max(z1, w.maxZ());
			}
			return new BoundingBox(x0, y0, z0, x1, y1, z1);
		}
		return piece.getBoundingBox();
	}

	private static BoundingBox worldBox(BoundingBox local, StructurePlaceSettings settings, BlockPos origin) {
		var corner = new BlockPos.MutableBlockPos();
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		for (int x : new int[]{local.minX(), local.maxX()})
			for (int y : new int[]{local.minY(), local.maxY()})
				for (int z : new int[]{local.minZ(), local.maxZ()}) {
					var w = StructureTemplate.calculateRelativePosition(settings, corner.set(x, y, z));
					x0 = Math.min(x0, w.getX() + origin.getX());
					y0 = Math.min(y0, w.getY() + origin.getY());
					z0 = Math.min(z0, w.getZ() + origin.getZ());
					x1 = Math.max(x1, w.getX() + origin.getX());
					y1 = Math.max(y1, w.getY() + origin.getY());
					z1 = Math.max(z1, w.getZ() + origin.getZ());
				}
		return new BoundingBox(x0, y0, z0, x1, y1, z1);
	}

	public BoundingBox getHouseBound(StructureConfig config) {
		var bound = piece.getBoundingBox();
		return new BoundingBox(
				bound.minX() + config.xzHouseShrink(),
				bound.minY() + config.floorHouseShrink(),
				bound.minZ() + config.xzHouseShrink(),
				bound.maxX() - config.xzHouseShrink(),
				bound.maxY() - config.topHouseShrink(),
				bound.maxZ() - config.xzHouseShrink()
		);
	}

	public BoundingBox getTotalBound() {
		return start.getBoundingBox().inflatedBy(-12);
	}

	@Nullable
	public BlockPos getContainerAround(StructureHomeHolder holder, BlockPos center, int rxz, int ry, int trail) {
		return HomeSearchUtil.searchBlock(containers, HomeSearchUtil::isValidChest,
				getRoomBound(holder.config()), holder.level(), center, rxz, ry, trail);
	}

	@Nullable
	public BlockPos getChairAround(StructureHomeHolder holder, BlockPos center, int rxz, int ry, int trail) {
		return HomeSearchUtil.searchBlock(chairs, HomeSearchUtil::isValidChair,
				getRoomBound(holder.config()), holder.level(), center, rxz, ry, trail);
	}

	public List<BlockFix> popFix(int count, FixStage stage) {
		if (verifier == null || !verifier.isValid()) return List.of();
		return verifier.popFix(count, stage);
	}

	public int getBrokenCount() {
		return abnormal.air.size() + abnormal.primary.size() + abnormal.secondary.size();
	}

	public StructureInfoUpdateToClient getAbnormality(StructureKey key) {
		if (cache == null) {
			return new StructureInfoUpdateToClient(key, -1, -1, -1);
		}
		return new StructureInfoUpdateToClient(key,
				abnormal.air.size(), abnormal.primary.size(), abnormal.secondary.size()
		);
	}

}
