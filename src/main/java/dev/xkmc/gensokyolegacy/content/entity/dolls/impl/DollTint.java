package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

/**
 * Doll tint contract: the west-lead dye a doll is recoloured with. It belongs to the doll layer
 * rather than the pairing base, because a kind with one untinted texture has nothing to dye —
 * {@link Impl} supplies the synced half and {@link DollTintModule} persists it.
 */
public interface DollTint extends DollEntityImpl {

	DyeColor getColor();

	interface Impl extends DollTint {

		@Override
		default DyeColor getColor() {
			return DyeColor.byId(getEntityData().get(DollEntity.DATA_COLOR));
		}

		default void setDyeColor(DyeColor color) {
			DollHost host = asDoll().getHost();
			if (host != null) {
				DollData data = host.findSummoned(asDoll().getUUID());
				if (data != null) {
					data.color = color;
				}
			}
			getEntityData().set(DollEntity.DATA_COLOR, color.getId());
		}

	}

	/**
	 * Tint persistence slice: the synced color definition plus {@link DollData} color
	 * round-trip. Runtime tint access stays in {@link DollTint}.
	 */
	final class DollTintModule implements DollModule {

		private final DollEntity doll;

		public DollTintModule(DollEntity doll) {
			this.doll = doll;
		}

		@Override
		public void defineSynchedData(SynchedEntityData.Builder builder) {
			builder.define(DollEntity.DATA_COLOR, DyeColor.RED.getId());
		}

		@Override
		public void writeValuesTo(SynchedEntityData sync, DollData data) {
			data.color = DyeColor.byId(sync.get(DollEntity.DATA_COLOR));
		}

		@Override
		public void readValuesFrom(SynchedEntityData sync, DollData data) {
			sync.set(DollEntity.DATA_COLOR, data.getColor().getId());
		}

		/** Recolor with a dye item, without consuming it. Same color is a no-op pass-through. */
		@Override
		public InteractionResult interact(Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!(stack.getItem() instanceof DyeItem dye)) return InteractionResult.PASS;
			DyeColor color = dye.getDyeColor();
			if (doll.getColor() == color) return InteractionResult.PASS;
			if (!doll.level().isClientSide()) {
				doll.setDyeColor(color);
			}
			return InteractionResult.CONSUME;
		}

	}
}
