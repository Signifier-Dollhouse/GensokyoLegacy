#!/usr/bin/env python3
"""Render only the sealed indoor air void, so room volumes are readable."""
import sys
from collections import deque

sys.path.insert(0, 'tools')
from structview import load_blocks
from indoor import state_of


def indoor_set(path):
    size, palette, grid, ents = load_blocks(path)
    sx, sy, sz = size
    st = {p: state_of(v) for p, v in grid.items()}
    seen = set()
    q = deque()
    for x in range(sx):
        for y in range(sy):
            for z in (0, sz - 1):
                if st.get((x, y, z), '.') != '.' and (x, y, z) not in seen:
                    seen.add((x, y, z)); q.append((x, y, z))
    for x in range(sx):
        for z in range(sz):
            for y in (0, sy - 1):
                if st.get((x, y, z), '.') != '.' and (x, y, z) not in seen:
                    seen.add((x, y, z)); q.append((x, y, z))
    for y in range(sy):
        for z in range(sz):
            for x in (0, sx - 1):
                if st.get((x, y, z), '.') != '.' and (x, y, z) not in seen:
                    seen.add((x, y, z)); q.append((x, y, z))
    while q:
        x, y, z = q.popleft()
        for d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
            n = (x+d[0], y+d[1], z+d[2])
            if 0 <= n[0] < sx and 0 <= n[1] < sy and 0 <= n[2] < sz \
               and st.get(n, '.') != '.' and n not in seen:
                seen.add(n); q.append(n)
    return size, st, set(p for p in st if st[p] == '-' and p not in seen)


if __name__ == '__main__':
    size, st, indoor = indoor_set(sys.argv[1])
    sx, sy, sz = size
    lo = [min(p[i] for p in indoor) for i in range(3)]
    hi = [max(p[i] for p in indoor) for i in range(3)]
    print('indoor void bbox  x %d..%d  y %d..%d  z %d..%d  cells=%d'
          % (lo[0], hi[0], lo[1], hi[1], lo[2], hi[2], len(indoor)))
    x0, x1 = lo[0], hi[0]
    z0, z1 = lo[2], hi[2]
    for y in range(lo[1], hi[1] + 1):
        print('--- y=%d   (O = indoor void, x %d..%d across, z %d..%d down) ---' % (y, x0, x1, z0, z1))
        print('      ' + ''.join(str(x % 10) for x in range(x0, x1 + 1)))
        for z in range(z0, z1 + 1):
            row = ''
            for x in range(x0, x1 + 1):
                if (x, y, z) in indoor:
                    row += 'O'
                elif st.get((x, y, z), '.') == '-':
                    row += 'o'   # open air, but reachable from outside
                elif st.get((x, y, z), '.') == '-':
                    row += 'a'
                else:
                    row += '#'
            print('z=%2d  %s' % (z, row))
