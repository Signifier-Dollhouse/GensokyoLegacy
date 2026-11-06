package dev.xkmc.gensokyolegacy.content.entity.foundation;

import dev.xkmc.l2core.base.entity.SyncedData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.item.ItemStack;

public class SyncedDataTypes {


	public static final SyncedData.Serializer<ItemStack> STACK = new SyncedData.Simple<>(EntityDataSerializers.ITEM_STACK, ItemStack.OPTIONAL_CODEC);

}
