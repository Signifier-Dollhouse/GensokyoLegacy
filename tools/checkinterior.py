#!/usr/bin/env python3
"""Validate a proposed StructureInterior (rooms + nodes) against a template.

Checks:
  * every room cell is indoor (never outdoor open air)
  * room boxes are pairwise disjoint
  * declared adjacencies actually touch
  * node cells sit inside the room they are declared for
  * node cells are open air with solid support (a real standing spot)
"""
import sys
from collections import defaultdict, deque

sys.path.insert(0, 'tools')
from structview import load_blocks
from indoor import state_of

ROOMS = [
    # name,            x0, y0, z0, x1, y1, z1
    ('tower_hall',     2,  1,  7,   9, 17, 15),
    ('corridor_gnd',  10,  1, 10,  11,  4, 12),
    ('hall_ground',   12,  1,  6,  21,  4, 14),
    ('stair_hall',    13,  1, 15,  21,  4, 16),
    ('east_room',     22,  1,  6,  26,  4, 16),
    ('wing_upper',    12,  5,  7,  26,  8, 14),
    ('landing',       13,  5, 15,  21,  8, 16),
    ('cabinet_nook',  23,  5, 15,  25,  8, 15),
    ('corridor_up',   10,  5, 10,  11,  8, 12),
]

# the integrity snapshot box: shell only, no ground plate / yard / margin foliage
HOUSE = (1, 1, 1, 29, 22, 18)

# the youkai bed, both halves; BedRefData deletes a bed whose cell above it
# falls outside every room
BED = [(21, 6, 7), (22, 6, 7), (21, 6, 8), (22, 6, 8)]

# posA, posB, roomA, roomB   (a shared node's position must sit in roomA)
NODES = [
    ((9, 2, 11), (9, 2, 11), 0, 1),
    ((12, 1, 11), (12, 1, 11), 2, 1),
    ((21, 1, 12), (21, 1, 12), 2, 4),
    ((21, 1, 15), (21, 1, 15), 3, 2),
    ((20, 1, 15), (15, 6, 15), 3, 6),
    ((14, 6, 15), (14, 6, 15), 6, 5),
    ((23, 6, 15), (23, 6, 15), 7, 5),
    ((11, 6, 11), (11, 6, 11), 8, 5),
    ((20, 1, 6), (20, 1, 6), 2, -1),
    ((26, 6, 8), (26, 6, 8), 5, -1),
]

# room pairs that touch but must NOT be joined by a node: the balcony shares a
# face with the tower hall, and the two corridors share one at y4/y5, but
# neither may become a route
FORBIDDEN_EDGES = [(0, 8), (1, 8)]

# room pairs that must share a face, because a node declares them adjacent
MUST_TOUCH = [(3, 6)]   # stair_hall / landing, the stair pair


def outdoor_set(size, st):
    sx, sy, sz = size
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
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if 0 <= n[0] < sx and 0 <= n[1] < sy and 0 <= n[2] < sz \
               and st.get(n, '.') != '.' and n not in seen:
                seen.add(n); q.append(n)
    return seen


def main(path):
    size, palette, grid, ents = load_blocks(path)
    sx, sy, sz = size
    st = {p: state_of(v) for p, v in grid.items()}
    out = outdoor_set(size, st)

    print('template %dx%dx%d' % (sx, sy, sz))
    ok = True

    print()
    print('--- room bounds ---')
    sets = []
    for name, x0, y0, z0, x1, y1, z1 in ROOMS:
        cells = {(x, y, z) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1) for z in range(z0, z1 + 1)}
        sets.append(cells)
        air = sum(1 for c in cells if st.get(c, '.') == '-')
        leaked = [c for c in cells if c in out and st.get(c, '.') == '-']
        print('  %-12s (%2d,%2d,%2d)-(%2d,%2d,%2d)  %5d cells, air %5d, outdoor-air %d'
              % (name, x0, y0, z0, x1, y1, z1, len(cells), air, len(leaked)))
        if leaked:
            ok = False
            print('      LEAK: outdoor air inside, e.g. %s' % sorted(leaked)[:6])

    print()
    print('--- disjointness ---')
    for i in range(len(sets)):
        for j in range(i + 1, len(sets)):
            inter = sets[i] & sets[j]
            if inter:
                ok = False
                print('  OVERLAP %s / %s : %d cells e.g. %s'
                      % (ROOMS[i][0], ROOMS[j][0], len(inter), sorted(inter)[:4]))
    if ok:
        print('  all disjoint')

    print()
    print('--- node cells ---')
    for pa, pb, ra, rb in NODES:
        shared = pa == pb
        checks = [(pa, 'posA', ra)]
        if not shared:
            # a dual node needs one staging cell in each room; a shared node's
            # single position deliberately sits in roomA only (see InteriorNode)
            checks.append((pb, 'posB', rb))
        for pos, which, ri in checks:
            name = ROOMS[ri][0]
            inside = pos in sets[ri]
            block = grid.get(pos, 'MISSING')
            is_air = st.get(pos, '#') == '-'
            below = grid.get((pos[0], pos[1] - 1, pos[2]), 'MISSING')
            below_solid = st.get((pos[0], pos[1] - 1, pos[2]), '#') == '.'
            flag = 'OK ' if (inside and is_air and below_solid) else 'BAD'
            if flag == 'BAD':
                ok = False
            print('  %s %s %-8s room %-12s in-box=%-5s air=%-5s support=%-5s  %s / below=%s'
                  % (flag, which, pos, name, inside, is_air, below_solid,
                     block.split('[')[0], below.split('[')[0]))
        if shared:
            print('       (shared doorway node: position sits in %s, boundary to %s)'
                  % (ROOMS[ra][0], ROOMS[rb][0]))

    print()
    print('--- declared edges: do the boxes touch, and is there a passage? ---')
    seen_edges = set()
    for pa, pb, ra, rb in NODES:
        if rb < 0:
            print('  %-12s -> outside   entry at %s' % (ROOMS[ra][0], pa))
            continue
        key = (min(ra, rb), max(ra, rb))
        if key in seen_edges:
            continue
        seen_edges.add(key)
        a, b = sets[ra], sets[rb]
        shared = set()
        for x, y, z in a:
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (x + d[0], y + d[1], z + d[2])
                if n in b:
                    shared.add((n, (x, y, z)))
        if not shared:
            print('  %-12s <-> %-12s : not adjacent (separated by solid structure)'
                  % (ROOMS[ra][0], ROOMS[rb][0]))
            if tuple(sorted((ra, rb))) in MUST_TOUCH:
                ok = False
                print('      REQUIRED to touch but does not')
            continue
        face_air = [(n, s) for n, s in shared if st.get(n, '.') == '-']
        doors = [(n, s) for n, s in shared
                 if 'door' in (grid.get(n) or '') or 'door' in (grid.get(s) or '')]
        print('  %-12s <-> %-12s : %d shared faces, %d open air, %d door'
              % (ROOMS[ra][0], ROOMS[rb][0], len(shared), len(face_air), len(doors)))

    print()
    print('--- house box ---')
    hx0, hy0, hz0, hx1, hy1, hz1 = HOUSE
    hcells = {(x, y, z) for x in range(hx0, hx1 + 1) for y in range(hy0, hy1 + 1) for z in range(hz0, hz1 + 1)}
    print('  (%2d,%2d,%2d)-(%2d,%2d,%2d)  %d cells (raster)'
          % (hx0, hy0, hz0, hx1, hy1, hz1, len(hcells)))
    for name, x0, y0, z0, x1, y1, z1 in ROOMS:
        outside = [c for c in sets[ROOMS.index((name, x0, y0, z0, x1, y1, z1))] if c not in hcells]
        if outside:
            ok = False
            print('  BAD room %s sticks out of the house box, e.g. %s' % (name, sorted(outside)[:4]))
    print('  all rooms inside the house box' if ok else '')

    print()
    print('--- bed must sit in a room (cell above it) ---')
    for p in BED:
        above = (p[0], p[1] + 1, p[2])
        hit = [ROOMS[i][0] for i, s in enumerate(sets) if above in s]
        if not hit:
            ok = False
        print('  %s %-14s above=%s -> %s' % ('OK ' if hit else 'BAD', str(p), above, hit or 'NO ROOM'))

    print()
    print('--- room graph: every room must be reachable ---')
    adj = defaultdict(set)
    for pa, pb, ra, rb in NODES:
        if rb < 0:
            continue
        adj[ra].add(rb)
        adj[rb].add(ra)
    seen = set()
    stack = [next(iter(adj))] if adj else []
    while stack:
        cur = stack.pop()
        if cur in seen:
            continue
        seen.add(cur)
        stack.extend(adj[cur] - seen)
    for i, (name, *_) in enumerate(ROOMS):
        if i not in adj:
            ok = False
            print('  BAD %s carries no node at all' % name)
        elif i not in seen:
            ok = False
            print('  BAD %s is unreachable from the rest of the graph' % name)
    if ok:
        print('  all %d rooms wired and mutually reachable' % len(ROOMS))

    print('  forbidden edges (touching but must stay unwired):')
    for a, b in FORBIDDEN_EDGES:
        wired = [n for n in NODES if {n[2], n[3]} == {a, b}]
        if wired:
            ok = False
            print('    BAD %s <-> %s is wired: %s' % (ROOMS[a][0], ROOMS[b][0], wired))
        else:
            print('    OK  %s <-> %s unwired' % (ROOMS[a][0], ROOMS[b][0]))

    print()
    print('RESULT:', 'PASS' if ok else 'FAIL')
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1]))
