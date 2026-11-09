package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Groups the entity data accessors a class defines so they can be registered and saved in one call.
 * 1.20.1 registers straight onto {@link SynchedEntityData}, so callers pass the entity's own
 * {@code entityData} from {@code defineSynchedData()}.
 */
public class SyncedData {

	public static final Serializer<Integer> INT;
	public static final Serializer<BlockPos> BLOCK_POS;
	public static final Serializer<String> STRING;
	public static final Serializer<Optional<UUID>> UUID;

	static {
		INT = new Simple<>(EntityDataSerializers.INT, Codec.INT);
		BLOCK_POS = new Simple<>(EntityDataSerializers.BLOCK_POS, BlockPos.CODEC);
		STRING = new Simple<>(EntityDataSerializers.STRING, Codec.STRING);
		UUID = new Opt<>(EntityDataSerializers.OPTIONAL_UUID, UUIDUtil.CODEC);
	}

	private final Definer cls;

	private final List<Data<?>> list = new ArrayList<>();

	@Nullable
	private final SyncedData parent;

	public SyncedData(Definer cls) {
		this.cls = cls;
		parent = null;
	}

	public SyncedData(Definer cls, SyncedData parent) {
		this.cls = cls;
		this.parent = parent;
	}

	public void register(SynchedEntityData data) {
		for (Data<?> entry : list) {
			entry.register(data);
		}
		if (parent != null)
			parent.register(data);
	}

	public <T> EntityDataAccessor<T> define(Serializer<T> ser, T init, @Nullable String name) {
		Data<T> data = new Data<>(ser, init, name);
		list.add(data);
		return data.data;
	}

	public void write(RegistryAccess pvd, CompoundTag tag, SynchedEntityData data) {
		for (Data<?> entry : list) {
			entry.write(pvd, tag, data);
		}
		if (parent != null)
			parent.write(pvd, tag, data);
	}

	public void read(RegistryAccess pvd, CompoundTag tag, SynchedEntityData data) {
		for (Data<?> entry : list) {
			entry.read(pvd, tag, data);
		}
		if (parent != null)
			parent.read(pvd, tag, data);
	}

	private class Data<T> {

		private final Serializer<T> ser;
		private final EntityDataAccessor<T> data;
		private final T init;
		private final String name;

		private Data(Serializer<T> ser, T init, @Nullable String name) {
			this.ser = ser;
			this.data = cls.define(ser.data());
			this.init = init;
			this.name = name;
		}

		private void register(SynchedEntityData data) {
			data.define(this.data, this.init);
		}

		public void write(RegistryAccess pvd, CompoundTag tag, SynchedEntityData entityData) {
			if (name == null) return;
			Tag ans = ser.write(pvd, entityData.get(data));
			if (ans != null) tag.put(name, ans);
		}

		public void read(RegistryAccess pvd, CompoundTag tag, SynchedEntityData entityData) {
			if (name == null) return;
			var in = tag.get(name);
			entityData.set(data, Optional.ofNullable(in).map(e -> ser.read(pvd, e)).orElse(init));
		}
	}

	public interface Serializer<T> {

		EntityDataSerializer<T> data();

		@Nullable
		Tag write(RegistryAccess pvd, T t);

		@Nullable
		T read(RegistryAccess pvd, Tag tag);

	}

	public record Simple<T>(EntityDataSerializer<T> data, Codec<T> codec) implements Serializer<T> {

		@Nullable
		public Tag write(RegistryAccess pvd, T t) {
			return codec.encodeStart(NbtOps.INSTANCE, t).result().orElse(null);
		}

		@Nullable
		public T read(RegistryAccess pvd, Tag tag) {
			return codec.parse(NbtOps.INSTANCE, tag).result().orElse(null);
		}

	}

	public record Opt<T>(EntityDataSerializer<Optional<T>> data, Codec<T> codec) implements Serializer<Optional<T>> {

		@Nullable
		public Tag write(RegistryAccess pvd, Optional<T> val) {
			return val.map(e -> codec.encodeStart(NbtOps.INSTANCE, e).result().orElse(null)).orElse(null);
		}

		public Optional<T> read(RegistryAccess pvd, Tag tag) {
			return codec.parse(NbtOps.INSTANCE, tag).result();
		}

	}

	public interface Definer {

		<T> EntityDataAccessor<T> define(EntityDataSerializer<T> ser);

	}

}
