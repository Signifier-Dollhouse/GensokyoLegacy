package dev.xkmc.gensokyolegacy.content.attachment.party.item;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodHandler;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyPlanner;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartySend;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.FoodResult;
import dev.xkmc.gensokyolegacy.content.client.debug.CharacterInfoClientManager;
import dev.xkmc.gensokyolegacy.content.client.debug.IDebugOverlayWand;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Comparator;
import java.util.List;

/**
 * A debug wand for the party system.
 *
 * <p>
 * <b>Hover a food block</b> to read what it is worth. A block holding one dish is
 * described by its serving — how many items that is, and what one is worth to eat. A
 * container of several is described by its totals instead, since there is no single
 * serving there to describe. Either way the last line is the appeal the block would
 * contribute to a party. Computed on the client from the block state and the item
 * registry, both of which the client already holds, so a live readout costs no packets.
 * Asking the server instead would mean a round trip per frame for numbers that cannot
 * disagree with the client's own copy.
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

	public PartyWand(Properties properties) {
		super(properties);
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
			// nothing here the wand can read, so it holds nothing either. Without the
			// clear a simulation sits on the item indefinitely: the player clicks a wall,
			// walks upstairs, and is still being told what that table would do with no
			// sign that anything replaced it. Both readable cases above replace it, so
			// this only ever fires where the wand would otherwise have nothing to say.
			stack.remove(PartyWandComponent.TYPE);
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
		var plan = outcome.plan();
		if (area == null || plan == null) return PartyWandData.refused(table, area);
		// the draw's roster, heaviest weight first. The weights are read here and then
		// dropped: the readout says who would come and nothing about how close any of
		// them was, so there is nothing to keep them on the item for.
		var guests = plan.pool().stream()
				.filter(c -> plan.roster().contains(c.type()))
				.sorted(Comparator.comparingInt(PartyPlanner.Candidate::weight).reversed())
				.map(PartyPlanner.Candidate::type)
				.toList();
		return new PartyWandData(table, area.tables(), area.seats(), area.food(),
				plan.capacity(), plan.appeal(), guests);
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

	/**
	 * One reading at a time, and what the player is looking at wins.
	 *
	 * <p>
	 * The wand can describe a dish, a character or a whole simulated party, and stacking
	 * them produces a twenty-line tooltip where the lower half is about a table thirty
	 * blocks away. So: a character is her own readout, a food block is the food readout,
	 * and the party detail is what is left over when the player is looking at neither.
	 *
	 * <p>
	 * The character half rides the existing entity debug overlay rather than a new packet
	 * — the same request, the same ten-tick throttle and the same cached response — so a
	 * guest's consumption also shows up under the debug wand and the glasses, which is
	 * where a debug tool belongs anyway.
	 */
	@Override
	public void addTooltip(Player player, ItemStack stack, List<Component> lines, long gameTime) {
		if (Minecraft.getInstance().hitResult instanceof EntityHitResult hit
				&& hit.getEntity() instanceof YoukaiEntity youkai) {
			CharacterInfoClientManager.tooltip(lines, gameTime, youkai);
			return;
		}
		var data = stack.get(PartyWandComponent.TYPE);
		// an invalid-food message is only honest when the wand holds nothing: with a
		// party detail on screen, something *is* here, the player just is not looking
		// at it
		if (describeFood(player, lines, data == null)) return;
		if (data != null) describeParty(player, data, lines);
	}

	/**
	 * The hovered food block, if the player is looking at one worth reading.
	 *
	 * <p>
	 * <b>One food on the block and several are read as two different questions.</b> A
	 * single dish has a size — how many items one serving is, and what one of those is
	 * worth — and those are the numbers that describe it. A container of mixed contents
	 * has no such thing: there is no one serving to describe, only a pile, so it is
	 * reported as the totals a party would actually count, which is serves, hunger and
	 * appeal for the block as a whole. Reading the first entry of a mixed container, as
	 * this used to, described one of its contents and silently implied it was the only one.
	 *
	 * @param allowInvalid whether to say so when there is a food block but nothing on it
	 * @return whether the food branch owns the overlay, and nothing else should be added
	 */
	private boolean describeFood(Player player, List<Component> lines, boolean allowInvalid) {
		if (!(player.pick(REACH, 0.0f, false) instanceof BlockHitResult hit)) return false;
		var level = player.level();
		var pos = hit.getBlockPos();
		var state = level.getBlockState(pos);
		// a container's food lives in its block entity, so this reads the world and not
		// the state - which works on the client as well, because a board's contents and
		// a rack's racks are both synced. A container that is not loaded simply reports
		// nothing and is left undescribed rather than guessed at.
		var valid = PartyFoodValue.results(level, pos, state).stream()
				.filter(FoodResult::valid).toList();
		if (valid.isEmpty()) {
			if (PartyFoodHandler.claims(state.getBlock()) == null) return false;
			if (!allowInvalid) return false;
			lines.add(GLLang.ItemDebug.PARTY_WAND_INVALID.get().withStyle(ChatFormatting.DARK_RED));
			return true;
		}
		lines.add(GLLang.ItemDebug.PARTY_WAND_FOOD.get(state.getBlock().getName()).withStyle(ChatFormatting.AQUA));
		if (valid.size() == 1) {
			var only = valid.get(0);
			// the size of a serving: two for a board that hands over a pair of nigiri,
			// one for everything else. Whether the block has seven of those left is a
			// question the multi branch below answers for the containers it applies to
			lines.add(GLLang.ItemDebug.PARTY_WAND_SERVING.get(only.serving().getCount())
					.withStyle(ChatFormatting.GRAY));
			lines.add(GLLang.ItemDebug.PARTY_WAND_FOOD_VALUES.get(only.nutrition(),
					String.format("%.1f", only.saturation())).withStyle(ChatFormatting.GRAY));
		} else {
			// summed here rather than through PartyFoodValue.totalHunger, because that
			// asks the container a second time and this overlay runs every frame: the
			// block entity has already been read once, above, and these are the same
			// sums over the same list
			int serves = 0, hunger = 0;
			for (var result : valid) {
				serves += result.serves();
				hunger += result.nutrition() * result.serves();
			}
			lines.add(GLLang.ItemDebug.PARTY_WAND_SERVES.get(serves).withStyle(ChatFormatting.GRAY));
			lines.add(GLLang.ItemDebug.PARTY_WAND_TOTAL_HUNGER.get(hunger).withStyle(ChatFormatting.GRAY));
		}
		// what this block is worth to a party: one appeal per food on it, with the
		// serving count deliberately not multiplying - see PartyArea.scan, where a
		// second cake somewhere else on the table would not be added again either
		int worth = 0;
		for (var entry : PartyFoodValue.appealPerFood(level.registryAccess(), level, pos, state)) {
			worth += entry.appeal();
		}
		lines.add(GLLang.ItemDebug.PARTY_WAND_ATTRACTION.get(worth).withStyle(ChatFormatting.GRAY));
		return true;
	}

	/**
	 * The simulated party: what the scan found, what the spread is worth, and who the
	 * draw chose.
	 *
	 * <p>
	 * Three lines of numbers and then a list of names. The numbers are what a host tunes
	 * against — the appeal figure is exactly the one every character was weighted by, so
	 * a spread that attracts nobody says so here before the roster does. Past that the
	 * readout stops: the weights, appetites and reputations that produced the draw are
	 * not shown, because a wall of near-identical rows per character buries the two
	 * things a reader actually came for.
	 */
	private void describeParty(Player player, PartyWandData data, List<Component> lines) {
		var bound = data.bound();
		lines.add(GLLang.ItemDebug.PARTY_WAND_PARTY.get(bound.getX(), bound.getY(), bound.getZ())
				.withStyle(ChatFormatting.GOLD));
		lines.add(GLLang.ItemDebug.PARTY_WAND_COUNTS.get(data.tables().size(), data.seats().size(), data.foods().size())
				.withStyle(ChatFormatting.GRAY));
		lines.add(GLLang.ItemDebug.PARTY_WAND_TOTALS.get(data.hunger(), data.appeal())
				.withStyle(ChatFormatting.GRAY));
		if (data.empty()) {
			lines.add(GLLang.ItemDebug.PARTY_WAND_NOBODY.get().withStyle(ChatFormatting.DARK_RED));
		} else {
			for (var guest : data.guests()) {
				lines.add(GLLang.ItemDebug.PARTY_WAND_GUEST.get(guest.getDescription())
						.withStyle(ChatFormatting.GREEN));
			}
		}
		// the food is being eaten while the wand sits in a hand, so a readout taken a
		// while back is worth flagging rather than presenting as current
		if (player.blockPosition().distSqr(bound) > 64) {
			lines.add(GLLang.ItemDebug.PARTY_WAND_STALE.get().withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	// --- tooltip ---

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemDebug.PARTY_WAND_HOVER.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemDebug.PARTY_WAND_EAT.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ItemDebug.PARTY_WAND_SIMULATE.get().withStyle(ChatFormatting.GRAY));
	}
}