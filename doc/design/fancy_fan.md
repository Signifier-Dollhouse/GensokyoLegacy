# The Fancy Fan

`FancyFanItem` — the fancy fan, and the one thing in the gift ladder that does something when
you swing it.

> Status: implemented. The fan is a gift (`GiftType.COLLECTION`) and a weapon of a sort: right-click
> and it blows a gust over everything in front of you.

The fan was a plain item until it was given a use. It is still a gift — the favour it is worth, the
shelf it sits on and the characters who like it are all unchanged — but it is no longer inert.

## 1. The gust

Right-click, and everything alive in the cone in front of the wielder is shoved along the
wielder's **own look vector**. The push is not radial: a mob at the edge of the cone is not thrown
back at the wielder, it is thrown *sideways* past them, which is what a cone of moving air does.

The cone is the wielder's yaw and pitch, widened by two half-angles and cut off at a reach:

| | value | measured from |
|---|---|---|
| reach | 9 blocks | the wielder's eyes, to the nearest point of the body |
| horizontal half-angle | 60° | the wielder's yaw |
| vertical half-angle | 45° | the wielder's pitch |
| cooldown | 10 ticks | — |

The two angles are separate rather than one angle off the look vector, so the cone stays a cone
rather than a sphere: a mob can be 60° to the side and level with the wielder and still be caught,
and a mob can be 45° above or below the aim and still be caught.

The angles are measured on the *horizontal* and *vertical* planes independently, which is the
practical reading of "±60° horizontally, ±45° vertically" but is not a true cone in the geometric
sense: a target at the corner of the block — 60° to the side *and* 45° up — is inside both
allowances and is caught, even though it is more than 60° from the look vector in three dimensions.

### Two ways to swing it

The fan answers to `Item#use` and to `Item#interactLivingEntity`, so it can be swung at the air or
pointed at a mob directly. Pointing it at a mob is the gesture a player actually reaches for, and
the gust is thrown from the wielder either way — the cone does not narrow to a single target.

A character who takes the fan as a gift answers first and keeps it: the gift module runs inside
`Mob#mobInteract`, which is ahead of `interactLivingEntity`, so right-clicking a youkai with the
fan hands it over and no gust is thrown. The fan is a gift before it is a weapon.

### The push

The impulse is **added** to whatever the target was already doing, not substituted for it, so a
target already running away from the wielder is not slowed down by the gust. The horizontal speed
that results is capped, so that spamming the fan cannot launch anything across the map.

| | value |
|---|---|
| impulse | 0.7 blocks per tick |
| horizontal speed ceiling | 1.2 blocks per tick |
| knockback resistance | scales the impulse by `1 - resistance` |

Only the *horizontal* speed is capped. A mob that is already falling keeps its fall speed, which is
why the cap is not on the whole velocity: capping the whole thing would have the fan slowing down
every mob that dropped into its cone. The vertical is left to gravity — a gust thrown upward adds
0.7 of lift, and the next tick of gravity takes it back, so a fan held at the sky does not turn
into a launch pad.

The resistance scaling is vanilla's `KNOCKBACK_RESISTANCE` attribute, so the heavy things stay
heavy: a mob that shrugs off a punch shrugs off the fan in proportion. An entity that has no such
attribute — an armour stand — is pushed as though it had none.

The velocity is written with `setDeltaMovement` and the target is flagged `hasImpulse` and
`hurtMarked`, the same way `SuikaSpell` shoves. The `hurtMarked` flag is what makes the server send
the new motion to the client; without it a player who is blown does not move.

### Who is caught

Everything alive in the cone, except:

- the wielder,
- anything allied to the wielder — a tamed pet, another player on the same scoreboard team,
- spectators and the dead.

Allies are excluded by `Entity#isAlliedTo`, which covers the scoreboard team and, through
`TamableAnimal`, a tamed animal whose owner is the wielder or one of the wielder's allies.

## 2. Cooldown

Ten ticks, through vanilla's `ItemCooldowns`, so it is accelerated by the *lively* effect like
every other cooldown in the mod (see [cooldown.md](cooldown.md)). Ten ticks is half a second, which
is fast enough to keep a crowd moving and slow enough that the fan is not a permanent shove.

## 3. Durability

The fan has none. Its `Item.Properties` carry no durability and nothing ever calls
`hurtAndBreak` on it, so the stack never wears out and never shows a durability bar. The tooltip
says so, in the same words the gourd uses.

This is why the fan can sit on Morichika's shelf at a price of 10-20 with a stock of one without
anyone worrying about how long it lasts: it lasts.

## 4. Files

- `content/item/gift/FancyFanItem.java` — the item, the cone test and the shove.
- `init/registrate/GLItems.java` — the registration, still a gift item with its datamaps.
- `init/data/GLLang.java` — the lore and the two tooltip lines.
