# Merging `main` into `backport` (2026-11-25)

`main` = 1.21.1 / NeoForge, tip `f4b7893b`. `backport` = 1.20.1 / Forge, tip
`918acb6b`. Merge base `18d47059` (`chat bubble and gap portal rendering tweak`),
so `main` is 85 commits ahead.

Previous syncs merged one `main` tip at a time (see the `Merge branch 'main' into
backport` commits). This plan keeps that model but splits the 85 commits into
phases so each merge is reviewable and a bad phase can be reverted alone.

## Method

- Work on a scratch branch, keep `backport` untouched until the end:
  `git switch -c merge/main-2026-11` from `backport`, tag `backport/pre-main-merge`.
- One phase = one or more `git merge --no-commit <endpoint>`; resolve, compile,
  `runData`, then commit each merge. The phase table lists the endpoints in
  merge order. Every endpoint has all its parents already merged, so a merge
  brings only that phase's commits (no phase can drag the whole branch in).
- Port the Java with [`api.md`](api.md); regenerate `src/generated` with
  `./gradlew runData` instead of resolving it by hand
  (see [`README.md`](README.md) §"Merging a 1.21.1 change").
- After a phase that changes zh_cn: run `organize.ResourceOrganizer`; after a
  phase that changes dialogs/quests: `python3 tools/export_dialogs.py`.
- Green gate per phase: `./gradlew compileJava -Dnet.minecraftforge.gradle.check.certs=false`.
  Run `./gradlew build` and one `runClient` at the end.
- A merge commit whose two parents are already merged (`e8ae691c`, `fbaaef28`,
  `e4d6d7a9`, `4206f2dc`, `334315d7`, `aed0a90f`, `733e47bb`, ...) has an
  almost-empty diff. Merge it anyway: it records ancestry and applies `main`'s
  own resolution where it differs from ours.

## Reorganization commits (merged alone)

These have almost no logic but a large or binary diff. Each is its own phase.

| commit | subject | size | why alone |
|---|---|---|---|
| `31815f82` | custom models in main resources | 19 files, -497 | pure move `src/generated` → `src/main` + drop a datagen provider |
| `d7262b43` | organize nbt | 78 files, +96/-96 | 48 `R100` moves of structure NBT |
| `807ee762` | scroll rename | 16 files, +61/-61 | block/item rename + asset moves |
| `0938fc0e` | root fix | 1 file, 0/0 | binary NBT rewrite only |
| `edddf207` | structure void | 16 files, 0/0 | binary NBT rewrite only |
| `d607d7d8` | design doc rework | 188 files, +646/-1430 | docs renames/rewrites |

## Bulk asset commits (also kept as their own step)

Not renames, but too large to review inside a feature phase.

| commit | subject | size |
|---|---|---|
| `76821054` | add textures | 131 files, +2758/-257 |
| `b55cd0b3` | alice | 3 files, +1713/-715 (geo/animation) |
| `7c17f3c5` | nue and byakuren | 27 files, +17305/-14 |
| `40f2f7f5` | myouren temple | 100 files, +1193/-11 (NBT + worldgen) |

## Phases

`endpoints` are the merge targets, in order. `commits` is what the phase brings
in (including commits pulled by a merge endpoint). Sizes are the sum of the
individual commit diffs; a merge commit's first-parent diff double-counts its
branch, so read them as an upper bound.

| # | phase | endpoints | commits | files | +/- | notes |
|---|---|---|---|---|---|---|
| 1 | basket | `c85ca016` | `e085ee90` `4e9fba7f` `ccc5ded1` | 42 | +2034/-53 | block + UI + models |
| 2 | kaatenn assets + dev sync | `3dcd59dc` `76821054` `cd3ead7f` `ea373765` | | 165 | +5331/-647 | `76821054` is the bulk textures/models; last two are empty merges |
| 3 | **REORG** custom models | `31815f82` | | 19 | +0/-497 | move only |
| 4 | treasure tower + stray doll | `0e326e8c` | `f8e7a700` `11dc7746` | 41 | +1684/-170 | entity/behavior + items |
| 5 | alice + kaatenn merge | `b55cd0b3` `b0a790d1` | | 10 | +3486/-1439 | bulk geo/animation, then merge |
| 6 | fallback structure + secondary tier | `e93d2b4a` `e8ae691c` `e7f24117` | | 198 | +8414/-1459 | worldgen + datagen; `e8ae691c` is the empty merge |
| 7 | interior wand | `380fa49b` | `04553b22` `c7c88640` `b37c174c` | 37 | +3116/-313 | item tools + docs |
| 8 | **REORG** organize nbt | `d7262b43` | | 78 | +96/-96 | NBT moves |
| 9 | wand fix + myouren temple | `40f2f7f5` | `53e66af3` | 109 | +1365/-43 | bulk NBT; also "wand fix" |
| 10 | deco set | `01afc812` | | 72 | +3188/-18 | scroll/lantern/censer/basket, mostly assets |
| 11 | **REORG** scroll rename | `807ee762` | | 16 | +61/-61 | rename |
| 12 | scroll pile + censer + merge | `be1a5cc5` `fbaaef28` | `e535afbd` | 105 | +4412/-48 | blocks/assets; `fbaaef28` is the empty merge |
| 13 | myouren update + lake + locust | `95e0eac2` | `358874fd` | 18 | +90/-10 | small worldgen |
| 14 | **REORG** root fix | `0938fc0e` | | 1 | 0/0 | binary NBT |
| 15 | terrain predicate + bastion | `d1a7250b` `4206f2dc` | `9cc0c7d8` `d0af1add` | 26 | +1357/-118 | worldgen + `4206f2dc` empty merge |
| 16 | scan + fairy dust + flower | `41306986` | `a6a1630e` `f56554eb` | 6 | +445/-22 | item/worldgen/command |
| 17 | **REORG** structure void | `edddf207` | | 16 | 0/0 | binary NBT |
| 18 | cobblestone + merge + scan cache | `fcd3b75a` `e4d6d7a9` `3b557f5f` | | 29 | +416/-113 | worldgen; `e4d6d7a9` empty merge |
| 19 | **REORG** design doc rework | `d607d7d8` | | 188 | +646/-1430 | docs only |
| 20 | merge dev + graveyard/tower/doll | `334315d7` `3f9c377c` `32f37fe4` `0fc3887d` `aed0a90f` | `da77e1bf` | 537 | +11889/-2628 | split per branch: graveyard → tower → doll; `334315d7`/`aed0a90f` empty merges |
| 21 | nue and byakuren | `7c17f3c5` | | 27 | +17305/-14 | bulk geo/animation |
| 22 | nue data + tower + merge | `d0935c07` `ec98d54d` `733e47bb` | `5a7725cc` | 66 | (merge double-counts 21) | entity/data; `733e47bb` empty merge |
| 23 | nue gift + main sync | `3b4b9543` `c0d82baa` | | 6 | +102/-9 | small |
| 24 | nazrin | `fceba383` `6f5987bd` `af8ee2ed` | | 52 | +8963/-171 | entity → quest → anim, one merge each |
| 25 | dialog zh + main sync | `bad9ec31` `96b1d4a7` | | 83 | +2392/-1943 | text/JSON port, not a reorg |
| 26 | quest/gift/tower + main sync | `d5047939` `b7fbd2b6` | `74ea683d` `11c90baf` `91cad53a` | 43 | +951/-254 | RPG + JEI |
| 27 | donation/gift/ufo + main sync | `1a825e68` `a7b3add9` `a90b1252` `c1d66f76` | `d41e08af` `eae78528` `67e63c0b` `d9e10902` `707557e2` | 137 | +1890/-539 | blocks/dialogs/sounds; 3 empty merges |
| 28 | patchouli + gift box + sync | `02491173` `123bb24a` `233e5ed3` | | 78 | +764/-250 | book text + gift box; `233e5ed3` empty merge |
| 29 | patchouli + advancements | `ff01b8bc` | `5868b1f0` | 55 | +621/-95 | book + advancement JSON |
| 30 | deco blocks + beds | `f4b7893b` | `28608e17` `57de66a7` | 10 | +112/-18 | final tail |

After phase 30, `git merge-base --is-ancestor main HEAD` must be true; if not,
run one last `git merge main` and resolve the remainder.

## Ordering rules that make this safe

- Every endpoint's parents are either in `backport` already or in an earlier
  phase (checked over all 85 commits). This is what bounds each merge diff.
- Never merge `main` itself until the last phase: `main` is not an ancestor of
  the phase endpoints on the side branches, and merging it early pulls all 85
  commits at once.
- A reorg phase is never combined with a logic phase, so a move-only conflict
  can be resolved with `git checkout --theirs`/`--ours` and verified by the
  datagen diff instead of reading the patch.

## Phase 1 preview (`git merge-tree HEAD c85ca016`)

Conflicts: `gradle.properties`, `libs/l2serial-*.jar` (deleted in 1.20.1, keep
deleted), `GensokyoLegacy.java`, `init/data/GLModConfig.java`,
`init/registrate/GLMisc.java`, `accesstransformer.cfg`, generated `en_us`/
`en_ud` lang and `loot_table/` vs `loot_tables/` (keep 1.20.1 layout, then
`runData`). No entity or RPG code conflicts, so phase 1 is a good calibration.

## Likely conflict hotspots

- `src/generated/**` in phases 2, 6, 9, 10, 12, 20, 22, 24, 29, 30 — regenerate.
- Java in `init/registrate/GL*.java`, `init/data/**`, `content/rpg/**`,
  `content/entity/**` — 1.21.1 → 1.20.1 API, see `api.md` §3/§4/§11/§12.
- `src/main/resources/META-INF/`, `gensokyolegacy.mixins.json`,
  `accesstransformer.cfg` — only phase 2/6 should touch the build/loader files;
  keep the 1.20.1 side.
- `patchouli_books` in phases 28/29 — path layout differs from 1.20.1.
