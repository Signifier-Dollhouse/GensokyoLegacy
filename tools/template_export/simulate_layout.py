"""Top-down preview of the magical forest vegetation layout, without starting the game.

usage: python simulate_layout.py [--seed 1] [--size 384] [-o preview/layout.png]

Re-implements the seed-pure placement of MagicalForestFeatures (jittered grids, grid exclusion, forest type /
clearing bands) and stamps the real template canopies. Use it to tune densities and spacing. Not simulated:
terrain (slope / water rejections), structures, and the exact vanilla noise - a stand-in noise is thresholded
at the designed area fractions instead. Keep the constants below in sync with MagicalForestFeatures.java.
"""
import argparse
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from nbtio import Template  # noqa: E402
from preview import font  # noqa: E402

M64 = (1 << 64) - 1

# ---- MagicalForestFeatures constants -------------------------------------------------------------------------
GIANT = dict(cell=28, margin=5, salt=7310001, chance=0.85)
GIANT_SPARSE = dict(GIANT, chance=0.4)
LARGE = dict(cell=18, margin=3, salt=7310002, chance=0.8)
LARGE_DENSE = dict(LARGE, chance=0.95)
GLADE_MUSHROOM = dict(cell=12, margin=2, salt=7310003, chance=0.7)
FRACTION_GLADE, FRACTION_OLD_GROWTH, FRACTION_CLEARING = 0.17, 0.25, 0.07

OAK_GIANT = ['tree/oak_giant_1', 'tree/oak_giant_2', 'tree/oak_giant_3', 'tree/oak_giant_4']
FIR_GIANT = ['tree/blue_fir_giant_1', 'tree/blue_fir_giant_2']
OAK_LARGE = ['tree/oak_large_1']
FIR_LARGE = ['tree/blue_fir_large_1', 'tree/blue_fir_large_2']
FIR_MEDIUM = ['tree/blue_fir_medium_1', 'tree/blue_fir_medium_2']
AZALEA = ['bush/azalea_1', 'bush/azalea_2', 'bush/azalea_3']


# ---- exact port of GridLayer.point (RandomSupport.mixStafford13 + Xoroshiro128PlusPlus) ------------------------

def _s64(v):
    v &= M64
    return v - (1 << 64) if v >> 63 else v


def mix_stafford13(z):
    z &= M64
    z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & M64
    z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & M64
    return z ^ (z >> 31)


class Xoroshiro:
    def __init__(self, lo, hi):
        self.lo, self.hi = lo & M64, hi & M64
        if (self.lo | self.hi) == 0:
            self.lo, self.hi = 0x9E3779B97F4A7C15, 0x6A09E667F3BCC909

    @staticmethod
    def _rotl(v, k):
        return ((v << k) | (v >> (64 - k))) & M64

    def next_long(self):
        s0, s1 = self.lo, self.hi
        result = (self._rotl((s0 + s1) & M64, 17) + s0) & M64
        s1 ^= s0
        self.lo = self._rotl(s0, 49) ^ s1 ^ ((s1 << 21) & M64)
        self.hi = self._rotl(s1, 28)
        return result

    def next_float(self):
        return (self.next_long() >> 40) * 5.9604645e-8

    def next_int(self, bound):
        i = self.next_long() & 0xFFFFFFFF
        j = i * bound
        k = j & 0xFFFFFFFF
        if k < bound:
            limit = ((~bound + 1) & 0xFFFFFFFF) % bound
            while k < limit:
                i = self.next_long() & 0xFFFFFFFF
                j = i * bound
                k = j & 0xFFFFFFFF
        return j >> 32


def grid_point(layer, seed, cx, cz):
    lo = mix_stafford13(seed ^ _s64(layer['salt'] * 0x9E3779B97F4A7C15))
    hi = mix_stafford13(_s64(cx * 0x632BE59BD9B4E019) + _s64(cz * 0x2545F4914F6CDD1D))
    rnd = Xoroshiro(lo ^ hi, hi)
    u = rnd.next_float()
    span = layer['cell'] - 2 * layer['margin']
    x = cx * layer['cell'] + layer['margin'] + rnd.next_int(span)
    z = cz * layer['cell'] + layer['margin'] + rnd.next_int(span)
    return (x, z) if u < layer['chance'] else None


def grid_points(layer, seed, size):
    n = size // layer['cell'] + 2
    pts = [grid_point(layer, seed, cx, cz) for cx in range(-1, n) for cz in range(-1, n)]
    return [p for p in pts if p]


def near(points, p, dist):
    return any((q[0] - p[0]) ** 2 + (q[1] - p[1]) ** 2 < dist * dist for q in points)


# ---- stand-in noise ------------------------------------------------------------------------------------------

def value_noise(size, scale, rng, octaves=2):
    out = np.zeros((size, size))
    amp = 1.0
    for _ in range(octaves):
        n = int(size / scale) + 3
        g = rng.standard_normal((n, n))
        xs = np.arange(size) / scale
        i = xs.astype(int)
        f = xs - i
        f = f * f * (3 - 2 * f)
        a = g[np.ix_(i, i)] * np.outer(1 - f, 1 - f) + g[np.ix_(i + 1, i)] * np.outer(f, 1 - f) \
            + g[np.ix_(i, i + 1)] * np.outer(1 - f, f) + g[np.ix_(i + 1, i + 1)] * np.outer(f, f)
        out += amp * a
        amp *= 0.5
        scale /= 2
    return out


# ---- templates -----------------------------------------------------------------------------------------------

CANOPY = {'oak_leaves': (52, 120, 38), 'blue_fir_leaves': (36, 140, 150), 'azalea': (96, 146, 58),
          'ghost_fire': (70, 215, 225), 'dream': (160, 80, 200), 'demonic': (170, 40, 56)}


class Stamp:
    def __init__(self, root, tid):
        tpl = Template.read(os.path.join(root, tid + '.nbt'))
        sx, _, sz = tpl.size
        self.mask = np.zeros((sz, sx), dtype=bool)
        self.height = tpl.size[1]
        kinds = {}
        for (x, y, z), s in tpl.blocks.items():
            name = s.split('[')[0]
            if 'leaves' in name or 'mushroom_block' in name:
                self.mask[z, x] = True
                kinds[name] = kinds.get(name, 0) + 1
        top = max(kinds, key=kinds.get)
        self.color = next(c for k, c in CANOPY.items() if k in top)

    def put(self, canvas, cover, x, z, rng, top_layer):
        m = np.rot90(self.mask, rng.randrange(4))
        if rng.random() < 0.5:
            m = m[:, ::-1]
        h, w = m.shape
        x0, z0 = x - w // 2, z - h // 2
        shade = rng.uniform(0.86, 1.12)
        col = [min(255, int(c * shade)) for c in self.color]
        size = cover.shape[0]
        for dz, dx in np.argwhere(m):
            px, pz = x0 + dx, z0 + dz
            if 0 <= px < size and 0 <= pz < size:
                if top_layer or not cover[pz, px]:
                    canvas[pz, px] = col
                cover[pz, px] = True


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--seed', type=int, default=1)
    ap.add_argument('--size', type=int, default=384)
    ap.add_argument('-o', default=os.path.join(HERE, 'preview', 'layout.png'))
    args = ap.parse_args()
    sys.stdout.reconfigure(encoding='utf-8')
    with open(os.path.join(HERE, 'manifest.json'), encoding='utf-8') as f:
        root = os.path.normpath(os.path.join(HERE, json.load(f)['output']))
    stamps = {}
    stamp = lambda tid: stamps.setdefault(tid, Stamp(root, tid))
    rng = random.Random(args.seed)
    nrng = np.random.default_rng(args.seed)
    size, seed = args.size, args.seed

    forest = value_noise(size, 128, nrng)
    clearing = value_noise(size, 32, nrng, octaves=1)
    glade_t, old_t = np.quantile(forest, FRACTION_GLADE), np.quantile(forest, 1 - FRACTION_OLD_GROWTH)
    clear_t = np.quantile(clearing, 1 - FRACTION_CLEARING)
    inside = lambda p: 0 <= p[0] < size and 0 <= p[1] < size
    zone = lambda p: 'glade' if forest[p[1], p[0]] < glade_t else ('old' if forest[p[1], p[0]] >= old_t else 'mixed')
    is_clearing = lambda p: clearing[p[1], p[0]] >= clear_t

    ground = {'glade': (112, 100, 108), 'mixed': (96, 150, 62), 'old': (86, 132, 70)}
    canvas = np.zeros((size, size, 3), dtype=np.uint8)
    for z in range(size):
        for x in range(size):
            c = ground[zone((x, z))]
            canvas[z, x] = [min(255, int(k * 1.18)) for k in c] if is_clearing((x, z)) else c
    cover = np.zeros((size, size), dtype=bool)

    giants_all = grid_points(GIANT, seed, size)              # exclusion sees every active point
    large_all = grid_points(LARGE_DENSE, seed, size)
    sparse = set(grid_points(GIANT_SPARSE, seed, size))
    large_normal = set(grid_points(LARGE, seed, size))

    placed = {'giant': [], 'large': [], 'medium': [], 'mushroom_large': []}
    low, mid, high = [], [], []                              # draw order: low first
    for p in giants_all:
        if not inside(p) or is_clearing(p):
            continue
        zn = zone(p)
        if zn == 'glade' and p not in sparse:
            continue
        pool = FIR_GIANT if zn != 'mixed' or rng.random() >= 0.5 else OAK_GIANT
        high.append((rng.choice(pool), p))
        placed['giant'].append(p)
    for p in large_all:
        if not inside(p) or is_clearing(p) or near(giants_all, p, 12):
            continue
        zn = zone(p)
        if zn == 'glade' or (zn == 'mixed' and p not in large_normal):
            continue
        pool = FIR_LARGE if zn == 'old' or rng.random() >= 0.5 else OAK_LARGE
        mid.append((rng.choice(pool), p))
        placed['large'].append(p)
    for p in grid_points(GLADE_MUSHROOM, seed, size):
        if inside(p) and zone(p) == 'glade' and not near(giants_all, p, 7) and not near(large_all, p, 5):
            low.append((rng.choice(['mushroom/ghost_fire_large_1', 'mushroom/ghost_fire_large_2',
                                    'mushroom/ghost_fire_large_3', 'mushroom/demonic_miasma_large_1',
                                    'mushroom/demonic_miasma_large_2', 'mushroom/dream_large_1']), p))
            placed['mushroom_large'].append(p)
    for cx in range(size // 16):
        for cz in range(size // 16):
            spot = lambda: (cx * 16 + rng.randrange(16), cz * 16 + rng.randrange(16))
            for _ in range(3):                                # medium trees
                p = spot()
                if zone(p) != 'glade' and not is_clearing(p) and not near(giants_all, p, 9) and not near(large_all, p, 8):
                    low.append((rng.choice(FIR_MEDIUM), p))
                    placed['medium'].append(p)
            for _ in range(4):                                # bushes
                p = spot()
                if not near(giants_all, p, 5) and not near(large_all, p, 4):
                    low.append((rng.choice(AZALEA), p))
            mushrooms = 2 if zone((cx * 16 + 8, cz * 16 + 8)) == 'glade' else (1 if rng.random() < 1 / 3 else 0)
            for _ in range(mushrooms):                        # medium mushrooms
                p = spot()
                if not near(giants_all, p, 5) and not near(large_all, p, 4):
                    kind = rng.choice(['ghost_fire', 'demonic_miasma', 'dream'])
                    low.append(('mushroom/%s_medium_%d' % (kind, rng.randint(1, 2)), p))

    for layer, top in ((low, False), (mid, True), (high, True)):
        for tid, p in layer:
            stamp(tid).put(canvas, cover, p[0], p[1], rng, top)

    scale = 3
    img = Image.fromarray(canvas).resize((size * scale, size * scale), Image.NEAREST)
    dr = ImageDraw.Draw(img)
    for p in placed['giant']:
        dr.ellipse([p[0] * scale - 3, p[1] * scale - 3, p[0] * scale + 3, p[1] * scale + 3], fill=(40, 24, 12), outline=(255, 255, 255))
    for p in placed['large']:
        dr.ellipse([p[0] * scale - 2, p[1] * scale - 2, p[0] * scale + 2, p[1] * scale + 2], fill=(40, 24, 12))
    for g in range(0, size, 16):
        dr.line([g * scale, 0, g * scale, 4], fill=(255, 255, 255))
        dr.line([0, g * scale, 4, g * scale], fill=(255, 255, 255))
    dr.text((8, 8), 'seed %d, %dx%d blocks, ticks = chunks; white dot = giant trunk, dark dot = large trunk' % (seed, size, size),
            fill=(255, 255, 255), font=font(13))
    os.makedirs(os.path.dirname(args.o), exist_ok=True)
    img.save(args.o)

    def nn(points, others=None):
        ds = []
        for p in points:
            pool = [q for q in (others or points) if q != p]
            if pool:
                ds.append(min(math.hypot(q[0] - p[0], q[1] - p[1]) for q in pool))
        return (min(ds), sum(ds) / len(ds)) if ds else (0, 0)

    chunks = (size / 16) ** 2
    print('canopy cover: %.1f%%' % (100 * cover.mean()))
    for zn in ('glade', 'mixed', 'old'):
        m = np.array([[zone((x, z)) == zn for x in range(size)] for z in range(size)])
        print('  %-6s area %4.1f%%  cover %4.1f%%' % (zn, 100 * m.mean(), 100 * cover[m].mean() if m.any() else 0))
    print('giants: %d (%.2f / chunk), nearest neighbour min %.1f mean %.1f' % (
        len(placed['giant']), len(placed['giant']) / chunks, *nn(placed['giant'])))
    print('large : %d (%.2f / chunk), nearest giant-or-large min %.1f mean %.1f' % (
        len(placed['large']), len(placed['large']) / chunks, *nn(placed['large'], placed['giant'] + placed['large'])))
    print('medium: %d (%.2f / chunk)   large mushrooms: %d' % (
        len(placed['medium']), len(placed['medium']) / chunks, len(placed['mushroom_large'])))
    print('saved', args.o)


if __name__ == '__main__':
    main()
