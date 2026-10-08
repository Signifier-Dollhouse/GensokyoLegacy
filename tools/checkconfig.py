#!/usr/bin/env python3
"""Cross-check a generated structure_config.json against the shipped template.

Reads the boxes straight out of the datamap so the checked values can never
drift from what actually ships.
"""
import json
import sys
from collections import defaultdict, deque

sys.path.insert(0, 'tools')
from structview import load_blocks
from indoor import state_of

CONFIG = 'src/generated/resources/data/gensokyolegacy/data_maps/worldgen/structure/structure_config.json'

# rooms that must stay reachable from the house, so a structure that is not
# wired at all (or has no interior) is not held to the graph checks
SKIP_GRAPH = {'hakurei_shrine', 'morichika_shop'}

# room-name pairs that touch but must never be joined by a node
FORBIDDEN_EDGES = [
    ('tower_hall', 'corridor_up'),
    ('corridor_gnd', 'corridor_up'),
]


def outdoor_set(size, st):
    sx, sy, sz = size
    seen = set()
    q = deque()

    def seed(pts):
        for p in pts:
            if st.get(p, '.') != '.' and p not in seen:
                seen.add(p); q.append(p)

    seed((x, y, z) for x in range(sx) for y in range(sy) for z in (0, sz - 1))
    seed((x, y, z) for x in range(sx) for z in range(sz) for y in (0, sy - 1))
    seed((x, y, z) for y in range(sy) for z in range(sz) for x in (0, sx - 1))
    while q:
        x, y, z = q.popleft()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if 0 <= n[0] < sx and 0 <= n[1] < sy and 0 <= n[2] < sz \
               and st.get(n, '.') != '.' and n not in seen:
                seen.add(n); q.append(n)
    return seen


def cells(box):
    x0, y0, z0, x1, y1, z1 = box
    return {(x, y, z) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1) for z in range(z0, z1 + 1)}


def main(structure_id, template):
    cfg = json.load(open(CONFIG))['values']['gensokyolegacy:' + structure_id]
    size, palette, grid, ents = load_blocks(template)
    sx, sy, sz = size
    st = {p: state_of(v) for p, v in grid.items()}
    out = outdoor_set(size, st)
    ok = True

    print('%s  template %dx%dx%d' % (structure_id, sx, sy, sz))

    house = cfg['house']
    print('  house boxes: %s' % house)
    hcells = set()
    for b in house:
        hcells |= cells(b)
    print('    raster cells: %d' % len(hcells))

    interior = cfg.get('interior') or {}
    rooms = [(r['name'], r['bound']) for r in interior.get('rooms', [])]
    nodes = interior.get('nodes', [])
    print('  interior rooms: %d' % len(rooms))
    sets = []
    for name, b in rooms:
        c = cells(b)
        sets.append(c)
        leaked = [p for p in c if st.get(p, '.') == '-' and p in out]
        outside = [p for p in c if p not in hcells]
        bad = leaked or outside
        if bad:
            ok = False
        print('    %-12s %-26s %5d cells  air %5d  %s'
              % (name, str(b), len(c), sum(1 for p in c if st.get(p, '.') == '-'),
                 'OK' if not bad else 'BAD leaked=%d outside-house=%d' % (len(leaked), len(outside))))

    print('  disjointness:')
    for i in range(len(sets)):
        for j in range(i + 1, len(sets)):
            if sets[i] & sets[j]:
                ok = False
                print('    OVERLAP %s / %s' % (rooms[i][0], rooms[j][0]))
    print('    %s' % ('all disjoint' if ok else 'see above'))

    print('  room graph:')
    if structure_id in SKIP_GRAPH or not rooms:
        print('    skipped (legacy room boxes, no interior graph)')
    else:
        adj = defaultdict(set)
        for n in nodes:
            ra, rb = n['roomA'], n['roomB']
            if rb < 0:
                continue
            if not (0 <= ra < len(rooms)) or not (0 <= rb < len(rooms)):
                ok = False
                print('    BAD node references a room index out of range: %d/%d' % (ra, rb))
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
        for i, (name, _) in enumerate(rooms):
            if i not in adj:
                ok = False
                print('    BAD %s carries no node at all' % name)
            elif i not in seen:
                ok = False
                print('    BAD %s is unreachable from the rest of the graph' % name)
        if ok:
            print('    all %d rooms wired and mutually reachable' % len(rooms))
        for n in nodes:
            a, b = n['roomA'], n['roomB']
            if b < 0:
                continue
            touch = any(
                (c[0] + d[0], c[1] + d[1], c[2] + d[2]) in sets[b]
                for c in sets[a]
                for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
            )
            if not touch:
                print('    note: %s <-> %s declared but the boxes share no face'
                      % (rooms[a][0], rooms[b][0]))

        byname = {name: i for i, (name, _) in enumerate(rooms)}
        print('  forbidden edges (touching but must stay unwired):')
        for x, y in FORBIDDEN_EDGES:
            if x not in byname or y not in byname:
                continue
            wired = [n for n in nodes
                     if {n['roomA'], n['roomB']} == {byname[x], byname[y]}]
            if wired:
                ok = False
                print('    BAD %s <-> %s is wired' % (x, y))
            else:
                print('    OK  %s <-> %s unwired' % (x, y))

    print('  nodes:')
    for n in nodes:
        pa = tuple(n['posA']); pb = tuple(n['posB']); ra = n['roomA']; rb = n['roomB']
        shared = pa == pb
        checks = [(pa, 'posA', ra)] + ([] if shared else [(pb, 'posB', rb)])
        line = []
        for pos, which, ri in checks:
            inside = pos in sets[ri]
            air = st.get(pos, '#') == '-'
            below = st.get((pos[0], pos[1] - 1, pos[2]), '#') == '.'
            good = inside and air and below
            if not good:
                ok = False
            line.append('%s%s:%s/air=%s/sup=%s' % (which, pos, rooms[ri][0], air, below))
        kind = 'entry' if rb < 0 else ('shared' if shared else 'dual/stair')
        print('    %-11s %-2d->%-2d  %s' % (kind, ra, rb, '  '.join(line)))

    print('  stairs:')
    # the run is the bottom-half spruce stair chain inside the stair shaft:
    # each step one along x and one up in y. Decorative spruce stairs on the
    # tower shelves are top-half and sit outside z15-16, so they drop out.
    cand = sorted(p for p, v in grid.items()
                  if 'spruce_stairs' in v and 'half=bottom' in v
                  and 15 <= p[2] <= 16 and p[1] <= 6)
    chain = set()
    for start in cand:
        if any(abs(c[0] - start[0]) <= 1 and abs(c[1] - start[1]) <= 1 for c in chain):
            continue
        group = {start}
        changed = True
        while changed:
            changed = False
            for c in cand:
                if c in group:
                    continue
                if any(abs(c[0] - g[0]) == 1 and abs(c[1] - g[1]) == 1 for g in group):
                    group.add(c); changed = True
        if len(group) > len(chain):
            chain = group
    run = sorted(chain, key=lambda p: p[1])
    if len(run) < 3:
        print('    no unambiguous stair run found for this structure, skipping')
        print()
        print('RESULT:', 'PASS' if ok else 'FAIL')
        return 0 if ok else 1
    lo, hi = run[0], run[-1]
    print('    run: %s' % ', '.join(str(c) for c in run))
    print('    lowest stair %s   highest stair %s' % (str(lo), str(hi)))
    dual = [n for n in nodes
            if tuple(n['posA']) != tuple(n['posB']) and n['roomB'] >= 0]
    for n in dual:
        pa = tuple(n['posA']); pb = tuple(n['posB'])
        da = max(abs(pa[0] - lo[0]), abs(pa[1] - lo[1]), abs(pa[2] - lo[2]))
        db = max(abs(pb[0] - hi[0]), abs(pb[1] - hi[1]), abs(pb[2] - hi[2]))
        # boxes must stay disjoint, so "touching" means sharing a face, not
        # overlapping: some cell of one is orthogonally adjacent to the other
        touching = any(
            (c[0] + d[0], c[1] + d[1], c[2] + d[2]) in sets[n['roomB']]
            for c in sets[n['roomA']]
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
        )
        print('    node %s -> %s : %d block(s) from lowest, %d from highest, rooms touch=%s'
              % (str(pa), str(pb), da, db, touching))
        if da != 1 or db != 1 or not touching:
            ok = False
        if pa not in run and pb not in run:
            print('      (staging cells sit clear of the run itself)')

    print()
    print('RESULT:', 'PASS' if ok else 'FAIL')
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1], sys.argv[2]))
