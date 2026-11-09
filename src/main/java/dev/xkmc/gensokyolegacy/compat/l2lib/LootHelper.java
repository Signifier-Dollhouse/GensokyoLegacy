package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * Loot-table helpers for the 1.20.1 loot API.
 */
public record LootHelper(RegistrateBlockLootTables pvd) {

	public LootPoolSingletonContainer.Builder<?> item(Item item) {
		return LootItem.lootTableItem(item);
	}

	public LootPoolSingletonContainer.Builder<?> item(Item item, int count) {
		return LootItem.lootTableItem(item)
				.apply(SetItemCountFunction.setCount(ConstantValue.exactly(count)));
	}

	public LootItemCondition.Builder intState(Block block, Property<Integer> prop, int val) {
		return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
				.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(prop, val));
	}

	public <T extends Comparable<T> & StringRepresentable> LootItemCondition.Builder enumState(
			Block block, Property<T> prop, T val) {
		return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
				.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(prop, val));
	}

}
