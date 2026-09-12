package dev.xkmc.gensokyolegacy.content.item.talisman.pocket;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.l2serial.serialization.marker.OnInject;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * 9-slot pocket storage as a mutable-free {@code @SerialClass}: each slot holds a
 * folded talisman and a reserve paper stack in two parallel arrays. Reads always
 * return copies and writes produce new instances, so stored {@link ItemStack}s are
 * never aliased or mutated in place. Like {@code DCStack}, the hash is computed
 * once and cached (lazily, since deserialization of {@code @SerialField} fields
 * happens after construction) to keep {@link #hashCode()} out of the hot path.
 */
@SerialClass
public class TalismanPocketData {

	public static final int MAX_SLOTS = 9;

	@SerialField
	private ItemStack[] foldedStacks = defaultStacks();
	@SerialField
	private ItemStack[] paperStacks = defaultStacks();

	private int hashCode;

	public TalismanPocketData() {
	}

	public static TalismanPocketData defaults() {
		return new TalismanPocketData();
	}

	@OnInject
	public void onInject() {
		foldedStacks = normalize(foldedStacks);
		paperStacks = normalize(paperStacks);
	}

	private static ItemStack[] normalize(@Nullable ItemStack[] data) {
		ItemStack[] ans = data.length == MAX_SLOTS ? data : Arrays.copyOf(data, MAX_SLOTS);
		for (int i = 0; i < MAX_SLOTS; i++) {
			ItemStack stack = ans[i];
			if (stack == null) ans[i] = ItemStack.EMPTY;
		}
		return ans;
	}

	private static ItemStack[] defaultStacks() {
		ItemStack[] ans = new ItemStack[MAX_SLOTS];
		Arrays.fill(ans, ItemStack.EMPTY);
		return ans;
	}

	private TalismanPocketData(ItemStack[] folded, ItemStack[] paper) {
		foldedStacks = folded;
		paperStacks = paper;
	}

	public ItemStack folded(int index) {
		if (index < 0 || index >= MAX_SLOTS) return ItemStack.EMPTY;
		ItemStack stack = foldedStacks[index];
		return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
	}

	public ItemStack paper(int index) {
		if (index < 0 || index >= MAX_SLOTS) return ItemStack.EMPTY;
		ItemStack stack = paperStacks[index];
		return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
	}

	public boolean hasFolded(int index) {
		if (index < 0 || index >= MAX_SLOTS) return false;
		ItemStack stack = foldedStacks[index];
		return stack != null && !stack.isEmpty();
	}

	public boolean hasPaper(int index) {
		if (index < 0 || index >= MAX_SLOTS) return false;
		ItemStack stack = paperStacks[index];
		return stack != null && !stack.isEmpty();
	}

	public boolean isEmpty(int index) {
		if (index < 0 || index >= MAX_SLOTS) return true;
		ItemStack fold = foldedStacks[index], paper = paperStacks[index];
		return (fold == null || fold.isEmpty()) && (paper == null || paper.isEmpty());
	}

	@Nullable
	public Item kind(int index) {
		if (index < 0 || index >= MAX_SLOTS) return null;
		ItemStack paper = paperStacks[index];
		if (paper != null && !paper.isEmpty()) return paper.getItem();
		ItemStack fold = foldedStacks[index];
		if (fold == null || fold.isEmpty()) return null;
		Holder<Item> holder = GLTalismans.DC_TALISMAN_PAPER.get(fold);
		return holder == null ? null : holder.value();
	}

	public TalismanPocketData withFolded(int index, ItemStack fold) {
		if (index < 0 || index >= MAX_SLOTS) return this;
		ItemStack[] f = foldedStacks.clone();
		f[index] = fold;
		return new TalismanPocketData(f, paperStacks);
	}

	public TalismanPocketData withPaper(int index, ItemStack paper) {
		if (index < 0 || index >= MAX_SLOTS) return this;
		ItemStack[] p = paperStacks.clone();
		p[index] = paper;
		return new TalismanPocketData(foldedStacks, p);
	}

	public TalismanPocketData with(int index, ItemStack fold, ItemStack paper) {
		if (index < 0 || index >= MAX_SLOTS) return this;
		ItemStack[] f = foldedStacks.clone();
		ItemStack[] p = paperStacks.clone();
		f[index] = fold;
		p[index] = paper;
		return new TalismanPocketData(f, p);
	}

	@Override
	public int hashCode() {
		int h = hashCode;
		if (h == 0) {
			h = 1;
			h = 31 * h + Arrays.hashCode(foldedStacks);
			h = 71 * h + Arrays.hashCode(paperStacks);
			hashCode = h == 0 ? 1 : h;
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof TalismanPocketData other && hashCode() == other.hashCode()) {
			return Arrays.equals(foldedStacks, other.foldedStacks) && Arrays.equals(paperStacks, other.paperStacks);
		}
		return false;
	}

}