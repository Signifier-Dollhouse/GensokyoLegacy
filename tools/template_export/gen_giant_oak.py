"""Procedural giant oaks in the style of the hand-built tree/oak_giant_1 and tree/oak_large_1.

usage: python gen_giant_oak.py            # writes tree/oak_giant_2..4.nbt + preview/generated_oaks.png

Style rules measured on the hand-built templates:
  * dark_oak_wood trunk with a blobby, stepwise tapering cross-section (4x4 minus corners -> 3x3 -> 2x2);
  * a whorl of 1-wide limbs radiating almost horizontally inside the crown, a short leader above it;
  * a flattened dome of oak_leaves, ~21 wide and 8 tall, ragged rim, ~10% holes, hollow around the limbs,
    sparse first and last layer;
  * vines on the rim of the lowest crown layers (side-attached, hanging 1-7), single 'up' vines under the crown;
  * moss carpet on exposed log tops, eugune on the trunk sides, cave vines under overhanging wood.
"""
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from export_templates import centre_on_anchor, fix_leaves, validate  # noqa: E402
from nbtio import Template  # noqa: E402

WOOD = 'minecraft:dark_oak_wood[axis=y]'
LEAF = 'minecraft:oak_leaves[distance=1,persistent=false,waterlogged=false]'
SIDES = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}
# relative crown layer -> envelope radius, measured on oak_giant_1 (layer 0 = sparse skirt)
DOME = [8.8, 10.4, 10.5, 10.0, 9.0, 7.6, 5.6, 3.3]


class Noise:
    """cheap smooth noise: a few random sines; angular() is periodic in the angle."""

    def __init__(self, rng, n=4):
        self.w = [(rng.uniform(0.6, 1.0) / (i + 1), rng.randint(1, 3) + i, rng.uniform(0, 6.283)) for i in range(n)]
        self.v = [(rng.uniform(0.15, 0.45), rng.uniform(0.15, 0.45), rng.uniform(0.2, 0.5), rng.uniform(0, 6.283))
                  for _ in range(n)]

    def angular(self, a, y=0.0):
        s = sum(w * math.sin(k * a + p + 0.35 * y) for w, k, p in self.w)
        return s / sum(w for w, _, _ in self.w)

    def field(self, x, y, z):
        return sum(math.sin(a * x + b * z + c * y + p) for a, b, c, p in self.v) / len(self.v)


class Tree:
    def __init__(self, seed):
        self.rng = random.Random(seed)
        self.logs = set()
        self.leaves = set()
        self.deco = {}

    # ---- wood ----

    def disc(self, cx, y, cz, r, noise=None):
        """fill cells whose centre lies within r of (cx, cz); cells are [i, i+1) so cx=1.0 is a cell corner."""
        ri = int(r) + 2
        for i in range(int(cx) - ri, int(cx) + ri + 1):
            for k in range(int(cz) - ri, int(cz) + ri + 1):
                dx, dz = i + 0.5 - cx, k + 0.5 - cz
                rr = r * (1 + 0.18 * noise.angular(math.atan2(dz, dx), y)) if noise else r
                if dx * dx + dz * dz <= rr * rr:
                    self.logs.add((i, y, k))

    def column(self, path, radii, noise):
        """path: [(y, cx, cz)] key points; radii: [(y, r)] key points. Linear interpolation per layer."""
        for y in range(path[0][0], path[-1][0] + 1):
            cx, cz = _lerp_path(path, y)
            self.disc(cx, y, cz, _lerp_keys(radii, y), noise)

    def limb(self, p0, p1, sag=0.0):
        """1-wide voxel line; sag > 0 bows the limb upwards in the middle."""
        n = int(max(abs(p1[i] - p0[i]) for i in range(3)) * 3) + 1
        cells = []
        for s in range(n + 1):
            t = s / n
            x = p0[0] + (p1[0] - p0[0]) * t
            y = p0[1] + (p1[1] - p0[1]) * t + sag * math.sin(math.pi * t)
            z = p0[2] + (p1[2] - p0[2]) * t
            c = (math.floor(x), int(round(y)), math.floor(z))
            if not cells or cells[-1] != c:
                cells.append(c)
        self.logs.update(cells)
        return cells

    def roots(self, cx, cz, r, count, noise):
        a0 = self.rng.uniform(0, 6.283)
        for i in range(count):
            a = a0 + 6.283 * i / count + self.rng.uniform(-0.35, 0.35)
            length = self.rng.choice((1, 2, 2, 3))
            for d in range(1, length + 1):
                x, z = cx + math.cos(a) * (r + d - 0.6), cz + math.sin(a) * (r + d - 0.6)
                for y in range(max(1, length + 1 - d)):
                    self.logs.add((math.floor(x), y, math.floor(z)))

    # ---- crown ----

    def crown(self, cx, base, cz, scale=1.0, stretch=(1.0, 1.0), noise=None, cavity=True):
        """flattened dome following DOME; cx/cz = crown axis, base = y of the sparse skirt layer."""
        rng = self.rng
        noise = noise or Noise(rng)
        holes = Noise(rng)
        for j, rad in enumerate(DOME):
            y = base + j
            r = rad * scale
            ri = int(r * max(stretch)) + 3
            for i in range(math.floor(cx) - ri, math.floor(cx) + ri + 1):
                for k in range(math.floor(cz) - ri, math.floor(cz) + ri + 1):
                    dx, dz = (i + 0.5 - cx) / stretch[0], (k + 0.5 - cz) / stretch[1]
                    d = math.hypot(dx, dz)
                    edge = r * (1 + 0.13 * noise.angular(math.atan2(dz, dx), j * 0.6)) + rng.uniform(-0.55, 0.55)
                    if d > edge:
                        continue
                    depth = edge - d
                    keep = 0.93
                    if j == 0:
                        keep = 0.42 if depth < 5 else 0.12          # skirt: a broken ring
                    elif j == len(DOME) - 1:
                        keep = 0.5
                    elif depth < 1.2:
                        keep = 0.8
                    if holes.field(i * 1.7, y * 2.3, k * 1.7) > 0.62:
                        keep *= 0.35
                    if cavity and j in (2, 3) and d < 4.6 * scale + 0.8 * noise.angular(math.atan2(dz, dx), 9):
                        keep = 0.1 if j == 2 else 0.3                  # hollow around the limbs
                    if rng.random() < keep:
                        self.leaves.add((i, y, k))

    def whorl(self, cx, y, cz, count, reach, rise=1, leader=3, twigs=True):
        """limbs radiating from the trunk top at height y, plus a leader. Returns limb tips."""
        rng = self.rng
        a0 = rng.uniform(0, 6.283)
        tips = []
        for i in range(count):
            a = a0 + 6.283 * i / count + rng.uniform(-0.3, 0.3)
            length = reach * rng.uniform(0.75, 1.0)
            tip = (cx + math.cos(a) * length, y + rise + rng.choice((0, 0, 1)), cz + math.sin(a) * length)
            self.limb((cx, y, cz), tip, sag=rng.uniform(0, 0.6))
            tips.append(tip)
            if twigs:                                       # a side twig fills the rim sector between two limbs
                t = rng.uniform(0.5, 0.75)
                b = a + rng.choice((-1, 1)) * rng.uniform(0.7, 1.1)
                p = (cx + math.cos(a) * length * t, y + rise * t, cz + math.sin(a) * length * t)
                q = (p[0] + math.cos(b) * rng.uniform(3, 4.5), p[1] + rng.choice((0, 1, 1)),
                     p[2] + math.sin(b) * rng.uniform(3, 4.5))
                self.limb(p, q)
        top = (cx + rng.uniform(-1, 1), y + leader, cz + rng.uniform(-1, 1))
        self.limb((cx, y, cz), top)
        a1 = rng.uniform(0, 6.283)
        upper = max(3, count - 2)
        for i in range(upper):                              # second tier: short limbs 2 layers above the whorl
            a = a1 + 6.283 * i / upper + rng.uniform(-0.4, 0.4)
            length = reach * rng.uniform(0.42, 0.6)
            self.limb((cx, y + 2, cz), (cx + math.cos(a) * length, y + 3 + rng.choice((0, 1)), cz + math.sin(a) * length))
        for _ in range(2):
            a = rng.uniform(0, 6.283)
            self.limb(top, (top[0] + math.cos(a) * 2.5, top[1] + rng.choice((0, 1)), top[2] + math.sin(a) * 2.5))
        return tips

    # ---- clean up + decoration ----

    def finish(self, vine_ratio=0.1, vine_max=5, moss=0.4, eugune=10, cave=0.5):
        """clean up the shape, then decorate. The decorations draw from the rng in this fixed order."""
        self._tuft_bare_wood()
        self.leaves -= self.logs
        # drop leaves that touch nothing (6-neighbourhood), twice
        for _ in range(2):
            self.leaves = {p for p in self.leaves if any(self._solid(_add(p, d)) for d in N6)}
        self._keep_connected()
        crown_floor = min(p[1] for p in self.leaves)
        self._hang_vines(crown_floor, vine_ratio, vine_max)
        self._moss_carpets(crown_floor, moss)
        self._cave_vines(crown_floor, cave)
        self._eugune(crown_floor, eugune)

    def _solid(self, p):
        return p in self.logs or p in self.leaves

    def _free(self, p):
        return p[1] >= 0 and not self._solid(p) and p not in self.deco

    def _tuft_bare_wood(self):
        """wood poking out of the crown gets a leaf tuft."""
        rng = self.rng
        floor = min(p[1] for p in self.leaves)
        for p in sorted(self.logs):
            if p[1] <= floor:
                continue
            around = [(p[0] + dx, p[1] + dy, p[2] + dz) for dx in (-1, 0, 1) for dy in (0, 1) for dz in (-1, 0, 1)]
            if sum(q in self.leaves for q in around) < 6:
                self.leaves.update(q for q in around if rng.random() < 0.8)

    def _hang_vines(self, crown_floor, vine_ratio, vine_max):
        """vines on the sides of the low crown rim, hanging down with the same face; single 'up' vines under the crown."""
        rng, free, solid = self.rng, self._free, self._solid
        spots = []
        for p in self.leaves:
            if p[1] > crown_floor + 2:
                continue
            for face, (dx, dz) in SIDES.items():
                q = (p[0] - dx, p[1], p[2] - dz)          # vine cell; the leaf is on its `face` side
                if free(q) and not solid((q[0], q[1] - 1, q[2])):
                    spots.append((q, face))
        rng.shuffle(spots)
        budget = int(len(self.leaves) * vine_ratio)
        for q, face in spots:
            if budget <= 0:
                break
            if not free(q) or any((q[0] + dx, q[1], q[2] + dz) in self.deco for dx, dz in SIDES.values()):
                continue
            length = min(vine_max, 1 + int(rng.expovariate(0.75)))
            for d in range(length):
                c = (q[0], q[1] - d, q[2])
                if not free(c) or c[1] < 3:
                    break
                self.deco[c] = _vine(face)
                budget -= 1
        under = [(p[0], p[1] - 1, p[2]) for p in self.leaves if p[1] <= crown_floor + 1]
        rng.shuffle(under)
        for c in under[:int(len(self.leaves) * 0.022)]:
            if free(c):
                self.deco[c] = _vine('up')

    def _moss_carpets(self, crown_floor, moss):
        """moss carpet on exposed log tops (trunk steps, roots, stubs) below the crown."""
        for p in sorted(self.logs):
            c = (p[0], p[1] + 1, p[2])
            if p[1] < crown_floor - 2 and self._free(c) and self.rng.random() < moss:
                self.deco[c] = 'minecraft:moss_carpet'

    def _cave_vines(self, crown_floor, cave):
        """cave vines under overhanging wood."""
        rng, free = self.rng, self._free
        for p in sorted(self.logs):
            c = (p[0], p[1] - 1, p[2])
            if 4 <= p[1] < crown_floor - 1 and free(c) and free((c[0], c[1] - 1, c[2])) and rng.random() < cave:
                length = rng.choice((1, 1, 2, 2, 3, 4))
                cells = []
                for d in range(length):
                    cc = (c[0], c[1] - d, c[2])
                    if not free(cc) or cc[1] < 3:
                        break
                    cells.append(cc)
                for n, cc in enumerate(cells):
                    last = n == len(cells) - 1
                    self.deco[cc] = ('minecraft:cave_vines[age=%d,berries=false]' % rng.randint(1, 21)) if last                         else 'minecraft:cave_vines_plant[berries=false]'

    def _eugune(self, crown_floor, count):
        """eugune on the trunk sides; FACING points away from the supporting log."""
        rng, free = self.rng, self._free
        spots = []
        for p in self.logs:
            if p[1] > min(11, crown_floor - 3):
                continue
            for face, (dx, dz) in SIDES.items():
                c = (p[0] + dx, p[1], p[2] + dz)
                if free(c):
                    spots.append((c, face))
        rng.shuffle(spots)
        for c, face in spots[:count]:
            if free(c):
                kind = rng.choice(['red'] * 5 + ['brown'] * 4 + ['ghost_fire'] * 2)
                self.deco[c] = 'gensokyolegacy:eugune_%s[facing=%s]' % (kind, face)

    def _keep_connected(self):
        """keep only what is 26-connected to the trunk base."""
        everything = self.logs | self.leaves
        start = [p for p in self.logs if p[1] == 0]
        seen = set(start)
        stack = list(start)
        while stack:
            p = stack.pop()
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        q = (p[0] + dx, p[1] + dy, p[2] + dz)
                        if q in everything and q not in seen:
                            seen.add(q)
                            stack.append(q)
        self.logs &= seen
        self.leaves &= seen

    def template(self):
        voxels = {p: WOOD for p in self.logs}
        voxels.update({p: LEAF for p in self.leaves})
        voxels.update(self.deco)
        tpl, low = centre_on_anchor(voxels)
        assert low == 0
        fix_leaves(tpl.blocks)
        return tpl


N6 = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]


def _add(p, d):
    return p[0] + d[0], p[1] + d[1], p[2] + d[2]


def _vine(face):
    return 'minecraft:vine[' + ','.join('%s=%s' % (f, str(f == face).lower())
                                        for f in ('east', 'north', 'south', 'up', 'west')) + ']'


def _lerp_keys(keys, y):
    for (y0, v0), (y1, v1) in zip(keys, keys[1:]):
        if y0 <= y <= y1:
            return v0 + (v1 - v0) * (y - y0) / max(1, y1 - y0)
    return keys[-1][1] if y > keys[-1][0] else keys[0][1]


def _lerp_path(path, y):
    for (y0, x0, z0), (y1, x1, z1) in zip(path, path[1:]):
        if y0 <= y <= y1:
            t = (y - y0) / max(1, y1 - y0)
            return x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
    return path[-1][1], path[-1][2]


# ---------------------------------------------------------------- archetypes

def single(seed):
    """one massive buttressed trunk, high crown (oak_large_1 scaled up to giant size)."""
    t = Tree(seed)
    rng, n = t.rng, Noise(t.rng)
    h = rng.randint(15, 17)
    dx, dz = rng.uniform(-2, 2), rng.uniform(-2, 2)
    t.column([(0, 1.0, 1.0), (h // 2, 1.0 + dx * 0.4, 1.0 + dz * 0.4), (h, 1.0 + dx, 1.0 + dz)],
             [(0, 2.45), (2, 2.0), (6, 1.9), (9, 1.55), (12, 1.15), (h, 0.8)], n)
    t.roots(1.0, 1.0, 2.2, rng.randint(4, 6), n)
    for _ in range(rng.randint(1, 2)):                     # broken limb stubs: overhangs for cave vines
        y, a = rng.randint(6, 10), rng.uniform(0, 6.283)
        cx, cz = _lerp_path([(0, 1.0, 1.0), (h, 1.0 + dx, 1.0 + dz)], y)
        t.limb((cx, y, cz), (cx + math.cos(a) * 4.2, y + 1, cz + math.sin(a) * 4.2))
    t.whorl(1.0 + dx, h, 1.0 + dz, rng.randint(6, 7), 9.0, leader=3)
    t.crown(1.0 + dx, h - 1, 1.0 + dz, noise=n)
    t.finish(vine_max=6)
    return t


def arch(seed):
    """two legs merging into one slender stem (the oak_giant_1 idea with another geometry)."""
    t = Tree(seed)
    rng, n = t.rng, Noise(t.rng)
    a = rng.uniform(0, 6.283)
    spread = rng.uniform(3.6, 4.6)
    merge, h = rng.randint(9, 11), rng.randint(15, 16)
    mx, mz = rng.uniform(-1, 1), rng.uniform(-1, 1)
    for s, r0 in ((1, 1.95), (-1, 1.75)):
        fx, fz = 1.0 + s * math.cos(a) * spread, 1.0 + s * math.sin(a) * spread
        t.column([(0, fx, fz), (4, 1.0 + (fx - 1.0) * 0.82, 1.0 + (fz - 1.0) * 0.82),
                  (merge - 2, 1.0 + (fx - 1.0) * 0.4, 1.0 + (fz - 1.0) * 0.4), (merge, 1.0 + mx, 1.0 + mz)],
                 [(0, r0 + 0.3), (2, r0), (merge - 2, 1.6), (merge, 1.5)], n)
        t.roots(fx, fz, r0, rng.randint(2, 3), n)
    t.column([(merge, 1.0 + mx, 1.0 + mz), (h, 1.0 + mx * 1.5, 1.0 + mz * 1.5)],
             [(merge, 1.9), (merge + 1, 1.5), (merge + 3, 1.1), (h, 0.8)], n)
    t.whorl(1.0 + mx * 1.5, h, 1.0 + mz * 1.5, rng.randint(6, 7), 9.0, leader=3)
    t.crown(1.0 + mx * 1.5, h - 1, 1.0 + mz * 1.5, noise=n)
    t.finish(vine_max=4, cave=0.6)
    return t


def forked(seed):
    """trunk forks into two leaders carrying two overlapping crowns at different heights."""
    t = Tree(seed)
    rng, n = t.rng, Noise(t.rng)
    fork = rng.randint(8, 9)
    a = rng.uniform(0, 6.283)
    t.column([(0, 1.0, 1.0), (fork, 1.0, 1.0)], [(0, 2.45), (2, 2.05), (5, 1.9), (fork, 1.6)], n)
    t.roots(1.0, 1.0, 2.2, rng.randint(4, 5), n)
    for s, top, off, scale in ((1, fork + 7, 4.2, 0.80), (-1, fork + 5, 3.6, 0.72)):
        ex, ez = 1.0 + s * math.cos(a) * off, 1.0 + s * math.sin(a) * off
        t.column([(fork, 1.0, 1.0), (top, ex, ez)], [(fork, 1.3), (fork + 2, 1.1), (top, 0.8)], n)
        t.whorl(ex, top, ez, rng.randint(4, 5), 6.4 * scale / 0.8, leader=2)
        t.crown(ex, top - 1, ez, scale=scale, noise=Noise(rng))
    t.finish(vine_max=5)
    return t


VARIANTS = [('tree/oak_giant_2', single, 11), ('tree/oak_giant_3', arch, 23), ('tree/oak_giant_4', forked, 5)]


def main():
    import json
    from preview import contact_sheet
    sys.stdout.reconfigure(encoding='utf-8')
    with open(os.path.join(HERE, 'manifest.json'), encoding='utf-8') as f:
        out_dir = os.path.normpath(os.path.join(HERE, json.load(f)['output']))
    items = []
    for tid, fn, seed in VARIANTS:
        tpl = fn(seed).template()
        path = os.path.join(out_dir, tid + '.nbt')
        tpl.write(path)
        validate(path, tpl)
        leaves = tpl.count(lambda s: 'leaves' in s)
        print('%-17s size %2dx%2dx%2d blocks %4d reach %2d leaves %4d (persistent %3d) wood %3d vine %3d '
              'moss %2d eugune %2d cave_vines %2d' % (
                  tid, *tpl.size, len(tpl.blocks), tpl.max_reach(), leaves,
                  tpl.count(lambda s: 'leaves' in s and 'persistent=true' in s),
                  tpl.count(lambda s: 'wood' in s), tpl.count(lambda s: ':vine' in s),
                  tpl.count(lambda s: 'moss' in s), tpl.count(lambda s: 'eugune' in s),
                  tpl.count(lambda s: 'cave_vines' in s)))
        items.append((tid, tpl))
    ref = Template.read(os.path.join(out_dir, 'tree', 'oak_giant_1.nbt'))
    contact_sheet([('hand-built oak_giant_1', ref)] + items, os.path.join(HERE, 'preview', 'generated_oaks.png'),
                  s=7, cols=2)


if __name__ == '__main__':
    main()
