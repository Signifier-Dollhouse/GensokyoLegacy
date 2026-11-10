package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.serialization.Codec;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Item data registration for 1.20.1. There is no component registry to write to; each value
 * is a named NBT entry, so registration simply builds the accessor. See
 * {@code doc/backport/api.md} §6 for the call-site rewrite.
 */
public record DCReg() {

	public static DCReg of(Reg parent) {
		return new DCReg();
	}

	public <T> DCVal<T> reg(String id, Codec<T> codec, boolean cache) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public T read(Tag tag) {
				return codec.parse(NbtOps.INSTANCE, tag).result().orElse(null);
			}

			@Override
			public Tag write(T val) {
				return codec.encodeStart(NbtOps.INSTANCE, val).result().orElseThrow();
			}
		});
	}

	public <T> DCVal<T> reg(String id, Class<T> cls, boolean cache) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public T read(Tag tag) {
				return TagCodec.valueFromTag(tag, cls);
			}

			@Override
			public Tag write(T val) {
				return TagCodec.valueToTag(cls, val, e -> true);
			}
		});
	}

	public <T> DCVal<T> enumVal(String id, EnumCodec<T> codec) {
		return reg(id, codec.codec(), true);
	}

	public DCVal<Integer> intVal(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public Integer read(Tag tag) {
				return tag instanceof IntTag ? ((IntTag) tag).getAsInt() : null;
			}

			@Override
			public Tag write(Integer val) {
				return IntTag.valueOf(val);
			}
		});
	}

	public DCVal<Double> doubleVal(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public Double read(Tag tag) {
				return tag instanceof DoubleTag ? ((DoubleTag) tag).getAsDouble() : null;
			}

			@Override
			public Tag write(Double val) {
				return DoubleTag.valueOf(val);
			}
		});
	}

	public DCVal<Float> floatVal(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public Float read(Tag tag) {
				return tag instanceof FloatTag ? ((FloatTag) tag).getAsFloat() : null;
			}

			@Override
			public Tag write(Float val) {
				return FloatTag.valueOf(val);
			}
		});
	}

	public DCVal<Long> longVal(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public Long read(Tag tag) {
				return tag instanceof LongTag ? ((LongTag) tag).getAsLong() : null;
			}

			@Override
			public Tag write(Long val) {
				return LongTag.valueOf(val);
			}
		});
	}

	public DCVal<String> str(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public String read(Tag tag) {
				return tag.getAsString();
			}

			@Override
			public Tag write(String val) {
				return StringTag.valueOf(val);
			}
		});
	}

	public DCVal<Unit> unit(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public Unit read(Tag tag) {
				return Unit.INSTANCE;
			}

			@Override
			public Tag write(Unit val) {
				return ByteTag.valueOf((byte) 1);
			}
		});
	}

	public <T> DCVal<Holder<T>> registry(String id, Registry<T> reg) {
		return reg(id, reg.holderByNameCodec(), false);
	}

	public DCVal<ResourceLocation> loc(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public ResourceLocation read(Tag tag) {
				return ResourceLocation.tryParse(tag.getAsString());
			}

			@Override
			public Tag write(ResourceLocation val) {
				return StringTag.valueOf(val.toString());
			}
		});
	}

	public DCVal<UUID> uuid(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public UUID read(Tag tag) {
				return NbtUtils.loadUUID(tag);
			}

			@Override
			public Tag write(UUID val) {
				return NbtUtils.createUUID(val);
			}
		});
	}

	public DCVal<DCStack> stack(String id) {
		return reg(id, ItemStack.CODEC.xmap(DCStack::new, DCStack::stack), true);
	}

	public DCVal<Component> component(String id) {
		return new DCVal<>(id, new DCVal.Ser<>() {
			@Override
			public Component read(Tag tag) {
				return Component.Serializer.fromJson(tag.getAsString());
			}

			@Override
			public Tag write(Component val) {
				return StringTag.valueOf(Component.Serializer.toJson(val));
			}
		});
	}

}
