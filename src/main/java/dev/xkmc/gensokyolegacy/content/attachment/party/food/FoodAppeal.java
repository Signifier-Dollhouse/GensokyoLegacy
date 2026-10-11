package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import net.minecraft.resources.ResourceLocation;

/**
 * How attractive one kind of food is, named by the id it is known under.
 *
 * <p>
 * <b>A pairing rather than a number, because appeal is counted once per food and not once
 * per serving.</b> A party counts what is on the table by asking, for each block in the
 * spread, "what food is this and what is that food worth" — and it has to be able to
 * recognise that the cake on the far end is the same cake as the one by the door. A bare
 * {@code int} cannot say which food it belonged to, so a caller holding one could only sum
 * them, which is precisely the answer the party system stopped wanting: twenty blocks of
 * the same modded casserole are one casserole, not twenty.
 *
 * <p>
 * The pairing also settles the other half of the rule without anyone having to ask. One
 * entry is per <b>id</b>, never per serving, so a cake with seven slices left offers one
 * entry worth one slice and a rack holding three buns offers one entry worth one bun — and
 * {@link #appeal()} is never multiplied by anything on the way out. A caller that wants
 * the old {@code serves × appeal} behaviour wants
 * {@link dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue#appealPerServe},
 * which is a different question and is still answered where it always was.
 *
 * <p>
 * A genuine value type: two entries for the same food are {@code equals}, unlike
 * {@link FoodResult}, whose {@code consume} lambda never is.
 *
 * @param id     what this food is, by the same one rule as {@link FoodResult#id()} — the
 *               serving item's id where there is one, the block's own id otherwise, which
 *               is what makes vanilla cake count under {@code minecraft:cake} and a
 *               cutting board under the sushi on it
 * @param appeal how attractive one serving of this food is at the table
 */
public record FoodAppeal(ResourceLocation id, int appeal) {
}
