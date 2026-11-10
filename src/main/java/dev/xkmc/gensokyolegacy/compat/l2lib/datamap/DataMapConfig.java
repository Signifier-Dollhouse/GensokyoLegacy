package dev.xkmc.gensokyolegacy.compat.l2lib.datamap;

import dev.xkmc.l2library.serial.config.BaseConfig;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;

/**
 * A data map's {@code values} object on 1.20.1. The map is declared by each concrete subclass,
 * because l2serial cannot resolve a {@code V} field inherited from a generic superclass.
 */
@SerialClass
public abstract class DataMapConfig<V> extends BaseConfig {

	public abstract LinkedHashMap<ResourceLocation, V> values();

}
