package dev.xkmc.gensokyolegacy.content.attachment.party.item;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The data component the party wand carries its simulated result on. Vanilla syncs item
 * components, so the overlay and the renderer read it with no transport of their own.
 *
 * @see doc/design/party/debug.md
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