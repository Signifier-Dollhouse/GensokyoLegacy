package dev.xkmc.gensokyolegacy.compat.l2lib;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Item data stored as a named NBT entry, standing in for a 1.21 data component.
 */
public final class DCVal<T> {

	public interface Ser<T> {

		T read(Tag tag);

		Tag write(T val);

	}

	private final String name;
	private final Ser<T> ser;

	public DCVal(String name, Ser<T> ser) {
		this.name = name;
		this.ser = ser;
	}

	@Nullable
	public T get(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(name)) return null;
		return ser.read(tag.get(name));
	}

	public T getOrDefault(ItemStack stack, T def) {
		T ans = get(stack);
		return ans == null ? def : ans;
	}

	public boolean has(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(name);
	}

	public ItemStack set(ItemStack stack, @Nullable T val) {
		if (val == null) {
			remove(stack);
			return stack;
		}
		stack.getOrCreateTag().put(name, ser.write(val));
		return stack;
	}

	public void remove(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag != null) tag.remove(name);
	}

}
