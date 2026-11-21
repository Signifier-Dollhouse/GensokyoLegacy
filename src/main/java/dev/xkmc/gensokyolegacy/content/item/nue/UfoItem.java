package dev.xkmc.gensokyolegacy.content.item.nue;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.util.DCStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The wrapper a trident hit leaves behind: it holds one stolen stack and hands it back after a
 * two-second hold, with no deadline and no way to open it early. A UFO carrying a loot table instead
 * rolls one item out of it when the hold finishes, which is what Nue leaves at a party. The three
 * colours are cosmetic and picked at random by {@link #wrap}.
 *
 * @see "doc/design/item/nue_trident.md"
 */
public class UfoItem extends Item {

	private static final int HOLD_TICKS = 40;

	public UfoItem(Properties properties) {
		super(properties);
	}

	public static ItemStack getContent(ItemStack stack) {
		DCStack content = GLItems.UFO_CONTENT.get(stack);
		return content == null ? ItemStack.EMPTY : content.stack();
	}

	@Nullable
	public static ResourceLocation getLootTable(ItemStack stack) {
		return GLItems.UFO_LOOT.get(stack);
	}

	public static ItemStack wrap(ItemStack held, RandomSource random) {
		ItemStack ufo = new ItemStack(randomUfo(random));
		GLItems.UFO_CONTENT.set(ufo, new DCStack(held.copy()));
		return ufo;
	}

	/**
	 * One of the three colours, equally likely. The colours are cosmetic, so this is only flavour.
	 */
	private static Item randomUfo(RandomSource random) {
		return switch (random.nextInt(3)) {
			case 1 -> GLItems.RED_UFO.get();
			case 2 -> GLItems.GREEN_UFO.get();
			default -> GLItems.BLUE_UFO.get();
		};
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (getContent(stack).isEmpty() && getLootTable(stack) == null) return InteractionResultHolder.pass(stack);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return HOLD_TICKS;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BRUSH;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack content = getContent(stack);
		if (!content.isEmpty()) return content;
		ResourceLocation table = getLootTable(stack);
		if (table == null || !(level instanceof ServerLevel server)) return stack;
		ItemStack loot = roll(server, entity, table);
		return loot.isEmpty() ? stack : loot;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.UFO_LORE.get());
		ItemStack content = getContent(stack);
		if (!content.isEmpty()) {
			list.add(GLLang.ItemUfo.CONTAINS.get(content.getHoverName()));
		}
		boolean opens = content.isEmpty() && getLootTable(stack) != null;
		list.add(opens ? GLLang.ItemUfo.OPEN.get() : GLLang.ItemUfo.HOLD.get());
	}

	/**
	 * One item out of the gift table, with the holder as the only context the table needs.
	 */
	private static ItemStack roll(ServerLevel level, LivingEntity entity, ResourceLocation table) {
		var lookup = level.getServer().reloadableRegistries().get().lookup(Registries.LOOT_TABLE).orElse(null);
		if (lookup == null) return ItemStack.EMPTY;
		var key = ResourceKey.create(Registries.LOOT_TABLE, table);
		LootTable loot = lookup.get(key).map(Holder::value).orElse(null);
		if (loot == null || loot == LootTable.EMPTY) return ItemStack.EMPTY;
		var params = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, entity)
				.withParameter(LootContextParams.ORIGIN, entity.position())
				.create(LootContextParamSets.GIFT);
		ArrayList<ItemStack> stacks = new ArrayList<>();
		loot.getRandomItems(params, stacks::add);
		return stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst();
	}

}
