"""Structure template (.nbt) model, writer and reader. Vanilla StructureTemplate layout, single palette."""
import gzip
import os
import struct

from mcworld import parse_state, read_nbt_file, state_str

DATA_VERSION = 3955  # 1.21.1

# Blocks that are not full cubes. Vanilla stores full cubes first so that supports exist before attachments.
NON_FULL = ('vine', 'eugune', 'carpet', 'cave_vines', 'evergreen_vine', 'glow_lichen', 'fallen_leaves',
            'hyphae', 'grass', 'fern', 'flower', 'petals')


def is_full_cube(state):
    name = state.split('[')[0].split(':', 1)[1]
    return not any(k in name for k in NON_FULL)


class Template:
    """blocks: {(x, y, z): 'ns:name[prop=value,...]'}; (0,0,0) is the min corner, y=0 the first layer above ground."""

    def __init__(self, size, blocks):
        self.size = tuple(int(v) for v in size)
        self.blocks = dict(blocks)

    @property
    def anchor(self):
        """Convention of this project: the anchor (trunk / stem base) is the centre of the bottom face."""
        return self.size[0] // 2, 0, self.size[2] // 2

    def count(self, pred):
        return sum(1 for s in self.blocks.values() if pred(s))

    def max_reach(self):
        ax, _, az = self.anchor
        return max(max(abs(x - ax), abs(z - az)) for (x, _, z) in self.blocks)

    # ---- io ----

    def to_bytes(self):
        states = sorted(set(self.blocks.values()))
        index = {s: i for i, s in enumerate(states)}
        order = sorted(self.blocks, key=lambda p: (not is_full_cube(self.blocks[p]), p[1], p[0], p[2]))
        out = bytearray()
        out += _named(10, '')
        out += _named(3, 'DataVersion') + struct.pack('>i', DATA_VERSION)
        out += _named(9, 'size') + bytes([3]) + struct.pack('>i', 3) + struct.pack('>iii', *self.size)
        out += _named(9, 'palette') + bytes([10]) + struct.pack('>i', len(states))
        for s in states:
            name, props = parse_state(s)
            out += _named(8, 'Name') + _str(name)
            if props:
                out += _named(10, 'Properties')
                for k in sorted(props):
                    out += _named(8, k) + _str(props[k])
                out += b'\x00'
            out += b'\x00'
        out += _named(9, 'blocks') + bytes([10]) + struct.pack('>i', len(order))
        for p in order:
            out += _named(9, 'pos') + bytes([3]) + struct.pack('>i', 3) + struct.pack('>iii', *p)
            out += _named(3, 'state') + struct.pack('>i', index[self.blocks[p]])
            out += b'\x00'
        out += _named(9, 'entities') + bytes([0]) + struct.pack('>i', 0)
        out += b'\x00'
        return bytes(out)

    def write(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        # mtime=0 keeps the output byte-identical across runs, so re-exports do not dirty git.
        with open(path, 'wb') as f:
            with gzip.GzipFile(filename='', mode='wb', fileobj=f, mtime=0) as g:
                g.write(self.to_bytes())

    @staticmethod
    def read(path):
        root = read_nbt_file(path)
        pal = [state_str(p['Name'], p.get('Properties')) for p in root['palette']]
        blocks = {tuple(b['pos']): pal[b['state']] for b in root['blocks']}
        return Template(root['size'], blocks)


def _str(s):
    b = s.encode('utf-8')
    return struct.pack('>H', len(b)) + b


def _named(tag, name):
    return bytes([tag]) + _str(name)
