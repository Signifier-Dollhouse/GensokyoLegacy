package dev.xkmc.gensokyolegacy.content.attachment.party.item;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodHandler;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyPlanner;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResult;
import dev.xkmc.gensokyolegacy.content.client.debug.IDebugOverlayWand;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A debug wand for the party system.
 *
 * <p>
 * <b>Hover a food block</b> to read what it is worth: serves, nutrition, saturation,
 * attraction, and how many serves of it a guest needs. Computed on the client from the
 * block state and the item registry, both of which the client already holds, so a live
 * readout costs no packets. Asking the server instead would mean a round trip per frame
 * for numbers that cannot disagree with the client's own copy.
 *
 * <p>
 * <b>Click a food block</b> to take one serving off it, exactly as a guest would.
 *
 * <p>
 * <b>Click a party table</b> to simulate a send and leave the answer on the wand. This half
 * runs on the server, because the plan scores characters by the player's reputation and
 * that is server state. It calls the identical {@link PartySend#plan} a real send calls
 * before it commits, so the readout cannot drift from what would actually happen — no
 * party is opened, no guest is spawned and no letter is spent. The answer travels back as
 * an item data component, which vanilla already keeps in sync. The blocks the scan found
 * travel with it as a packet, and are drawn in the world for as long as the wand is held —
 * the counts on the overlay say <em>how many</em> chairs and dishes, the draw says
 * <em>where</em>.
 */
public class PartyWand extends Item implements IDebugOverlayWand {

	/** How far the overlay will look for a food block to describe. */
	private static final double REACH = 6.0d;

	/**
	 * The cheapest and dearest appetite among all characters, as a serving count range.
	 * <p>
	 * Cached because it walks every entity type and the overlay asks every frame. Reset on
	 * datapack sync, since {@code party_taste} is datapack data.
	 */
	private static int[] fulfillmentRange;

	public PartyWand(Properties properties) {
		super(properties);
	}

	/** Forget the cached appetite range; called on datapack sync. */
	public static void invalidate() {
		fulfillmentRange = null;
	}

	// --- click ---

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getPlayer() instanceof ServerPlayer sp && context.getLevel() instanceof ServerLevel level) {
			var stack = context.getItemInHand();
			var pos = context.getClickedPos();
			var state = level.getBlockState(pos);
			if (PartyFoodValue.isFood(level, pos, state)) {
				PartyFoodValue.consume(level, pos, state);
				return InteractionResult.SUCCESS;
			}
			if (state.is(GLTagGen.PARTY_TABLE)) {
				var outcome = PartySend.plan(level, sp, pos, PartySend.reputationOf(sp));
				stack.set(PartyWandComponent.TYPE, readout(pos, outcome));
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.CONSUME;
	}

	/**
	 * Summarise an already-planned outcome. Pure — it reads the plan and changes nothing.
	 * <p>
	 * Takes the {@link PartySend.Outcome} rather than planning itself, so a caller that
	 * has already planned (preview, which prints the same plan) does not walk the room a
	 * second time and risk reporting on a different area than the one it printed.
	 */
	public static PartyWandData readout(BlockPos table, PartySend.Outcome outcome) {
		// the area rides along even on a refusal: a table the wand refuses is exactly
		// the case where the host needs to see what the scan did and did not take
		var area = outcome.area();
		if (area == null) return PartyWandData.refused(table, List.of(), List.of(), List.of());
		var plan = outcome.plan();
		if (plan == null) return PartyWandData.refused(table, area.tables(), area.seats(), area.food());

		var roster = plan.roster();
		int spent = plan.pool().stream()
				.filter(c -> roster.contains(c.type()))
				.mapToInt(PartyPlanner.Candidate::fulfillment)
				.sum();
		var candidates = new ArrayList<PartyWandData.Candidate>(plan.pool().size());
		plan.pool().stream()
				.sorted(Comparator.comparingInt(PartyPlanner.Candidate::weight).reversed())
				.forEach(c -> candidates.add(new PartyWandData.Candidate(
						c.type(), c.weight(), c.fulfillment(), c.reputation(), roster.contains(c.type()))));
		return new PartyWandData(table, area.tables(), area.seats(), area.food(),
				plan.capacity(), spent, candidates);
	}

	/**
	 * Record an outcome on the wand in this player's hand, if they are holding one.
	 * <p>
	 * For {@code /gensokyo party preview}, which is a scan the wand should show. A
	 * <em>send</em> deliberately does not go through here: the host is holding a letter,
	 * not the wand, and silently rewriting a wand somewhere in their inventory because
	 * they sent a party would be a surprising thing for a debug tool to do to an item.
	 *
	 * @return whether a wand was there to record it
	 */
	public static boolean recordIfHeld(ServerPlayer sp, BlockPos table, PartySend.Outcome outcome) {
		for (var hand : InteractionHand.values()) {
			var stack = sp.getItemInHand(hand);
			if (!(stack.getItem() instanceof PartyWand)) continue;
			stack.set(PartyWandComponent.TYPE, readout(table, outcome));
			return true;
		}
		return false;
	}

	// --- overlay ---

	@Override
	public void addTooltip(Player player, ItemStack stack, List<Component> lines, long gameTime) {
		describeFood(player, lines);
		describeParty(player, stack, lines);
	}

	/** The hovered food block, if the player is looking at one worth reading. */
	private void describeFood(Player player, List<Component> lines) {
		if (!(player.pick(REACH, 0.0f, false) instanceof BlockHitResult hit)) return;
		var level = player.level();
		var pos = hit.getBlockPos();
		var state = level.getBlockState(pos);
		// a container's food lives in its block entity, so this reads the world and not
		// the state - which works on the client as well, because a board's contents and
		// a rack's racks are both synced. A container that is not loaded simply reports
		// nothing and is left undescribed rather than guessed at.
		var results = PartyFoodValue.results(level, pos, state);
		var valid = results.stream().filter(FoodResult::valid).toList();
		if (valid.isEmpty()) {
			if (PartyFoodHandler.claims(state.getBlock()) == null) return;
			lines.add(GLLang.ItemDebug.PARTY_WAND_INVALID.get().withStyle(ChatFormatting.DARK_RED));
			return;
		}
		int servings = valid.stream().mapToInt(FoodResult::serves).sum();
		// the first food on it, named as what it is - a container lists one entry per
		// kind of thing it holds, and the block's own name is still what the player is
		// looking at, so it is the block that gets named
		var first = valid.get(0);
		var props = first.food();
		int perServe = PartyFoodValue.appealPerServe(level.registryAccess(), level, pos, state);
		lines.add(GLLang.ItemDebug.PARTY_WAND_FOOD.get(state.getBlock().getName()).withStyle(ChatFormatting.AQUA));
		lines.add(GLLang.ItemDebug.PARTY_WAND_SERVES.get(servings).withStyle(ChatFormatting.GRAY));
		lines.add(GLLang.ItemDebug.PARTY_WAND_FOOD_VALUES.get(props.nutrition(), String.format("%.1f", props.saturation()))
				.withStyle(ChatFormatting.GRAY));
		lines.add(GLLang.ItemDebug.PARTY_WAND_ATTRACTION.get(perServe, perServe * servings)
				.withStyle(ChatFormatting.GRAY));
		var range = servesToSatisfy(player, perServe);
		lines.add((range == null
				? GLLang.ItemDebug.PARTY_WAND_FULFILLMENT_NONE.get()
				: GLLang.ItemDebug.PARTY_WAND_FULFILLMENT.get(range))
				.withStyle(ChatFormatting.GRAY));
	}

	/**
	 * Serves of this food needed by the easiest and the fussiest character, as one number
	 * or a range. Both ends are worth seeing: the low end says whether the dish is worth
	 * laying out at all, the high end whether it could ever feed everyone at the table.
	 *
	 * @return the range as text, or null when there are no characters to compare against
	 */
	private static String servesToSatisfy(Player player, int perServe) {
		if (perServe <= 0) return null;
		var range = fulfillmentRange;
		if (range == null) {
			range = buildFulfillmentRange(player.level());
			fulfillmentRange = range;
		}
		if (range == null) return null;
		int low = Math.max(1, (int) Math.ceil(range[0] / (double) perServe));
		int high = Math.max(1, (int) Math.ceil(range[1] / (double) perServe));
		return low == high ? String.valueOf(low) : low + "-" + high;
	}

	/**
	 * The smallest and largest fulfillment any <b>eligible</b> character asks for, or
	 * null if no character is eligible at all.
	 * <p>
	 * Skips characters with no {@code party_taste}, because they cannot be invited and
	 * their numbers would otherwise drag the range away from the characters the host
	 * can actually seat.
	 */
	private static int[] buildFulfillmentRange(Level level) {
		int low = Integer.MAX_VALUE;
		int high = 0;
		boolean any = false;
		for (var type : BuiltInRegistries.ENTITY_TYPE) {
			if (!PartySend.isCharacter(type)) continue;
			var taste = PartyTaste.of(level.registryAccess(), (EntityType<?>) type);
			if (taste == null) continue;
			any = true;
			low = Math.min(low, taste.fulfillment());
			high = Math.max(high, taste.fulfillment());
		}
		return any ? new int[]{low, high} : null;
	}

	private void describeParty(Player player, ItemStack stack, List<Component> lines) {
		var data = stack.get(PartyWandComponent.TYPE);
		if (data == null) return;
		var bound = data.bound();
		lines.add(GLLang.ItemDebug.PARTY_WAND_PARTY.get(bound.getX(), bound.getY(), bound.getZ())
				.withStyle(ChatFormatting.GOLD));
		lines.add(GLLang.ItemDebug.PARTY_WAND_COUNTS.get(data.tables().size(), data.seats().size(), data.foods().size())
				.withStyle(ChatFormatting.GRAY));
		lines.add(GLLang.ItemDebug.PARTY_WAND_TOTALS.get(data.hunger(), data.needed())
				.withStyle(ChatFormatting.GRAY));
		if (data.empty()) {
			lines.add(GLLang.ItemDebug.PARTY_WAND_NOBODY.get().withStyle(ChatFormatting.DARK_RED));
			return;
		}
		for (var candidate : data.candidates()) lines.add(candidateLine(candidate));
		// the food is being eaten while the wand sits in a hand, so a readout taken a
		// while back is worth flagging rather than presenting as current
		if (player.blockPosition().distSqr(bound) > 64) {
			lines.add(GLLang.ItemDebug.PARTY_WAND_STALE.get().withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	private static Component candidateLine(PartyWandData.Candidate candidate) {
		var style = candidate.invited() ? ChatFormatting.GREEN
				: candidate.declined() ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY;
		return GLLang.ItemDebug.PARTY_WAND_CANDIDATE.get(candidate.type().getDescription(),
				String.valueOf(candidate.weight()), candidate.reputation(),
				candidate.fulfillment(), candidate.invited() ? "+" : "-").withStyle(style);
	}

	// --- tooltip ---

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemDebug.PARTY_WAND_HOVER.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemDebug.PARTY_WAND_EAT.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemDebug.PARTY_WAND_SIMULATE.get().withStyle(ChatFormatting.GRAY));
	}
}