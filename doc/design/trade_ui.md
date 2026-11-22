# Trade screen

The trade menu shows one character's offers as a 5x3 board of icon slots, with a filter rail on
the right. The GUI texture is `textures/gui/container/trade.png` (252x240, blitted 256x240); the
reference art with baked-in buttons is not shipped, every button is a sprite.

## Layout

| Element | Position |
| --- | --- |
| Offer slot `(col, row)` | `56 + col*32`, `39 + row*39` (col 0-4, row 0-2) |
| Price tag | `slot.x - 6`, `slot.y + 17`, 27x12 |
| Money plate | `24, 75`, 13x8 |
| Filter buttons | `x = 217`, `y = 61 + kind.ordinal()*14`, 12x12 |
| Next page | `217, 103`, 12x12 |
| Previous page | `217, 117`, 12x12 |
| Page indicator | centred, `y = 29`, only when `maxPage > 1` |
| Player inventory | `47, 157`; hotbar `+58` |

The buttons sit on the 12 px notches of the right rail, in the same order as the reference art:
trade (bag), buying (coin), processing (hammer), then next and previous.

## Offer kinds

`OfferKind` is derived from which side carries `GLTagGen.CURRENCY`:

- result is currency - `BUYING`: the character pays the player, the player hands over the
  ingredients (the "sell" side from the player's point of view).
- an ingredient is currency - `TRADE`: the player pays coins for the result.
- neither - `PROCESSING`: ingredients are worked into the result.

A currency result wins if both sides carry currency; no such offer exists in current data. The
kind decides the slot icon as well: `TRADE` and `PROCESSING` show the result, `BUYING` shows the
first ingredient (what the player gives), since the coin result is already on the price tag.

## Price tags

Each filled slot draws a 27x12 tag under it: `price_trade` (beige), `price_buying` (dark) or
`price_processing` (metal). The tag carries `¥N` where `N` is the currency ingredient count for
`TRADE` and the result count for `BUYING`; `PROCESSING` tags stay blank. The tooltip is expected
to cover the rest: a priced offer names its cost in the action line or in the tag, so only
`PROCESSING` offers, or offers with more than one ingredient, list their ingredients.

`BUYING` icons show the ingredient count in green when the player has enough for one trade and
red otherwise, mirroring the vanilla count colour. A `TRADE` price turns red when the player's
carried currency is below it.

## Money display

The coin plate on the left rail (`24, 75`, 13x8) shows how much currency the player carries:
the total count of `GLTagGen.CURRENCY` stacks in the inventory and offhand. The number is
scaled down when it would not fit the plate.

## Filters and paging

The three filter buttons are a single toggle set: at most one is active and only that kind is
listed, none active lists everything, clicking the active button clears the filter. A kind with
no offers is disabled, and a filter whose last offer disappears falls back to "all". Changing
the filter resets to page 1.

Filtering and paging are server-authoritative, synced to the client through
`DataSlot`s in `TradeMenu`: `filter` (0 = all, otherwise `kind.ordinal() + 1`), `available`
(one bit per kind), `page` and `maxPage`. Button clicks reuse `clickMenuButton` with
`PAGE_PREV = -1`, `PAGE_NEXT = -2` and `-3 - kind.ordinal()` for the filters.

Each button sprite is a three-state set: `tab_<kind>_normal`, `tab_<kind>_hover` (also used
while selected) and `tab_<kind>_disabled`; pagination uses `next_*` and `prev_*` the same way.
