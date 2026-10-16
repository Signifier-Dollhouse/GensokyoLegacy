#!/usr/bin/env python3
"""Plot the biome bands the climate grid cuts, by continentalness and height, and derive their bounds.

The grid picks a biome per 4x4x4 cell by nearest-cell lookup over seven climate axes. Two of them
do the underground classification: `depth` and `continentalness`. Depth is 3d and reads roughly 0
at a column's own surface, falling one unit per 128 blocks; continentalness fixes the height band.
So the grid draws a curve in that plane, and where it lands in y is a property of the offset spline
rather than of the numbers written in the source. This measures that curve instead of assuming it.

Everything here is the real selection rule, not a drawing of it. `Climate.ParameterPoint.fitness`
is the sum of the squared distance outside each axis span, so a query lands in a *box* around a
climate cell and the boundaries are hyperbolae, not straight lines. Two things follow and both are
drawn: a cell wins outright inside its own box, and between boxes the nearest one wins on the
quadratic. Saturated fill is the first, pale tint the second — the tint is not slack in the numbers,
it is where the bounds are actually being interpolated rather than claimed.

`--bounds` gives one climate region and is the default:

    D_S   depth upper bound for the surface biomes, everywhere
    D_C0  depth lower bound for the mountain cave
    D_C1  depth upper bound for the mountain cave
    D_U   depth lower bound for the underground biomes
    C_LO  continentalness lower bound for the mountain cave
    C_HI  continentalness upper bound for the underground biomes

`--slices N` instead derives N cave regions from the measured curve, each thresholded at the
largest depth at the cap inside it. Equal depth spans minimise how far the cap can stray, and the
floor over N slices is the total depth range divided by N.

Outputs `<dimension>_cave_bands.png` and `<dimension>_cave_bands.txt`. The txt is the table to
quote in the design doc. The tool **exits non-zero** if any column's underground reaches above
`--cap-y`, so it can gate the change it describes.

    python3 tools/cave_bands.py gensokyolegacy:gensokyo --seed -1838080655014084215 --origin=-384,-384
"""
import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import terrain as T
from terrain import (Bad, DataPack, Dimension, INK, MOD_PACK, Panel, Canvas, find_dimension,
                     find_vanilla_pack, quantize, ticks, to_float)

CAP_Y = 50

# the depth at which the router's large-cave gate opens: `clamp(gain * depth - floor, 0, 1)` leaves
# zero at or below floor/gain. The cave's top edge is meant to land on it, so the biome covers
# exactly the volume the halls are cut from.
CAVE_GATE = 0.25 / 3.0

# the lowland handover, which is what the surface and underground bounds add up to: the cut between
# two spans sits at the midpoint of the two, so D_S + D_U = 1.0 leaves it exactly at 0.5.
HANDOVER = 0.5

# the continentalness where the mountain bands start, the edge between the upper ground and the
# mountain. Below it nothing is added and nothing moves.
MOUNTAIN_FROM = 0.118

# (surface depth upper, cave depth lower, cave depth upper, underground depth lower,
#  cave continentalness lower, underground continentalness upper)
#
# D_S and D_U sum to 1.0 so the lowland cut stays at HANDOVER, and D_C0 sits on D_S so the surface/cave
# cut lands on CAVE_GATE. D_C1 and C_HI are the pair the cap decides: pushing either further lets the
# underground creep above y 50, and they are worth very little — the cave loses about 12 blocks of mean
# thickness going from (1.30, 0.60) to these, and the looser pair breached the cap in five columns.
BOUNDS = (0.1, 0.3, 0.53, 0.53, 0.5, 0.1, 1.0)

# the cave's continentalness ceiling. Closed rather than open, so past it the cave pays the
# same squared continentalness penalty as the surface and underground cells and competes with
# them on equal terms, instead of owning every column above it outright.
CAVE_CONT_HIGH = 1.0

DEFAULT_SLICES = 4
BUCKET = 0.02
DEPTH_LOW, DEPTH_HIGH = -1.5, 1.5
# only the mountain carries the offset to push depth that far: 2.02 at the floor under the
# summit against 1.05 under the water, so its underground row needs its own ceiling
HELL_HIGH = 2.0  # the climate codec refuses a parameter outside [-2, 2]

SURFACE_FILL = (150, 196, 140)
CAVE_FILL = (118, 162, 200)
UNDER_FILL = (198, 138, 138)
LINE = {"surface": (40, 92, 40), "cave": (46, 116, 196), "under": (196, 62, 62)}
MARK = (28, 30, 34)


def tint(colour, amount=0.62):
    """A pale version of the same colour, for the spans no cell actually claims."""
    return tuple(int(value + (255 - value) * amount) for value in colour)


def median(values):
    ordered = sorted(values)
    return ordered[len(ordered) // 2]


def height_of(depth, depth_at_cap, cap_y):
    """`depth = d50 - (y - cap_y) / 128`, inverted. Depth falls one unit per 128 blocks."""
    return cap_y + 128.0 * (depth_at_cap - depth)


def read_regions(dimension_json):
    """The continentalness spans of the *surface* rows only, in order.

    Read out of the json rather than repeated here, so retuning `REGION_EDGES` cannot leave this
    tool drawing bands the game does not have. Only the shallow rows: the underground and cave rows
    carry continentalness spans too, and taking those as regions invents a surface cell over the
    cave's own continentalness, which then competes with the cave for the rock between the two depth
    bounds. The surface rows are the ones starting at the lowest depth of all.
    """
    biomes = dimension_json["generator"]["biome_source"]["biomes"]
    shallow = min(entry["parameters"]["depth"][0] for entry in biomes)
    spans = sorted({tuple(entry["parameters"]["continentalness"]) for entry in biomes
                    if entry["parameters"]["depth"][0] == shallow})
    return [span for span in spans if span[1] > span[0]]


def cells_from_bounds(bounds, regions):
    """The cells the underground classification rests on: one surface cell per band, one underground
    row, and the cave region.

    The underground is a single continentalness row rather than one per band, because which of the four
    underground biomes a column gets depends on the climate above it rather than on where the ground
    sits. Temperature and humidity pick *which* underground biome but never which family, so the four
    cells collapse to one here without moving a boundary.
    """
    d_surface, d_c0, d_c1, d_u, c_lo, c_hi = bounds[:6]
    cave_high = bounds[6] if len(bounds) > 6 else CAVE_CONT_HIGH
    d_hell = bounds[7] if len(bounds) > 7 else None
    # the mountain underground row's own continentalness floor, which is what actually moves the
    # crossover against the low-continentalness row; the depth bound provably does not
    hell_cont = bounds[8] if len(bounds) > 8 else c_lo
    cells = [("surface", low, high, DEPTH_LOW, d_surface) for low, high in regions]
    cells.append(("under", -1.0, c_hi, d_u, DEPTH_HIGH))
    if d_hell is not None:
        # a second underground row over the cave's own continentalness range, which is what sets the
        # cave/underground split there: the first row's continentalness penalty is too far away to do it
        cells.append(("under", hell_cont, cave_high, d_hell, HELL_HIGH))
    # the cave's continentalness span is closed on both sides, so past its ceiling it pays the same
    # squared penalty as anything else and competes on equal terms instead of owning every column
    cells.append(("cave", c_lo, cave_high, d_c0, d_c1))
    return cells


def cells_from_slices(edges, thresholds, regions):
    """The bands as they stand now: a flat lowland split, and N cave regions above the gate.

    A band below the mountain keeps the flat `HANDOVER` split. Above it the surface cell stops at
    the cave gate and the underground threshold varies per slice, so the cave has to be inserted
    slice by slice to keep the two ends of it tiling.
    """
    cells = []
    mountain = []
    for low, high in regions:
        if high <= MOUNTAIN_FROM:
            cells.append(("surface", low, high, DEPTH_LOW, HANDOVER))
            cells.append(("under", low, high, HANDOVER, DEPTH_HIGH))
        else:
            mountain.append((low, high))
    for index in range(len(edges) - 1):
        edge, upper = edges[index], edges[index + 1]
        for low, high in mountain:
            span_low, span_high = max(edge, low), min(upper, high)
            if span_high <= span_low:
                continue
            cells.append(("surface", span_low, span_high, DEPTH_LOW, CAVE_GATE))
            cells.append(("cave", span_low, span_high, CAVE_GATE, thresholds[index]))
            cells.append(("under", span_low, span_high, thresholds[index], DEPTH_HIGH))
    return cells


def prepare(cells, cont):
    """One column's worth of work: each cell's continentalness cost, which depth cannot move."""
    prepared = []
    for name, c_low, c_high, d_low, d_high in cells:
        cost = 0.0
        if cont < c_low:
            cost += (c_low - cont) ** 2
        elif cont > c_high:
            cost += (cont - c_high) ** 2
        prepared.append((name, cost, d_low, d_high))
    return prepared


def column_counts(prepared, step=0.002, low=-1.4, high=1.4):
    """Per family, how many depths it wins and how many of those it wins *inside* its box.

    Counted per depth rather than per family: a column passes through at most three families, so
    counting bands would weight a one-block band the same as a two-hundred-block one and report a
    claimed share that has nothing to do with the column's volume.
    """
    counts = {"surface": [0, 0], "cave": [0, 0], "under": [0, 0]}
    depth = low
    while depth <= high + 1e-9:
        name_won, claimed = classify(prepared, depth)
        counts[name_won][0] += 1
        counts[name_won][1] += 1 if claimed else 0
        depth += step
    return counts


def column_bands(prepared, step=0.002, low=-1.4, high=1.4):
    """The families this column passes through, and where each first wins.

    The boundary is the depth at which the winner changes hands, which is not the midpoint of the
    two spans: a continentalness penalty on one of them moves it. Every caller wants this, so it is
    computed once here rather than three times with three sets of off-by-one list indexing.
    """
    bands = []
    previous = None
    depth = low
    while depth <= high + 1e-9:
        name_won, claimed = classify(prepared, depth)
        if name_won != previous:
            bands.append([name_won, depth, claimed])
            previous = name_won
        depth += step
    return bands


def classify(prepared, depth):
    """The winning family and whether the query sits inside that cell's box.

    A zero total means the query is inside every span it was measured against, so the cell was
    claimed outright. Anything else lost on the squared falloff, which is the interpolation.
    """
    best_name, best_fit = None, None
    for name, cont_cost, d_low, d_high in prepared:
        cost = cont_cost
        if depth < d_low:
            cost += (d_low - depth) ** 2
        elif depth > d_high:
            cost += (depth - d_high) ** 2
        if best_fit is None or cost < best_fit:
            best_name, best_fit = name, cost
    return best_name, best_fit == 0.0


# --------------------------------------------------------------------------------------
# sampling
# --------------------------------------------------------------------------------------

def sample(dimension, size, origin):
    """One pass over the map: per continentalness bucket, the median surface and depth at the cap."""
    def point(x, y, z):
        cell_x = ((x >> 2) << 2) + 2
        cell_z = ((z >> 2) << 2) + 2
        cell_y = ((y >> 2) << 2) + 2
        dimension.column.move_to(cell_x, cell_z)
        return tuple(quantize(to_float(function(x, y, z, dimension.column))) for function in dimension.sampler)

    buckets = {}
    for chunk_z in range(origin[1], origin[1] + size):
        for chunk_x in range(origin[0], origin[0] + size):
            x = chunk_x * 16 + 8
            z = chunk_z * 16 + 8
            dimension.column.move_to(x, z)
            height = T.surface_height(dimension.density, dimension.min_y, dimension.max_y, x, z, dimension.column)
            if height is None:
                continue
            cont = point(x, height, z)[2] / 10000.0
            key = round(round(cont / BUCKET) * BUCKET, 4)
            bucket = buckets.setdefault(key, {"cont": [], "surface": [], "depth": []})
            bucket["cont"].append(cont)
            bucket["surface"].append(height)
            bucket["depth"].append(point(x, CAP_Y, z)[4] / 10000.0)
    return buckets


def build_slices(buckets, count):
    """Continentalness edges that give every slice an equal span of depth at the cap.

    Returns `(edges, thresholds)`: the slice is `edges[i] .. edges[i + 1]`, and its threshold is
    the largest depth at the cap in it, never below `HANDOVER`. Taking the largest is what keeps
    the cap at or below `CAP_Y` — the line is `cap_y + 128 * (d50 - threshold)`, so a threshold at
    or above every `d50` in its slice can only put it at or below.
    """
    keys = sorted(k for k in buckets if k >= MOUNTAIN_FROM)
    if not keys:
        raise Bad("no sampled column reaches continentalness %.3f" % MOUNTAIN_FROM)
    depth = {k: median(buckets[k]["depth"]) for k in keys}

    low, high = depth[keys[0]], depth[keys[-1]]
    edges = [keys[0]]
    for step in range(1, count):
        target = low + (high - low) * step / count
        crossed = next((k for k in keys if depth[k] >= target), keys[-1])
        if crossed > edges[-1]:
            edges.append(crossed)
    edges.append(float("inf"))

    thresholds = []
    for index in range(len(edges) - 1):
        inside = [depth[k] for k in keys if edges[index] <= k < edges[index + 1]]
        thresholds.append(max(HANDOVER, max(inside)) if inside else HANDOVER)
    return edges, thresholds


def cells_for(buckets, args):
    """The cell table for whichever mode was asked for, with the slice edges either way."""
    regions = read_regions(json.load(open(args.dimension_json)))
    if args.slices:
        edges, thresholds = build_slices(buckets, args.slices)
        return cells_from_slices(edges, thresholds, regions), regions, edges, thresholds
    bounds = args.bounds or BOUNDS
    return cells_from_bounds(bounds, regions), regions, [bounds[4], float("inf")], [bounds[3]]


def write_config(path, bounds, regions, name, seed, size, origin, cap_y, slices, argv):
    """The configuration on its own, so a graph can be read without re-deriving it."""
    d_s, d_c0, d_c1, d_u, c_lo, c_hi = bounds[:6]
    cave_hi = bounds[6] if len(bounds) > 6 else CAVE_CONT_HIGH
    lines = ["cave band configuration: %s" % name, "",
             "  mode            %s" % ("%d cave regions" % slices if slices else "one cave region"),
             "  cave gate       depth %.4f   (CAVE_GATE, the depth DF_LARGE_CAVES opens at)" % CAVE_GATE,
             "  mountain from   continentalness %.3f   (the upper ground / mountain edge)" % MOUNTAIN_FROM,
             "  cap             y %d   the underground must not reach above it" % cap_y,
             ""]
    if slices:
        lines.append("  slices are derived from the measured curve; run the tool for their thresholds")
    else:
        lines.extend([
            "  bounds, in the order D_S D_C0 D_C1 D_U C_LO C_HI C_CAVE_HI",
            "    %-8s %.3f   depth upper bound for the surface biomes, every band"
            % ("D_S", d_s),
            "    %-8s %.3f   depth lower bound for the mountain cave" % ("D_C0", d_c0),
            "    %-8s %.3f   depth upper bound for the mountain cave" % ("D_C1", d_c1),
            "    %-8s %.3f   depth lower bound for the underground biomes" % ("D_U", d_u),
            "    %-8s %.3f   continentalness lower bound for the mountain cave" % ("C_LO", c_lo),
            "    %-8s %.3f   continentalness upper bound for the underground biomes" % ("C_HI", c_hi),
            "    %-8s %.3f   continentalness upper bound for the mountain cave" % ("C_CAVE_HI", cave_hi),
            "",
            "  derived, and these are what the game actually draws",
            "    surface / cave cut        depth %.4f   (midpoint of D_S and D_C0)"
            % ((d_s + d_c0) / 2),
            "    cave / underground cut   depth %.4f   (midpoint of D_C1 and D_U)"
            % ((d_c1 + d_u) / 2),
            "    lowland surface / underground cut   depth %.4f   (midpoint of D_S and D_U)"
            % ((d_s + d_u) / 2),
            "    D_S + D_U = %.3f, so the lowland cut is at %.2f; the shipped lowland split is %.2f"
            % (d_s + d_u, (d_s + d_u) / 2, HANDOVER),
        ])
        if abs(d_c0 - d_s) < 1e-9:
            lines.append("    D_C0 sits on D_S, so the surface and cave tile with no interpolation")
        if abs(d_c1 - d_u) < 1e-9:
            lines.append("    D_C1 sits on D_U, so the cave and underground tile with no interpolation")
    lines.extend(["",
                  "  continentalness bands, read out of the generated biome list",
                  "    " + "  ".join("%.3f..%.3f" % span for span in regions),
                  "",
                  "  sample          %d x %d chunks from %d,%d, seed %d" % (size, size, origin[0], origin[1], seed),
                  "  reproduce       python3 tools/cave_bands.py %s" % " ".join(argv)])
    with open(path, "w") as handle:
        handle.write("\n".join(lines) + "\n")


def edges_label(edges, index):
    low = edges[index]
    if index + 1 >= len(edges) or edges[index + 1] == float("inf"):
        return "%.3f+" % low
    return "%.3f-%.3f" % (low, edges[index + 1])


# --------------------------------------------------------------------------------------
# the plot
# --------------------------------------------------------------------------------------

def plot(path, buckets, cells, regions, edges, thresholds, name, seed, size, origin, cap_y, slices, bounds):
    canvas = Canvas(1080, 880)
    canvas.text(24, 18, "BIOME BANDS BY CONTINENTALNESS AND HEIGHT", INK, 2)
    canvas.text(24, 44, "%s  seed %d  %d x %d chunk sample from %d,%d"
                % (name, seed, size, size, origin[0], origin[1]), INK)
    canvas.text(24, 60, "median column per %.2f continentalness bucket, cap y %d, %s"
                % (BUCKET, cap_y, "%d cave regions" % slices if slices else "one cave region"), INK)

    keys = sorted(buckets)
    surface_top = max(median(buckets[k]["surface"]) for k in keys)
    deep = min(height_of((BOUNDS[2] + BOUNDS[3]) / 2, median(buckets[k]["depth"]), cap_y) for k in keys)
    y_range = (min(-64, deep - 12), surface_top + 16)
    x_high = max(median(buckets[k]["cont"]) for k in keys)
    panel = Panel(canvas, (96, 100, 700, 660), (-1.05, x_high + 0.06), y_range, "")
    y_ticks = ticks(y_range[0], y_range[1], 32)
    x_ticks = ticks(-1.0, x_high, 0.25)
    panel.grid(x_ticks, y_ticks, ["%g" % value for value in x_ticks], [str(v) for v in y_ticks])

    fills = {"surface": SURFACE_FILL, "cave": CAVE_FILL, "under": UNDER_FILL}
    pitch = panel.px(BUCKET) - panel.px(0.0)
    span = y_range[1] - y_range[0]

    # one pass per column, one row per pixel: classify, then fill saturated or pale
    edge = panel.px(median(buckets[keys[0]]["cont"])) - pitch // 2
    claimed_share = {"surface": [0, 0], "cave": [0, 0], "under": [0, 0]}
    surface_line, gate_line, cut_line = [], [], []
    for index, key in enumerate(keys):
        bucket = buckets[key]
        cont = median(bucket["cont"])
        d50 = median(bucket["depth"])
        prepared = prepare(cells, cont)
        x = edge
        edge = panel.px(median(buckets[keys[index + 1]]["cont"])) if index + 1 < len(keys) \
            else panel.px(cont) + pitch // 2
        width = edge - x
        first_seen = {}
        previous = None
        for row in range(panel.y, panel.y + panel.height):
            y = y_range[1] - (row - panel.y) / panel.height * span
            depth = d50 - (y - cap_y) / 128.0
            name_won, claimed = classify(prepared, depth)
            if name_won != previous:
                # the boundary is where the family actually changes hands, which is not the midpoint
                # of the two spans: the continentalness penalty on the underground moves it
                first_seen[name_won] = depth
                previous = name_won
            colour = fills[name_won] if claimed else tint(fills[name_won])
            canvas.rect(x, row, width, 1, colour)
            counted = claimed_share[name_won]
            counted[0] += 1
            counted[1] += 1 if claimed else 0
        surface_line.append(panel.point(cont, median(bucket["surface"])))
        gate_line.append(panel.point(cont, height_of(CAVE_GATE, d50, cap_y)))
        cut_line.append(panel.point(cont, height_of(first_seen.get("under", DEPTH_HIGH), d50, cap_y)))

    canvas.polyline(surface_line, LINE["surface"], 2)
    canvas.polyline(gate_line, LINE["cave"], 1)
    canvas.polyline(cut_line, LINE["under"], 2)

    y_cap = panel.py(cap_y)
    for x in range(panel.x, panel.x + panel.width):
        if (x // 6) % 2 == 0:
            canvas.rect(x, y_cap, 1, 1, MARK)
    canvas.text(panel.x + 8, y_cap - 16, "CAP Y %d" % cap_y, MARK)

    x_edge = panel.px(MOUNTAIN_FROM)
    for y in range(panel.y, panel.y + panel.height):
        if (y // 6) % 2 == 0:
            canvas.rect(x_edge, y, 1, 1, MARK)
    canvas.text(x_edge + 6, panel.y + 6, "MOUNTAIN FROM %.3f" % MOUNTAIN_FROM, MARK)

    left = panel.x + panel.width + 20
    y = panel.y + 40
    for name_won, label in (("surface", "SURFACE BIOME"), ("cave", "MOUNTAIN CAVE"),
                            ("under", "UNDERGROUND BIOMES")):
        claimed = claimed_share[name_won][1] / max(1, claimed_share[name_won][0])
        canvas.rect(left, y, 14, 6, fills[name_won])
        canvas.rect(left, y + 7, 14, 6, tint(fills[name_won]))
        canvas.text(left + 21, y + 2, label, INK)
        canvas.text(left + 21, y + 13, "%.0f%% CLAIMED" % (100 * claimed), INK)
        y += 34
    canvas.text(left, y + 6, "TOP ROW CLAIMED, BOTTOM TINTED", INK)
    canvas.text(left, y + 22, "TINT IS THE QUADRATIC FALLOFF,", INK)
    canvas.text(left, y + 38, "NOT A GAP IN THE DATA", INK)
    y += 66

    if not slices:
        d_s, d_c0, d_c1, d_u, c_lo, c_hi = bounds[:6]
        cave_hi = bounds[6] if len(bounds) > 6 else CAVE_CONT_HIGH
        for label, value in (("C_CAVE_HI CAVE CONT HIGH", cave_hi), ("D_S   SURFACE DEPTH HIGH", d_s), ("D_C0  CAVE DEPTH LOW", d_c0),
                             ("D_C1  CAVE DEPTH HIGH", d_c1), ("D_U   UNDERGROUND DEPTH LOW", d_u),
                             ("C_LO  CAVE CONT LOW", c_lo), ("C_HI  UNDERGROUND CONT HIGH", c_hi)):
            canvas.text(left, y, "%-26s %.2f" % (label, value), INK)
            y += 15
        same = abs((d_s + d_u) / 2 - HANDOVER) < 1e-6
        canvas.text(left, y + 8, "LOWLAND CUT AT DEPTH %.2f," % ((d_s + d_u) / 2), INK)
        canvas.text(left, y + 23, "SHIPPED SPLIT IS %.2f%s"
                    % (HANDOVER, "" if same else "   MOVED"), INK)
    else:
        canvas.text(left, y, "SLICE  CONTINENTALNESS  DEPTH", INK)
        y += 18
        for index, threshold in enumerate(thresholds):
            canvas.text(left, y, "%-6s%-16s%.2f" % (index + 1, edges_label(edges, index), threshold), INK)
            y += 16

    canvas.text(24, canvas.height - 44, "SOLID FILL IS A CELL THAT CLAIMS THE COLUMN.", INK)
    canvas.text(24, canvas.height - 28, "PALE FILL LOST ON THE QUADRATIC FALLOFF BETWEEN CELLS.", INK)
    canvas.write(path)


# --------------------------------------------------------------------------------------
# the report
# --------------------------------------------------------------------------------------

def report(path, buckets, cells, regions, edges, thresholds, name, seed, size, origin, cap_y, mode, bounds,
           target_y=CAP_Y):
    lines = ["cave band report: %s" % name,
             "%d x %d chunk sample from %d,%d, seed %d, median column per %.2f continentalness bucket"
             % (size, size, origin[0], origin[1], seed, BUCKET),
             "cave gate depth %.4f, lowland handover %.2f, cap y %d, mountain from continentalness %.3f"
             % (CAVE_GATE, HANDOVER, cap_y, MOUNTAIN_FROM),
             "mode %s, %d cells, %d continentalness bands"
             % (mode, len(cells), len(regions)),
             ""]
    if not mode.startswith("slices"):
        d_s, d_c0, d_c1, d_u, c_lo, c_hi = bounds[:6]
        cave_hi = bounds[6] if len(bounds) > 6 else CAVE_CONT_HIGH
        lines.append("  D_S %.2f + D_U %.2f = %.2f, so the lowland cut stays at %.2f"
                     % (d_s, d_u, d_s + d_u, (d_s + d_u) / 2))
        lines.append("  effective surface/cave cut at depth %.3f, cave/underground cut at %.3f"
                     % ((d_s + d_c0) / 2, (d_c1 + d_u) / 2))
        lines.append("  cave continentalness from %.3f, underground to %.3f" % (c_lo, c_hi))
    else:
        lines.append("  slices, continentalness .. depth threshold")
        for index, threshold in enumerate(thresholds):
            lines.append("    %-16s d2 %.2f" % (edges_label(edges, index), threshold))

    counts = {"surface": [0, 0], "cave": [0, 0], "under": [0, 0]}
    under_top, thick, lost, mountain_top = [], [], [], []
    for key in sorted(buckets):
        bucket = buckets[key]
        cont = median(bucket["cont"])
        d50 = median(bucket["depth"])
        prepared = prepare(cells, cont)
        for name_won, (total, claimed) in column_counts(prepared).items():
            counts[name_won][0] += total
            counts[name_won][1] += claimed
        bands = {name_won: start for name_won, start, claimed in column_bands(prepared)}
        if "cave" in bands and "under" in bands:
            # the boundary between the two, which is where the cave stops and the deep starts
            mountain_top.append((cont, height_of(bands["under"], d50, cap_y)))
        if "under" in bands:
            under_top.append(height_of(bands["under"], d50, cap_y))
        else:
            lost.append(cont)
        if "cave" in bands:
            bottom = bands.get("under", DEPTH_HIGH)
            thick.append(height_of(bands["cave"], d50, cap_y) - height_of(bottom, d50, cap_y))

    lines.extend(["",
                  "  claimed by a cell, versus won on the falloff",
                  "    surface %5.1f%%   cave %5.1f%%   underground %5.1f%%"
                  % tuple(100 * counts[k][1] / max(1, counts[k][0]) for k in
                          ("surface", "cave", "under"))])
    if thick:
        lines.append("  cave band thickness    %.0f .. %.0f blocks" % (min(thick), max(thick)))
    if lost:
        lines.append("  no underground below cont %.2f..%.2f (%d columns)"
                     % (min(lost), max(lost), len(lost)))
    # The target is the *average* boundary, not a hard ceiling: the cave and the underground do not share
    # any continentalness, so every cave/underground boundary is interpolated and a mean is the honest
    # thing to report. Where the cave exists is the range the mean is taken over.
    mountain = [y for cont, y in mountain_top if cont >= bounds[4]]
    if mountain:
        mean = sum(mountain) / len(mountain)
        lines.append("  cave/underground boundary over the mountain  y %.0f .. %.0f, mean %.0f"
                     % (min(mountain), max(mountain), mean))
        lines.append("    target mean y %.0f   off by %+.0f blocks   %d columns"
                     % (target_y, mean - target_y, len(mountain)))
    breach = [y for y in under_top if y > cap_y + 0.5]
    lines.append("  underground reaches y %.0f at highest, cap y %d   %s"
                 % (max(under_top) if under_top else float("nan"), cap_y,
                    "OK" if not breach else "BREACH IN %d COLUMNS" % len(breach)))
    with open(path, "w") as handle:
        handle.write("\n".join(lines) + "\n")
    return breach


def print_table(buckets, cells, cap_y):
    print("  cont   surface   cave gate   under top   family             claimed")
    breach = 0
    for key in sorted(buckets):
        bucket = buckets[key]
        cont = median(bucket["cont"])
        if cont < -0.05:
            continue
        d50 = median(bucket["depth"])
        prepared = prepare(cells, cont)
        bands = column_bands(prepared)
        gate = height_of(CAVE_GATE, d50, cap_y)
        under = [start for name_won, start, claimed in bands if name_won == "under"]
        top = height_of(under[0], d50, cap_y) if under else float("nan")
        if under and top > cap_y:
            breach += 1
        print("  %+.2f    %4d       %6.0f    %8.0f    %-18s %s"
              % (cont, median(bucket["surface"]), gate, top,
                 "/".join(name_won for name_won, _, _ in bands),
                 "".join("C" if claimed else "." for _, _, claimed in bands)))
    return breach


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dimension", help="namespace:path of a dimension, or a path to its json")
    parser.add_argument("--seed", type=int, default=0, help="world seed (default: 0)")
    parser.add_argument("--size", type=int, default=128, help="chunks per side of the sample")
    parser.add_argument("--origin", default="0,0", help="chunk coordinates of the sample's near corner")
    parser.add_argument("--slices", type=int, default=0,
                        help="derive N cave regions from the curve instead of using --bounds")
    parser.add_argument("--bounds",
                        help="D_S,D_C0,D_C1,D_U,C_LO,C_HI to use instead of the defaults")
    parser.add_argument("--label", default="",
                        help="suffix the output files, so several configurations can sit side by side")
    parser.add_argument("--cap-y", type=int, default=CAP_Y, help="height the underground biomes must not cross")
    parser.add_argument("--target-y", type=int, default=CAP_Y,
                        help="height the cave/underground boundary should average at, reported against")
    parser.add_argument("--out", default=T.DEFAULT_OUT, help="output directory")
    parser.add_argument("--datapack", action="append", default=[],
                        help="extra datapack root, a directory or a jar (repeatable)")
    parser.add_argument("--vanilla", help="vanilla datapack jar or directory (default: the client-extra jar)")
    args = parser.parse_args()

    if args.slices < 0:
        raise Bad("--slices wants a count, or nothing for the default bounds")
    if args.bounds:
        parts = [part.strip() for part in args.bounds.split(",") if part.strip()]
        if len(parts) not in (6, 7, 8, 9):
            raise Bad("--bounds wants D_S,D_C0,D_C1,D_U,C_LO,C_HI[,C_CAVE_HI[,D_HELL]], got %d values" % len(parts))
        try:
            args.bounds = tuple(float(part) for part in parts)
        except ValueError:
            raise Bad("--bounds wants six numbers, got " + args.bounds)
        for index, (name, low, high) in enumerate((("D_S", -1.5, 1.5), ("D_C0", -1.5, 1.5),
                                                  ("D_C1", -1.5, 1.5), ("D_U", -1.5, 1.5),
                                                  ("C_LO", -1.5, 1.5), ("C_HI", -1.5, 1.5),
                                                  ("C_CAVE_HI", -1.5, 1.5),
                                                  ("D_HELL", -1.5, 3.0),
                                                  ("C_HELL_LO", -1.5, 1.5))[:len(args.bounds)]):
            if not low <= args.bounds[index] <= high:
                raise Bad("--bounds %s is %.3f, outside the climate range %.1f..%.1f"
                          % (name, args.bounds[index], low, high))

    vanilla = args.vanilla or find_vanilla_pack()
    if vanilla is None:
        raise Bad("no vanilla datapack found in %s, pass --vanilla" % T.ARTIFACTS)
    pack = DataPack([os.path.abspath(vanilla)] + [os.path.abspath(path) for path in args.datapack] + [MOD_PACK])
    dimension_json = find_dimension(pack, args.dimension)
    args.dimension_json = args.dimension if os.path.isfile(args.dimension) else None
    if args.dimension_json is None:
        args.dimension_json = os.path.join(MOD_PACK, "data", dimension_json["type"].split(":")[0],
                                           "dimension", dimension_json["type"].split(":")[1] + ".json")

    try:
        origin = tuple(int(part) for part in args.origin.split(","))
    except ValueError:
        raise Bad("--origin wants chunk coordinates as X,Z")

    name = dimension_json.get("type", args.dimension).replace(":", "_")
    dimension = Dimension(pack, dimension_json, args.seed, caves=False, cheese=False)
    buckets = sample(dimension, args.size, origin)
    if not buckets:
        raise Bad("the sample found no surface")

    cells, regions, edges, thresholds = cells_for(buckets, args)
    bounds = args.bounds or BOUNDS
    mode = "slices %d" % args.slices if args.slices else "bounds %s" % (tuple(bounds),)

    os.makedirs(args.out, exist_ok=True)
    suffix = "_" + args.label if args.label else ""
    png = os.path.join(args.out, name + "_cave_bands" + suffix + ".png")
    txt = os.path.join(args.out, name + "_cave_bands" + suffix + ".txt")
    config = os.path.join(args.out, name + "_cave_bands" + suffix + "_config.txt")
    plot(png, buckets, cells, regions, edges, thresholds, name, args.seed, args.size, origin,
         args.cap_y, args.slices, bounds)
    breach = report(txt, buckets, cells, regions, edges, thresholds, name, args.seed, args.size,
                    origin, args.cap_y, mode, bounds, args.target_y)
    write_config(config, bounds, regions, name, args.seed, args.size, origin, args.cap_y,
                 args.slices, [args.dimension, "--seed", str(args.seed), "--origin",
                               ",".join(str(v) for v in origin)]
                 + (["--slices", str(args.slices)] if args.slices else [])
                 + (["--bounds", ",".join("%.3f" % v for v in bounds)] if args.bounds else [])
                 + (["--label", args.label] if args.label else []))

    print_table(buckets, cells, args.cap_y)
    print("\n  %d cells over %d bands" % (len(cells), len(regions)))
    print(png)
    print(txt)
    print(config)
    if breach:
        raise Bad("the underground crosses y %d in %d columns" % (args.cap_y, len(breach)))


if __name__ == "__main__":
    try:
        main()
    except Bad as bad:
        sys.exit("cave_bands: %s" % bad)