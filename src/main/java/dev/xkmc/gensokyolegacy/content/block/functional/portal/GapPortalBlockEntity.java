package dev.xkmc.gensokyolegacy.content.block.functional.portal;

import dev.xkmc.gensokyolegacy.content.attachment.gap.GapMapping;
import dev.xkmc.gensokyolegacy.content.attachment.gap.GapMappingData;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2library.base.tile.BaseBlockEntity;
import dev.xkmc.l2modularblock.tile_api.TickableBlockEntity;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SerialClass
public class GapPortalBlockEntity extends BaseBlockEntity implements IPortalBlockEntity, TickableBlockEntity {

	public static boolean isInGap(Level level) {
		return level.dimension().location().equals(GLDimensionGen.GAP.location());
	}

	@SerialField
	@Nullable
	public UUID id;

	@SerialField
	@Nullable
	public PortalSide side;

	@SerialField
	public boolean pending = true;

	public GapPortalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void tick() {
		updatePending();
	}

	@Override
	public @Nullable PortalDestination getPortalDestination(ServerLevel level, Entity e, BlockPos pos) {
		if (getBlockState().getValue(BlockStateProperties.HALF) == Half.TOP) {
			if (level.getBlockEntity(pos.below()) instanceof GapPortalBlockEntity be) {
				return be.getPortalDestination(level, e, pos.below());
			}
		}
		if (id == null) return null;
		var data = GapMappingData.get(level).get(id);
		if (data == null || data.isPending()) return null;
		PortalSide curSide = getSide();
		PortalSide targetSide = curSide.other();
		BlockPos targetPos = data.posAt(targetSide);
		ResourceLocation targetDim = data.dimAt(targetSide);
		if (targetPos == null || targetDim == null) return null;
		ServerLevel targetLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, targetDim));
		if (targetLevel == null) return null;
		ensurePortalAt(targetLevel, targetPos, id, targetSide);
		return PortalDestination.of(targetLevel, e, Vec3.atBottomCenterOf(targetPos));
	}

	public void setPlacedBy(ItemStack stack) {
		if (!(level instanceof ServerLevel sl)) return;
		boolean hadUuid = GLItems.DC_UUID.has(stack);
		PortalSide sideVal = GLItems.DC_PORTAL_SIDE.has(stack) ? GLItems.DC_PORTAL_SIDE.get(stack) : null;
		if (!hadUuid) {
			var nid = UUID.randomUUID();
			GLItems.DC_UUID.set(stack, nid);
			sideVal = PortalSide.ENTRY;
			GLItems.DC_PORTAL_SIDE.set(stack, sideVal);
		}
		if (sideVal == null) sideVal = PortalSide.ENTRY;
		id = GLItems.DC_UUID.get(stack);
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
	 *       – non-GAP entry → GAP exit (proportional Y), GAP entry → Gensokyo exit (proportional Y).
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
			data.set(id, new GapMapping(null, null, null, null).with(side, here, hereDim));
		} else if (prev.isPending()) {
			if (prev.isSidePending(side)) {
				data.set(id, prev.with(side, here, hereDim));
			} else {
				BlockPos oldPos = prev.posAt(side);
				ResourceLocation oldDim = prev.dimAt(side);
				if (!here.equals(oldPos) || !hereDim.equals(oldDim)) {
					destroyIfLoaded(sl, oldPos);
					data.set(id, prev.with(side, here, hereDim));
				}
			}
		} else {
			BlockPos oldPos = prev.posAt(side);
			ResourceLocation oldDim = prev.dimAt(side);
			if (!here.equals(oldPos) || !hereDim.equals(oldDim)) {
				destroyPos(sl, oldPos, oldDim);
				data.set(id, prev.with(side, here, hereDim));
			}
		}
	}

	private static void destroyIfLoaded(ServerLevel sl, @Nullable BlockPos pos) {
		if (pos != null && sl.isLoaded(pos)) sl.destroyBlock(pos, false);
	}

	private static void destroyPos(ServerLevel sl, @Nullable BlockPos pos, @Nullable ResourceLocation dim) {
		if (pos == null || dim == null) return;
		var other = sl.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
		if (other != null && other.isLoaded(pos)) other.destroyBlock(pos, false);
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
		BlockPos expectedPos = prev.posAt(curSide);
		ResourceLocation expectedDim = prev.dimAt(curSide);
		boolean matches = here.equals(expectedPos) && hereDim.equals(expectedDim);
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

	public void updatePending() {
		if (!(level instanceof ServerLevel sl)) return;
		boolean shouldPending = true;
		if (id != null) {
			var data = GapMappingData.get(sl).get(id);
			shouldPending = data == null || data.isPending();
		} else {
			shouldPending = false;
		}
		if (pending != shouldPending) {
			pending = shouldPending;
			sync();
		}
	}

	public ItemStack getItem() {
		var ans = GLBlocks.GAP_PORTAL.asStack();
		if (id != null) {
			GLItems.DC_UUID.set(ans, id);
			GLItems.DC_PORTAL_SIDE.set(ans, getSide());
		}
		return ans;
	}

	public ItemStack getItem(PortalSide side) {
		var ans = GLBlocks.GAP_PORTAL.asStack();
		if (id != null) {
			GLItems.DC_UUID.set(ans, id);
			GLItems.DC_PORTAL_SIDE.set(ans, side);
		}
		return ans;
	}

}
