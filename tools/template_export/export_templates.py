"""Export hand-built vegetation templates from the creative save into structure .nbt files.

usage: python export_templates.py [--save <world dir>] [--only <id substring>] [--no-preview]

Per template (see manifest.json):
  1. take the blocks of the template's palette inside the box (air / cave_air / stray superflat plants never match);
  2. keep the connected component(s) rooted in the box, so neighbours poking in are dropped;
  3. anchor = centroid of the lowest log / stem layer; pad so the anchor is the centre of the bottom face;
  4. recompute leaf distance; distance <= 6 -> persistent=false, distance 7 -> persistent=true;
  5. write gzip NBT (DataVersion 3955) and validate it by reading it back.
"""
import argparse
import json
import os
import sys
from collections import Counter, deque

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from mcworld import Volume, World, parse_state, state_str  # noqa: E402
from nbtio import Template  # noqa: E402

MARGIN = 3
MAX_REACH = 15  # features may only write inside the 3x3 chunks around the origin chunk
NEIGH6 = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]


def is_log(name):
    return name.endswith('_wood') or name.endswith('_log') or name.endswith('_stem')


def is_leaves(name):
    return name.endswith('_leaves')


def components(mask, reach):
    """label connected voxels; two voxels connect when every axis differs by at most `reach`."""
    label = np.zeros(mask.shape, dtype=np.int32)
    neigh = [(dy, dz, dx) for dy in range(-reach, reach + 1) for dz in range(-reach, reach + 1)
             for dx in range(-reach, reach + 1) if (dy, dz, dx) != (0, 0, 0)]
    H, Zn, Xn = mask.shape
    cur = 0
    for start in np.argwhere(mask):
        start = tuple(start)
        if label[start]:
            continue
        cur += 1
        label[start] = cur
        dq = deque([start])
        while dq:
            y, z, x = dq.popleft()
            for dy, dz, dx in neigh:
                n = (y + dy, z + dz, x + dx)
                if 0 <= n[0] < H and 0 <= n[1] < Zn and 0 <= n[2] < Xn and mask[n] and not label[n]:
                    label[n] = cur
                    dq.append(n)
    return label, cur


def fix_leaves(blocks):
    """vanilla leaf distance: 6-neighbour BFS from logs through leaves, capped at 7."""
    dist = {}
    dq = deque()
    for p, s in blocks.items():
        if is_log(s.split('[')[0]):
            dist[p] = 0
            dq.append(p)
    while dq:
        p = dq.popleft()
        d = dist[p]
        if d >= 7:
            continue
        for dy, dz, dx in NEIGH6:
            n = (p[0] + dx, p[1] + dy, p[2] + dz)
            s = blocks.get(n)
            if s is not None and is_leaves(s.split('[')[0]) and dist.get(n, 99) > d + 1:
                dist[n] = d + 1
                dq.append(n)
    kept = 0
    for p, s in list(blocks.items()):
        name, props = parse_state(s)
        if not is_leaves(name):
            continue
        d = min(7, dist.get(p, 7))
        props['distance'] = str(d)
        props['persistent'] = 'true' if d >= 7 else 'false'
        props['waterlogged'] = 'false'
        kept += d >= 7
        blocks[p] = state_str(name, props)
    return kept


def centre_on_anchor(voxels):
    """voxels: {(x, y, z): state} in any coordinates, y already relative to the first layer above ground."""
    low = min(p[1] for p, s in voxels.items() if is_log(s.split('[')[0]))
    base = [p for p, s in voxels.items() if p[1] == low and is_log(s.split('[')[0])]
    cx = sum(p[0] for p in base) / len(base)
    cz = sum(p[2] for p in base) / len(base)
    # the anchor must be an actual log so that the trunk always lands on the feature origin
    ax, _, az = min(base, key=lambda p: (p[0] - cx) ** 2 + (p[2] - cz) ** 2)
    rx = max(abs(p[0] - ax) for p in voxels)
    rz = max(abs(p[2] - az) for p in voxels)
    top = max(p[1] for p in voxels)
    size = (2 * rx + 1, top + 1, 2 * rz + 1)
    return Template(size, {(p[0] - ax + rx, p[1], p[2] - az + rz): s for p, s in voxels.items()}), low


def extract(world, entry, palettes):
    x0, z0, x1, z1 = entry['box']
    y0, y1 = entry['y']
    allowed = set(palettes[entry['palette']])
    v = Volume(world, x0 - MARGIN, z0 - MARGIN, x1 + MARGIN, z1 + MARGIN, ymin=y0, ymax=y1 + MARGIN + 1)
    ok = np.array([v.base_name(i) in allowed for i in range(len(v.names))])
    mask = ok[v.arr]
    logs = np.array([is_log(v.base_name(i)) for i in range(len(v.names))])[v.arr] & mask
    inside = np.zeros(mask.shape, dtype=bool)
    inside[:y1 - y0 + 1, MARGIN:-MARGIN, MARGIN:-MARGIN] = True
    # trees carry detached decorations (hanging vines, carpets) -> tolerate a 1 block gap
    reach = 1 if entry['id'].startswith('mushroom/') else 2
    label, n = components(mask, reach)
    keep = [k for k in range(1, n + 1) if (logs & inside & (label == k)).any()]
    if not keep:
        raise SystemExit('%s: no log/stem found inside the box' % entry['id'])
    sel = np.isin(label, keep)
    voxels = {}
    for y, z, x in np.argwhere(sel):
        voxels[(int(x), int(y), int(z))] = v.names[v.arr[y, z, x]]
    if not entry.get('split_by_log'):
        return [(entry['id'], voxels)]
    seeds = sorted(p for p, s in voxels.items() if is_log(s.split('[')[0]))
    groups = [dict() for _ in seeds]
    for p, s in voxels.items():
        i = min(range(len(seeds)), key=lambda k: sum((a - b) ** 2 for a, b in zip(p, seeds[k])))
        groups[i][p] = s
    return [('%s_%d' % (entry['id'], i + 1), g) for i, g in enumerate(groups)]


def validate(path, tpl):
    back = Template.read(path)
    assert back.size == tpl.size and back.blocks == tpl.blocks, 'round trip mismatch'
    ax, ay, az = back.anchor
    assert is_log(back.blocks[(ax, ay, az)].split('[')[0]), 'anchor is not a log/stem'
    for (x, y, z), s in back.blocks.items():
        assert 0 <= x < back.size[0] and 0 <= y < back.size[1] and 0 <= z < back.size[2], 'block outside size'
        assert 'air' not in s.split('[')[0], 'air in template'
    assert back.max_reach() <= MAX_REACH, 'reach %d > %d: cannot be placed by a feature' % (back.max_reach(), MAX_REACH)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--save')
    ap.add_argument('--only', default='')
    ap.add_argument('--no-preview', action='store_true')
    args = ap.parse_args()
    sys.stdout.reconfigure(encoding='utf-8')
    with open(os.path.join(HERE, 'manifest.json'), encoding='utf-8') as f:
        manifest = json.load(f)
    world = World(args.save or manifest['save'])
    out_dir = os.path.normpath(os.path.join(HERE, manifest['output']))
    done = []
    for entry in manifest['templates']:
        if args.only not in entry['id']:
            continue
        for tid, voxels in extract(world, entry, manifest['palettes']):
            tpl, low = centre_on_anchor(voxels)
            if low != 0:
                raise SystemExit('%s: lowest log is %d layers above y[0]; fix "y" in the manifest' % (tid, low))
            kept = fix_leaves(tpl.blocks)
            path = os.path.join(out_dir, tid + '.nbt')
            tpl.write(path)
            validate(path, tpl)
            cnt = Counter(s.split('[')[0].split(':')[1] for s in tpl.blocks.values())
            leaves = tpl.count(lambda s: is_leaves(s.split('[')[0]))
            print('%-36s size %2dx%2dx%2d  blocks %4d  reach %2d  leaves %4d (persistent %2d)  %s' % (
                tid, *tpl.size, len(tpl.blocks), tpl.max_reach(), leaves, kept,
                ', '.join('%s %d' % kv for kv in cnt.most_common(4))))
            done.append((tid, path))
    print('%d templates -> %s' % (len(done), out_dir))
    if done and not args.no_preview:
        from preview import contact_sheet
        sheet = os.path.join(HERE, 'preview', 'exported.png')
        contact_sheet([(tid, Template.read(p)) for tid, p in done], sheet)
        print('preview ->', sheet)


if __name__ == '__main__':
    main()
