#!/usr/bin/env python3
"""Copy the water lily art in ``temp/`` into the mod's textures and cut the two item icons.

The artist draws the lily in ``temp/``: one model per size, and a 64x64 sheet per look of the large
one. A model addresses a rectangle of one texture, so the large lily is one model and seven
textures, and the blockstate picks between them.

The model uvs are in the 0..16 model space whatever the texture's pixel size is, so the sheets are
copied over untouched and the models address them as they are. Run from the repository root::

    python3 tools/water_lily_textures.py

It writes ``textures/block/nature/water_lily_<n>.png`` for the seven looks, ``water_lily_small.png``
for the small lily's sheet, and the two item icons, ``large_water_lily.png`` and
``small_water_lily.png``, which are the pad on its own and double as the block particle texture.
"""
import os
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEMP = os.path.join(ROOT, "temp")
OUT = os.path.join(ROOT, "src/main/resources/assets/gensokyolegacy/textures/block/nature")

# the seven looks of the large lily, in the order of LargeWaterLilyBlock.VARIANT
SHEETS = {
    0: "莲叶（大）.png",
    1: "莲叶+莲花（大）.png",
    2: "莲叶+莲花花苞（大）.png",
    3: "莲叶+莲藕（大）.png",
    4: "莲叶1.png",
    5: "莲叶2.png",
    6: "莲叶3.png",
}
SMALL_SHEET = "莲叶（小）.png"

# item icon -> (sheet, x0, y0, x1, y1)
ICONS = {
    "large_water_lily": ("莲叶（大）.png", 0, 0, 16, 16),
    "small_water_lily": ("莲叶（小）.png", 1, 2, 16, 16),
}


def decode(path):
    """Read a png into (width, height, channels, pixels)."""
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    pos, idat = 8, b""
    w = h = bd = ct = None
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        typ = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + ln]
        if typ == b"IHDR":
            w, h, bd, ct, _, _, _ = struct.unpack(">IIBBBBB", chunk)
        elif typ == b"IDAT":
            idat += chunk
        pos += 12 + ln
    raw = zlib.decompress(idat)
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    stride = w * ch
    out = bytearray()
    prev = bytearray(stride)
    p = 0
    for _ in range(h):
        ft = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        if ft == 1:
            for i in range(ch, stride):
                line[i] = (line[i] + line[i - ch]) & 255
        elif ft == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif ft == 3:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif ft == 4:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                b = prev[i]
                c = prev[i - ch] if i >= ch else 0
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out += line
        prev = line
    return w, h, ch, bytes(out)


def encode(path, w, h, rgba):
    """Write rgba pixels as a png."""
    raw = bytearray()
    stride = w * 4
    for y in range(h):
        raw.append(0)
        raw += rgba[y * stride:(y + 1) * stride]

    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    blob = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(blob)
    print(f"{os.path.relpath(path, ROOT)} ({w}x{h})")


def cut(px, w, x0, y0, x1, y1):
    """Cut a rectangle out of a decoded png."""
    out = bytearray()
    for y in range(y0, y1):
        for x in range(x0, x1):
            i = (y * w + x) * 4
            out += px[i:i + 4]
    return bytes(out), x1 - x0, y1 - y0


def main():
    for i, sheet in SHEETS.items():
        w, h, ch, px = decode(os.path.join(TEMP, sheet))
        assert ch == 4, sheet
        encode(os.path.join(OUT, "water_lily_%d.png" % i), w, h, px)
    w, h, ch, px = decode(os.path.join(TEMP, SMALL_SHEET))
    assert ch == 4, SMALL_SHEET
    encode(os.path.join(OUT, "water_lily_small.png"), w, h, px)

    # the item icons are the pad alone, on its own square
    sheets = {}
    for sheet in {v[0] for v in ICONS.values()}:
        w, h, ch, px = decode(os.path.join(TEMP, sheet))
        sheets[sheet] = (w, px)
    for out_name, (sheet, x0, y0, x1, y1) in ICONS.items():
        w, px = sheets[sheet]
        data, sw, sh = cut(px, w, x0, y0, x1, y1)
        icon = bytearray(16 * 16 * 4)
        ox, oy = (16 - sw) // 2, (16 - sh) // 2
        for y in range(sh):
            dst = ((oy + y) * 16 + ox) * 4
            icon[dst:dst + sw * 4] = data[y * sw * 4:(y + 1) * sw * 4]
        encode(os.path.join(OUT, out_name + ".png"), 16, 16, bytes(icon))


if __name__ == "__main__":
    sys.exit(main())
