# The Genbu ravine cave (`content/worldgen/feature/ravine/`)

The ravine is a cut in the mountain with a flat floor and a domed ceiling, cut into the face of a
cliff. It is the one piece of the ravine that has to be *made* rather than read: the climate grid
gives the ravine a cell, the surface rules give it grass, and neither of them gives it the thing
the biome is named for. `gensokyo_biome/ravine_cave` is that thing.

It is a feature, not a density function, because the shape is not a field. A ravine is a room cut
into a particular wall on a particular hillside, and the wall has to be *found* before anything can
be cut.

---

## 1. The shape

A quarter of an ellipsoid, with its two flat faces on the floor and on the cliff.

```
        the cliff face
             |
    .--------+--------.
   /         |         \
  |          |          |   the mouth, in the face
  |          |          |   flat along the bottom, arched over the top
  '----------|----------'
             |
             |____ the floor, flat
```

The ellipsoid has three semi-axes: `a` along the face, `b` up and `c` into the rock. Its centre is
on the cliff face at floor level, and the quarter that is removed is the one **above** the floor and
**inside** the rock. What is left of the ellipsoid is the dome.

The consequences that matter:

- **the floor is flat**, because it is a cut, not a curve. It is the same plane the ground outside
  the cliff is on, so the mouth can be walked into.
- **the ceiling is a dome**, because it is the ellipsoid's own surface. The dome's apex is `b`
  above the floor and it comes down to meet the floor at the edge of the cut, so the chamber closes
  to a point as it goes into the rock.
- **the opening is half an ellipse** — flat along the bottom, arched over the top — because the
  cliff face cuts the ellipsoid through its centre.

The half-ellipse mouth is what makes the cave read as a cave and not as a doorway: the floor runs
straight out of the rock and the roof curves down to it.

## 2. Finding the cliff

The cliff is found from the heightmap alone, with a **box**: three slices across the fall and seven
along it, read both ways round.

```
   the box, seven along the face and three across it

   slice 1  ────────────────────────
   slice 2  ────────────────────────
   slice 3  ────────────────────────
```

The three slices are the wall's two sides and the step between them. A wall is there when one of
the outer slices stands clear above the other:

```
   mean(slice 1) > 3 + mean(slice 3)      slice 1 is the inside, slice 3 the outside
   mean(slice 3) > 3 + mean(slice 1)      the other way round
```

The comparison is between the **means** of the two slices. The mean is the right measure and the
extremes are the wrong one: a slice of seven columns is one column of noise away from failing a
`min` against a `max`, and on this terrain that is most of them. The mean asks what the wall does
on average, which is what a wall is.

The `3` is the clearance the chamber needs under the cliff top.

Two boxes are read, three across `x` and seven along `z`, and the same three across `z` and seven
along `x`, so a wall is found whichever way it faces.

**Where the wall's face stands.** The middle slice is the step between the two sides, so the face
is the bigger of the two steps into it, and the lower column of that pair is the outside. The rock
lies the other way from there, and that is the direction the chamber is cut.

**The cost.** A box is 42 heightmap reads, and there are six positions a chunk to read one at, so
the scan is 252 reads a chunk — a fortieth of the cost of reading a box at every column of the
chunk, and the same wall is found either way.

## 3. The fit

The heightmap says where the wall is and how tall it is. It cannot say whether there is rock behind
it, so the fit is in two halves.

**From the heightmap.** The column outside the wall gives the **floor level** — the cave's floor
is the ground outside the cliff, so the mouth is level with it and can be walked into. The column
behind it gives the **cliff top**, and the **height of the cliff** between them is the ceiling on
the dome: the dome has to fit under the cliff top with a roof of rock over it, so the cliff decides
the height and the config's is only a limit on it.

**From the blocks.** The chamber is checked before it is cut:

- the block under the floor has to be solid, or the chamber is a hole in the cliff with nothing to
  stand on;
- the block over the dome has to be solid, or the chamber opens on the hill above.

A site that fails either is left alone rather than half cut, which is the difference between a
cave and a breach.

## 4. The cut

For every column of the chamber's footprint — the ellipse of `u` and `d` — the dome's height is
the ellipsoid's own:

```
   (u/a)² + (d/c)² + ((y - floor) / b)² <= 1
```

and the blocks under it are replaced with cave air. The floor's own block is left, and laid in
basalt.

The rim is left standing: the blocks just outside the ellipsoid are rock, and they are what makes
the mouth an arch rather than a hole.

## 5. The floor

The ravine's floor treatment — basalt plates and the pools over them — is the ravine's on the
surface, and the chamber carries it into the rock. The chamber's floor is basalt and the middle of
it is flooded, the pool covering the inner part of the ellipse, where the dome is deepest.

This is why the cave lays the basalt itself rather than leaving it to the ravine's own plates:
`ravine_basalt` is placed on the **no-leaves heightmap**, and the heightmap of a column behind a
cliff is the cliff's top. The plates would go on the hill and not in the cave. The chamber is
therefore the ravine's floor, laid by the chamber.

## 6. Where it runs

The chamber is the ravine's **first** feature in the vegetal decoration step:

1. the chamber is cut, and its floor laid in basalt;
2. the plates, the dripleaves on them, the pools cut into the steps, the moss and the stones;
3. the swamp's trees and grass;
4. the lush caves' growth.

The order is the ravine's own rule — a pool of basalt under a patch of grass that grew first is
grass standing on water — with the chamber put in front of all of it, because everything else is
laid on the ground it makes.

The chamber is the only thing in the ravine that is not placed on a heightmap, so it is the only
one that is not a surface feature: it is in the rock, under the cliff, and the growth that hangs
off a cave ceiling has to be given a cave ceiling to hang off.

## 7. What was measured

`tools/slopes.py` reads the same heightmap the feature reads, with the same box and the same rule,
at the same six positions a chunk, on the blocks of forty ravine chunks at seed
−1838080655014084215:

```
python3 tools/slopes.py gensokyolegacy:gensokyo --seed -1838080655014084215 \
    --scan 128 --samples 40 --biome gensokyolegacy:genbu_ravine
```

| | |
|---|---|
| positions read | 240 |
| a wall found | 70 (29.2%) |
| the chamber cut | 7 (2.9% of the positions) |
| a chunk of the ravine carries | 0.17 chambers |

And why the rest were not:

| | |
|---|---|
| no wall | 170 |
| no room under the cliff top | 37 |
| no roof over the dome | 17 |
| no floor under it | 9 |

The wall is found often enough — three positions in ten — and what costs the chambers is the fit:
the cliff has to be tall enough at the column the face stands on for the dome to go under it, and
there has to be rock above the dome and below the floor. A cave that opens on the hill above is
worse than no cave.

**Six positions a chunk is the count to start from.** At 2.9% of positions, it is the count and not
the rule that sets how many chambers the ravine has.

## 8. What is not done

- **The growth.** The chamber's walls and ceiling are where the ravine's lush caves' growth belongs,
  and none of it reaches them: `lush_caves_vegetation` and its kin are placed by a random height
  and a twelve-block scan for the cave floor, which finds a chamber this size by luck. They want a
  placement that scans the column for the roofed air under the cliff.
- **The plates.** `ravine_basalt` still lays plates on the ravine's *surface*, which is the
  mountain's slope, and the chamber's floor is laid by the chamber. The two never meet.
