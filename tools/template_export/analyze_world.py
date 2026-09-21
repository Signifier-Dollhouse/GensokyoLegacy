"""Top-down map + vegetation metrics of a generated world, to verify the magical forest worldgen.

usage: python analyze_world.py --save <world dir> [--center X Z] [--radius 160] [-o preview/world.png]

Reads region files only. Reports canopy cover, forest-floor plant cover under / outside canopies, and trunk
blocks left hanging in the air (roots that did not reach the ground), and renders two maps: the view from
above, and the forest floor with every canopy removed.
"""
import argparse
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from mcworld import World, decode_section, state_str  # noqa: E402
from preview import color_of, font  # noqa: E402

YMIN, YMAX = -64, 320
EXTRA = {
    'minecraft:grass_block': (106, 170, 64), 'minecraft:dirt': (134, 96, 67), 'minecraft:coarse_dirt': (119, 85, 59),
    'minecraft:rooted_dirt': (144, 103, 76), 'minecraft:podzol': (91, 63, 24), 'minecraft:mycelium': (120, 104, 112),
    'minecraft:moss_block': (89, 109, 45), 'minecraft:stone': (125, 125, 125), 'minecraft:water': (60, 90, 220),
    'minecraft:sand': (219, 207, 163), 'minecraft:gravel': (131, 127, 126), 'minecraft:snow': (245, 250, 250),
    'minecraft:dark_oak_leaves': (40, 92, 28), 'minecraft:jungle_leaves': (48, 128, 24),
    'minecraft:cherry_leaves': (240, 170, 200), 'minecraft:short_grass': (124, 190, 84), 'minecraft:fern': (100, 170, 80),
    'minecraft:tall_grass': (124, 190, 84), 'minecraft:large_fern': (100, 170, 80),
    'gensokyolegacy:cedar_fallen_leaves': (52, 150, 150), 'gensokyolegacy:broom_grass': (150, 190, 90),
    'gensokyolegacy:bracken': (110, 160, 70), 'gensokyolegacy:star_flower': (250, 240, 140),
}


def kind_of(name):
    n = name.split(':', 1)[1]
    if n in ('air', 'cave_air', 'void_air'):
        return 0                                   # air
    if n.endswith('_leaves') or n.endswith('mushroom_block'):
        return 1                                   # canopy
    if n.endswith('_wood') or n.endswith('_log') or n.endswith('_stem'):
        return 2                                   # trunk
    if n in ('water', 'lava'):
        return 5
    if any(k in n for k in ('vine', 'eugune', 'carpet', 'fallen_leaves', 'grass', 'fern', 'flower', 'bracken',
                            'mushroom', 'dandelion', 'poppy', 'tulip', 'orchid', 'allium', 'bluet', 'daisy',
                            'cornflower', 'lily', 'azalea', 'bush', 'sapling', 'lichen', 'petals', 'hyphae',
                            'peony', 'lilac', 'rose', 'sunflower', 'sugar_cane', 'pumpkin', 'melon', 'snow')) \
            and n not in ('grass_block', 'moss_block', 'snow_block'):
        return 3                                   # plant / attachment
    return 4                                       # ground / anything solid


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--save', required=True)
    ap.add_argument('--center', type=int, nargs=2, default=(0, 0))
    ap.add_argument('--radius', type=int, default=160)
    ap.add_argument('-o', default=os.path.join(HERE, 'preview', 'world.png'))
    args = ap.parse_args()
    sys.stdout.reconfigure(encoding='utf-8')
    world = World(args.save)
    x0, z0 = args.center[0] - args.radius, args.center[1] - args.radius
    size = 2 * args.radius
    top_rgb = np.zeros((size, size, 3), dtype=np.uint8)
    floor_rgb = np.zeros((size, size, 3), dtype=np.uint8)
    generated = np.zeros((size, size), dtype=bool)
    canopy = np.zeros((size, size), dtype=bool)
    planted = np.zeros((size, size), dtype=bool)
    wet = np.zeros((size, size), dtype=bool)
    hanging = []
    names, index = ['minecraft:air'], {'minecraft:air': 0}
    biomes = {}

    for cx in range(x0 >> 4, ((x0 + size - 1) >> 4) + 1):
        for cz in range(z0 >> 4, ((z0 + size - 1) >> 4) + 1):
            ch = world.chunk(cx, cz)
            if ch is None or ch.get('Status') not in ('minecraft:full', 'full'):
                continue
            col = np.zeros((YMAX - YMIN, 16, 16), dtype=np.int32)
            for sec in ch.get('sections', []):
                for b in (sec.get('biomes') or {}).get('palette', []):
                    biomes[b] = biomes.get(b, 0) + 1
                dec = decode_section(sec)
                if dec is None:
                    continue
                pal, idx = dec
                lut = np.zeros(len(pal), dtype=np.int32)
                for i, p in enumerate(pal):
                    s = state_str(p['Name'], None)
                    if s not in index:
                        index[s] = len(names)
                        names.append(s)
                    lut[i] = index[s]
                y = sec['Y'] * 16 - YMIN
                if 0 <= y < YMAX - YMIN:
                    col[y:y + 16] = lut[idx]
            kinds = np.array([kind_of(n) for n in names])[col]
            for lz in range(16):
                for lx in range(16):
                    px, pz = cx * 16 + lx - x0, cz * 16 + lz - z0
                    if not (0 <= px < size and 0 <= pz < size):
                        continue
                    k = kinds[:, lz, lx]
                    solid = np.nonzero(k)[0]
                    if len(solid) == 0:
                        continue
                    generated[pz, px] = True
                    top = solid[-1]
                    ground = np.nonzero(k >= 4)[0][-1]
                    wet[pz, px] = k[ground] == 5
                    canopy[pz, px] = bool(((k[ground + 1:] == 1) | (k[ground + 1:] == 2)).any())
                    planted[pz, px] = k[ground + 1] == 3 if ground + 1 < len(k) else False
                    shade = 0.6 + min(0.6, max(0, top + YMIN - 60) / 90.0)
                    top_rgb[pz, px] = [min(255, int(c * shade)) for c in _color(names[col[top, lz, lx]])]
                    f = ground + 1 if planted[pz, px] else ground
                    floor_rgb[pz, px] = _color(names[col[f, lz, lx]])
                    # lowest trunk block of the column hanging in the air
                    trunk = np.nonzero(k[ground + 1:] == 2)[0]
                    if len(trunk) and trunk[0] > 0 and k[ground + trunk[0]] == 0:
                        gap = int(trunk[0])
                        below = k[ground + 1:ground + 1 + gap]
                        if (below == 0).all() and gap <= 6 and k[ground] == 4:
                            hanging.append((cx * 16 + lx, ground + 1 + gap + YMIN, cz * 16 + lz, gap))

    n = int(generated.sum())
    if n == 0:
        raise SystemExit('no fully generated chunks in the requested area')
    land = generated & ~wet
    print('area: %d of %d columns generated, %.1f%% water' % (n, size * size, 100 * wet[generated].mean()))
    print('biomes (section palette hits):', ', '.join('%s %d' % kv for kv in sorted(biomes.items(), key=lambda kv: -kv[1])[:6]))
    print('canopy cover on land: %.1f%%' % (100 * canopy[land].mean()))
    print('forest floor with a plant / carpet / litter: under canopy %.1f%%, open ground %.1f%%' % (
        100 * planted[land & canopy].mean() if (land & canopy).any() else 0,
        100 * planted[land & ~canopy].mean() if (land & ~canopy).any() else 0))
    # leaning trunks, limb stubs and bent mushroom stems hang by design (the flat showcase scores ~5 per giant
    # tree); roots that failed to reach the ground show up as a surplus of small gaps on sloped terrain
    gaps = [sum(1 for h in hanging if h[3] == g) for g in range(1, 7)]
    print('trunk columns hanging over air: %d, by gap 1..6: %s' % (len(hanging), gaps))
    for h in hanging[:8]:
        print('   x=%d y=%d z=%d gap=%d' % h)

    scale = 2
    sheet = Image.new('RGB', (size * scale * 2 + 12, size * scale + 24), (24, 26, 32))
    sheet.paste(Image.fromarray(top_rgb).resize((size * scale, size * scale), Image.NEAREST), (0, 24))
    sheet.paste(Image.fromarray(floor_rgb).resize((size * scale, size * scale), Image.NEAREST), (size * scale + 12, 24))
    dr = ImageDraw.Draw(sheet)
    dr.text((6, 5), 'from above   x[%d,%d] z[%d,%d]' % (x0, x0 + size - 1, z0, z0 + size - 1), fill=(255, 255, 255), font=font(13))
    dr.text((size * scale + 18, 5), 'forest floor (canopies removed)', fill=(255, 255, 255), font=font(13))
    os.makedirs(os.path.dirname(os.path.abspath(args.o)), exist_ok=True)
    sheet.save(args.o)
    print('saved', args.o)


def _color(name):
    return EXTRA.get(name) or color_of(name)


if __name__ == '__main__':
    main()
