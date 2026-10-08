package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The data component the party wand carries its simulated result on.
 * <p>
 * A component rather than a chat message or a packet because it needs no machinery at
 * all: vanilla keeps item components in sync to the client on its own, so the server can
 * plan a party once and the overlay can read the answer whenever the wand is held.
 */
public final class PartyWandComponent {

	public static final DataComponentType<PartyWandData> TYPE = new DataComponentType.Builder<PartyWandData>()
			.persistent(PartyWandData.CODEC)
			.build();

	/** Called from the mod's register event; see {@code GLMetaEventHandlers}. */
	public static void register(RegisterEvent event) {
		event.register(Registries.DATA_COMPONENT_TYPE, GensokyoLegacy.loc("party_wand"), () -> TYPE);
	}

	private PartyWandComponent() {
	}
}