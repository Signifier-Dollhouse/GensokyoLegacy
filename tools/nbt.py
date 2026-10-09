#!/usr/bin/env python3
"""NBT parser with strict validation and byte-level diagnostics."""
import gzip
import struct
import sys

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 7, 8, 9, 10, 11, 12
NAMES = {0: 'END', 1: 'BYTE', 2: 'SHORT', 3: 'INT', 4: 'LONG', 5: 'FLOAT', 6: 'DOUBLE',
         7: 'BYTE_ARRAY', 8: 'STRING', 9: 'LIST', 10: 'COMPOUND', 11: 'INT_ARRAY', 12: 'LONG_ARRAY'}


class Bad(Exception):
    pass


class R:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def need(self, n):
        if self.i + n > len(self.d):
            raise Bad('eof at %d need %d' % (self.i, n))

    def u1(self):
        self.need(1); v = self.d[self.i]; self.i += 1; return v

    def i1(self):
        self.need(1); v = struct.unpack_from('>b', self.d, self.i)[0]; self.i += 1; return v

    def i2(self):
        self.need(2); v = struct.unpack_from('>h', self.d, self.i)[0]; self.i += 2; return v

    def i4(self):
        self.need(4); v = struct.unpack_from('>i', self.d, self.i)[0]; self.i += 4; return v

    def i8(self):
        self.need(8); v = struct.unpack_from('>q', self.d, self.i)[0]; self.i += 8; return v

    def f4(self):
        self.need(4); v = struct.unpack_from('>f', self.d, self.i)[0]; self.i += 4; return v

    def f8(self):
        self.need(8); v = struct.unpack_from('>d', self.d, self.i)[0]; self.i += 8; return v

    def s(self):
        n = self.i2()
        self.need(n)
        v = self.d[self.i:self.i + n].decode('utf-8', 'replace')
        self.i += n
        return v


def payload(r, t):
    if t == TAG_BYTE:
        return r.i1()
    if t == TAG_SHORT:
        return r.i2()
    if t == TAG_INT:
        return r.i4()
    if t == TAG_LONG:
        return r.i8()
    if t == TAG_FLOAT:
        return r.f4()
    if t == TAG_DOUBLE:
        return r.f8()
    if t == TAG_BYTE_ARRAY:
        n = r.i4(); r.need(n)
        v = list(struct.unpack_from('>%db' % n, r.d, r.i)); r.i += n; return v
    if t == TAG_STRING:
        return r.s()
    if t == TAG_LIST:
        start = r.i
        it = r.u1()
        n = r.i4()
        if it == TAG_END:
            # empty list: element type is TAG_END but the int length is still present
            return []
        if n < 0 or n > 2000000:
            raise Bad('list %s type=%s n=%d at %d' % (r.d[start - 1:start + 20].hex(), NAMES.get(it), n, start))
        return [payload(r, it) for _ in range(n)]
    if t == TAG_COMPOUND:
        out = {}
        while True:
            tt = r.u1()
            if tt == TAG_END:
                return out
            if tt not in NAMES:
                raise Bad('bad tag %d at %d' % (tt, r.i - 1))
            name = r.s()
            out[name] = payload(r, tt)
    if t == TAG_INT_ARRAY:
        n = r.i4(); r.need(4 * n)
        v = list(struct.unpack_from('>%di' % n, r.d, r.i)); r.i += 4 * n; return v
    if t == TAG_LONG_ARRAY:
        n = r.i4(); r.need(8 * n)
        v = list(struct.unpack_from('>%dq' % n, r.d, r.i)); r.i += 8 * n; return v
    raise Bad('unhandled tag %d at %d' % (t, r.i))


def load(path):
    data = open(path, 'rb').read()
    if data[:2] == b'\x1f\x8b':
        data = gzip.decompress(data)
    r = R(data)
    t = r.u1()
    if t != TAG_COMPOUND:
        raise Bad('root tag %d' % t)
    r.s()
    root = {}
    while True:
        tt = r.u1()
        if tt == TAG_END:
            break
        name = r.s()
        root[name] = payload(r, tt)
    return root, r.i, len(data)


def ruler(r, before=64, after=96):
    lo = max(0, r.i - before)
    hi = min(len(r.d), r.i + after)
    for base in range(lo, hi, 16):
        chunk = r.d[base:base + 16]
        mark = '  <-- offset %d' % r.i if base <= r.i < base + 16 else ''
        print('%06x  %-47s  %s%s' % (base, ' '.join('%02x' % b for b in chunk),
                                     ''.join(chr(b) if 32 <= b < 127 else '.' for b in chunk), mark))


if __name__ == '__main__':
    try:
        root, consumed, total = load(sys.argv[1])
    except Bad as e:
        print('BAD:', e)
        raw = gzip.decompress(open(sys.argv[1], 'rb').read())
        r = R(raw)
        ruler(r)
        sys.exit(1)
    print('OK consumed=%d total=%d keys=%s' % (consumed, total, list(root.keys())))
