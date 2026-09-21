"""Read-only NBT + Anvil region reader for MC 1.18+ chunk format (no third-party deps besides numpy)."""
import gzip
import os
import struct
import zlib

import numpy as np


class NBTReader:
    def __init__(self, data):
        self.b = data
        self.p = 0

    def _u(self, fmt, n):
        v = struct.unpack_from(fmt, self.b, self.p)[0]
        self.p += n
        return v

    def string(self):
        n = self._u('>H', 2)
        s = self.b[self.p:self.p + n].decode('utf-8', 'replace')
        self.p += n
        return s

    def payload(self, t):
        if t == 1:
            return self._u('>b', 1)
        if t == 2:
            return self._u('>h', 2)
        if t == 3:
            return self._u('>i', 4)
        if t == 4:
            return self._u('>q', 8)
        if t == 5:
            return self._u('>f', 4)
        if t == 6:
            return self._u('>d', 8)
        if t == 7:
            n = self._u('>i', 4)
            v = self.b[self.p:self.p + n]
            self.p += n
            return v
        if t == 8:
            return self.string()
        if t == 9:
            et = self._u('>b', 1)
            n = self._u('>i', 4)
            return [self.payload(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = self._u('>b', 1)
                if tt == 0:
                    break
                name = self.string()
                d[name] = self.payload(tt)
            return d
        if t == 11:
            n = self._u('>i', 4)
            v = np.frombuffer(self.b, dtype='>i4', count=n, offset=self.p)
            self.p += 4 * n
            return v
        if t == 12:
            n = self._u('>i', 4)
            v = np.frombuffer(self.b, dtype='>u8', count=n, offset=self.p)
            self.p += 8 * n
            return v
        raise ValueError('bad tag %d' % t)

    def root(self):
        t = self._u('>b', 1)
        if t != 10:
            raise ValueError('root not compound')
        self.string()
        return self.payload(10)


def read_nbt_file(path):
    with open(path, 'rb') as f:
        raw = f.read()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    return NBTReader(raw).root()


class Region:
    def __init__(self, path):
        with open(path, 'rb') as f:
            self.data = f.read()

    def chunk(self, cx, cz):
        i = (cx & 31) + (cz & 31) * 32
        off = struct.unpack_from('>I', self.data, i * 4)[0]
        sector = off >> 8
        if sector == 0:
            return None
        p = sector * 4096
        length, comp = struct.unpack_from('>IB', self.data, p)
        blob = self.data[p + 5:p + 4 + length]
        if comp == 2:
            raw = zlib.decompress(blob)
        elif comp == 1:
            raw = gzip.decompress(blob)
        elif comp == 3:
            raw = blob
        else:
            raise ValueError('unsupported chunk compression %d' % comp)
        return NBTReader(raw).root()


def decode_section(sec):
    """returns (palette, 16x16x16 palette-index array [y][z][x]) or None"""
    bs = sec.get('block_states')
    if bs is None:
        return None
    pal = bs['palette']
    if 'data' not in bs or len(pal) == 1:
        return pal, np.zeros((16, 16, 16), dtype=np.int32)
    bits = max(4, (len(pal) - 1).bit_length())
    per = 64 // bits
    mask = (1 << bits) - 1
    arr = np.asarray(bs['data'], dtype=np.uint64)
    shifts = np.arange(per, dtype=np.uint64) * np.uint64(bits)
    vals = (arr[:, None] >> shifts[None, :]) & np.uint64(mask)
    vals = vals.reshape(-1)[:4096].astype(np.int32)
    return pal, vals.reshape(16, 16, 16)


def state_str(name, props):
    if props:
        return name + '[' + ','.join('%s=%s' % (k, props[k]) for k in sorted(props)) + ']'
    return name


def parse_state(s):
    """'ns:name[a=b,c=d]' -> (name, {a: b, c: d})"""
    if '[' not in s:
        return s, {}
    name, rest = s.split('[', 1)
    props = dict(kv.split('=', 1) for kv in rest[:-1].split(','))
    return name, props


class World:
    def __init__(self, save_dir, dim_dir='region'):
        self.dir = os.path.join(save_dir, dim_dir)
        self._regions = {}

    def chunk(self, cx, cz):
        key = (cx >> 5, cz >> 5)
        if key not in self._regions:
            path = os.path.join(self.dir, 'r.%d.%d.mca' % key)
            self._regions[key] = Region(path) if os.path.exists(path) else None
        r = self._regions[key]
        return None if r is None else r.chunk(cx, cz)


class Volume:
    """Dense block volume over a world-coordinate box. arr[y - ymin][z - z0][x - x0] = id into names."""

    def __init__(self, world, x0, z0, x1, z1, ymin=-64, ymax=320):
        self.x0, self.z0, self.x1, self.z1 = x0, z0, x1, z1
        self.ymin, self.ymax = ymin, ymax
        self.names = ['minecraft:air']
        self.index = {'minecraft:air': 0}
        self.arr = np.zeros((ymax - ymin, z1 - z0 + 1, x1 - x0 + 1), dtype=np.int32)
        self.missing = []
        for cx in range(x0 >> 4, (x1 >> 4) + 1):
            for cz in range(z0 >> 4, (z1 >> 4) + 1):
                ch = world.chunk(cx, cz)
                if ch is None:
                    self.missing.append((cx, cz))
                    continue
                for sec in ch.get('sections', []):
                    dec = decode_section(sec)
                    if dec is None:
                        continue
                    pal, idx = dec
                    if len(pal) == 1 and pal[0]['Name'] == 'minecraft:air':
                        continue
                    lut = np.zeros(len(pal), dtype=np.int32)
                    for i, p in enumerate(pal):
                        lut[i] = self.intern(state_str(p['Name'], p.get('Properties')))
                    g = lut[idx]
                    by0 = sec['Y'] * 16
                    bx0, bz0 = cx * 16, cz * 16
                    xs0, xs1 = max(bx0, x0), min(bx0 + 15, x1)
                    zs0, zs1 = max(bz0, z0), min(bz0 + 15, z1)
                    ys0, ys1 = max(by0, ymin), min(by0 + 15, ymax - 1)
                    if xs0 > xs1 or zs0 > zs1 or ys0 > ys1:
                        continue
                    self.arr[ys0 - ymin:ys1 - ymin + 1, zs0 - z0:zs1 - z0 + 1, xs0 - x0:xs1 - x0 + 1] = \
                        g[ys0 - by0:ys1 - by0 + 1, zs0 - bz0:zs1 - bz0 + 1, xs0 - bx0:xs1 - bx0 + 1]

    def intern(self, s):
        i = self.index.get(s)
        if i is None:
            i = self.index[s] = len(self.names)
            self.names.append(s)
        return i

    def base_name(self, gid):
        return self.names[gid].split('[')[0]
