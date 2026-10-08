#!/usr/bin/env python3
"""Indoor/outdoor analysis of a structure template, tolerant of decoration.

Cells are reduced to three states:
  '.'  blocking (full cube: walls, floors, roof, furniture)
  ' '  passable but not indoor air candidate (glass, panes, ice...)
  '-'  open air (indoor or outdoor)

Outdoor is flood filled from outside the volume through passable cells; whatever
open air survives is genuinely indoors, even when furniture plugs the volume.
"""
import sys
from collections import deque

sys.path.insert(0, 'tools')
from structview import load_blocks

# non-full-cube decoration that you can stand/walk through or ignore for volume
AIRLIKE_PREFIX = (
    'minecraft:air',
    'minecraft:short_grass', 'minecraft:tall_grass', 'minecraft:fern',
    'minecraft:dandelion', 'minecraft:poppy', 'minecraft:vine',
    'minecraft:light_blue_carpet', 'minecraft:end_rod', 'minecraft:chain',
    'minecraft:glow_lichen', 'minecraft:torch', 'minecraft:snow',
)
AIRLIKE_EXACT = {
    'minecraft:paintings', 'minecraft:sculk',
}


def state_of(v):
    if v.startswith(AIRLIKE_PREFIX) or v in AIRLIKE_EXACT:
        return '-'
    # panes, glass, leaves, bars, doors and trapdoors are wall openings for the
    # purpose of "can you walk from indoors to outdoors": you cannot walk through
    # a closed window, and a leaf mass or a railing is not a doorway.
    return '.'


def analyse(path, verbose=True):
    size, palette, grid, ents = load_blocks(path)
    sx, sy, sz = size
    st = {}
    for p, v in grid.items():
        st[p] = state_of(v)

    # outdoor flood fill over non-blocking cells
    outdoor = set()
    q = deque()
    for x in range(sx):
        for y in range(sy):
            for z in (0, sz - 1):
                if st.get((x, y, z), '.') != '.' and (x, y, z) not in outdoor:
                    outdoor.add((x, y, z)); q.append((x, y, z))
    for x in range(sx):
        for z in range(sz):
            for y in (0, sy - 1):
                if st.get((x, y, z), '.') != '.' and (x, y, z) not in outdoor:
                    outdoor.add((x, y, z)); q.append((x, y, z))
    for y in range(sy):
        for z in range(sz):
            for x in (0, sx - 1):
                if st.get((x, y, z), '.') != '.' and (x, y, z) not in outdoor:
                    outdoor.add((x, y, z)); q.append((x, y, z))
    while q:
        x, y, z = q.popleft()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if 0 <= n[0] < sx and 0 <= n[1] < sy and 0 <= n[2] < sz \
               and st.get(n, '.') != '.' and n not in outdoor:
                outdoor.add(n); q.append(n)

    indoor = [p for p, s in st.items() if s == '-' and p not in outdoor]
    comps = []
    rem = set(indoor)
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
    print('%s size=%dx%dx%d' % (path, sx, sy, sz))
    print('outdoor passable=%d  indoor open=%d  components=%d'
          % (len(outdoor), len(indoor), len(comps)))
    for c in comps:
        xs = [p[0] for p in c]; ys = [p[1] for p in c]; zs = [p[2] for p in c]
        print('  volume %5d  x %2d..%2d  y %2d..%2d  z %2d..%2d'
              % (len(c), min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))
    if verbose:
        print()
        for y in range(sy):
            print('--- y=%d   (# block, o passable-not-openair, - open air, . empty) ---' % y)
            print('    ' + ''.join(str(x % 10) for x in range(sx)))
            for z in range(sz):
                row = ''
                for x in range(sx):
                    p = (x, y, z)
                    s = st.get(p)
                    if s == '-':
                        row += 'o' if p in outdoor else '-'
                    elif s == ' ':
                        row += 'o'
                    elif s == '.':
                        row += '#'
                    else:
                        row += '.'
                print('%3d %s' % (z, row))
    return size, st, outdoor, comps


if __name__ == '__main__':
    analyse(sys.argv[1], verbose=('--quiet' not in sys.argv))
