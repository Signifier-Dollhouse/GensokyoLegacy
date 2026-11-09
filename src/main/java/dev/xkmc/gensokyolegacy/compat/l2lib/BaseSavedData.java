package dev.xkmc.gensokyolegacy.compat.l2lib;

import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

@SerialClass
public class BaseSavedData<T> extends SavedData {

	private final Class<T> cls;

	public BaseSavedData(Class<T> cls) {
		this.cls = cls;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		TagCodec.toTag(tag, cls, this);
		return tag;
	}

	@Override
	public boolean isDirty() {
		return true;
	}

}
