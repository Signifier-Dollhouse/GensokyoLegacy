package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.common.world.PieceBeardifierModifier;

/**
 * Jigsaw piece with its own Beardifier adjustment, so a bastion keeps the gate on BEARD_THIN while the other pieces
 * opt out with {@link TerrainAdjustment#NONE}. It has its own {@link StructurePieceType} so the choice survives a
 * structure-start reload.
 *
 * @see "doc/design/structure/terrain_fit.md"
 */
public class GLBeardedPiece extends PoolElementStructurePiece implements PieceBeardifierModifier {

	private static final String ADJUSTMENT = "terrain_adjustment";

	private final TerrainAdjustment adjustment;

	public GLBeardedPiece(StructureTemplateManager manager, StructurePoolElement element, BlockPos position,
						  int groundLevelDelta, Rotation rotation, BoundingBox box, TerrainAdjustment adjustment) {
		super(manager, element, position, groundLevelDelta, rotation, box);
		this.adjustment = adjustment;
	}

	public GLBeardedPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(context, tag);
		this.adjustment = TerrainAdjustment.CODEC
				.parse(NbtOps.INSTANCE, StringTag.valueOf(tag.getString(ADJUSTMENT)))
				.result().orElse(TerrainAdjustment.NONE);
	}

	/**
	 * Copy a placed jigsaw piece into a bearded piece, junctions included.
	 */
	public static GLBeardedPiece of(StructureTemplateManager manager, PoolElementStructurePiece piece,
									TerrainAdjustment adjustment) {
		GLBeardedPiece ans = new GLBeardedPiece(manager, piece.getElement(), piece.getPosition(),
				piece.getGroundLevelDelta(), piece.getRotation(), piece.getBoundingBox(), adjustment);
		for (JigsawJunction junction : piece.getJunctions()) {
			ans.addJunction(junction);
		}
		return ans;
	}

	@Override
	public StructurePieceType getType() {
		return GLWorldGen.BEARDED.get();
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString(ADJUSTMENT, adjustment.getSerializedName());
	}

	@Override
	public BoundingBox getBeardifierBox() {
		return getBoundingBox();
	}

	@Override
	public TerrainAdjustment getTerrainAdjustment() {
		return adjustment;
	}

}
