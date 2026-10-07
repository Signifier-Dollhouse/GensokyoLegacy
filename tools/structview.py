#!/usr/bin/env python3
"""Visualize / analyse a Minecraft structure template (.nbt).

Usage:
  python3 tools/structview.py <file.nbt>            # summary + palette counts
  python3 tools/structview.py <file.nbt> layers      # per-y cross sections
  python3 tools/structview.py <file.nbt> air         # enclosed air pockets (flood fill)
  python3 tools/structview.py <file.nbt> plan y      # single y layer
"""
import sys
from collections import Counter, deque

sys.path.insert(0, 'tools')
from nbt import load


def load_blocks(path):
    root, _, _ = load(path)
    size = root['size']
    palette = []
    for p in root['palette']:
        name = p.get('Name', '?')
        props = p.get('Properties', {})
        palette.append(name + ('' if not props else '[' + ','.join('%s=%s' % kv for kv in sorted(props.items())) + ']'))
    grid = {}
    for b in root['blocks']:
        x, y, z = b['pos']
        grid[(x, y, z)] = palette[b['state']]
    ents = root.get('entities') or []
    return size, palette, grid, ents


def summary(path):
    size, palette, grid, ents = load_blocks(path)
    print('%s  size=%dx%dx%d  blocks=%d  palette=%d  entities=%d'
          % (path, size[0], size[1], size[2], len(grid), len(palette), len(ents)))
    xs = [p[0] for p in grid]; ys = [p[1] for p in grid]; zs = [p[2] for p in grid]
    print('  extents x %d..%d  y %d..%d  z %d..%d' % (min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))
    print('  entities:', [(e.get('blockPos'), e.get('nbt', {}).get('id')) for e in ents])
    c = Counter(grid.values())
    for name, n in c.most_common(60):
        print('   %6d  %s' % (n, name))


def layers(path):
    size, palette, grid, ents = load_blocks(path)
    sx, sy, sz = size
    short = {}
    used = sorted(set(grid.values()))
    for i, u in enumerate(used):
        short[u] = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'[i % 62]
    legend = {}
    for i, u in enumerate(used):
        legend[short[u]] = u
    for y in range(sy):
        print('--- y=%d ---' % y)
        print('    ' + ''.join(str(x % 10) for x in range(sx)))
        for z in range(sz):
            row = ''
            for x in range(sx):
                v = grid.get((x, y, z))
                row += short.get(v, '.') if v is not None else '.'
            print('%3d %s' % (z, row))
    print()
    for k in sorted(legend):
        print('%s = %s' % (k, legend[k]))


def plan(path, y):
    size, palette, grid, ents = load_blocks(path)
    sx, sy, sz = size
    print('y=%d  (x across, z down), . = not in template' % y)
    print('    ' + ''.join(str(x % 10) for x in range(sx)))
    for z in range(sz):
        row = ''
        for x in range(sx):
            v = grid.get((x, y, z))
            if v is None:
                row += '.'
            elif v.startswith('minecraft:air'):
                row += ' '
            else:
                row += '#'
        print('%3d %s' % (z, row))


def air(path):
    """Flood fill air that cannot reach the outside of the template volume."""
    size, palette, grid, ents = load_blocks(path)
    sx, sy, sz = size
    solid = set(p for p, v in grid.items() if not v.startswith('minecraft:air'))
    seen = set()
    q = deque()
    for x in range(sx):
        for z in range(sz):
            for y in (0, sy - 1):
                if (x, y, z) not in solid and (x, y, z) not in seen:
                    seen.add((x, y, z)); q.append((x, y, z))
    for x in range(sx):
        for y in range(sy):
            for z in (0, sz - 1):
                if (x, y, z) not in solid and (x, y, z) not in seen:
                    seen.add((x, y, z)); q.append((x, y, z))
    for y in range(sy):
        for z in range(sz):
            for x in (0, sx - 1):
                if (x, y, z) not in solid and (x, y, z) not in seen:
                    seen.add((x, y, z)); q.append((x, y, z))
    while q:
        x, y, z = q.popleft()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if 0 <= n[0] < sx and 0 <= n[1] < sy and 0 <= n[2] < sz and n not in solid and n not in seen:
                seen.add(n); q.append(n)
    inside = [p for p in grid if p not in seen and grid[p].startswith('minecraft:air')]
    print('enclosed air cells: %d' % len(inside))
    comps = []
    rem = set(inside)
    while rem:
        start = rem.pop()
        comp = {start}
        q = deque([start])
        while q:
            x, y, z = q.popleft()
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (x + d[0], y + d[1], z + d[2])
                if n in rem:
                    rem.discard(n); comp.add(n); q.append(n)
        comps.append(comp)
    comps.sort(key=len, reverse=True)
    for c in comps:
        xs = [p[0] for p in c]; ys = [p[1] for p in c]; zs = [p[2] for p in c]
        print('  pocket size %5d  x %d..%d  y %d..%d  z %d..%d'
              % (len(c), min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))


if __name__ == '__main__':
    cmd = sys.argv[2] if len(sys.argv) > 2 else 'summary'
    if cmd == 'summary':
        summary(sys.argv[1])
    elif cmd == 'layers':
        layers(sys.argv[1])
    elif cmd == 'air':
        air(sys.argv[1])
    elif cmd == 'plan':
        plan(sys.argv[1], int(sys.argv[3]))
