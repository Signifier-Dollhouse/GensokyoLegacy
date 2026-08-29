package dev.xkmc.gensokyolegacy.content.block.portal;

import dev.xkmc.gensokyolegacy.content.attachment.gap.GapMapping;
import dev.xkmc.gensokyolegacy.content.attachment.gap.GapMappingData;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.base.tile.BaseBlockEntity;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SerialClass
public class GapPortalBlockEntity extends BaseBlockEntity implements IPortalBlockEntity {

	public static boolean isInGap(Level level) {
		return level.dimension().location().equals(GLDimensionGen.GAP.location());
	}

	@SerialField
	@Nullable
	public UUID id;

	@SerialField
	@Nullable
	public PortalSide side;

	public GapPortalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public @Nullable DimensionTransition getPortalDestination(ServerLevel level, Entity e, BlockPos pos) {
		if (getBlockState().getValue(BlockStateProperties.HALF) == Half.TOP) {
			if (level.getBlockEntity(pos.below()) instanceof GapPortalBlockEntity be) {
				return be.getPortalDestination(level, e, pos.below());
			}
		}
		if (id == null) return null;
		var data = GapMappingData.get(level).get(id);
		if (data == null || data.isPending()) return null;
		PortalSide curSide = getSide();
		BlockPos targetPos;
		ResourceLocation targetDim;
		PortalSide targetSide;
		if (curSide == PortalSide.ENTRY) {
			targetPos = data.exitPos();
			targetDim = data.exitDim();
			targetSide = PortalSide.EXIT;
		} else {
			targetPos = data.entryPos();
			targetDim = data.entryDim();
			targetSide = PortalSide.ENTRY;
		}
		if (targetPos == null || targetDim == null) return null;
		ServerLevel targetLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, targetDim));
		if (targetLevel == null) return null;
		ensurePortalAt(targetLevel, targetPos, id, targetSide);
		Vec3 vec3 = targetPos.getBottomCenter();
		return new DimensionTransition(targetLevel, vec3, e.getDeltaMovement(), e.getYRot(), e.getXRot(),
				DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
	}

	public void setPlacedBy(ItemStack stack) {
		if (!(level instanceof ServerLevel sl)) return;
		boolean hadUuid = stack.has(GLItems.DC_UUID);
		PortalSide sideVal = stack.has(GLItems.DC_PORTAL_SIDE) ? stack.get(GLItems.DC_PORTAL_SIDE) : null;
		if (!hadUuid) {
			var nid = UUID.randomUUID();
			stack.set(GLItems.DC_UUID, nid);
			sideVal = PortalSide.ENTRY;
			stack.set(GLItems.DC_PORTAL_SIDE, sideVal);
		}
		if (sideVal == null) sideVal = PortalSide.ENTRY;
		id = stack.get(GLItems.DC_UUID);
		side = sideVal;
		initData(sideVal);
	}

	/**
	 * Pairing invariant: every fresh item (no uuid) creates exactly 2 uuid-bearing endpoints
	 * (block or item). Supports teleport between any 2 points (same or different dimension,
	 * with or without GAP) once the pair is complete. Uses entry/exit naming.
	 * <ul>
	 *   <li>Fresh item (no uuid, any dimension, ENTRY): place block as ENTRY pending
	 *       {entry=here, exit=null} and replace hand with same-uuid EXIT item (see {@link GapPortalItem}).
	 *       Consistent whether in GAP or not.</li>
	 *   <li>Use EXIT item on ENTRY portal (same uuid) while pending: generates the missing side
	 *       – non-GAP entry → GAP exit (proportional Y), GAP entry → overworld exit (spawn).
	 *       Also handles the mirrored case (EXIT block + ENTRY item). Consumes the item.</li>
	 *   <li>Item with uuid+side on complete pair: moves the side matching its side.</li>
	 *   <li>Picking up via {@link GapPortalBlock} clears that endpoint (making pending) and gives item with side.</li>
	 * </ul>
	 */
	public void initData(PortalSide side) {
		if (!(level instanceof ServerLevel sl)) return;
		if (id == null) return;
		var data = GapMappingData.get(sl);
		var prev = data.get(id);
		BlockPos here = getBlockPos();
		ResourceLocation hereDim = sl.dimension().location();
		if (prev == null) {
			// Fresh pair: always pending with only the placed side, consistent whether in GAP or not.
			// The other side will be generated when player uses the paired item on this portal.
			if (side == PortalSide.ENTRY) {
				data.set(id, new GapMapping(here, hereDim, null, null));
			} else {
				data.set(id, new GapMapping(null, null, here, hereDim));
			}
			return;
		}
		if (prev.isPending()) {
			// Fill the null side matching the placed side
			if (side == PortalSide.ENTRY) {
				if (prev.entryPos() == null || prev.entryDim() == null) {
					data.set(id, new GapMapping(here, hereDim, prev.exitPos(), prev.exitDim()));
				} else if (prev.exitPos() == null || prev.exitDim() == null) {
					// Entry already present but we are placing ENTRY again at different pos -> move entry
					if (!here.equals(prev.entryPos()) || !hereDim.equals(prev.entryDim())) {
						destroyIfLoaded(sl, prev.entryPos());
						data.set(id, new GapMapping(here, hereDim, prev.exitPos(), prev.exitDim()));
					}
				}
			} else { // EXIT
				if (prev.exitPos() == null || prev.exitDim() == null) {
					data.set(id, new GapMapping(prev.entryPos(), prev.entryDim(), here, hereDim));
				} else if (prev.entryPos() == null || prev.entryDim() == null) {
					if (!here.equals(prev.exitPos()) || !hereDim.equals(prev.exitDim())) {
						destroyIfLoaded(sl, prev.exitPos());
						data.set(id, new GapMapping(prev.entryPos(), prev.entryDim(), here, hereDim));
					}
				}
			}
			return;
		}
		// Complete pair: move the side matching the placed item's side
		if (side == PortalSide.ENTRY) {
			if (here.equals(prev.entryPos()) && hereDim.equals(prev.entryDim())) return;
			destroyEntryPos(sl, prev);
			data.set(id, new GapMapping(here, hereDim, prev.exitPos(), prev.exitDim()));
		} else {
			if (here.equals(prev.exitPos()) && hereDim.equals(prev.exitDim())) return;
			destroyExitPos(sl, prev);
			data.set(id, new GapMapping(prev.entryPos(), prev.entryDim(), here, hereDim));
		}
	}

	private static void destroyIfLoaded(ServerLevel sl, @Nullable BlockPos pos) {
		if (pos != null && sl.isLoaded(pos)) sl.destroyBlock(pos, false);
	}

	private static void destroyEntryPos(ServerLevel sl, GapMapping prev) {
		if (prev.entryPos() == null || prev.entryDim() == null) return;
		var other = sl.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, prev.entryDim()));
		if (other != null && other.isLoaded(prev.entryPos())) other.destroyBlock(prev.entryPos(), false);
	}

	private static void destroyExitPos(ServerLevel sl, GapMapping prev) {
		if (prev.exitPos() == null || prev.exitDim() == null) return;
		var other = sl.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, prev.exitDim()));
		if (other != null && other.isLoaded(prev.exitPos())) other.destroyBlock(prev.exitPos(), false);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (!(level instanceof ServerLevel sl)) return;
		if (id == null) return;
		var data = GapMappingData.get(sl);
		var prev = data.get(id);
		if (prev == null) return;
		BlockPos here = getBlockPos();
		ResourceLocation hereDim = sl.dimension().location();
		PortalSide curSide = getSide();
		boolean matches;
		if (curSide == PortalSide.ENTRY) {
			matches = prev.entryPos() != null && here.equals(prev.entryPos()) && hereDim.equals(prev.entryDim());
		} else {
			matches = prev.exitPos() != null && here.equals(prev.exitPos()) && hereDim.equals(prev.exitDim());
		}
		if (!matches) {
			sl.destroyBlock(here, false);
			if (sl.getBlockState(here.above()).getBlock() == GLBlocks.GAP_PORTAL.get()) {
				sl.destroyBlock(here.above(), false);
			}
		}
	}

	public static void ensurePortalAt(ServerLevelAccessor sl, BlockPos pos, UUID id, PortalSide side) {
		GapPortalForcer.placePortal(sl, pos, id, side);
	}

	public PortalSide getSide() {
		if (side != null) return side;
		return PortalSide.ENTRY;
	}

	public ItemStack getItem() {
		var ans = GLBlocks.GAP_PORTAL.asStack();
		if (id != null) {
			ans.set(GLItems.DC_UUID, id);
			ans.set(GLItems.DC_PORTAL_SIDE, getSide());
		}
		return ans;
	}

	public ItemStack getItem(PortalSide side) {
		var ans = GLBlocks.GAP_PORTAL.asStack();
		if (id != null) {
			ans.set(GLItems.DC_UUID, id);
			ans.set(GLItems.DC_PORTAL_SIDE, side);
		}
		return ans;
	}

}
