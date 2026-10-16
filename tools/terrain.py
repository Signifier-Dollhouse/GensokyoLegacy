#!/usr/bin/env python3
"""
Export three preview images of a dimension's terrain: a biome map, a height map and a
parameter map.

All three are one pixel per chunk, 512x512 by default, so the default export covers
4096x4096 blocks around the world origin:

    out/terrain/<dimension>_biomes.png      every biome in its own color
    out/terrain/<dimension>_height.png      blue below sea level, one color per 16 blocks above
    out/terrain/<dimension>_parameters.png  the eight regions temperature/vegetation/erosion tile
    out/terrain/<dimension>.txt             biome colors, map shares, height histogram

`--spline` plots the continentalness spline instead, in three panels:

    out/terrain/<dimension>_spline.png    the spline, the height profile it draws, the band shares
    out/terrain/<dimension>_spline.txt    the same numbers as a table

`--voids` reports the caves instead of drawing them, since one pixel per chunk cannot
show a volume:

    out/terrain/<dimension>_caves.txt    carved volume, surface openings, void sizes

See "## The spline plot" below.

    python3 tools/terrain.py gensokyolegacy:gensokyo
    python3 tools/terrain.py gensokyolegacy:gensokyo --seed 12345 --size 1024
    python3 tools/terrain.py gensokyolegacy:gensokyo --spline
    python3 tools/terrain.py gensokyolegacy:gensokyo --voids --cheese
    python3 tools/terrain.py gensokyolegacy:gap --out out/gap

The seed matters: the same seed as the world gives the same terrain, so a map can be
diffed against what the game generated (`run/saves/*/level.dat` holds the seed).

## What it reads

The dimension json names a biome source and a noise settings entry, and the noise
settings entry carries the **whole noise router**, so nothing here mirrors
`NoiseRouterData`: the terrain comes from the same json the game loads. The vanilla
density functions and noise parameters (`minecraft:overworld/continents`,
`minecraft:offset`, ...) are read out of the NeoForge client-extra jar, and the mod's
own entries out of `src/generated/resources`. `--datapack` adds more roots.

`noise_settings` gives the y range and `sea_level` gives the water line, and the router
gives the six climate axes (`temperature`, `vegetation` = humidity, `continents`,
`erosion`, `depth`, `ridges` = weirdness). Biomes come from the multi-noise grid in the
dimension json, resolved nearest-cell with vanilla's quantised fitness, sampled at the
4x4x4 cell centre of the surface block the way `Climate.Sampler` does.

The parameter map drops the biome grid and draws the climate axes themselves: each of
`temperature`, `vegetation` and `erosion` is cut once, at `CLIMATE_CUTS`, and a pixel takes
the colour of the one of the eight regions its cell lands in. It is the partition the grid
is built out of, so it shows which parts of the map are decided by which axis.

The height map is the **terrain** surface, the topmost block whose density is positive,
so under the sea level it shows the sea floor rather than the water surface: what the
game records in its `WORLD_SURFACE` heightmap is the water, a few blocks higher.

## What it approximates

The surface is found by scanning y down from the top of the world in steps of 16 and
then walking the bracket, so the answer is exact rather than quantised (the terrain
density is monotone in y, so a step cannot skip a surface). Two things are dropped:

- **caves.** `--caves` evaluates the router's `final_density` verbatim instead of the
  pruned chain. Caves only remove blocks, so the pruned surface is the cave-free one.
- **base_3d_noise ("cheese").** It is worth a few blocks of height and costs 40 simplex
  octaves per sample. `--cheese` keeps it.

The blender is always identity: `blend_alpha` is 1.0 wherever no other chunk is in
range, which for this mod is everywhere.

The pruned chain is `postProcess(slideOverworld(sloped_cheese))`, which is the terrain
term of vanilla's `final_density` with the cave branches cut. `postProcess` and
`slideOverworld` are built here because they only exist as Java, see `NoiseRouterData`.

`--voids` uses both chains: the pruned one as the datum every void is measured from, and
the full one for the air. It does not model the aquifer, so an air pocket below sea level
reads as air where the game would fill it.

## The spline plot

`--spline` draws the spline the height chain splines continentalness through, and what that
spline does to the shape of the map. It finds the `spline` nodes keyed on the router's
`continents` inside the `depth` entry — the chain that places the surface — and asks the
game's own chain where each spline value puts the surface, by pinning every such spline to
one value and scanning for the surface the way the height map does. So the heights are the
density the game builds, not a formula of the y gradient written out here, and a router
that rescaled would move the curve without touching this tool.

Where the map sits comes from sampling the `continents` field over a grid of chunk centres,
the same grid size and origin as the map (`--size`, `--origin`), and reading the shares off
that sample's distribution. Three panels:

1. **surface y against continentalness**, with the sampled `continents` distribution shaded
   under it, so the curve can be read against the continentalness that actually occurs. A
   flat stretch past the last of the sampled data is a height the spline spends on nothing.
2. **surface y against the share of the map below it.** A straight diagonal is a height
   gradient spread evenly over the map; a horizontal run is a share of the map held at one
   height, which is what a mesa is. Drawn in red.
3. **share of the map per single block of height.** `CubicSpline` with zero derivatives
   interpolates as `low + (high - low) * t²`, so each segment crowds its height toward its
   lower edge and the bars spike at every breakpoint. A wide bar is a shelf; a run of tall
   bars is a mesa.

None of this needs the surface chain per column, only per spline value, so it is far
cheaper than a map: one noise field over the grid and a few hundred vertical scans.
"""

import argparse
import bisect
import hashlib
import json
import math
import os
import struct
import sys
import zlib
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD_PACK = os.path.join(ROOT, "src/generated/resources")
ARTIFACTS = os.path.join(ROOT, "build/moddev/artifacts")
DEFAULT_OUT = os.path.join(ROOT, "out/terrain")

MASK64 = (1 << 64) - 1
MASK32 = (1 << 32) - 1
GOLDEN_RATIO_64 = -7046029254386353131
SILVER_RATIO_64 = 7640891576956012809
# XoroshiroRandomSource scales nextDouble by this literal, a float rounded constant
DOUBLE_UNIT = struct.unpack("f", struct.pack("f", 1.110223E-16))[0]
# PerlinNoise wraps inputs so the permutation index stays in range
WRAP = 3.3554432E7
NORMAL_NOISE_INPUT_FACTOR = 1.0181268882175227


class Bad(Exception):
    """Anything the tool cannot do, reported with a sentence rather than a traceback."""


# --------------------------------------------------------------------------------------
# random and noise, ported from RandomSupport / XoroshiroRandomSource / PerlinNoise
# --------------------------------------------------------------------------------------


def rotl(value, bits):
    return ((value << bits) | (value >> (64 - bits))) & MASK64


class Xoroshiro:
    """`Xoroshiro128PlusPlus` plus the `nextInt` and `nextDouble` of `XoroshiroRandomSource`."""

    __slots__ = ("lo", "hi")

    def __init__(self, lo, hi):
        if (lo | hi) == 0:
            lo, hi = GOLDEN_RATIO_64, SILVER_RATIO_64
        self.lo = lo & MASK64
        self.hi = hi & MASK64

    @classmethod
    def of_seed(cls, seed):
        """`XoroshiroRandomSource(long)` / `RandomSupport.upgradeSeedTo128bit`."""
        lo = (seed ^ SILVER_RATIO_64) & MASK64
        return cls(mix_stafford13(lo), mix_stafford13((lo + GOLDEN_RATIO_64) & MASK64))

    def next_long(self):
        lo, hi = self.lo, self.hi
        result = (rotl((lo + hi) & MASK64, 17) + lo) & MASK64
        hi ^= lo
        self.lo = (rotl(lo, 49) ^ hi ^ (hi << 21)) & MASK64
        self.hi = rotl(hi, 28)
        return result

    def next_double(self):
        return (self.next_long() >> 11) * DOUBLE_UNIT

    def next_int(self, bound):
        """`XoroshiroRandomSource.nextInt(int)`, which throws away the low half for uniformity."""
        i = self.next_long() & MASK32
        j = i * bound
        k = j & MASK32
        if k < bound:
            limit = ((1 << 32) - bound) % bound
            while k < limit:
                i = self.next_long() & MASK32
                j = i * bound
                k = j & MASK32
        return j >> 32

    def consume(self, count):
        for _ in range(count):
            self.next_long()

    def fork_positional(self):
        """`XoroshiroRandomSource.forkPositional`, a factory seeded by the next two longs."""
        return (self.next_long(), self.next_long())


def mix_stafford13(seed):
    """`RandomSupport.mixStafford13`."""
    seed = ((seed ^ (seed >> 30)) * -4658895280553007687) & MASK64
    seed = ((seed ^ (seed >> 27)) * -7723592293110705685) & MASK64
    return seed ^ (seed >> 31)


def seed_from_hash_of(name):
    """`RandomSupport.seedFromHashOf`: the md5 of the key string, as two signed longs."""
    digest = hashlib.md5(name.encode("utf-8")).digest()
    return (int.from_bytes(digest[:8], "big", signed=True),
            int.from_bytes(digest[8:], "big", signed=True))


class PositionalRandom:
    """`XoroshiroRandomSource.XoroshiroPositionalRandomFactory`, the only thing `fromHashOf` needs."""

    __slots__ = ("lo", "hi")

    def __init__(self, lo, hi):
        self.lo = lo & MASK64
        self.hi = hi & MASK64

    def from_hash_of(self, name):
        lo, hi = seed_from_hash_of(name)
        return Xoroshiro(lo ^ self.lo, hi ^ self.hi)


GRADIENT = ((1, 1, 0), (-1, 1, 0), (1, -1, 0), (-1, -1, 0),
            (1, 0, 1), (-1, 0, 1), (1, 0, -1), (-1, 0, -1),
            (0, 1, 1), (0, -1, 1), (0, 1, -1), (0, -1, -1),
            (1, 1, 0), (0, -1, 1), (-1, 1, 0), (0, -1, -1))


def wrap(value):
    """`PerlinNoise.wrap`, keeps the lattice index inside the permutation table."""
    return value - math.floor(value / WRAP + 0.5) * WRAP


class ImprovedNoise:
    """One octave: a seeded permutation table, the `SimplexNoise.GRADIENT` dot product."""

    __slots__ = ("p", "xo", "yo", "zo")

    def __init__(self, random):
        self.xo = random.next_double() * 256.0
        self.yo = random.next_double() * 256.0
        self.zo = random.next_double() * 256.0
        p = bytearray(range(256))
        for k in range(256):
            j = random.next_int(256 - k)
            p[k], p[k + j] = p[k + j], p[k]
        self.p = p

    def noise(self, x, y, z, y_scale=0.0, y_max=0.0):
        x += self.xo
        y += self.yo
        z += self.zo
        i = math.floor(x)
        j = math.floor(y)
        k = math.floor(z)
        dx = x - i
        dy = y - j
        dz = z - k
        # the blended noise samples y on a coarser lattice, clamped to the cell below y
        y_used = 0.0
        if y_scale != 0.0:
            y_used = y_max if 0.0 <= y_max < dy else dy
            y_used = math.floor(y_used / y_scale + 1.0E-7) * y_scale
        return self._lerp3(i, j, k, dx, dy - y_used, dz, dy)

    def _lerp3(self, gx, gy, gz, dx, wdy, dz, dy):
        p = self.p
        gradient = GRADIENT
        i = p[gx & 0xFF]
        j = p[(gx + 1) & 0xFF]
        k = p[(i + gy) & 0xFF]
        l = p[(i + gy + 1) & 0xFF]
        i1 = p[(j + gy) & 0xFF]
        j1 = p[(j + gy + 1) & 0xFF]
        # `SimplexNoise.GRADIENT` dots, one per cell corner
        d0 = p[(k + gz) & 0xFF] & 15
        d1 = p[(i1 + gz) & 0xFF] & 15
        d2 = p[(l + gz) & 0xFF] & 15
        d3 = p[(j1 + gz) & 0xFF] & 15
        d4 = p[(k + gz + 1) & 0xFF] & 15
        d5 = p[(i1 + gz + 1) & 0xFF] & 15
        d6 = p[(l + gz + 1) & 0xFF] & 15
        d7 = p[(j1 + gz + 1) & 0xFF] & 15
        g = gradient[d0]
        v0 = g[0] * dx + g[1] * wdy + g[2] * dz
        g = gradient[d1]
        v1 = g[0] * (dx - 1.0) + g[1] * wdy + g[2] * dz
        g = gradient[d2]
        v2 = g[0] * dx + g[1] * (wdy - 1.0) + g[2] * dz
        g = gradient[d3]
        v3 = g[0] * (dx - 1.0) + g[1] * (wdy - 1.0) + g[2] * dz
        g = gradient[d4]
        v4 = g[0] * dx + g[1] * wdy + g[2] * (dz - 1.0)
        g = gradient[d5]
        v5 = g[0] * (dx - 1.0) + g[1] * wdy + g[2] * (dz - 1.0)
        g = gradient[d6]
        v6 = g[0] * dx + g[1] * (wdy - 1.0) + g[2] * (dz - 1.0)
        g = gradient[d7]
        v7 = g[0] * (dx - 1.0) + g[1] * (wdy - 1.0) + g[2] * (dz - 1.0)
        sx = _smoothstep(dx)
        sy = _smoothstep(dy)
        sz = _smoothstep(dz)
        a = v0 + (v1 - v0) * sx
        b = v2 + (v3 - v2) * sx
        c = v4 + (v5 - v4) * sx
        d = v6 + (v7 - v6) * sx
        e = a + (b - a) * sy
        f = c + (d - c) * sy
        return e + (f - e) * sz


def _smoothstep(value):
    return value * value * value * (value * (value * 6.0 - 15.0) + 10.0)


def clamp(value, low, high):
    return low if value < low else (high if value > high else value)


class PerlinNoise:
    """`PerlinNoise`: a stack of `ImprovedNoise` octaves, either factory-seeded or sequential."""

    __slots__ = ("levels", "amplitudes", "input_factor", "value_factor")

    def __init__(self, random, first_octave, amplitudes, legacy):
        self.amplitudes = amplitudes
        self.levels = levels = [None] * len(amplitudes)
        if legacy:
            self._fill_legacy(random, first_octave)
        else:
            factory = PositionalRandom(*random.fork_positional())
            for k, amplitude in enumerate(amplitudes):
                if amplitude != 0.0:
                    levels[k] = ImprovedNoise(factory.from_hash_of("octave_%d" % (first_octave + k)))
        size = len(amplitudes)
        self.input_factor = 2.0 ** first_octave
        self.value_factor = 2.0 ** (size - 1) / (2.0 ** size - 1.0)

    def _fill_legacy(self, random, first_octave):
        """`useNewFactory == false`: one octave at a time, highest octave first, zero ones skipped."""
        size = len(self.amplitudes)
        highest = -first_octave
        if 0 <= highest < size and self.amplitudes[highest] != 0.0:
            self.levels[highest] = ImprovedNoise(random)
        for k in range(highest - 1, -1, -1):
            if k < size and self.amplitudes[k] != 0.0:
                self.levels[k] = ImprovedNoise(random)
            else:
                random.consume(262)

    def value(self, x, y, z, y_scale=0.0, y_max=0.0):
        total = 0.0
        input_factor = self.input_factor
        value_factor = self.value_factor
        for k, level in enumerate(self.levels):
            if level is not None:
                total += (self.amplitudes[k] * value_factor
                          * level.noise(wrap(x * input_factor), wrap(y * input_factor), wrap(z * input_factor),
                                        y_scale * input_factor, y_max * input_factor))
            input_factor *= 2.0
            value_factor *= 0.5
        return total

    def octave(self, index):
        return self.levels[len(self.levels) - 1 - index]


class NormalNoise:
    """`NormalNoise`: two perlin stacks, one sampled at a slightly offset input."""

    __slots__ = ("first", "second", "value_factor")

    def __init__(self, random, first_octave, amplitudes):
        self.first = PerlinNoise(random, first_octave, amplitudes, legacy=False)
        self.second = PerlinNoise(random, first_octave, amplitudes, legacy=False)
        used = [i for i, a in enumerate(amplitudes) if a != 0.0]
        if not used:
            self.value_factor = 0.0
            return
        # `NormalNoise.expectedDeviation` over the span of the octaves that are not zero
        deviation = 0.1 * (1.0 + 1.0 / (max(used) - min(used) + 1))
        self.value_factor = (1.0 / 6.0) / deviation

    def value(self, x, y, z):
        factor = NORMAL_NOISE_INPUT_FACTOR
        return ((self.first.value(x, y, z)
                 + self.second.value(x * factor, y * factor, z * factor)) * self.value_factor)


class BlendedNoise:
    """`BlendedNoise`: a cell noise lerping between two 16 octave limit noises."""

    __slots__ = ("min_limit", "max_limit", "main", "xz_multiplier", "y_multiplier", "xz_factor", "y_factor", "smear")

    def __init__(self, random, xz_scale, y_scale, xz_factor, y_factor, smear):
        self.min_limit = PerlinNoise(random, -15, [1.0] * 16, legacy=True)
        self.max_limit = PerlinNoise(random, -15, [1.0] * 16, legacy=True)
        self.main = PerlinNoise(random, -7, [1.0] * 8, legacy=True)
        self.xz_multiplier = 684.412 * xz_scale
        self.y_multiplier = 684.412 * y_scale
        self.xz_factor = xz_factor
        self.y_factor = y_factor
        self.smear = smear

    def value(self, block_x, block_y, block_z):
        x = block_x * self.xz_multiplier
        y = block_y * self.y_multiplier
        z = block_z * self.xz_multiplier
        dx = x / self.xz_factor
        dy = y / self.y_factor
        dz = z / self.xz_factor
        smear = self.y_multiplier * self.smear
        smeared = smear / self.y_factor
        main_total = 0.0
        amplitude = 1.0
        for i in range(8):
            level = self.main.octave(i)
            if level is not None:
                main_total += level.noise(wrap(dx * amplitude), wrap(dy * amplitude), wrap(dz * amplitude),
                                          smeared * amplitude, dy * amplitude) / amplitude
            amplitude *= 0.5
        blend = (main_total / 10.0 + 1.0) / 2.0
        at_max = blend >= 1.0
        at_min = blend <= 0.0
        low_total = 0.0
        high_total = 0.0
        amplitude = 1.0
        for j in range(16):
            wx = wrap(x * amplitude)
            wy = wrap(y * amplitude)
            wz = wrap(z * amplitude)
            ws = smear * amplitude
            if not at_max:
                level = self.min_limit.octave(j)
                if level is not None:
                    low_total += level.noise(wx, wy, wz, ws, y * amplitude) / amplitude
            if not at_min:
                level = self.max_limit.octave(j)
                if level is not None:
                    high_total += level.noise(wx, wy, wz, ws, y * amplitude) / amplitude
            amplitude *= 0.5
        low = low_total / 512.0
        high = high_total / 512.0
        if blend < 0.0:
            value = low
        elif blend > 1.0:
            value = high
        else:
            value = low + blend * (high - low)
        return value / 128.0


# --------------------------------------------------------------------------------------
# datapacks
# --------------------------------------------------------------------------------------


class DataPack:
    """Density functions and noise parameters from datapack roots, which may be jars."""

    REGISTRIES = ("dimension", "worldgen/noise_settings", "worldgen/density_function", "worldgen/noise")

    def __init__(self, roots):
        self.entries = {registry: {} for registry in self.REGISTRIES}
        for root in roots:
            self._load(root)

    def _load(self, root):
        if not root:
            return
        if os.path.isdir(root):
            self._load_dir(root)
        elif os.path.isfile(root):
            self._load_jar(root)
        else:
            raise Bad("datapack root not found: " + root)

    def _load_dir(self, root):
        for registry in self.REGISTRIES:
            base = os.path.join(root, "data")
            if not os.path.isdir(base):
                continue
            for namespace in os.listdir(base):
                directory = os.path.join(base, namespace, registry)
                if not os.path.isdir(directory):
                    continue
                for dirpath, _, names in os.walk(directory):
                    for name in names:
                        if not name.endswith(".json"):
                            continue
                        path = os.path.join(dirpath, name)
                        key = namespace + ":" + os.path.relpath(path, directory)[:-5].replace(os.sep, "/")
                        with open(path, "r", encoding="utf-8") as handle:
                            self.entries[registry][key] = json.load(handle)

    def _load_jar(self, root):
        import zipfile
        with zipfile.ZipFile(root) as jar:
            for name in jar.namelist():
                if not name.startswith("data/") or not name.endswith(".json"):
                    continue
                parts = name.split("/")
                if len(parts) < 4:
                    continue
                namespace = parts[1]
                for registry in self.REGISTRIES:
                    span = len(registry.split("/"))
                    if parts[2:2 + span] != registry.split("/"):
                        continue
                    path = "/".join(parts[2 + span:-1] + [parts[-1][:-len(".json")]])
                    self.entries[registry][namespace + ":" + path] = json.loads(jar.read(name))
                    break

    def get(self, registry, key):
        entries = self.entries.get(registry)
        if entries is None:
            raise Bad("unknown registry " + registry)
        if key not in entries:
            raise Bad("no entry %s in %s" % (key, registry))
        return entries[key]


def find_vanilla_pack():
    if not os.path.isdir(ARTIFACTS):
        return None
    for name in sorted(os.listdir(ARTIFACTS)):
        if "client-extra" in name and name.endswith(".jar"):
            return os.path.join(ARTIFACTS, name)
    return None


# --------------------------------------------------------------------------------------
# density function graph, compiled to closures
# --------------------------------------------------------------------------------------


def to_float(value):
    """Java computes climate samples in `float`, and the biome lookup quantises them."""
    return struct.unpack("f", struct.pack("f", value))[0]


# which fields of each density function hold a child density function, from their codecs; used
# to walk a graph without mistaking a noise key or a spline's points for a reference
CHILD_FIELDS = {
    "add": ("argument1", "argument2"), "mul": ("argument1", "argument2"),
    "min": ("argument1", "argument2"), "max": ("argument1", "argument2"),
    "abs": ("argument",), "square": ("argument",), "cube": ("argument",),
    "half_negative": ("argument",), "quarter_negative": ("argument",), "squeeze": ("argument",),
    "clamp": ("input",), "weird_scaled_sampler": ("input",),
    "range_choice": ("input", "when_in_range", "when_out_of_range"),
    "shifted_noise": ("shift_x", "shift_y", "shift_z"),
    "spline": ("spline.coordinate",),
    "interpolated": ("argument",), "flat_cache": ("argument",), "cache_2d": ("argument",),
    "cache_once": ("argument",), "cache_all_in_cell": ("argument",), "blend_density": ("argument",),
    "beardifier": ("argument",), "constant": ("argument",),
    "noise": (), "shift": (), "shift_a": (), "shift_b": (), "old_blended_noise": (),
    "y_clamped_gradient": (), "blend_alpha": (), "blend_offset": (), "end_islands": (), "zero": (),
}


class Column:
    """The 2d memo slots of `cache_2d` and `flat_cache`, keyed on the point being evaluated."""

    __slots__ = ("x", "z", "slot_x", "slot_z", "slot_value")

    def __init__(self, count):
        self.x = None
        self.z = None
        self.slot_x = [None] * count
        self.slot_z = [None] * count
        self.slot_value = [0.0] * count

    def move_to(self, x, z):
        self.x = x
        self.z = z

    def get(self, slot):
        if self.slot_x[slot] == self.x and self.slot_z[slot] == self.z:
            return self.slot_value[slot]
        return None

    def put(self, slot, value):
        self.slot_x[slot] = self.x
        self.slot_z[slot] = self.z
        self.slot_value[slot] = value


class World:
    """Compiles density functions of one dimension for one seed."""

    def __init__(self, pack, seed, caves, cheese):
        self.pack = pack
        self.seed = seed
        self.caves = caves
        self.cheese = cheese
        self.positional = PositionalRandom(*Xoroshiro.of_seed(seed).fork_positional())
        self.noises = {}
        self.density_functions = {}
        self.cheese_free = {}
        self.slot_count = 0
        # the spline plot pins the chain's own continentalness spline to a value to ask where
        # the surface lands; `spline_nodes` holds it, matched on its points so that a second
        # spline over the same coordinate elsewhere in the chain keeps its own scale
        self.spline_nodes = []
        self.spline_value = None

    # -- noise -------------------------------------------------------------------------

    def noise(self, key):
        """`Noises.instantiate`: one `NormalNoise` per registry key, seeded by the key's hash."""
        if key not in self.noises:
            params = self.pack.get("worldgen/noise", key)
            self.noises[key] = NormalNoise(self.positional.from_hash_of(key), params.get("firstOctave", 0),
                                          params.get("amplitudes", []))
        return self.noises[key]

    def blended_noise(self, node):
        return BlendedNoise(self.positional.from_hash_of("minecraft:terrain"), node["xz_scale"], node["y_scale"],
                            node["xz_factor"], node["y_factor"], node["smear_scale_multiplier"])

    # -- density functions -------------------------------------------------------------

    def compile(self, node, cheese_zeroed=False):
        """Compile one node into `f(x, y, z, column) -> float`.

        `cheese_zeroed` replaces `old_blended_noise` with zero, which is what makes the cheap
        surface chain. Entries without that term share one compiled function, so the 2d memo
        slots are not duplicated between the climate axes and the surface chain.
        """
        if isinstance(node, bool):
            return _constant_of(node)
        if isinstance(node, (int, float)):
            return _constant_of(node)
        if isinstance(node, str):
            zeroed = cheese_zeroed and not self.is_cheese_free(node)
            key = (node, zeroed)
            if key not in self.density_functions:
                self.density_functions[key] = self.compile(self.pack.get("worldgen/density_function", node), zeroed)
            return self.density_functions[key]

        kind = node["type"]
        if not isinstance(kind, str):
            raise Bad("density function node has no type: " + json.dumps(node)[:120])
        kind = kind.split(":", 1)[1] if ":" in kind else kind
        builder = getattr(self, "_" + kind, None)
        if builder is None:
            raise Bad("unsupported density function type: " + kind)
        return builder(node, cheese_zeroed)

    def is_cheese_free(self, key):
        """Whether a registry entry reaches no `old_blended_noise`, following references."""
        if key not in self.cheese_free:
            self.cheese_free[key] = not self._has_cheese(self.pack.get("worldgen/density_function", key), {key})
        return self.cheese_free[key]

    def _has_cheese(self, node, seen):
        if isinstance(node, str):
            if node in seen:
                return False
            seen.add(node)
            return self._has_cheese(self.pack.get("worldgen/density_function", node), seen)
        if not isinstance(node, dict):
            return False
        kind = node.get("type")
        if not isinstance(kind, str):
            return False
        kind = kind.split(":")[-1]
        if kind == "old_blended_noise":
            return True
        if kind not in CHILD_FIELDS:
            raise Bad("no field table for density function type: " + kind)
        for path in CHILD_FIELDS[kind]:
            child = node
            for step in path.split("."):
                if not isinstance(child, dict) or step not in child:
                    child = None
                    break
                child = child[step]
            if self._has_cheese(child, seen):
                return True
        return False

    def _cache_2d(self, node, cheese_zeroed):
        """A function that cannot see y, memoised per column: continents, erosion, the offset spline."""
        slot = self.slot_count
        self.slot_count += 1
        inner = self.compile(node["argument"], cheese_zeroed)

        def cached(x, y, z, column):
            value = column.get(slot)
            if value is None:
                value = inner(x, y, z, column)
                column.put(slot, value)
            return value

        return cached

    def _flat_cache(self, node, cheese_zeroed):
        return self._cache_2d(node, cheese_zeroed)

    def _identity(self, node, cheese_zeroed):
        """The markers that only matter beside another chunk; with an empty blender all are identity."""
        return self.compile(node["argument"], cheese_zeroed)

    def _interpolated(self, node, cheese_zeroed):
        return self._identity(node, cheese_zeroed)

    def _cache_once(self, node, cheese_zeroed):
        return self._identity(node, cheese_zeroed)

    def _cache_all_in_cell(self, node, cheese_zeroed):
        return self._identity(node, cheese_zeroed)

    def _blend_density(self, node, cheese_zeroed):
        return self._identity(node, cheese_zeroed)

    def _beardifier(self, node, cheese_zeroed):
        return _constant_of(0.0)

    # arithmetic, `Ap2` and the constant shortcuts

    def _constant(self, node, cheese_zeroed):
        value = float(node["argument"])
        return lambda x, y, z, column: value

    def _add(self, node, cheese_zeroed):
        first = self.compile(node["argument1"], cheese_zeroed)
        second = self.compile(node["argument2"], cheese_zeroed)

        def compute(x, y, z, column):
            return first(x, y, z, column) + second(x, y, z, column)

        return compute

    def _mul(self, node, cheese_zeroed):
        first = self.compile(node["argument1"], cheese_zeroed)
        second = self.compile(node["argument2"], cheese_zeroed)

        def compute(x, y, z, column):
            value = first(x, y, z, column)
            return 0.0 if value == 0.0 else value * second(x, y, z, column)

        return compute

    def _min(self, node, cheese_zeroed):
        first = self.compile(node["argument1"], cheese_zeroed)
        second = self.compile(node["argument2"], cheese_zeroed)

        def compute(x, y, z, column):
            return min(first(x, y, z, column), second(x, y, z, column))

        return compute

    def _max(self, node, cheese_zeroed):
        first = self.compile(node["argument1"], cheese_zeroed)
        second = self.compile(node["argument2"], cheese_zeroed)

        def compute(x, y, z, column):
            return max(first(x, y, z, column), second(x, y, z, column))

        return compute

    # unary transforms, `DensityFunctions.Mapped`

    def _mapped(self, kind, node, cheese_zeroed):
        inner = self.compile(node["argument"], cheese_zeroed)
        if kind == "abs":
            return lambda x, y, z, column: abs(inner(x, y, z, column))
        if kind == "square":
            return lambda x, y, z, column: inner(x, y, z, column) ** 2
        if kind == "cube":
            return lambda x, y, z, column: inner(x, y, z, column) ** 3
        if kind == "half_negative":
            return lambda x, y, z, column: _half_negative(inner(x, y, z, column))
        if kind == "quarter_negative":
            return lambda x, y, z, column: _quarter_negative(inner(x, y, z, column))
        if kind == "squeeze":
            return lambda x, y, z, column: _squeeze(inner(x, y, z, column))
        raise Bad("unsupported mapped function: " + kind)

    def _abs(self, node, cheese_zeroed):
        return self._mapped("abs", node, cheese_zeroed)

    def _square(self, node, cheese_zeroed):
        return self._mapped("square", node, cheese_zeroed)

    def _cube(self, node, cheese_zeroed):
        return self._mapped("cube", node, cheese_zeroed)

    def _half_negative(self, node, cheese_zeroed):
        return self._mapped("half_negative", node, cheese_zeroed)

    def _quarter_negative(self, node, cheese_zeroed):
        return self._mapped("quarter_negative", node, cheese_zeroed)

    def _squeeze(self, node, cheese_zeroed):
        return self._mapped("squeeze", node, cheese_zeroed)

    def _clamp(self, node, cheese_zeroed):
        inner = self.compile(node["input"], cheese_zeroed)
        low = float(node["min"])
        high = float(node["max"])
        return lambda x, y, z, column: clamp(inner(x, y, z, column), low, high)

    def _blend_alpha(self, node, cheese_zeroed):
        return lambda x, y, z, column: 1.0

    def _blend_offset(self, node, cheese_zeroed):
        return lambda x, y, z, column: 0.0

    def _y_clamped_gradient(self, node, cheese_zeroed):
        from_y = float(node["from_y"])
        to_y = float(node["to_y"])
        from_value = float(node["from_value"])
        to_value = float(node["to_value"])
        span = to_y - from_y

        def compute(x, y, z, column):
            return clamp(from_value + (y - from_y) * (to_value - from_value) / span,
                         min(from_value, to_value), max(from_value, to_value))

        return compute

    # noise sampling

    def _noise(self, node, cheese_zeroed):
        noise = self.noise(node["noise"])
        xz_scale = float(node.get("xz_scale", 1.0))
        y_scale = float(node.get("y_scale", 1.0))

        def compute(x, y, z, column):
            return noise.value(x * xz_scale, y * y_scale, z * xz_scale)

        return compute

    def _shifted_noise(self, node, cheese_zeroed):
        noise = self.noise(node["noise"])
        shift_x = self.compile(node["shift_x"], cheese_zeroed)
        shift_y = self.compile(node["shift_y"], cheese_zeroed)
        shift_z = self.compile(node["shift_z"], cheese_zeroed)
        xz_scale = float(node["xz_scale"])
        y_scale = float(node["y_scale"])

        def compute(x, y, z, column):
            return noise.value(x * xz_scale + shift_x(x, y, z, column),
                               y * y_scale + shift_y(x, y, z, column),
                               z * xz_scale + shift_z(x, y, z, column))

        return compute

    def _shift(self, node, cheese_zeroed):
        noise = self.noise(node["argument"])

        def compute(x, y, z, column):
            return noise.value(x * 0.25, y * 0.25, z * 0.25) * 4.0

        return compute

    def _shift_a(self, node, cheese_zeroed):
        noise = self.noise(node["argument"])

        def compute(x, y, z, column):
            return noise.value(x * 0.25, 0.0, z * 0.25) * 4.0

        return compute

    def _shift_b(self, node, cheese_zeroed):
        noise = self.noise(node["argument"])

        def compute(x, y, z, column):
            return noise.value(z * 0.25, x * 0.25, 0.0) * 4.0

        return compute

    def _old_blended_noise(self, node, cheese_zeroed):
        if cheese_zeroed:
            return lambda x, y, z, column: 0.0
        noise = self.blended_noise(node)

        def compute(x, y, z, column):
            return noise.value(x, y, z)

        return compute

    def _weird_scaled_sampler(self, node, cheese_zeroed):
        inner = self.compile(node["input"], cheese_zeroed)
        noise = self.noise(node["noise"])
        mapper = _rarity_3d if node["rarity_value_mapper"] == "type_1" else _rarity_2d

        def compute(x, y, z, column):
            rarity = mapper(inner(x, y, z, column))
            return rarity * abs(noise.value(x / rarity, y / rarity, z / rarity))

        return compute

    # control flow and splines

    def _range_choice(self, node, cheese_zeroed):
        inner = self.compile(node["input"], cheese_zeroed)
        when_in = self.compile(node["when_in_range"], cheese_zeroed)
        when_out = self.compile(node["when_out_of_range"], cheese_zeroed)
        low = float(node["min_inclusive"])
        high = float(node["max_exclusive"])

        def compute(x, y, z, column):
            value = inner(x, y, z, column)
            branch = when_in if low <= value < high else when_out
            return branch(x, y, z, column)

        return compute

    def _spline(self, node, cheese_zeroed):
        if self.spline_value is not None and any(node["spline"] == held for held in self.spline_nodes):
            return _constant_of(self.spline_value)
        coordinate = self.compile(node["spline"]["coordinate"], cheese_zeroed)
        points = [(p["location"], p["value"], p.get("derivative", 0.0)) for p in node["spline"]["points"]]
        locations = [p[0] for p in points]
        count = len(points)
        return _spline_of(coordinate, points, locations, count)

    def reset_cache(self):
        """Drop the compiled density functions, which is how the spline override takes effect."""
        self.density_functions.clear()

    # -- the chains vanilla builds in Java ----------------------------------------------

    def _post_process(self, inner):
        """`NoiseRouterData.postProcess`: interpolated density, squeezed and scaled."""
        scaled = (lambda x, y, z, column: inner(x, y, z, column) * 0.64)
        return lambda x, y, z, column: _squeeze(scaled(x, y, z, column))

    def _slide(self, inner, min_y, height, top_blend, top_target, top_out, bottom_blend, bottom_target, bottom_out):
        """`NoiseRouterData.slide`, the y fade that keeps terrain out of the world's ends.

        The blend points are offsets from the world's top and bottom; over the overworld the
        top fades between y 240 and 256, the bottom between y -64 and -40. `height` is the
        world height, not the top y.
        """
        top = _y_gradient(min_y + height - top_blend, min_y + height - top_target, 1.0, 0.0)
        bottom = _y_gradient(min_y + bottom_blend, min_y + bottom_target, 0.0, 1.0)

        def compute(x, y, z, column):
            inside = _lerp(top(x, y, z, column), top_out, inner(x, y, z, column))
            return _lerp(bottom(x, y, z, column), bottom_out, inside)

        return compute

    def surface_density(self, router, min_y, height):
        """The terrain term of `final_density`, with the cave branches cut."""
        if self.caves:
            return self.compile(router["final_density"])
        key = router["depth"] if isinstance(router["depth"], str) else None
        if key is None:
            raise Bad("cannot prune the cave branches: the router's depth is not a registry reference")
        # the router names its depth entry `<prefix>/depth`, and the rest of the terrain sits
        # under the same prefix, so `<prefix>/sloped_cheese` is the term that places the surface
        namespace, _, path = key.partition(":")
        prefix = path.rsplit("/", 1)[0] if "/" in path else ""
        sloped_cheese = namespace + ":" + (prefix + "/" if prefix else "") + "sloped_cheese"
        return self._post_process(self._slide(self.compile(sloped_cheese, not self.cheese),
                                              min_y, height, 80, 64, -0.078125, 0, 24, 0.1171875))


def _constant_of(value):
    return lambda x, y, z, column: float(value)


def _lerp(delta, start, end):
    return start + delta * (end - start)


def _half_negative(value):
    return value if value > 0.0 else value * 0.5


def _quarter_negative(value):
    return value if value > 0.0 else value * 0.25


def _squeeze(value):
    value = clamp(value, -1.0, 1.0)
    return value / 2.0 - value * value * value / 24.0


def _y_gradient(from_y, to_y, from_value, to_value):
    span = to_y - from_y

    def compute(x, y, z, column):
        return clamp(from_value + (y - from_y) * (to_value - from_value) / span,
                     min(from_value, to_value), max(from_value, to_value))

    return compute


def _rarity_3d(value):
    """`NoiseRouterData.QuantizedSpaghettiRarity.getSpaghettiRarity3D`."""
    if value < -0.5:
        return 0.75
    if value < 0.0:
        return 1.0
    return 1.5 if value < 0.5 else 2.0


def _rarity_2d(value):
    """`NoiseRouterData.QuantizedSpaghettiRarity.getSphaghettiRarity2D`."""
    if value < -0.75:
        return 0.5
    if value < -0.5:
        return 0.75
    if value < 0.5:
        return 1.0
    return 2.0 if value < 0.75 else 3.0


def _spline_of(coordinate, points, locations, count):
    """`CubicSpline.Multipoint`: hermite between the bracketing points, linear outside."""

    def compute(x, y, z, column):
        value = coordinate(x, y, z, column)
        index = bisect.bisect_right(locations, value) - 1
        if index < 0:
            derivative = points[0][2]
            return points[0][1] if derivative == 0.0 else points[0][1] + derivative * (value - locations[0])
        if index == count - 1:
            derivative = points[index][2]
            return points[index][1] if derivative == 0.0 else points[index][1] + derivative * (value - locations[index])
        here = points[index]
        there = points[index + 1]
        span = locations[index + 1] - locations[index]
        delta = (value - locations[index]) / span
        low = here[1]
        high = there[1]
        rise = (here[2] + there[2]) * span - (high - low)
        fall = (-here[2] + there[2]) * span - (high - low)
        return _lerp(delta, low, high) + delta * (1.0 - delta) * _lerp(delta, rise, fall)

    return compute


# --------------------------------------------------------------------------------------
# biome source
# --------------------------------------------------------------------------------------

AXES = ("temperature", "humidity", "continentalness", "erosion", "depth", "weirdness")


def quantize(value):
    return int(value * 10000.0)


class BiomeSource:
    """Nearest-cell lookup over the `multi_noise` grid, as `Climate.ParameterList` does."""

    def __init__(self, source):
        kind = source.get("type", "minecraft:multi_noise")
        if kind == "minecraft:fixed":
            self.fixed = source["biome"]
            self.cells = []
            return
        if kind != "minecraft:multi_noise":
            raise Bad("unsupported biome source type: " + kind)
        self.fixed = None
        self.cells = []
        for entry in source["biomes"]:
            parameters = entry["parameters"]
            cell = [entry["biome"], quantize(parameters["offset"])]
            for axis in AXES:
                span = parameters[axis]
                low = quantize(span[0] if isinstance(span, list) else span)
                high = quantize(span[1] if isinstance(span, list) else span)
                cell.append((low, high))
            self.cells.append(cell)

    def biome_at(self, point):
        """`point` is the six quantised climate values, in `AXES` order."""
        if self.fixed is not None:
            return self.fixed
        best_biome = None
        best_fitness = None
        for cell in self.cells:
            fitness = cell[1] * cell[1]
            for i in range(len(AXES)):
                low, high = cell[2 + i]
                value = point[i]
                above = value - high
                below = low - value
                distance = above if above > 0 else (below if below > 0 else 0)
                fitness += distance * distance
            if best_fitness is None or fitness < best_fitness:
                best_fitness = fitness
                best_biome = cell[0]
        return best_biome


# --------------------------------------------------------------------------------------
# colors
# --------------------------------------------------------------------------------------

# Gensokyo's own palette, consulted before the vanilla table below. Four families, so a biome
# map reads as terrain rather than as eighteen unrelated hues: water blue, the mountain and
# everything that sits on it dark red, the summit white, and the two open-ground bands in
# greens and yellows. The four underground biomes and the mountain's rock are neither ground
# nor surface and never show on a surface map, so they get their own near-black ramp.
GENSAKYO_COLORS = {
    # water
    "misty_lake": (34, 78, 158), "misty_lake_shore": (120, 170, 214),
    # lower ground: plains first, so the yellows read as the sunny end of the plain
    "sunflower_plains": (222, 224, 118), "muenzuka": (170, 186, 96),
    "human_village": (198, 198, 74), "myouren_temple_barrens": (196, 168, 84),
    # upper ground: the same greens pushed darker, which is the only hint the map gives of height
    "magical_forest": (74, 148, 74), "bamboo_forest_of_the_lost": (96, 172, 92),
    "sakura_forest": (140, 190, 110), "hill_of_the_nameless": (122, 158, 68),
    # the mountain and the settlement on it
    "untrodden_valley": (140, 62, 48), "genbu_ravine": (120, 48, 40),
    "youkai_mountain_foothills": (158, 74, 56), "sanctuary": (104, 40, 36),
    "tengu_settlement": (176, 88, 66), "secret_heaven_cliff": (92, 34, 30),
    "youkai_mountain_summit": (245, 245, 248),
    # below the ground
    "mountain_interior": (72, 28, 26), "former_hell": (78, 48, 48),
    "hell_of_blood_pools": (104, 44, 40), "hell_of_blazing_fires": (92, 56, 34),
    "animal_realm": (56, 60, 50),
}

# biome colors as Minecraft's own map renders them, from misode's deepslate palette
BIOME_COLORS = {
    "badlands": (217, 69, 21), "badlands_plateau": (202, 140, 101), "bamboo_jungle": (118, 142, 20),
    "bamboo_jungle_hills": (59, 71, 10), "basalt_deltas": (64, 54, 54), "beach": (250, 222, 85),
    "birch_forest": (48, 116, 68), "birch_forest_hills": (31, 95, 50), "cold_ocean": (32, 32, 112),
    "crimson_forest": (221, 8, 8), "dark_forest": (64, 81, 26), "dark_forest_hills": (104, 121, 66),
    "deep_cold_ocean": (32, 32, 56), "deep_frozen_ocean": (64, 64, 144), "deep_lukewarm_ocean": (0, 0, 64),
    "deep_ocean": (0, 0, 48), "deep_warm_ocean": (0, 0, 80), "desert": (250, 148, 24),
    "desert_hills": (210, 95, 18), "desert_lakes": (255, 188, 64), "end_barrens": (39, 30, 61),
    "end_highlands": (232, 244, 178), "end_midlands": (194, 187, 136), "eroded_badlands": (255, 109, 61),
    "flower_forest": (45, 142, 73), "forest": (5, 102, 33), "frozen_ocean": (112, 112, 214),
    "frozen_river": (160, 160, 255), "giant_spruce_taiga": (129, 142, 121),
    "giant_spruce_taiga_hills": (109, 119, 102), "giant_tree_taiga": (89, 102, 81),
    "giant_tree_taiga_hills": (69, 79, 62), "gravelly_hills": (136, 136, 136),
    "gravelly_mountains": (136, 136, 136), "ice_spikes": (180, 220, 220), "jungle": (83, 123, 9),
    "jungle_edge": (98, 139, 23), "sparse_jungle": (98, 139, 23), "jungle_hills": (44, 66, 5),
    "lukewarm_ocean": (0, 0, 144), "meadow": (169, 197, 80), "mangrove_swamp": (36, 196, 142),
    "modified_badlands_plateau": (242, 180, 141), "modified_gravelly_mountains": (120, 152, 120),
    "modified_jungle": (123, 163, 49), "modified_jungle_edge": (138, 179, 63),
    "modified_wooded_badlands_plateau": (216, 191, 141), "mountain_edge": (114, 120, 154),
    "extreme_hills": (96, 96, 96), "mountains": (96, 96, 96), "windswept_hills": (96, 96, 96),
    "mushroom_field_shore": (160, 0, 255), "mushroom_fields": (255, 0, 255), "nether_wastes": (191, 59, 59),
    "ocean": (0, 0, 112), "plains": (141, 179, 96), "river": (0, 0, 255),
    "savanna": (189, 178, 95), "savanna_plateau": (167, 157, 100),
    "shattered_savanna": (229, 218, 135), "windswept_savanna": (229, 218, 135),
    "shattered_savanna_plateau": (207, 197, 140), "small_end_islands": (16, 12, 28),
    "snowy_beach": (250, 240, 192), "snowy_mountains": (160, 160, 160), "snowy_taiga": (49, 85, 74),
    "snowy_taiga_hills": (36, 63, 54), "snowy_taiga_mountains": (89, 125, 114),
    "snowy_tundra": (255, 255, 255), "snowy_plains": (255, 255, 255), "soul_sand_valley": (94, 56, 48),
    "stone_shore": (162, 162, 132), "stony_shore": (162, 162, 132), "sunflower_plains": (181, 219, 136),
    "swamp": (7, 249, 178), "swamp_hills": (47, 255, 218), "taiga": (11, 102, 89),
    "taiga_hills": (22, 57, 51), "taiga_mountains": (51, 142, 129), "tall_birch_forest": (88, 156, 108),
    "tall_birch_hills": (71, 135, 90), "the_end": (59, 39, 84), "the_void": (0, 0, 0),
    "warm_ocean": (0, 0, 172), "warped_forest": (73, 144, 123), "wooded_badlands_plateau": (176, 151, 101),
    "wooded_badlands": (176, 151, 101), "wooded_hills": (34, 85, 28), "wooded_mountains": (80, 112, 80),
    "windswept_forest": (80, 112, 80), "snowy_slopes": (140, 195, 222), "lofty_peaks": (196, 168, 193),
    "jagged_peaks": (196, 168, 193), "snowcapped_peaks": (200, 198, 200), "frozen_peaks": (200, 198, 200),
    "stony_peaks": (82, 92, 103), "grove": (150, 150, 189), "lush_caves": (112, 255, 79),
    "dripstone_caves": (140, 124, 0), "deep_dark": (10, 14, 19), "badlands_pillar": (217, 69, 21),
}

# one color per 16 blocks of height, from the shoreline up into the peaks
HEIGHT_BANDS = ((106, 168, 79), (154, 205, 50), (205, 203, 80), (222, 184, 72), (196, 155, 80),
                (170, 130, 86), (150, 110, 90), (135, 100, 95), (150, 130, 130), (170, 160, 160),
                (200, 195, 195), (225, 225, 225), (245, 245, 250))
HEIGHT_STEP = 16

# the cave report's sampling, coarse in y because a cave is a volume and not a pixel
VOID_SPACING = 24
VOID_STEP = 4
VOID_COLUMNS = 26
VOID_BAND = 32
VOID_DEEP = 40
# the legend's bands run between these; below the low end and from the high end up are one bin each
HEIGHT_BIN_LOW = 64
HEIGHT_BIN_HIGH = 208
WATER_TOP = (86, 156, 224)
WATER_DEEP = (0, 16, 80)


# the climate grid's own cuts, one per axis, and the eight regions they tile the parameter
# space into. Gensokyo splits temperature, vegetation and erosion once each at 0, which is what
# gives the plain its eight cells (`doc/design/gensokyo_dimension.md` §4). Hardcoded the way
# `cave_bands.py` hardcodes its bounds, so retuning the grid means retuning these.
CLIMATE_CUTS = (0.0, 0.0, 0.0)

# ordered 4 * cold + 2 * dry + 1 * low erosion, so the index is the three flags read out
PARAMETER_REGIONS = (
    "hot humid high erosion", "hot humid low erosion",
    "hot dry high erosion", "hot dry low erosion",
    "cold humid high erosion", "cold humid low erosion",
    "cold dry high erosion", "cold dry low erosion",
)

# hue from temperature, greenness from vegetation, and a lightness step from erosion, so the
# erosion half is the lighter member of each pair
PARAMETER_COLORS = (
    (166, 128, 72), (140, 156, 68),
    (214, 172, 108), (190, 200, 116),
    (66, 92, 140), (104, 136, 178),
    (54, 128, 118), (108, 178, 152),
)

# a column with no surface has no climate point to classify, since the sampler needs a y
PARAMETER_NONE = (24, 24, 28)


def parameter_region(point):
    """Which of the eight the three cuts tile, for one `climate_point`."""
    return (4 if point[0] < CLIMATE_CUTS[0] else 0) + (2 if point[1] < CLIMATE_CUTS[1] else 0) \
        + (1 if point[3] < CLIMATE_CUTS[2] else 0)


def biome_color(key):
    color = GENSAKYO_COLORS.get(key.split(":")[-1]) or BIOME_COLORS.get(key.split(":")[-1])
    if color is not None:
        return color
    # unknown biomes get a stable color of their own, hashed from the name like deepslate does
    digest = hashlib.md5(key.encode("utf-8")).digest()
    return digest[0], digest[1], digest[2]


def height_color(y, sea_level, min_y):
    if y < sea_level:
        depth = (sea_level - y) / max(1, sea_level - min_y)
        return tuple(int(round(a + (b - a) * depth)) for a, b in zip(WATER_TOP, WATER_DEEP))
    return HEIGHT_BANDS[min((y - sea_level) // HEIGHT_STEP, len(HEIGHT_BANDS) - 1)]


# --------------------------------------------------------------------------------------
# rendering
# --------------------------------------------------------------------------------------


def surface_height(density, min_y, max_y, x, z, column):
    """Highest y with a positive density, which is the top solid block: the surface.

    Scans down from the top of the world in steps of 16 and then walks the bracket upward, so
    the answer is exact rather than quantised. The 2d memo of `column` stays valid across the
    probes, so the climate fields and the offset spline are sampled once per column however
    many y values the scan takes.
    """
    bracket = None
    y = max_y - 1
    while y >= min_y:
        if density(x, y, z, column) > 0.0:
            bracket = y
            break
        y -= HEIGHT_STEP
    if bracket is None:
        return None
    for probe in range(bracket + 1, min(max_y, bracket + HEIGHT_STEP)):
        if density(x, probe, z, column) <= 0.0:
            return probe - 1
    # the probe above the bracket was already empty, so a window of solid density means the
    # surface is its top block; returning the bracket reports it 15 blocks low, which is what
    # fabricated the flat shelves the height map used to show along the plain
    return min(max_y - 1, bracket + HEIGHT_STEP - 1)


def climate_point(sampler, x, y, z, column):
    """`Climate.Sampler.sample`, quantised like `Climate.target`."""
    return tuple(quantize(to_float(function(x, y, z, column))) for function in sampler)


class Dimension:
    """Everything one row of pixels needs."""

    def __init__(self, pack, dimension, seed, caves, cheese):
        self.world = World(pack, seed, caves, cheese)
        generator = dimension["generator"]
        if generator.get("type") != "minecraft:noise":
            raise Bad("dimension " + str(dimension.get("type")) + " is not a noise dimension")
        settings_key = generator["settings"]
        if not isinstance(settings_key, str):
            raise Bad("inline noise settings are not supported")
        self.settings = pack.get("worldgen/noise_settings", settings_key)
        if self.settings.get("legacy_random_source"):
            raise Bad("legacy_random_source worlds are not supported")
        router = self.settings.get("noise_router")
        if not router:
            raise Bad("noise settings " + settings_key + " carry no noise router")
        self.router = router
        self.min_y = self.settings["noise"]["min_y"]
        self.max_y = self.min_y + self.settings["noise"]["height"]
        self.sea_level = self.settings["sea_level"]
        self.density = self.world.surface_density(router, self.min_y, self.settings["noise"]["height"])
        self.sampler = [self.world.compile(router[axis]) for axis in
                        ("temperature", "vegetation", "continents", "erosion", "depth", "ridges")]
        # the spline plot needs the climate field on its own, and the key to recognise the
        # splines keyed on it; an inline router entry has no key and so cannot be pinned
        self.continents_key = router["continents"] if isinstance(router["continents"], str) else None
        self.continents = self.sampler[2]
        self.biomes = BiomeSource(generator["biome_source"])
        self.column = Column(self.world.slot_count)


def render_row(dimension, chunk_x, chunk_z, size):
    """One scanline of each map, plus the per row counts the legend needs."""
    biomes = bytearray()
    heights = bytearray()
    parameters = bytearray()
    counts = Counter()
    total = 0
    for pixel in range(size):
        x = (chunk_x + pixel) * 16 + 8
        z = chunk_z * 16 + 8
        column = dimension.column
        column.move_to(x, z)
        height = surface_height(dimension.density, dimension.min_y, dimension.max_y, x, z, column)
        if height is None:
            counts["_no_surface"] += 1
            total += 1
            biomes += bytes(biome_color("minecraft:the_void"))
            heights += bytes(height_color(dimension.min_y, dimension.sea_level, dimension.min_y))
            parameters += bytes(PARAMETER_NONE)
            continue
        # biomes are picked per 4x4x4 cell at the cell centre, so that is the point sampled
        cell_x = ((x >> 2) << 2) + 2
        cell_z = ((z >> 2) << 2) + 2
        cell_y = ((height >> 2) << 2) + 2
        column.move_to(cell_x, cell_z)
        point = climate_point(dimension.sampler, cell_x, cell_y, cell_z, column)
        biome = dimension.biomes.biome_at(point)
        region = parameter_region(point)
        counts[biome] += 1
        counts["_param:" + PARAMETER_REGIONS[region]] += 1
        counts["_height:" + str(height)] += 1
        total += 1
        biomes += bytes(biome_color(biome))
        heights += bytes(height_color(height, dimension.sea_level, dimension.min_y))
        parameters += bytes(PARAMETER_COLORS[region])
    return bytes(biomes), bytes(heights), bytes(parameters), counts, total


# --------------------------------------------------------------------------------------
# png
# --------------------------------------------------------------------------------------


def write_png(path, width, height, rows):
    raw = b"".join(b"\x00" + row for row in rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & MASK32)

    header = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    with open(path, "wb") as handle:
        handle.write(b"\x89PNG\r\n\x1a\n")
        handle.write(chunk(b"IHDR", header))
        handle.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        handle.write(chunk(b"IEND", b""))


# --------------------------------------------------------------------------------------
# plots
# --------------------------------------------------------------------------------------

# A 5x7 cell font: one glyph is seven rows of five columns, written left to right so the
# shapes can be read off the source. Uppercase only, which is what axis labels want.
FONT = {
    " ": "....." "....." "....." "....." "....." "....." ".....",
    "!": "..#.." "..#.." "..#.." "..#.." "..#.." "....." "..#..",
    "%": "##..#" "##.#." "...#." "..#.." ".#..." ".#.##" "#..##",
    "(": "...#." "..#.." ".#..." ".#..." ".#..." "..#.." "...#.",
    ")": ".#..." "..#.." "...#." "...#." "...#." "..#.." ".#...",
    "+": "....." "..#.." "..#.." "#####" "..#.." "..#.." ".....",
    ",": "....." "....." "....." "....." "..#.." "..#.." ".#...",
    "-": "....." "....." "....." "#####" "....." "....." ".....",
    ".": "....." "....." "....." "....." "....." "..#.." "..#..",
    "/": "....#" "...#." "...#." "..#.." ".#..." ".#..." "#....",
    "0": ".###." "#...#" "#..##" "#.#.#" "##..#" "#...#" ".###.",
    "1": "..#.." ".##.." "..#.." "..#.." "..#.." "..#.." ".###.",
    "2": ".###." "#...#" "....#" "...#." "..#.." ".#..." "#####",
    "3": "#####" "...#." "..#.." "...#." "....#" "#...#" ".###.",
    "4": "...#." "..##." ".#.#." "#..#." "#####" "...#." "...#.",
    "5": "#####" "#...." "####." "....#" "....#" "#...#" ".###.",
    "6": "..##." ".#..." "#...." "####." "#...#" "#...#" ".###.",
    "7": "#####" "....#" "...#." "..#.." ".#..." ".#..." ".#...",
    "8": ".###." "#...#" "#...#" ".###." "#...#" "#...#" ".###.",
    "9": ".###." "#...#" "#...#" ".####" "....#" "...#." ".##..",
    ":": "....." "..#.." "..#.." "....." "..#.." "..#.." ".....",
    "<": "...#." "..#.." ".#..." "#...." ".#..." "..#.." "...#.",
    "=": "....." "....." "#####" "....." "#####" "....." ".....",
    ">": "#...." ".#..." "..#.." "...#." "..#.." ".#..." "#....",
    "?": ".###." "#...#" "....#" "...#." "..#.." "....." "..#..",
    "A": ".###." "#...#" "#...#" "#####" "#...#" "#...#" "#...#",
    "B": "####." "#...#" "#...#" "####." "#...#" "#...#" "####.",
    "C": ".###." "#...#" "#...." "#...." "#...." "#...#" ".###.",
    "D": "####." "#...#" "#...#" "#...#" "#...#" "#...#" "####.",
    "E": "#####" "#...." "#...." "####." "#...." "#...." "#####",
    "F": "#####" "#...." "#...." "####." "#...." "#...." "#....",
    "G": ".###." "#...#" "#...." "#.###" "#...#" "#...#" ".###.",
    "H": "#...#" "#...#" "#...#" "#####" "#...#" "#...#" "#...#",
    "I": ".###." "..#.." "..#.." "..#.." "..#.." "..#.." ".###.",
    "J": "..###" "...#." "...#." "...#." "...#." "#..#." ".##..",
    "K": "#...#" "#..#." "#.#.." "##..." "#.#.." "#..#." "#...#",
    "L": "#...." "#...." "#...." "#...." "#...." "#...." "#####",
    "M": "#...#" "##.##" "#.#.#" "#.#.#" "#...#" "#...#" "#...#",
    "N": "#...#" "##..#" "#.#.#" "#.#.#" "#..##" "#...#" "#...#",
    "O": ".###." "#...#" "#...#" "#...#" "#...#" "#...#" ".###.",
    "P": "####." "#...#" "#...#" "####." "#...." "#...." "#....",
    "Q": ".###." "#...#" "#...#" "#...#" "#.#.#" "#..#." ".##.#",
    "R": "####." "#...#" "#...#" "####." "#.#.." "#..#." "#...#",
    "S": ".####" "#...." "#...." ".###." "....#" "....#" "####.",
    "T": "#####" "..#.." "..#.." "..#.." "..#.." "..#.." "..#..",
    "U": "#...#" "#...#" "#...#" "#...#" "#...#" "#...#" ".###.",
    "V": "#...#" "#...#" "#...#" "#...#" "#...#" ".#.#." "..#..",
    "W": "#...#" "#...#" "#...#" "#.#.#" "#.#.#" "##.##" "#...#",
    "X": "#...#" "#...#" ".#.#." "..#.." ".#.#." "#...#" "#...#",
    "Y": "#...#" "#...#" ".#.#." "..#.." "..#.." "..#.." "..#..",
    "Z": "#####" "....#" "...#." "..#.." ".#..." "#...." "#####",
}
FONT_WIDTH = 5
FONT_HEIGHT = 7
FONT_ADVANCE = FONT_WIDTH + 1


class Canvas:
    """An RGB raster with the primitives the plots need.

    Coordinates are pixels with y down, and every primitive clips, so a curve can be drawn
    across a whole data range and left to run off its panel instead of bounded per point.
    """

    def __init__(self, width, height, background=(255, 255, 255)):
        self.width = width
        self.height = height
        self.pixels = bytearray(bytes(background) * (width * height))

    def point(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            offset = (y * self.width + x) * 3
            self.pixels[offset:offset + 3] = bytes(color)

    def rect(self, x, y, width, height, color):
        """A filled rectangle; the slice is exactly the visible span, so the buffer cannot grow."""
        row = bytes(color) * max(0, min(x + width, self.width) - max(x, 0))
        for line in range(max(y, 0), min(y + height, self.height)):
            start = (line * self.width + max(x, 0)) * 3
            self.pixels[start:start + len(row)] = row

    def frame(self, x, y, width, height, color):
        self.rect(x, y, width, 1, color)
        self.rect(x, y + height - 1, width, 1, color)
        self.rect(x, y, 1, height, color)
        self.rect(x + width - 1, y, 1, height, color)

    def line(self, x0, y0, x1, y1, color, width=1):
        """Bresenham with a square brush, so `width` grows the line evenly in both axes."""
        dx = abs(x1 - x0)
        dy = -abs(y1 - y0)
        step_x = 1 if x0 < x1 else -1
        step_y = 1 if y0 < y1 else -1
        error = dx + dy
        low = -(width - 1) // 2
        high = width - 1 - low
        while True:
            for oy in range(low, high + 1):
                for ox in range(low, high + 1):
                    self.point(x0 + ox, y0 + oy, color)
            if x0 == x1 and y0 == y1:
                return
            doubled = 2 * error
            if doubled >= dy:
                error += dy
                x0 += step_x
            if doubled <= dx:
                error += dx
                y0 += step_y

    def polyline(self, points, color, width=1):
        for (x0, y0), (x1, y1) in zip(points, points[1:]):
            self.line(x0, y0, x1, y1, color, width)

    def text(self, x, y, string, color, scale=1):
        for step, character in enumerate(string.upper()):
            glyph = FONT.get(character, FONT["?"])
            for row in range(FONT_HEIGHT):
                bits = glyph[row * FONT_WIDTH:(row + 1) * FONT_WIDTH]
                for column in range(FONT_WIDTH):
                    if bits[column] != ".":
                        self.rect(x + (step * FONT_ADVANCE + column) * scale,
                                  y + row * scale, scale, scale, color)

    def text_width(self, string, scale=1):
        return (len(string) * FONT_ADVANCE - 1) * scale

    def rows(self):
        stride = self.width * 3
        return [bytes(self.pixels[y * stride:(y + 1) * stride]) for y in range(self.height)]

    def write(self, path):
        write_png(path, self.width, self.height, self.rows())


class Panel:
    """One framed box with the mapping from a data rectangle onto its pixels, y up.

    A mapping, not a clip: a value outside the range still has a pixel, so a caller that
    plots past its own range draws over its neighbour rather than losing the value.
    """

    def __init__(self, canvas, box, x_range, y_range, title, right_range=None):
        self.canvas = canvas
        self.x, self.y, self.width, self.height = box
        self.x_range = x_range
        self.y_range = y_range
        self.right_range = right_range
        canvas.frame(self.x, self.y, self.width, self.height, INK)
        canvas.text(self.x, self.y - 22, title, INK, 2)

    def px(self, value):
        low, high = self.x_range
        return int(round(self.x + (value - low) / (high - low) * self.width))

    def py(self, value):
        low, high = self.y_range
        return int(round(self.y + self.height - (value - low) / (high - low) * self.height))

    def point(self, x_value, y_value):
        return self.px(x_value), self.py(y_value)

    def grid(self, x_ticks, y_ticks, x_labels=None, y_labels=None):
        """Faint rules with their values, left aligned on the y axis and centred on the x axis."""
        for step, value in enumerate(x_ticks):
            x = self.px(value)
            self.canvas.line(x, self.y, x, self.y + self.height - 1, GRID)
            if x_labels is not None and x_labels[step]:
                label = x_labels[step]
                self.canvas.text(x - self.canvas.text_width(label) // 2, self.y + self.height + 6,
                                 label, INK)
        for step, value in enumerate(y_ticks):
            y = self.py(value)
            self.canvas.line(self.x, y, self.x + self.width - 1, y, GRID)
            if y_labels is not None:
                self.canvas.text(self.x - 6 - self.canvas.text_width(y_labels[step]), y - 3, y_labels[step], INK)

    def right_axis(self, values, labels):
        """A second y axis on the right, for a series scaled against `right_range`.

        The mapping is its own rather than the left one, so the two axes say different things
        about the same box and neither has to be read as the other.
        """
        for value, label in zip(values, labels):
            low, high = self.right_range
            y = int(round(self.y + self.height - (value - low) / (high - low) * self.height))
            self.canvas.text(self.x + self.width + 6, y - 3, label, INK)

    def scale_to_height(self, value):
        """Map a value of the right hand range onto the panel's pixels."""
        low, high = self.right_range
        return self.y + self.height - (value - low) / (high - low) * self.height

    def axis_label(self, text):
        self.canvas.text(self.x + self.width // 2 - self.canvas.text_width(text) // 2,
                         self.y + self.height + 20, text, INK)


INK = (28, 30, 34)
GRID = (223, 226, 231)
CURVE = (46, 116, 196)
CURVE_HI = (198, 62, 62)
DENSITY = (196, 214, 240)
BAR = (216, 150, 66)
SEA = (58, 126, 196)
# the continentalness histogram bins in panel 1, wide enough to be read and narrow enough to
# show the peak the sample sits on
DENSITY_WIDTH = 0.02


# --------------------------------------------------------------------------------------
# the spline plot
# --------------------------------------------------------------------------------------


def find_splines(pack, node, coordinate, seen, found):
    """Every `spline` node keyed on `coordinate`, as the json of each, following references."""
    if isinstance(node, str):
        if node in seen:
            return
        seen.add(node)
        find_splines(pack, pack.get("worldgen/density_function", node), coordinate, seen, found)
        return
    if isinstance(node, list):
        for child in node:
            find_splines(pack, child, coordinate, seen, found)
        return
    if not isinstance(node, dict):
        return
    kind = str(node.get("type", "")).split(":")[-1]
    if kind == "spline":
        if node["spline"]["coordinate"] == coordinate:
            found.append(node["spline"])
        return
    for path in CHILD_FIELDS.get(kind, ()):
        child = node
        for step in path.split("."):
            if isinstance(child, dict) and step in child:
                child = child[step]
            else:
                child = None
                break
        find_splines(pack, child, coordinate, seen, found)


def spline_points(spline):
    """The point list of a `spline` node: `(location, value, derivative)` triples."""
    return [(p["location"], p["value"], p.get("derivative", 0.0)) for p in spline["points"]]


def spline_lookup(points):
    """`f(continentalness) -> spline value`, off the same interpolation the compiled chain uses."""

    def coordinate(value, y, z, column):
        return value

    evaluate = _spline_of(coordinate, points, [p[0] for p in points], len(points))
    return lambda value: evaluate(value, 0.0, 0.0, None)


class SplineProfile:
    """The surface height as a step function of the spline value, read off the game's chain.

    The chain's own spline is pinned to a value and the surface scanned the way the height map
    scans it, so these are the heights the game builds rather than a formula of the y gradient
    written out here, and a rescaled router moves them by itself.

    The height rises with the value, so what is worth solving for is the value at which each
    height *begins*. Bisecting for those makes a lookup a table index, which matters because a
    breakpoint sits exactly on the integer the game reports: sampling a curve instead would put
    it a probe away, and near a breakpoint the curve is at its flattest.
    """

    # halvings of a range of about 1.5, which resolves a threshold to ~1e-10 and so well below
    # the float noise of the chain that reads it
    STEPS = 34

    def __init__(self, dimension, points):
        self.dimension = dimension
        values = [point[1] for point in points]
        pad = 0.02 * (max(values) - min(values))
        self.low = min(values) - pad
        self.high = max(values) + pad
        lowest = self.surface(self.low)
        self.heights = list(range(lowest, self.surface(self.high) + 1))
        self.edges = [self._edge(y) for y in self.heights]

    def surface(self, value):
        """The surface y for one spline value, from a full scan of the chain."""
        world = self.dimension.world
        world.spline_value = value
        world.reset_cache()
        density = world.surface_density(self.dimension.router, self.dimension.min_y,
                                        self.dimension.settings["noise"]["height"])
        column = Column(world.slot_count)
        column.move_to(0, 0)
        height = surface_height(density, self.dimension.min_y, self.dimension.max_y, 0, 0, column)
        world.spline_value = None
        world.reset_cache()
        return self.dimension.min_y if height is None else height

    def _edge(self, height):
        """The lowest spline value whose surface reaches `height`."""
        low, high = self.low, self.high
        for _ in range(self.STEPS):
            middle = (low + high) * 0.5
            if self.surface(middle) >= height:
                high = middle
            else:
                low = middle
        return high

    def height(self, value):
        index = bisect.bisect_right(self.edges, value) - 1
        return self.heights[clamp(index, 0, len(self.heights) - 1)]


def sample_continents(dimension, chunk_x, chunk_z, size):
    """The `continents` value at every chunk centre of one scanline.

    Takes the chunk coordinates directly, like `render_row`, so the same `work` list drives
    both and neither has to know who adds the row. The field is memoised per column, so the y
    it is sampled at cannot change it; the sea level is passed rather than zero because that
    is the y the game samples the climate at, and a router whose continentalness is not 2d
    would notice.
    """
    function = dimension.continents
    column = dimension.column
    values = []
    for pixel in range(size):
        x = (chunk_x + pixel) * 16 + 8
        z = chunk_z * 16 + 8
        column.move_to(x, z)
        values.append(function(x, dimension.sea_level, z, column))
    return values


def _worker_sample(job):
    row, chunk_x, chunk_z, size = job
    return row, sample_continents(_WORKER["dimension"], chunk_x, chunk_z, size)


class SplineModel:
    """One spline, the sample of the field it reads, and what the two say about the map.

    Everything the plot and the table need, so neither reaches for a global or for the other.
    `shares` is left to `height_shares`, which has to walk the whole sample.
    """

    def __init__(self, dimension, points, continents):
        self.dimension = dimension
        self.points = points
        self.continents = continents
        self.total = len(continents)
        self.lookup = spline_lookup(points)
        self.profile = SplineProfile(dimension, points)
        self.shares = {}

    def surface(self, continentalness):
        """The surface y the spline puts at a continentalness."""
        return self.profile.height(self.lookup(continentalness))

    def share(self, continentalness):
        """The share of the sample, in percent, at or below a continentalness."""
        return 100.0 * bisect.bisect_left(self.continents, continentalness) / self.total


def height_shares(model):
    """`{surface y: share of the sample in percent}`, from the sampled field and the profile.

    The sample says how much of the map sits at each continentalness and the profile says
    what height that buys, so the height histogram falls out of the two without scanning a
    column per height.
    """
    counts = Counter()
    for value in model.continents:
        counts[model.surface(value)] += 1
    return {y: 100.0 * count / model.total for y, count in counts.items()}


def cumulative(shares):
    """The same shares accumulated by height: `(share at or below y, y)`, ascending in y."""
    running = 0.0
    curve = []
    for y in sorted(shares):
        running += shares[y]
        curve.append((running, y))
    return curve


def nice_step(span):
    """The round tick step at or above a span, on a 1/1.25/2/2.5/5 series so any scale works."""
    if span <= 0.0:
        return 1.0
    magnitude = 10.0 ** math.floor(math.log10(span))
    for multiple in (1.0, 1.25, 2.0, 2.5, 5.0, 10.0):
        if magnitude * multiple >= span:
            return magnitude * multiple
    return magnitude * 10.0


def nice_axis(span):
    """`(axis top, tick step)` for a span, at about five divisions.

    The step alone is not the axis top; returning both keeps a range of 1.1% off an axis that
    ends at 1% and clips its own peak.
    """
    step = nice_step(span / 5.0)
    return math.ceil(span / step) * step, step


def ticks(low, high, step):
    """The multiples of `step` inside a range, low first."""
    first = math.ceil(low / step) * step
    count = int((high - first) / step) + 1
    return [value for value in (first + index * step for index in range(count)) if low <= value <= high]


def density_bins(values, low, high, width):
    """`[share of the sample per bin]`, in percent, over `width`-wide bins of continentalness."""
    count = max(1, int(math.ceil((high - low) / width)))
    bins = [0] * count
    for value in values:
        index = int((value - low) / width)
        if 0 <= index < count:
            bins[index] += 1
    return [100.0 * bin_count / len(values) for bin_count in bins]


def plot_density(canvas, panel, values, low, high, width):
    """The sampled distribution as a histogram under the curve, on the panel's right hand range.

    The bins are a fixed width in continentalness rather than one per pixel, so the right hand
    axis can be read; a per pixel column states its own width nowhere and cannot be.
    """
    bins = density_bins(values, low, high, width)
    base = panel.y + panel.height - 1
    for index, share in enumerate(bins):
        left = panel.x + int(index * (panel.width * width / (high - low)))
        right = panel.x + int((index + 1) * (panel.width * width / (high - low)))
        top_y = max(panel.y, int(round(panel.scale_to_height(share))))
        canvas.rect(left, top_y, max(1, right - left), base - top_y, DENSITY)


def plot_spline(path, model, size, origin, seed, name):
    """The three panels: the curve, the profile it draws over the map, the shares per block."""
    canvas = Canvas(960, 1150)
    dimension = model.dimension
    canvas.text(24, 20, "CONTINENTALNESS SPLINE", INK, 2)
    canvas.text(24, 46, "%s   seed %d   %d x %d chunk sample from %d,%d   sea level %d"
                % (name, seed, size, size, origin[0], origin[1], dimension.sea_level), INK)

    shares = model.shares
    y_range = (min(shares) - 6, max(shares) + 10)
    y_ticks = ticks(y_range[0], y_range[1], HEIGHT_STEP)
    y_labels = [str(value) for value in y_ticks]

    # panel 1: the curve, against the continentalness the sample actually holds
    x_range = (min(model.continents[0], model.points[0][0]) - 0.05,
               max(model.continents[-1], model.points[-1][0]))
    peak = max(density_bins(model.continents, x_range[0], x_range[1], DENSITY_WIDTH))
    right_top, right_step = nice_axis(peak * 1.1)
    right_range = (0.0, right_top)
    top = Panel(canvas, (150, 100, 750, 290), x_range, y_range,
                "1  SURFACE Y AGAINST CONTINENTALNESS", right_range)
    x_ticks = ticks(x_range[0], x_range[1], 0.25)
    top.grid(x_ticks, y_ticks, ["%g" % value for value in x_ticks], y_labels)
    plot_density(canvas, top, model.continents, x_range[0], x_range[1], DENSITY_WIDTH)
    top.right_axis(ticks(right_range[0], right_range[1], right_step), ["%g" % value for value in
                  ticks(right_range[0], right_range[1], right_step)])
    canvas.line(top.x, top.py(dimension.sea_level), top.x + top.width - 1, top.py(dimension.sea_level), SEA)
    canvas.text(top.x + top.width - 82, top.py(dimension.sea_level) - 12, "SEA LEVEL", SEA)
    curve = []
    for index in range(1201):
        value = x_range[0] + (x_range[1] - x_range[0]) * index / 1200.0
        curve.append(top.point(value, model.surface(value)))
    canvas.polyline(curve, CURVE, 2)
    for location, _, _ in model.points:
        if x_range[0] <= location <= x_range[1]:
            x, y = top.point(location, model.surface(location))
            canvas.rect(x - 3, y - 3, 7, 7, CURVE_HI)
            canvas.frame(x - 3, y - 3, 7, 7, INK)
    top.axis_label("continentalness")
    canvas.text(top.x, top.y + top.height + 34, "SHADED: %% OF THE MAP PER %g OF CONTINENTALNESS, RIGHT AXIS"
                % DENSITY_WIDTH, INK)

    # panel 2: the same curve with the share of the map on the x axis, where a flat run is a mesa
    middle = Panel(canvas, (150, 500, 750, 240), (0.0, 100.0), y_range,
                   "2  SURFACE Y AGAINST THE SHARE OF THE MAP BELOW IT")
    middle.grid(list(range(0, 101, 10)), y_ticks, [str(value) for value in range(0, 101, 10)], y_labels)
    canvas.line(middle.x, middle.py(dimension.sea_level), middle.x + middle.width - 1,
                middle.py(dimension.sea_level), SEA)
    stairs = [middle.point(share, y) for share, y in cumulative(shares)]
    canvas.polyline(stairs, CURVE, 2)
    for (x0, y0), (x1, y1) in zip(stairs, stairs[1:]):
        # a vertical run is a stretch of the map that buys a block of height or less
        if abs(x1 - x0) <= 2:
            canvas.line(x0, y0, x1, y1, CURVE_HI, 3)
    middle.axis_label("% of the map")

    # panel 3: one bar per block of height, where the spline's t^2 crowding shows up
    bar_top, bar_step = nice_axis(max(shares.values()) * 1.1)
    bottom = Panel(canvas, (150, 860, 750, 190), y_range, (0.0, bar_top),
                   "3  SHARE OF THE MAP PER BLOCK OF HEIGHT")
    bottom.grid(y_ticks, ticks(0.0, bar_top, bar_step), y_labels,
                ["%g" % value for value in ticks(0.0, bar_top, bar_step)])
    base = bottom.y + bottom.height - 1
    for y in sorted(shares):
        left = bottom.px(y)
        top_y = max(bottom.y, bottom.py(shares[y]))
        canvas.rect(left, top_y, max(1, bottom.px(y + 1) - left), base - top_y, BAR)
    canvas.line(bottom.x, bottom.py(dimension.sea_level), bottom.x + bottom.width - 1,
                bottom.py(dimension.sea_level), SEA)
    bottom.axis_label("surface y")

    canvas.text(24, 1084, "SPLINE POINTS", CURVE_HI)
    canvas.text(150, 1084, "SHADED: SAMPLED CONTINENTALNESS", INK)
    canvas.rect(404, 1082, 18, 10, BAR)
    canvas.text(428, 1084, "SHARE AT ONE HEIGHT", INK)
    canvas.text(24, 1106, "A TALL BAR IN PANEL 3 IS ONE HEIGHT HOLDING A LARGE PART OF THE MAP, WHICH IS"
                          " WHAT A MESA IS. IT IS THE", INK)
    canvas.text(24, 1122, "SAME FACT IN PANEL 2 AS A VERTICAL RUN: A STRETCH OF THE MAP BUYING NO HEIGHT.",
                INK)
    canvas.write(path)


def write_spline_table(path, model, size, origin, seed, name):
    dimension = model.dimension
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("spline plot: %s\n" % name)
        handle.write("%d x %d chunk sample from chunk %d,%d, seed %d, sea level %d\n"
                     % (size, size, origin[0], origin[1], seed, dimension.sea_level))
        handle.write("heights from the game's own surface chain, density: %s\n"
                     % ("full final_density" if dimension.world.caves else
                        "surface chain, caves dropped" + ("" if dimension.world.cheese else ", cheese dropped")))
        handle.write("\n  continentalness    spline   surface y   share below   blocks per 1%%\n")
        running = 0.0
        for index, (location, value, _) in enumerate(model.points):
            y = model.surface(location)
            share = model.share(location)
            span = share - running
            running = share
            after = model.surface(model.points[index + 1][0]) if index + 1 < len(model.points) else None
            rate = "%6.0f" % (abs(after - y) / span * 100.0) if after is not None and span > 0.01 else "     -"
            handle.write("  %14.3f  %8.4f  %10d   %10.2f%%   %s\n" % (location, value, y, share, rate))
        handle.write("\nHEIGHT SHARES\n")
        bins = {}
        for y, share in model.shares.items():
            index = height_bin(y, HEIGHT_BIN_LOW, HEIGHT_BIN_HIGH)
            bins[index] = bins.get(index, 0.0) + share
        for index in sorted(bins):
            if index == 0:
                label, sample = "< %3d" % HEIGHT_BIN_LOW, dimension.sea_level - 1
            elif index == (HEIGHT_BIN_HIGH - HEIGHT_BIN_LOW) // HEIGHT_STEP + 1:
                label, sample = "%3d+ " % HEIGHT_BIN_HIGH, HEIGHT_BIN_HIGH
            else:
                low = HEIGHT_BIN_LOW + (index - 1) * HEIGHT_STEP
                label, sample = "%3d-%d" % (low, low + HEIGHT_STEP - 1), low
            handle.write("  %s  #%02x%02x%02x  %6.2f%%\n"
                         % (label, *height_color(sample, dimension.sea_level, dimension.min_y), bins[index]))


def run_spline_plot(args, pack, dimension_json, name, jobs, origin):
    """Sample the continentalness field, read the surface chain against the spline, plot both."""
    dimension = Dimension(pack, dimension_json, args.seed, args.caves, args.cheese)
    if dimension.continents_key is None:
        raise Bad("the router's continentalness is inline, so there is no spline to plot")
    found = []
    find_splines(pack, dimension.router["depth"], dimension.continents_key, set(), found)
    if not found:
        raise Bad("no spline over %s in the router's depth chain" % dimension.continents_key)
    # only the depth chain's own spline is pinned; `factor` is a spline over the same
    # coordinate with a different scale, and pinning that too flips the density's sign
    dimension.world.spline_nodes = found
    points = spline_points(max(found, key=lambda spline: len(spline["points"])))

    sys.stderr.write("%s: sampling continentalness over %d x %d chunks from (%d, %d), seed %d, "
                     "%d workers\n" % (name, args.size, args.size, origin[0], origin[1], args.seed, jobs))
    work = [(row, origin[0], origin[1] + row, args.size) for row in range(args.size)]
    sampled = run_worker_pool(jobs, (pack, dimension_json, args.seed, args.caves, args.cheese),
                              work, _worker_sample)
    continents = sorted(value for _, row_values in sampled for value in row_values)

    model = SplineModel(dimension, points, continents)
    sys.stderr.write("%s: %d surface heights against the chain\n" % (name, len(model.profile.heights)))
    model.shares = height_shares(model)

    os.makedirs(args.out, exist_ok=True)
    plot_spline(os.path.join(args.out, name + "_spline.png"), model, args.size, origin, args.seed, name)
    write_spline_table(os.path.join(args.out, name + "_spline.txt"), model, args.size, origin, args.seed, name)
    print(os.path.join(args.out, name + "_spline.png"))
    print(os.path.join(args.out, name + "_spline.txt"))


# --------------------------------------------------------------------------------------
# driver
# --------------------------------------------------------------------------------------

_WORKER = {}


def _worker_init(pack, dimension_json, seed, caves, cheese):
    _WORKER["dimension"] = Dimension(pack, dimension_json, seed, caves, cheese)


def _worker_row(job):
    row, chunk_x, chunk_z, size = job
    biomes, heights, parameters, counts, total = render_row(_WORKER["dimension"], chunk_x, chunk_z, size)
    return row, biomes, heights, parameters, counts, total


def run_worker_pool(jobs, initargs, work, function):
    """Run one job per scanline, in a pool unless there is only the one worker.

    The map and the spline plot both scan scanlines and neither needs the other's payload, so
    the pool is set up once here rather than twice. `function` must be importable by name,
    which rules out a lambda.
    """
    if jobs <= 1 or len(work) <= 1:
        _worker_init(*initargs)
        return [function(job) for job in work]
    import multiprocessing
    with multiprocessing.Pool(jobs, initializer=_worker_init, initargs=initargs) as pool:
        return list(pool.imap(function, work, chunksize=1))


# -- the cave report ------------------------------------------------------------------


def _void_init(pack, dimension_json, seed, cheese):
    """Both chains the report needs: the full final density for the air, the pruned one for the surface."""
    _WORKER["full"] = Dimension(pack, dimension_json, seed, caves=True, cheese=cheese)
    _WORKER["pruned"] = Dimension(pack, dimension_json, seed, caves=False, cheese=cheese)


def _void_column(point):
    """One sampled column: the cave-free surface, and whether each y sample below it is air."""
    full, pruned = _WORKER["full"], _WORKER["pruned"]
    x, z = point
    # the scan reads the 2d memo without moving it, so each column has to be positioned first
    pruned.column.move_to(x, z)
    surface = surface_height(pruned.density, full.min_y, full.max_y, x, z, pruned.column)
    if surface is None:
        return None
    air = []
    for y in range(full.min_y, full.max_y, VOID_STEP):
        full.column.move_to(x, z)
        air.append(full.density(x, y, z, full.column) < 0.0)
    return surface, air


def write_caves(path, name, pack, dimension_json, dimension, seed, jobs, cheese):
    """Carved volume, surface openings and void sizes, which the two maps cannot show.

    The maps are one pixel per chunk, so a cave is at most a pixel of noise in them. This walks the world
    vertically instead: every `VOID_SPACING` blocks, one column of the full `final_density`, with the
    cave-free surface as the datum. The numbers that answer "are the underground spaces there, and does the
    ground still have a roof" are the carved share of the rock under that datum, the share of columns whose
    deepest void is `VOID_DEEP` blocks or more, and the share of columns open at the surface itself.
    """
    rows = [(x, z)
            for x in range(-VOID_COLUMNS * VOID_SPACING // 2, VOID_COLUMNS * VOID_SPACING // 2, VOID_SPACING)
            for z in range(-VOID_COLUMNS * VOID_SPACING // 2, VOID_COLUMNS * VOID_SPACING // 2, VOID_SPACING)]
    jobs = max(1, min(jobs, len(rows)))
    results = [row for row in run_void_pool(jobs, (pack, dimension_json, seed, cheese), rows) if row is not None]
    if not results:
        raise Bad("no column of the sample reached the surface: nothing to report")

    min_y = dimension.min_y
    bands, rock, spans, roofs = Counter(), Counter(), [], []
    openings = deep = 0
    for surface, air in results:
        top = max(index for index, y in enumerate(range(min_y, dimension.max_y, VOID_STEP)) if y <= surface)
        run, deepest, roof = 0, 0, None
        for index in range(top + 1):
            band = (min_y + index * VOID_STEP) // VOID_BAND * VOID_BAND
            rock[band] += 1
            if air[index]:
                bands[band] += 1
                run += VOID_STEP
                deepest = max(deepest, run)
                if roof is None:
                    roof = surface - (min_y + index * VOID_STEP)
            else:
                run = 0
        # an opening is air at the surface or the two blocks under it
        if any(air[index] for index in (top, top - 1, top - 2) if index >= 0):
            openings += 1
        if deepest >= VOID_DEEP:
            deep += 1
        spans.append(deepest)
        roofs.append(roof)

    columns_sampled = len(results)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("cave report: %s\n" % name)
        handle.write("%d columns %d blocks apart, y step %d, seed %d, sea level %d\n"
                     % (columns_sampled, VOID_SPACING, VOID_STEP, seed, dimension.sea_level))
        handle.write("air from the full final_density, the datum is the surface chain with the caves cut\n")
        handle.write("\n  carved fraction of the rock under the surface, by %d block band\n" % VOID_BAND)
        for band in sorted(rock):
            handle.write("    y %4d..%-4d  %6.2f%%\n" % (band, band + VOID_BAND - 1,
                                                        100.0 * bands[band] / rock[band]))
        handle.write("    %-13s %6.2f%%\n" % ("all",
                                              100.0 * sum(bands.values()) / sum(rock.values())))
        handle.write("\n  surface openings           %6.2f%%  (%d columns)\n"
                     % (100.0 * openings / columns_sampled, openings))
        handle.write("  column with a >=%d void   %6.2f%%\n"
                     % (VOID_DEEP, 100.0 * deep / columns_sampled))
        spans.sort()
        handle.write("  deepest void per column    median %d, p90 %d, max %d\n"
                     % (spans[len(spans) // 2], spans[int(len(spans) * 0.9)], spans[-1]))
        # the roof over the shallowest cave, which is what a cave mouth would have to break
        roofs = sorted(roof for roof in roofs if roof is not None)
        if roofs:
            handle.write("  rock over the shallowest   min %d, p1 %d, p10 %d, median %d\n"
                         % (roofs[0], roofs[max(1, len(roofs) // 100)],
                            roofs[len(roofs) // 10], roofs[len(roofs) // 2]))


def run_void_pool(jobs, initargs, work):
    """`_worker_column` over a pool, or in line for a single worker."""
    if jobs <= 1 or len(work) <= 1:
        _void_init(*initargs)
        return [_void_column(point) for point in work]
    import multiprocessing
    with multiprocessing.Pool(jobs, initializer=_void_init, initargs=initargs) as pool:
        return list(pool.imap(_void_column, work, chunksize=8))


def find_dimension(pack, argument):
    """Accepts `namespace:path` or a path to a dimension json."""
    if os.path.isfile(argument):
        with open(argument, "r", encoding="utf-8") as handle:
            return json.load(handle)
    if ":" not in argument:
        raise Bad("dimension " + argument + " needs a namespace, or a path to its json")
    try:
        return pack.get("dimension", argument)
    except Bad:
        raise Bad("no datapack dimension called " + argument
                  + " (vanilla dimensions are code, not datapack: pass a path to a json instead)")


def height_bin(y, low, high):
    """Which band a height falls in: 0 below `low`, 1..n in steps of 16, n+1 at or above `high`."""
    if y < low:
        return 0
    return min((y - low) // HEIGHT_STEP, (high - low) // HEIGHT_STEP) + 1


def write_legend(path, name, dimension, size, origin, seed, counts, total):
    heights = {key[len("_height:"):]: value for key, value in counts.items() if key.startswith("_height:")}
    regions = {key[len("_param:"):]: value for key, value in counts.items() if key.startswith("_param:")}
    biomes = {key: value for key, value in counts.items() if not key.startswith("_")}
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("terrain export: %s\n" % name)
        handle.write("%d x %d chunks (%d blocks square) from chunk %d,%d, sea level %d, y %d..%d\n"
                     % (size, size, size * 16, origin[0], origin[1], dimension.sea_level,
                        dimension.min_y, dimension.max_y))
        handle.write("seed %d, sampled at chunk centres, density: %s\n"
                     % (seed, "full final_density" if dimension.world.caves else
                        "surface chain, caves dropped" + ("" if dimension.world.cheese else ", cheese dropped")))
        handle.write("\nBIOMES\n")
        for biome, count in sorted(biomes.items(), key=lambda item: -item[1]):
            red, green, blue = biome_color(biome)
            handle.write("  %-40s #%02x%02x%02x  %6.2f%%  %d\n"
                         % (biome, red, green, blue, 100.0 * count / total, count))
        if regions:
            handle.write("\nPARAMETERS  temperature / vegetation / erosion, each cut at %s\n"
                         % ", ".join("%+.2f" % cut for cut in CLIMATE_CUTS))
            for label in sorted(regions):
                index = PARAMETER_REGIONS.index(label)
                red, green, blue = PARAMETER_COLORS[index]
                handle.write("  %-40s #%02x%02x%02x  %6.2f%%  %d\n"
                             % (label, red, green, blue, 100.0 * regions[label] / total, regions[label]))
        if heights:
            lowest = min(int(key) for key in heights)
            highest = max(int(key) for key in heights)
            mean = sum(int(key) * count for key, count in heights.items()) / total
            bins = {}
            for key, count in heights.items():
                index = height_bin(int(key), HEIGHT_BIN_LOW, HEIGHT_BIN_HIGH)
                bins[index] = bins.get(index, 0) + count
            underwater = sum(count for key, count in heights.items()
                             if int(key) < dimension.sea_level)
            handle.write("\nHEIGHT\n")
            handle.write("  lowest %d, highest %d, mean %.1f, under sea level %.2f%%\n"
                         % (lowest, highest, mean, 100.0 * underwater / total))
            for index in sorted(bins):
                if index == 0:
                    label = "< %3d" % HEIGHT_BIN_LOW
                    sample = dimension.sea_level - 1
                elif index == (HEIGHT_BIN_HIGH - HEIGHT_BIN_LOW) // HEIGHT_STEP + 1:
                    label = "%3d+ " % HEIGHT_BIN_HIGH
                    sample = HEIGHT_BIN_HIGH
                else:
                    low = HEIGHT_BIN_LOW + (index - 1) * HEIGHT_STEP
                    label = "%3d-%d" % (low, low + HEIGHT_STEP - 1)
                    sample = low
                handle.write("  %s  #%02x%02x%02x  %6.2f%%  %d\n"
                             % (label, *height_color(sample, dimension.sea_level, dimension.min_y),
                                100.0 * bins[index] / total, bins[index]))


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dimension", help="namespace:path of a dimension, or a path to its json")
    parser.add_argument("--seed", type=int, default=0, help="world seed (default: 0)")
    parser.add_argument("--size", type=int, default=512, help="chunks per side, also the pixels per side")
    parser.add_argument("--origin", default="0,0", help="chunk coordinates of the top left pixel")
    parser.add_argument("--out", default=DEFAULT_OUT, help="output directory")
    parser.add_argument("--datapack", action="append", default=[],
                        help="extra datapack root, a directory or a jar (repeatable)")
    parser.add_argument("--vanilla", help="vanilla datapack jar or directory (default: the client-extra jar)")
    parser.add_argument("--caves", action="store_true", help="evaluate the router's full final_density")
    parser.add_argument("--cheese", action="store_true", help="keep the base_3d_noise term")
    parser.add_argument("--voids", action="store_true",
                        help="report the caves: carved volume, surface openings and void sizes")
    parser.add_argument("--spline", action="store_true",
                        help="plot the continentalness spline instead of the two maps")
    parser.add_argument("--jobs", type=int, default=0, help="worker processes (default: one per core)")
    args = parser.parse_args()

    vanilla = args.vanilla or find_vanilla_pack()
    if vanilla is None:
        raise Bad("no vanilla datapack found in %s, pass --vanilla" % ARTIFACTS)
    pack = DataPack([os.path.abspath(vanilla)] + [os.path.abspath(path) for path in args.datapack] + [MOD_PACK])
    dimension_json = find_dimension(pack, args.dimension)

    try:
        origin_x, origin_z = (int(part) for part in args.origin.split(","))
    except ValueError:
        raise Bad("--origin wants chunk coordinates as X,Z")

    name = dimension_json.get("type", args.dimension).replace(":", "_")
    jobs = args.jobs if args.jobs > 0 else (os.cpu_count() or 1)
    jobs = max(1, min(jobs, args.size))

    if args.spline:
        run_spline_plot(args, pack, dimension_json, name, jobs, (origin_x, origin_z))
        return

    if args.voids:
        # the report wants the full final_density for the air and the pruned chain for the surface, and
        # builds both itself, so `cheese` is the only switch that reaches it
        dimension = Dimension(pack, dimension_json, args.seed, caves=False, cheese=args.cheese)
        os.makedirs(args.out, exist_ok=True)
        sys.stderr.write("%s: %d x %d columns of caves from (%d, %d), seed %d, %d workers\n"
                         % (name, VOID_COLUMNS, VOID_COLUMNS, origin_x, origin_z, args.seed, jobs))
        write_caves(os.path.join(args.out, name + "_caves.txt"), name, pack, dimension_json,
                    dimension, args.seed, jobs, args.cheese)
        print(os.path.join(args.out, name + "_caves.txt"))
        return

    dimension = Dimension(pack, dimension_json, args.seed, args.caves, args.cheese)
    work = [(row, origin_x, origin_z + row, args.size) for row in range(args.size)]

    sys.stderr.write("%s: %d x %d chunks from (%d, %d), seed %d, %d workers\n"
                     % (name, args.size, args.size, origin_x, origin_z, args.seed, jobs))
    results = run_worker_pool(jobs, (pack, dimension_json, args.seed, args.caves, args.cheese),
                              work, _worker_row)

    counts = Counter()
    total = 0
    for _, _, _, _, row_counts, row_total in results:
        counts.update(row_counts)
        total += row_total

    os.makedirs(args.out, exist_ok=True)
    write_png(os.path.join(args.out, name + "_biomes.png"), args.size, args.size, [row[1] for row in results])
    write_png(os.path.join(args.out, name + "_height.png"), args.size, args.size, [row[2] for row in results])
    write_png(os.path.join(args.out, name + "_parameters.png"), args.size, args.size, [row[3] for row in results])
    write_legend(os.path.join(args.out, name + ".txt"), name, dimension, args.size,
                 (origin_x, origin_z), args.seed, counts, total)
    print(os.path.join(args.out, name + "_biomes.png"))
    print(os.path.join(args.out, name + "_height.png"))
    print(os.path.join(args.out, name + "_parameters.png"))
    print(os.path.join(args.out, name + ".txt"))


if __name__ == "__main__":
    try:
        main()
    except Bad as failure:
        sys.exit("terrain.py: " + str(failure))