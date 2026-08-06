package dev.xkmc.gensokyolegacy.content.attachment.datamap;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public record DialogConfig(String greeting, String trade) {

	@Nullable
	public static DialogConfig of(EntityType<?> key) {
		return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(key).getData(GLMeta.DIALOG_DATA.reg());
	}

}
