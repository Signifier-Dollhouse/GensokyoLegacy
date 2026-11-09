package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public record StructBed(
		Holder<EntityType<?>> entity,
		CharacterConfig data,
		Holder<Block>[] bed
) {

	public StructBed(Holder<EntityType<?>> entity,
					 CharacterConfig data,
					 Holder<Block> bed) {
		this(entity, data, new Holder[]{bed});
	}

	@SuppressWarnings("unchecked")
	public StructBed(RegistryEntry<? extends EntityType<?>> entity,
					 CharacterConfig data,
					 Holder<Block> bed) {
		this((Holder<EntityType<?>>) (Holder<?>) entity.get().builtInRegistryHolder(), data, bed);
	}

}
