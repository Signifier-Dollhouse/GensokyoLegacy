package dev.xkmc.gensokyolegacy.content.rpg.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Hands over a fixed stack, for rewards the player picks from a menu rather than the quest's own
 * reward list.
 */
public record GiveItemAction(Item item, int count) implements DialogAction<GiveItemAction> {

	public static final Codec<GiveItemAction> CODEC = RecordCodecBuilder.create(i -> i.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(GiveItemAction::item),
			Codec.INT.optionalFieldOf("count", 1).forGetter(GiveItemAction::count)
	).apply(i, GiveItemAction::new));

	@Override
	public void execute(ActionContext context) {
		context.sp().getInventory().placeItemBackInInventory(new ItemStack(item, count));
	}

	@Override
	public Codec<GiveItemAction> codec() {
		return CODEC;
	}

}
