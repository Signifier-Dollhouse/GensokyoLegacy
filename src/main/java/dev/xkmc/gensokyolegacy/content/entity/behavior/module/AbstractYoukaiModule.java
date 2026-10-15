package dev.xkmc.gensokyolegacy.content.entity.behavior.module;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class AbstractYoukaiModule {

	private final ResourceLocation id;
	protected final YoukaiEntity self;

	public AbstractYoukaiModule(ResourceLocation id, YoukaiEntity self) {
		this.id = id;
		this.self = self;
	}

	public ResourceLocation getId() {
		return id;
	}

	/**
	 * Defines whatever synced accessors this module owns. Runs inside the vanilla {@code Entity}
	 * constructor, before any subclass state exists, so it must be static-safe and is handed the
	 * builder rather than the entity.
	 *
	 * @see "doc/design/youkai/entity.md"
	 */
	public void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	public InteractionResult interact(Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

	public void tickClient() {

	}

	public void tickServer() {

	}

	public boolean handleEntityEvent(byte pId) {
		return false;
	}

	public void onKilled() {
	}
}
