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
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Debug wand: hover a dish to price it, click a table to simulate a send and hold the answer.
 * The answer lives on the item as a data component, which vanilla already syncs.
 *
 * @see "doc/design/party/debug.md"
 */
public class PartyWand extends Item implements IDebugOverlayWand {

	/**
	 * How far the overlay will look for a food block to describe.
	 */
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
				// the identical plan a real send runs before it commits; nothing is
				// opened, spawned or spent
				var outcome = PartySend.plan(level, sp, pos, PartySend.reputationOf(sp));
				GLItems.PARTY_WAND_DATA.set(stack, readout(pos, outcome));
				return InteractionResult.SUCCESS;
			}
			// a click the wand cannot read clears the held reading
			stack.remove(GLItems.PARTY_WAND_DATA.get());
		}
		return InteractionResult.CONSUME;
	}

	/**
	 * Summarise an already-planned outcome. Pure — it reads the plan and changes nothing, and takes the
	 * {@link PartySend.Outcome} rather than planning itself, so a caller that has already planned
	 * does not walk the room twice.
	 */
	public static PartyWandData readout(BlockPos table, PartySend.Outcome outcome) {
		// the area rides along even on a refusal: that is where the host needs to see
		// what the scan did and did not take
		var area = outcome.area();
		var plan = outcome.plan();
		if (area == null || plan == null) return PartyWandData.refused(table, area);
		// the chosen roster only, heaviest weight first: the readout says who would
		// come, and nothing about how close any of them was
		List<EntityType<?>> guests = new ArrayList<>(plan.pool().stream()
				.filter(c -> plan.roster().contains(c.type()))
				.sorted(Comparator.comparingInt(PartyPlanner.Candidate::weight).reversed())
				.map(PartyPlanner.Candidate::type)
				.toList());
		return new PartyWandData(table, area.tables(), area.seats(), area.food(),
				plan.capacity(), plan.appeal(), guests);
	}

	/**
	 * Record an outcome on the wand in this player's hand, if they are holding one. Preview, which is a
	 * scan, goes through here; a <em>send</em> deliberately does not, because silently rewriting a wand
	 * in the host's inventory is surprising.
	 *
	 * @return whether a wand was there to record it
	 */
	public static boolean recordIfHeld(ServerPlayer sp, BlockPos table, PartySend.Outcome outcome) {
		for (var hand : InteractionHand.values()) {
			var stack = sp.getItemInHand(hand);
			if (!(stack.getItem() instanceof PartyWand)) continue;
			GLItems.PARTY_WAND_DATA.set(stack, readout(table, outcome));
			return true;
		}
		return false;
	}

	// --- overlay ---

	/**
	 * One reading at a time, and what the player is looking at wins: a character is her own readout,
	 * a food block is the food readout, and the party detail is what is left over.
	 *
	 * @see "doc/design/party/debug.md#4-one-reading-at-a-time"
	 */
	@Override
	public void addTooltip(Player player, ItemStack stack, List<Component> lines, long gameTime) {
		if (Minecraft.getInstance().hitResult instanceof EntityHitResult hit
				&& hit.getEntity() instanceof YoukaiEntity youkai) {
			CharacterInfoClientManager.tooltip(lines, gameTime, youkai);
			return;
		}
		var data = GLItems.PARTY_WAND_DATA.get(stack);
		// the invalid-food message is only honest when the wand holds nothing: with a
		// party detail on screen, something *is* here, the player just is not looking at it
		if (describeFood(player, lines, data == null)) return;
		if (data != null) describeParty(player, data, lines);
	}

	/**
	 * The hovered food block, if the player is looking at one worth reading. One dish is described
	 * by its serving; a container holding several has no single serving and is described by its totals.
	 *
	 * @param allowInvalid whether to say so when there is a food block but nothing on it
	 * @return whether the food branch owns the overlay, and nothing else should be added
	 */
	private boolean describeFood(Player player, List<Component> lines, boolean allowInvalid) {
		if (!(player.pick(REACH, 0.0f, false) instanceof BlockHitResult hit)) return false;
		var level = player.level();
		var pos = hit.getBlockPos();
		var state = level.getBlockState(pos);
		// a container's food lives in its block entity, and that is synced, which is why
		// this works on the client
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
			// a serving's size, which is not always one: a cutting board hands over the
			// whole stack it holds
			lines.add(GLLang.ItemDebug.PARTY_WAND_SERVING.get(only.serving().getCount())
					.withStyle(ChatFormatting.GRAY));
			lines.add(GLLang.ItemDebug.PARTY_WAND_FOOD_VALUES.get(only.nutrition(),
					String.format("%.1f", only.saturation())).withStyle(ChatFormatting.GRAY));
		} else {
			// summed here rather than through PartyFoodValue.totalHunger, which would
			// ask the container a second time in an overlay that runs every frame
			int serves = 0, hunger = 0;
			for (var result : valid) {
				serves += result.serves();
				hunger += result.nutrition() * result.serves();
			}
			lines.add(GLLang.ItemDebug.PARTY_WAND_SERVES.get(serves).withStyle(ChatFormatting.GRAY));
			lines.add(GLLang.ItemDebug.PARTY_WAND_TOTAL_HUNGER.get(hunger).withStyle(ChatFormatting.GRAY));
		}
		// one appeal per distinct food, with the serve count deliberately not
		// multiplying, exactly as the scan counts it — select.md §1
		int worth = 0;
		for (var entry : PartyFoodValue.appealPerFood(level.registryAccess(), level, pos, state)) {
			worth += entry.appeal();
		}
		lines.add(GLLang.ItemDebug.PARTY_WAND_ATTRACTION.get(worth).withStyle(ChatFormatting.GRAY));
		return true;
	}

	/**
	 * The simulated party: what the scan found, what the spread is worth, and who the draw chose.
	 * The weights and appetites behind the draw are not shown.
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
		// the food is being eaten while the wand sits in a hand, so a stale reading
		// is flagged rather than presented as current
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