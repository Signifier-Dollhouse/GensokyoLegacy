#!/usr/bin/env python3
"""
Export two preview images of a dimension's terrain: a biome map and a height map.

Both are one pixel per chunk, 512x512 by default, so the default export covers
4096x4096 blocks around the world origin:

    out/terrain/<dimension>_biomes.png    every biome in its own color
    out/terrain/<dimension>_height.png    blue below sea level, one color per 16 blocks above
    out/terrain/<dimension>.txt           biome colors, map shares, height histogram

    python3 tools/terrain.py gensokyolegacy:gensokyo
    python3 tools/terrain.py gensokyolegacy:gensokyo --seed 12345 --size 1024
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
        coordinate = self.compile(node["spline"]["coordinate"], cheese_zeroed)
        points = [(p["location"], p["value"], p.get("derivative", 0.0)) for p in node["spline"]["points"]]
        locations = [p[0] for p in points]
        count = len(points)
        return _spline_of(coordinate, points, locations, count)

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
# the legend's bands run between these; below the low end and from the high end up are one bin each
HEIGHT_BIN_LOW = 64
HEIGHT_BIN_HIGH = 208
WATER_TOP = (86, 156, 224)
WATER_DEEP = (0, 16, 80)


def biome_color(key):
    color = BIOME_COLORS.get(key.split(":")[-1])
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
    return bracket


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
        self.biomes = BiomeSource(generator["biome_source"])
        self.column = Column(self.world.slot_count)


def render_row(dimension, chunk_x, chunk_z, size):
    """One scanline of each map, plus the per row counts the legend needs."""
    biomes = bytearray()
    heights = bytearray()
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
            continue
        # biomes are picked per 4x4x4 cell at the cell centre, so that is the point sampled
        cell_x = ((x >> 2) << 2) + 2
        cell_z = ((z >> 2) << 2) + 2
        cell_y = ((height >> 2) << 2) + 2
        column.move_to(cell_x, cell_z)
        point = climate_point(dimension.sampler, cell_x, cell_y, cell_z, column)
        biome = dimension.biomes.biome_at(point)
        counts[biome] += 1
        counts["_height:" + str(height)] += 1
        total += 1
        biomes += bytes(biome_color(biome))
        heights += bytes(height_color(height, dimension.sea_level, dimension.min_y))
    return bytes(biomes), bytes(heights), counts, total


# --------------------------------------------------------------------------------------
# png
# --------------------------------------------------------------------------------------


def write_png(path, size, rows):
    raw = b"".join(b"\x00" + row for row in rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & MASK32)

    header = struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0)
    with open(path, "wb") as handle:
        handle.write(b"\x89PNG\r\n\x1a\n")
        handle.write(chunk(b"IHDR", header))
        handle.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        handle.write(chunk(b"IEND", b""))


# --------------------------------------------------------------------------------------
# driver
# --------------------------------------------------------------------------------------

_WORKER = {}


def _worker_init(pack, dimension_json, seed, caves, cheese):
    _WORKER["dimension"] = Dimension(pack, dimension_json, seed, caves, cheese)


def _worker_row(job):
    row, chunk_x, chunk_z, size = job
    biomes, heights, counts, total = render_row(_WORKER["dimension"], chunk_x, chunk_z, size)
    return row, biomes, heights, counts, total


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

    dimension = Dimension(pack, dimension_json, args.seed, args.caves, args.cheese)
    work = [(row, origin_x, origin_z + row, args.size) for row in range(args.size)]

    sys.stderr.write("%s: %d x %d chunks from (%d, %d), seed %d, %d workers\n"
                     % (name, args.size, args.size, origin_x, origin_z, args.seed, jobs))
    if jobs == 1:
        _worker_init(pack, dimension_json, args.seed, args.caves, args.cheese)
        results = [_worker_row(job) for job in work]
    else:
        import multiprocessing
        with multiprocessing.Pool(jobs, initializer=_worker_init,
                                 initargs=(pack, dimension_json, args.seed, args.caves, args.cheese)) as pool:
            results = list(pool.imap(_worker_row, work, chunksize=1))

    counts = Counter()
    total = 0
    for _, _, _, row_counts, row_total in results:
        counts.update(row_counts)
        total += row_total

    os.makedirs(args.out, exist_ok=True)
    write_png(os.path.join(args.out, name + "_biomes.png"), args.size, [row[1] for row in results])
    write_png(os.path.join(args.out, name + "_height.png"), args.size, [row[2] for row in results])
    write_legend(os.path.join(args.out, name + ".txt"), name, dimension, args.size,
                 (origin_x, origin_z), args.seed, counts, total)
    print(os.path.join(args.out, name + "_biomes.png"))
    print(os.path.join(args.out, name + "_height.png"))
    print(os.path.join(args.out, name + ".txt"))


if __name__ == "__main__":
    try:
        main()
    except Bad as failure:
        sys.exit("terrain.py: " + str(failure))