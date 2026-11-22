# Want Bubble — Design Doc

> **Status:** implemented — compiles. Not yet played in a live world, so the feel of the twelve-to-twenty-second clock and of the ten-second show window is reasoned from the code rather than observed.
> **Package:** `content/entity/behavior/module/IBubbleModule.java` (what a renderer asks for) and `content/entity/behavior/module/AbstractBubbleModule.java` (the clock and its state), drawn by `content/entity/youkai/renderer/YoukaiBubbleLayer.java`. One consumer today: `FairyFeedModule`.
> **Related:** `structure/youkai_character.md` (the module system this rides on), `party/send.md` (the other half of feeding a fairy)

## What it is

A bubble over a character's head carrying one item, put up every twelve to twenty seconds and gone ten seconds later. Nobody has to be looking and nothing is owed: she is thinking out loud, and the point of it is that the player learns what to hand her by watching rather than by being told.

A fairy has no dialog of her own (`FairyEntity` drops `TalkModule`), so this is the whole of what the fairy family has to say.

## 1. Components

| Piece | File | Job |
|---|---|---|
| `IBubbleModule` | `content/entity/behavior/module/IBubbleModule.java` | the contract a renderer asks for: `wants()`, `mayShowWant()`, and the three pieces of draw state |
| `AbstractBubbleModule` | `content/entity/behavior/module/AbstractBubbleModule.java` | the clock, the roll, and the client-side state; extends `AbstractYoukaiModule` and implements `IBubbleModule` |
| `YoukaiBubbleLayer` | `content/entity/youkai/renderer/YoukaiBubbleLayer.java` | draws the pill, its tail and the item; looks the module up by interface, not by kind of character |
| `FairyFeedModule` | `content/entity/behavior/module/FairyFeedModule.java` | the fairy answer: what she asks for, and when she is not asking |
| `CirnoFeedModule` | `content/entity/characters/fairy/CirnoFeedModule.java` | inherits the bubble unchanged; only the gifts differ |

A character gets a bubble by having a module that is an `IBubbleModule` in its list and by its renderer adding `new YoukaiBubbleLayer<>(this)`. Nothing else is wired: `YoukaiEntity` is untouched, because the clock runs out of the module tick `YoukaiEntity.tickEffects` already drives.

### Why the module owns it

Subclassing rather than composing is what lets the bubble ride on a module that already has a reason to exist. A fairy's ask is about what she eats, so it belongs on the module that decides what she eats — `FairyFeedModule` is the only thing on her that knows the dish list. A second module would have needed its own id, its own entry in every `createModules()`, and a hook on the entity to tick it.

## 2. Entirely client-side

The clock, the roll and the drawing all run on the client. Nothing goes on the wire and nothing is saved; the server's copy of the three state fields stays empty for ever.

That is affordable because nothing is a promise. The server has no opinion about what she is asking for, so it has no reason to be the authority on it, and the cost on the wire would have been an entity event broadcast to everyone watching. It also means:

- **Two players watching one character can see different thoughts over her** — separate clocks, separate rolls. Acceptable: nobody is being told anything.
- **A player who starts tracking her waits out the same twelve to twenty seconds** as one who has been watching her, because the first delay is rolled when the client entity is built rather than firing immediately.
- **The server cannot stop it**, and does not need to.

### History: the entity event it replaced

The first version ran as `FairyWantTask`, an always-behavior on `IDLE`/`PLAY`/`AT_HOME` that broadcast one byte, `87 + index * 3 + placement`. That was broken: `GeoYoukaiAnim.ANIM_EVENT_BASE` is 71, so 87 is `THINK_02` (71 + 16) on a rig that maps it. Since `handleAnimEvent` runs before the module chain, the four payloads landing on ids 87-90 were swallowed on Cirno — no bubble, plus a spurious dialog animation. Going client-side retires the id space entirely; nothing is claimed above `ANIM_EVENT_BASE` now.

One consequence of leaving the brain: **the bubble is no longer activity-gated**. A client cannot read the brain, so the bubble is hers whenever you can see her, and the two states the client does know about are handled in §4.

## 3. The clock

```
wantDelay = nextInt(240, 401)          // rolled once, at module construction
every client tick:
    if (--wantDelay > 0) return
    wantDelay = nextInt(240, 401)
    if (!mayShowWant()) { clear the bubble; return }
    bubble  = wants().get(random).get()
    corner  = nextInt(3)
    bubbleAt = self.tickCount
```

It runs in `AbstractYoukaiModule.tickClient`, which `YoukaiEntity.tickEffects` calls for every module — hence the `level().isClientSide()` guard at the top of `tickClient`: the hook is named for the client but runs on both sides, and a want rolled on the server would be state nothing ever reads.

The first delay is rolled in the field initializer rather than on the first tick, because a client entity is constructed the moment it starts being tracked and a want on tick one is a bubble over her head before the player has seen her.

## 4. What goes in the bubble, and when it is suppressed

`FairyFeedModule.wants()` returns a fairy cake, a cookie or a cake. The cake and cookie are exactly what `isFood` recognises, so the bubble only ever asks for things feeding her actually answers. The fairy cake is the cheap one and sits first; the roll is uniform, so a third of her thoughts are about the thing that is easiest to hand her.

`mayShowWant()` is the veto, and there are four of them:

| State | Read from | Why |
|---|---|---|
| eating | `YoukaiFlags.FED`, synced by `FairyFeedModule.tickServer` | a bubble asking for a treat she cannot be handed is worse than no bubble — the same reasoning `FairyInterestSensor` keeps her from following anybody while it runs |
| asleep | `isSleeping()`, a synced entity flag | the same, for a fairy lying in a bed: not asking, and a thought drawn over a sleeper reads as a glitch |
| partying | `isPartying()`, a synced flag | at a party she has already been served; a want over her head there is the wrong half of the trade |
| hostile or fleeing | `isAggressive()`, `YoukaiFlags.FLEEING` | a fairy being chased or chasing is not in the mood to be handed a cookie, and the bubble would be read as a target marker |

The veto is asked **twice**: at the roll, which clears whatever was on screen, and again at the draw. A want that comes up while she is busy is dropped rather than held back until she is free again — she is owed nothing and there is another one twelve to twenty seconds behind it. The draw-side check also covers the want expiring *while* she becomes busy: the bubble vanishes at once rather than staying up over a fairy who just fell asleep. An earlier version held the clock at zero across a cooldown, which made a bubble pop up the instant the cooldown ran out; dropping it is both simpler and better paced.

The feed cooldown has to be synced because the client is the side asking, and `getCoolDown()` is server-only state. `YoukaiFlags.FED` was already the bit the generic `FeedModule` puts up for its own client effects, and a fairy has no `FeedModule`, so the bit was free for her.

## 5. How it is drawn

`YoukaiBubbleLayer` is a `GeoRenderLayer`, not a world pass: the pose a layer is handed is already the one a name tag is drawn in. `GeoEntityRenderer#actuallyRender` applies the body yaw inside a frame it pushes and pops itself, so by the time layers run the pose carries the entity's position and nothing else. That makes billboarding one `mulPose` against `cameraOrientation()`, and it means the bubble is drawn with the entity — same buffers, same pass, same distance.

Everything after that is drawn in the frame the billboard and the scale leave behind: `+x` right, `+y` down, `+z` away from the camera, one unit to a sheet pixel. So a bubble put off to one side is off to the side of the *screen* rather than of the world, which is what "at her upper left" means once the camera is not straight on.

That frame is the 1.20.1 name tag's, exactly: `cameraOrientation()` then `scale(-S, -S, S)`. The negative x and y are what turn the camera's `+x` left and `+y` up into screen right and screen down, and they also reverse the quad's winding — which is why the pill and tails can use `RenderType.text` (culling on) and still be visible. **Do not add the `Axis.YP.rotationDegrees(180)` the 1.21.1 version carries**: 1.21.1 bakes a `Math.PI` offset into `Camera.setRotation`, so its frame is already `+x` right, and the extra half turn there merely mirrors the sprite back. In 1.20.1 it double-flips x, mirroring the sheet and flipping the winding again, and the culled quads disappear — that was the "invisible bubble" bug.

Order of operations in `render`: resolve the module → skip an empty stack or an invisible entity → `mayShowWant()` → skip outside the 200-tick show window → skip past 64 blocks → put the pill's centre over her head → billboard and scale → shift sideways by the placement → draw the pill → draw the tail → draw the item.

### The sheet

`textures/bubble/bubble.png` is a 256px square whose top-left corner holds the pill, `48 × 24`. The three `16 × 16` cells at `y = 24` are the same tail at three angles; the placement rolled with the want picks one, and the tail is drawn just behind the pill (`z = 0.05`, away from the camera) so the pill's outline covers the join. Left and right placement put the tail 24px off centre and 9px down, so it hangs from under the near end of the pill; the centre placement puts it 8px down, straight under the middle.

The item is centred on the pill, 16px across — one item-model unit after the `scale(16, 16, 16)` — and is pushed 0.5 item units towards the camera after an `Axis.XP.rotationDegrees(180)` flip. The flip puts the model's own up and front the right way round in this y-down, z-away frame; the push then leaves the whole unit cube in front of the sprite's plane, so the pill cannot hide the back half of a cake.

It is rendered with `ItemDisplayContext.GUI` and full brightness: GUI so a cake comes out isometric rather than as its flat inventory sprite, full brightness for the same reason an item frame uses it — what she is asking for has to be legible at night, and it is a picture of an item rather than an item sitting in the dark.

## 6. Tuning

| Number | Where | Value | Why that number |
|---|---|---|---|
| `MIN_DELAY` / `MAX_DELAY` | `AbstractBubbleModule` | 240 / 400 | twelve to twenty seconds: often enough to read as a habit, sparse enough not to be a ticker |
| `SHOW_TICKS` | `YoukaiBubbleLayer` | 200 | ten seconds, so a want can be walked over to and answered before it fades, and the next one is a new thought rather than a flicker of the same one |
| `WANT_PLACEMENTS` | `AbstractBubbleModule` | 3 | rolled per want so she does not always think out of the same corner |
| `SCALE` | `YoukaiBubbleLayer` | 0.025 | 48px pill becomes 1.2 blocks wide — just over a name plate: readable, but not the thing the player is looking at |
| `LIFT` | `YoukaiBubbleLayer` | 0.25 | gap above her hitbox; she hovers, so there is nothing for the bubble to sit on, and the hitbox is measured rather than the model because the model is not what the bubble belongs to |
| `SIDE_SHIFT` | `YoukaiBubbleLayer` | 12 | sheet pixels, 0.3 blocks: small on purpose — a fairy is 0.4 blocks wide, and a bubble a whole block off her head stops reading as hers |
| `ICON_LIFT` | `YoukaiBubbleLayer` | 0.5 | item-model units towards the camera: half the unit cube, so the pill cannot swallow the back of the model |
| `RANGE` | `YoukaiBubbleLayer` | 64 | the range a name tag stops being drawn at; past it the bubble is smaller than the item in it |
| `COOL_DOWN` | `FairyFeedModule` | 400 | twenty seconds of not being fed — the same number the temptation sensor respects |

## 7. Limits

- **No server authority.** A malicious client could draw a bubble whenever it liked. Nothing in the game reads it, so there is nothing to gain.
- **Not cross-client consistent.** See §2; deliberate.
- **Not activity-gated.** A fighting or talking fairy's bubble is on screen; the client cannot ask the brain. Sleep, eating, partying and combat are the exceptions (§4).
- **`tickCount` freezes outside the entity-ticking radius**, so a bubble can freeze mid-display on a fairy the client renders but does not tick. Vanilla name tags have the same exposure.
- **One bubble at a time.** A second want overwrites the first rather than queueing.