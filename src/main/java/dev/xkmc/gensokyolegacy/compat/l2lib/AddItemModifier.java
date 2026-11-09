package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AddItemModifier extends LootModifier {

	public static final Codec<AddItemModifier> CODEC = RecordCodecBuilder.create(i -> codecStart(i).and(i.group(
			ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item),
			ForgeRegistries.ITEMS.getCodec().optionalFieldOf("fail")
					.forGetter(m -> m.fail == Items.AIR ? Optional.empty() : Optional.of(m.fail)),
			Codec.DOUBLE.optionalFieldOf("chance").forGetter(m -> Optional.ofNullable(m.chance))
	)).apply(i, AddItemModifier::new));

	public final Item item, fail;

	@Nullable
	public final Double chance;

	protected AddItemModifier(LootItemCondition[] conditionsIn, Item item, Optional<Item> fail, Optional<Double> chance) {
		super(conditionsIn);
		this.item = item;
		this.fail = fail.orElse(Items.AIR);
		this.chance = chance.orElse(null);
	}

	public AddItemModifier(Item item, @Nullable Double chance, LootItemCondition... conditionsIn) {
		this(item, Items.AIR, chance, conditionsIn);
	}

	public AddItemModifier(Item item, Item fail, @Nullable Double chance, LootItemCondition... conditionsIn) {
		super(conditionsIn);
		this.item = item;
		this.fail = fail;
		this.chance = chance;
	}

	@Override
	protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		if (!context.hasParam(LootContextParams.DAMAGE_SOURCE)) {
			return generatedLoot;
		}
		if (chance == null || context.getRandom().nextDouble() <= chance) {
			generatedLoot.add(new ItemStack(item));
		} else if (fail != Items.AIR) {
			generatedLoot.add(new ItemStack(fail));
		}
		return generatedLoot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}

}
