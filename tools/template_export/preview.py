"""Isometric previews of structure templates (PIL). usage: python preview.py <file.nbt>... [-o out.png]"""
import hashlib
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from nbtio import Template, is_full_cube  # noqa: E402

COLORS = {
    'minecraft:grass_block': (106, 170, 64),
    'minecraft:oak_leaves': (60, 130, 40),
    'minecraft:dark_oak_leaves': (45, 100, 30),
    'minecraft:azalea_leaves': (90, 140, 50),
    'minecraft:flowering_azalea_leaves': (170, 110, 170),
    'minecraft:dark_oak_wood': (60, 40, 20),
    'minecraft:dark_oak_log': (60, 40, 20),
    'minecraft:oak_wood': (110, 85, 50),
    'minecraft:vine': (30, 95, 25),
    'minecraft:cave_vines': (120, 130, 40),
    'minecraft:cave_vines_plant': (120, 130, 40),
    'minecraft:moss_carpet': (110, 135, 50),
    'gensokyolegacy:blue_fir_leaves': (40, 150, 160),
    'gensokyolegacy:blue_fir_wood': (30, 70, 90),
    'gensokyolegacy:blue_fir_log': (30, 80, 100),
    'gensokyolegacy:evergreen_vine': (20, 110, 60),
    'gensokyolegacy:evergreen_vine_plant': (20, 110, 60),
    'gensokyolegacy:ghost_fire_mushroom_block': (60, 220, 230),
    'gensokyolegacy:cyan_mushroom_stem': (170, 230, 230),
    'gensokyolegacy:dream_mushroom_block': (160, 70, 200),
    'gensokyolegacy:purple_mushroom_stem': (210, 180, 230),
    'gensokyolegacy:demonic_miasma_mushroom_block': (170, 30, 50),
    'gensokyolegacy:red_mushroom_stem': (230, 170, 170),
    'gensokyolegacy:eugune_red': (255, 80, 80),
    'gensokyolegacy:eugune_brown': (190, 140, 95),
    'gensokyolegacy:eugune_ghost_fire': (100, 255, 255),
}
BG = (24, 26, 32)


def color_of(name):
    if name in COLORS:
        return COLORS[name]
    h = hashlib.md5(name.encode()).digest()
    return 80 + h[0] % 120, 80 + h[1] % 120, 80 + h[2] % 120


def font(size=12):
    for p in (r'C:\Windows\Fonts\consola.ttf', r'C:\Windows\Fonts\arial.ttf'):
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def shade(c, f):
    return tuple(max(0, min(255, int(k * f))) for k in c)


def iso(tpl, s=8, rot=0, ground=True, title=''):
    """rot = quarter turns around Y. Returns a PIL image."""
    sx, sy, sz = tpl.size
    names = sorted(set(v.split('[')[0] for v in tpl.blocks.values()))
    idx = {n: i + 1 for i, n in enumerate(names)}
    pad = 2 if ground else 0
    arr = np.zeros((sy + 1, sz + 2 * pad, sx + 2 * pad), dtype=np.int32)
    for (x, y, z), st in tpl.blocks.items():
        arr[y + 1, z + pad, x + pad] = idx[st.split('[')[0]]
    full = np.array([False] + [is_full_cube(n) for n in names])
    flat = np.array([False] + ['carpet' in n or 'fallen_leaves' in n for n in names])
    cols = [BG] + [color_of(n) for n in names]
    if ground:
        arr[0, :, :] = len(names) + 1
        full = np.append(full, True)
        flat = np.append(flat, False)
        cols.append(COLORS['minecraft:grass_block'])
    arr = np.rot90(arr, k=rot, axes=(1, 2))
    H, Zn, Xn = arr.shape
    hs = s // 2
    img = Image.new('RGB', ((Xn + Zn) * s + 24, (Xn + Zn) * hs + H * s + 40), BG)
    dr = ImageDraw.Draw(img)
    ox, oy = Zn * s + 12, H * s + 20
    solid = full[arr]
    for d in range(Xn + Zn - 1):
        for x in range(max(0, d - Zn + 1), min(Xn, d + 1)):
            z = d - x
            for y in range(H):
                g = arr[y, z, x]
                if g == 0:
                    continue
                c = cols[g]
                px, py = ox + (x - z) * s, oy + (x + z) * hs - y * s
                if flat[g]:
                    b = py + s - 2
                    dr.polygon([(px, b), (px + s, b + hs), (px, b + 2 * hs), (px - s, b + hs)], fill=shade(c, 1.1))
                    continue
                if not full[g]:
                    cx, cy, r = px, py + hs + s // 2, max(2, s // 3)
                    dr.polygon([(cx, cy - r - 1), (cx + r, cy), (cx, cy + r + 1), (cx - r, cy)], fill=shade(c, 1.15))
                    continue
                if y + 1 >= H or not solid[y + 1, z, x]:
                    dr.polygon([(px, py), (px + s, py + hs), (px, py + 2 * hs), (px - s, py + hs)],
                               fill=shade(c, 1.15), outline=shade(c, 0.95))
                if z + 1 >= Zn or not solid[y, z + 1, x]:
                    dr.polygon([(px - s, py + hs), (px, py + 2 * hs), (px, py + 2 * hs + s), (px - s, py + hs + s)],
                               fill=shade(c, 0.85), outline=shade(c, 0.7))
                if x + 1 >= Xn or not solid[y, z, x + 1]:
                    dr.polygon([(px + s, py + hs), (px, py + 2 * hs), (px, py + 2 * hs + s), (px + s, py + hs + s)],
                               fill=shade(c, 0.62), outline=shade(c, 0.5))
    if title:
        dr.text((6, 4), title, fill=(255, 255, 255), font=font(12))
    return img


def contact_sheet(items, path, s=None, rots=(0, 2), cols=4):
    """items: [(title, Template)]"""
    cells = []
    for title, tpl in items:
        big = max(tpl.size[0], tpl.size[2])
        scale = s or (10 if big <= 9 else (7 if big <= 15 else 5))
        views = [iso(tpl, s=scale, rot=r) for r in rots]
        w, h = sum(v.width for v in views), max(v.height for v in views)
        cell = Image.new('RGB', (w, h + 18), BG)
        x = 0
        for v in views:
            cell.paste(v, (x, 18))
            x += v.width
        ImageDraw.Draw(cell).text((6, 3), '%s  %dx%dx%d' % (title, *tpl.size), fill=(255, 255, 255), font=font(12))
        cells.append(cell)
    rows = [cells[i:i + cols] for i in range(0, len(cells), cols)]
    W = max(sum(c.width for c in r) for r in rows)
    Ht = sum(max(c.height for c in r) for r in rows)
    sheet = Image.new('RGB', (W, Ht), BG)
    y = 0
    for r in rows:
        x = 0
        for c in r:
            sheet.paste(c, (x, y))
            x += c.width
        y += max(c.height for c in r)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.save(path)
    return sheet


if __name__ == '__main__':
    args = sys.argv[1:]
    out = os.path.join(HERE, 'preview', 'preview.png')
    if '-o' in args:
        i = args.index('-o')
        out = args[i + 1]
        del args[i:i + 2]
    contact_sheet([(os.path.splitext(os.path.basename(p))[0], Template.read(p)) for p in args], out)
    print('saved', out)
