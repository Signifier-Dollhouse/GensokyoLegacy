# Wasteland gravestone offering

Food placed on the lower half of a wasteland gravestone starts a 100-tick ritual. End rod
particles play around the base, and the offering is consumed at the end; the container named by
`FoodProperties.usingConvertsTo` remains on the grave. The offering stays takeable during the
ritual, and taking it back halts the summon.

An offering must be worth at least 5 saturation (`nutrition * saturation * 2`) and carry no
harmful effect. Anything else is refused with "This doesn't seem appropriate as an offering",
and a non-food item, or a bare hand on a grave holding nothing, answers "Place food as offering
to summon a ghost"; both are system messages.

The ghost is a wild `StrayGhostEntity`, spawned above the grave with a random wild power, so
it can be bound with the bone flute. `ChunkMiscDataAttachment` keeps two timestamps for the
grave's chunk: a start that holds the chunk for the ritual's 5 seconds, and a finish that locks
it for one minute. A halted ritual leaves only the start lock, so the chunk frees up after 5
seconds; a finished summon locks it for a minute. Another offering in the same chunk is refused
until both have expired.

| variant | chance |
| --- | --- |
| foodie | `nutrition * 5%` |
| lively | `beneficial effects * 10%` |
| curios | the remainder |

When the first two sum above 100%, they become weights instead and curios can no longer be
rolled. For example, nutrition 20 (100%) plus two beneficial effects (20%) rolls foodie vs
lively at 100:20.
