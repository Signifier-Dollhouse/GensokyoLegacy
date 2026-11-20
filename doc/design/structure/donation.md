# Donation Box

A donation box accepts coins only inside the structure named by its `donation_box` block data map. The
data map also carries the weighted rewards, so a box placed in any other structure (or none) is inert.

## Data map

`data/<namespace>/data_maps/block/donation_box.json`, keyed by block:

```json
{
  "values": {
    "gensokyolegacy:donation_box": {
      "structure": "gensokyolegacy:hakurei_shrine",
      "effects": [
        { "effect": "gensokyolegacy:looting", "level": 1, "weight": 1 },
        { "effect": "gensokyolegacy:spiritual", "level": 1, "weight": 1 },
        { "effect": "minecraft:resistance", "level": 1, "weight": 1 }
      ]
    }
  }
}
```

`level` is the effect level as shown to the player and stored one amplifier lower. `weight` is the
share of the draw; entries with weight 0 never come up. Durations are not part of the data map:
`donationEffectDuration` and `donationRegenDuration` in the server config own them.

## Credit

`CharacterAttachment` keeps one credit counter per structure id. Each coin is one credit and each
coin stack is `GLItems.COIN_STACK_VALUE` (8). When the counter reaches `CREDIT_COST` the box spends
that much and rolls one effect from its data map. Leftover credit stays with the player.

A roll skips effects the player already has at the same or a higher level; if every effect is
skipped, the credit stays banked until one of them expires.

Every payout also strips all harmful effects and applies regeneration III for
`donationRegenDuration` ticks; the rolled effect lasts `donationEffectDuration` ticks.

## Inputs

- **Right click with a coin or coin stack:** consumes one item and adds its value.
- **Throw a coin item entity:** `ItemEntity`'s thrower is the player. The box's collision lid sits at
  15/16, so a coin that lands on it overlaps the box block cell; `DonationBoxBlock.entityInside`
  consumes the whole stack. No tick scan is involved.
- **Coin danmaku:** `CoinDanmakuBulletEntity.dropCoin()` writes the shooter into the dropped item
  entity's thrower, so the same item-entity path picks it up.

`DonationBoxBlockEntity.insert()` re-resolves the home at the box position before accepting anything,
so a box moved out of its structure stops working even though it still carries its old structure key.
