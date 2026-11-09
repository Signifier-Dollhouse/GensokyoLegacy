# 1.20.1 backport

Status: A0 (L2 libraries), B0 (FastProjectileAPI) and B1 (DanmakuAPI) are
complete in their repos. A1 (mod bootstrap) is done — the Forge 1.20.1 build
configures, resolves both classpaths and passes `prepareRuns`; no Java source
has been ported yet. Current detail in [`state.md`](state.md), the source map
in [`source-port.md`](source-port.md). Branch `backport`.

Gensokyo Legacy is being moved from NeoForge 1.21.1 to Forge 1.20.1. This is a
cross-generation port, not a loader swap: the 1.21 line of the L2 libraries was
rewritten (registration, capabilities, networking, config), so the mod's
dependency layer has to be rebuilt for 1.20.1 before any content compiles.

## Source and target

| | source | target |
|---|---|---|
| loader | NeoForge 21.1.238 | Forge 47.1.3 |
| Java | 21 | 17 |
| Gradle | 8.8 | 8.10 |
| build plugin | `net.neoforged.moddev` 2.0.80 | `net.minecraftforge.gradle` `[6.0,6.2)` |
| Registrate | `MC1.21-1.3.0+50` | `MC1.20-1.3.11` |
| L2 base | l2core 3.0.8 | l2library 2.5.x + backported 3.x deltas |
| mod descriptor | `META-INF/neoforge.mods.toml` | `META-INF/mods.toml` |
| `pack_format` | 9 (stale) | 15 |
| mod version | `1.0.7-beta.1+1.21.1` | to be chosen |

Reference implementation: `~/git/delight/Youkai-Homecoming-1.20` (Youkai's
Feasts, Forge 1.20.1). It is a guide, not an oracle: it ships a stale
`pack.mcmeta` (9), imports `fastprojectileapi` without declaring it, and has
Touhou Little Maid commented out.

## Scale

- 1,205 Java files, 97,502 lines.
- 21 mixins, 48 packet classes, 136 attachment files, 71 datagen classes.
- 2,971 generated resource files.
- 91 files depend on danmaku / fast-projectile APIs.

Library imports in `src/main/java`:

| package | imports |
|---|---|
| `dev.xkmc.l2serial` | 283 |
| `dev.xkmc.danmakuapi` | 260 |
| `dev.xkmc.l2modularblock` | 158 |
| `dev.xkmc.l2core` | 127 |
| `dev.xkmc.l2itemselector` | 52 |
| `dev.xkmc.fastprojectileapi` | 24 |
| `dev.xkmc.l2menustacker` | 22 |
| `dev.xkmc.l2damagetracker` | 15 |
| `dev.xkmc.l2tabs` | 11 |
| `dev.xkmc.l2library` | 5 |

`l2harvester` is never imported, but it is jar-in-jar'd: `youkaisfeasts`
declares it a mandatory dependency and the slim build does not ship it.

## Decisions

Resolved:

| # | decision | choice |
|---|---|---|
| D1 | loader target | Forge 1.20.1 only |
| D2 | L2 strategy | 1.20.1 L2 2.x with the legacy API; add only the contents the mod needs; L2Library-1.20 stands in for L2Core |
| D3 | danmaku | port DanmakuAPI and FastProjectileAPI, using `Youkai-Homecoming-1.20` `main` as the 1.20.1 reference |
| D4 | data components | shim layer over NBT, keep call sites |
| D12 | data maps | `BaseConfig` / `ConfigTypeEntry` in l2library 1.20.1, not a custom reload listener |
| D19 | L2DamageTracker, L2Tabs | target the legacy 1.20.1 API; do not backport the 1.21.1 refactor |
| D20 | L2Serial | legacy 1.20.1 API; backport fixes only, no 3.x packet model |
| D21 | L2Library / L2Core | legacy 1.20.1 API and class location; backport the missing classes into `dev.xkmc.l2library` |
| D22 | L2ItemSelector | legacy 1.20.1 API; overlay lives in `l2library.base.overlay` |
| D23 | L2ModularBlock | backport the 3.x contents, keep the legacy class location (no `core/` move) |
| O2 | `DoubleBlockImpl` | restore the missing 1.21.1 content first, then port it down |
| O3 | Touhou Little Maid | keep; same integration exists in Youkai-Homecoming-1.20 |
| O4 | l2harvester | not ported and never imported; jar-in-jar the 1.20.1 build because `youkaisfeasts` requires it |
| O5 | L2ScreenTracker | use the legacy `l2screentracker` names; backport `CacheMouseToClient` / `MouseCache` |
| O6 | registration framework | mod-side compat layer on need; no `init/reg` additions to the L2Library jar |
| O7 | BaseConfig reads | confirmed: `ConfigTypeEntry.getEntry(id)` + `PacketHandlerWithConfig` |
| D24 | L2 dependencies | L2Library slim is an explicit external dependency; the jars it ships are `implementation`; only non-shipped libraries are `jarJar`'d |

No open decisions remain; the library work plan is in
[`library-plan.md`](library-plan.md).

The L2Core enchantment system is explicitly out of scope for 1.20.1.
`DataGenHolder` is the one exception — it lives in that package but is a generic
datagen holder used by the mod.

## Documents

- [`state.md`](state.md) — current work state: versions, artifacts, blockers.
- [`source-port.md`](source-port.md) — compile baseline, mechanical rewrites, compat surface, port order.
- [`libraries.md`](libraries.md) — per-library state, 3.x deltas, jar/source gaps.
- [`discrepancies.md`](discrepancies.md) — class- and signature-level API differences.
- [`library-plan.md`](library-plan.md) — jar-by-jar work plan, build order, outputs.
- [`plan.md`](plan.md) — workstreams, phase order, exit criteria, risks.
- [`decisions.md`](decisions.md) — decision log with rationale.
