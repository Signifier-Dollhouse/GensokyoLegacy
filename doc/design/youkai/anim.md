# Youkai Animation — the rig, the stacking, and the two fades

`GeoYoukaiAnim` is the GeckoLib half of a character: `registerControllers` is the same for every rig, only
`getAnim` (slot → clip) and `stateAnimController` (the state loop) vary. This doc is the *why* behind the
controller layout — in particular why a one-shot is faded in by its own controller but faded out by the loop.

## 1. The three controllers

Registered in bone-writing order, which is what makes the layering work at all:

| Controller | Transition | Plays |
|---|---|---|
| `wink` | 0 | the `wink` blink, looping; first, so everything after it overwrites the face |
| `main` | 5 | the state loop (`stateAnimController`) and the hand-back (§3) |
| `one_shot` | 5 | one triggered clip at a time, from `getAnim` via `triggerableAnim` |

A GeckoLib controller plays exactly one animation, and a bone is written by the last controller that animates
it. Layering is therefore **per bone, not additive**: `one_shot` overrides the bones its clip has tracks for,
and `main` keeps driving the rest — that is what keeps her sitting, hovering or walking under a greet. A clip
with a full-body track wipes the loop's pose on those bones for its whole length; the fix for that is authoring
(§5), not code.

Splitting the loop from the one-shots is not optional: a single controller would have to *replace* the loop
with the clip, so every bone the clip does not carry would freeze (or reset to rest) until it ended.

## 2. What a "transition" is in GeckoLib 4

`AnimationController.setAnimation` + `transitionLength` does not blend two animations. It starts a
`TRANSITIONING` phase that, on the first tick, copies the current **bone snapshot** into the controller's own
`boneSnapshots`, and then, for `transitionLength` ticks, lerps from that snapshot to **frame 0** of the
incoming animation (`AnimationController#process`, `AnimationProcessor#tickAnimation`).

The snapshot it reads is the *shared* one, as it stands at the start of that controller's turn in the frame —
i.e. after every controller registered before it has already written. Two consequences:

- **Fading in is free.** When a trigger fires on `one_shot`, `main` has already written this frame, so the
  clip's transition starts from the pose on screen.
- **Fading out has nothing to work with.** When the clip runs out, `AnimationController` just sets `STOPPED`
  and writes nothing; there is no second animation to lerp to, and `main` is still running, so the shared
  bones snap from the clip's last pose to the loop pose in one frame.

The same ordering is why a mirror ("`one_shot` idles on the loop too") does not help: it would read the
snapshot after `main` refreshed it, so it would fade from the loop pose rather than from the clip pose, at the
cost of evaluating the loop twice on every entity every frame.

## 3. The hand-back

The loop does the fade-out, because it is the first controller of the frame and therefore the only one whose
transition source is still the previous frame's final pose:

1. `OneShotController#process` catches the clip's last tick **one frame early** (F-1) and rewrites its queue
   entry as `HOLD_ON_LAST_FRAME`, then latches `finished`. A held entry is written instead of dropped, so
   F-1 renders the clip's final pose (GeckoLib would otherwise set `STOPPED` and emit nothing — the snap).
2. On frame F, `loopController` consumes the latch **before** `stateAnimController` runs, drops the one-shot
   with `stopTriggeredAnimation()` (only if the clip it held still owns the controller), and calls
   `resetCurrentAnimation()`.
3. `setAnimation` rebuilds from the reload flag and re-enters `TRANSITIONING`, fading the held final pose
   into the loop's frame 0 over `ANIM_TRANSITION`.

A latch rather than a direct check because the end is only reachable at the end of F-1, and `main` has
already run by then. Detecting it inside `one_shot`'s own handler would be a frame late — `main` would have
overwritten the snapshot, and the fade would start from the loop's own pose, which is the snap we are
avoiding. Catching it early rather than after `super.process` is what keeps a pose to fade from: the frame
that leaves `RUNNING` writes nothing at all.

**Cost: the loop restarts at frame 0.** GeckoLib's transition always samples the target at tick 0, so a
phase-sensitive cycle (`walk`) gets its foot phase reset when a clip ends. `idle` / `sit` / hover are
unaffected, and ending a clip on the loop's frame-0 pose hides it (§5).

**Known limit: clip → clip.** The incoming clip fades from the loop pose, not from the outgoing clip's last
frame, for the same snapshot reason. Dialog chains and gift reactions read fine because those clips start
from the ambient pose; a clip that starts far from it will pop.

## 4. Holds

`thenPlayAndHold` (`EAT`) parks the controller in `PAUSED` on the last frame, so `hasAnimationFinished()` is
never true and the hand-back never runs — by design, the chew sits there until the next dish. Nothing ends a
hold on its own, so `YoukaiEating` ends it: when the chew clock reaches zero, or `stopEating` clears it, the
entity broadcasts `ANIM_EVENT_STOP` and `OneShotController#releaseHeld` drops the trigger (and latches
`finished`, so the loop fades back in from the held pose).

`releaseHeld` only touches a clip whose last stage is `HOLD_ON_LAST_FRAME`. So when something else already
took the controller — the fairy feed plays `swing` right after the chew — the release is ignored and the
natural hand-back of that clip runs instead.

## 5. Authoring rules

- **A one-shot clip carries only the bones it moves.** Everything it tracks is taken from the loop for its
  whole duration; everything it leaves out keeps stacking.
- **End a clip on the loop's frame-0 pose** when the loop is a moving cycle, so the fade-back and the phase
  restart are invisible.
- **Bone reset time is 5, the same as `ANIM_TRANSITION`.** A bone only the clip animated fades back to rest
  at the same rate the loop fades in, so the two halves of the release read as one motion.
- **A clip the rig cannot resolve** logs "Unable to find animation" every frame; that is why `getAnim`
  returns empty for unmapped slots and `addTriggerableAnims` only registers the mapped ones.
