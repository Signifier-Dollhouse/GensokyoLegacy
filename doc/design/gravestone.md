# Wasteland gravestone offering

Food placed on the lower half of a wasteland gravestone starts a 100-tick ritual. End rod
particles play around the base, interaction is locked, and the offering is consumed at the
end; the container named by `FoodProperties.usingConvertsTo` remains on the grave.

The ghost is a wild `StrayGhostEntity`, spawned above the grave with a random wild power, so
it can be bound with the bone flute.

| variant | chance |
| --- | --- |
| foodie | `nutrition * 5%` |
| lively | `beneficial effects * 10%` |
| curios | the remainder |

When the first two sum above 100%, they become weights instead and curios can no longer be
rolled. For example, nutrition 20 (100%) plus two beneficial effects (20%) rolls foodie vs
lively at 100:20.
