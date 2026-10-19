package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A gadget item: the one doll item that carries no state at all. It summons a kind of its own and
 * comes back as itself whatever the entry held, so it has no bar, no tint, no name and no loadout.
 *
 * @see "doc/design/doll/gadget.md"
 */
public class DollGadgetItem extends DollItem {

	public DollGadgetItem(Properties props, ResourceLocation type) {
		super(props, type);
	}

	/** Nothing to carry back: a gadget is summoned whole or not at all. */
	@Override
	protected ItemStack makeStack(@Nullable DollData data) {
		return new ItemStack(this);
	}

	/** A gadget has no loadout, so the click is nothing at all — not an empty editor. */
	@Override
	public void handleClick(ServerPlayer sp, PlayerSlot<?> slot) {
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		return Optional.empty();
	}

}