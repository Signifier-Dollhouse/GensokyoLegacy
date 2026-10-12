# Party Area — tables, seats and the walk

Everything the party touches — food, seats, tables, guests, the anchor — is found by walking out from the block the player clicked. The walk is the unit of "a party": it covers the connected space the host laid the party out in, and stops at a wall.

## 1. Tags

Two block tags, both **generated** by `GLTagGen.onBlockTagGen`:

```java
/** Tables a party may be laid out on. Our large dining tables only — see food_mods.md §6. */
public static final TagKey<Block> PARTY_TABLE = block("party_table");

/** Seats. A guest needs somewhere to sit; no seats, no party. */
public static final TagKey<Block> PARTY_SEAT = block("party_seat");
```

```java
private static void genPartyTag(RegistrateTagsProvider.IntrinsicImpl<Block> pvd) {
    pvd.addTag(PARTY_TABLE).addTag(LARGE_TABLE);
    pvd.addTag(PARTY_SEAT).addTag(CHAIR).addTag(STOOL);
    for (var wood : YSF_WOODS) {
        pvd.addTag(PARTY_TABLE).addOptional(ResourceLocation.parse("youkaisfeasts:" + wood + "_dining_table"));
        pvd.addTag(PARTY_SEAT).addOptional(ResourceLocation.parse("youkaisfeasts:" + wood + "_dining_chair"));
    }
}
```

`LARGE_TABLE` already contains the 11 `*_large_table` blocks; `CHAIR` the 11 `*_large_chair` plus `wooden_large_chair_scarlet_devil_mansion`; `STOOL` the 11 `*_chair` stools. Nesting those rather than listing them keeps the party tags in step as blocks are added.

Youkai's Feasts' dining tables and chairs go in as optional entries, which the tag loader drops when the mod is absent — that is what keeps it optional rather than a hard dependency, and no Java here names one of its blocks. It uses the same 11 wood names we do, so one loop covers both tags.

**There is no `party_food` tag.** Food is decided entirely in code, not by a tag and not by a datapack — see `isFood` in food.md §1. A `BlockFoodHandler` recognising the block, and finding something worth eating in it, is the whole test; the `party_food` data map only revalues a block that already passed it. There is no third way, and no tag to keep in step.

### 1.1 One trap worth naming: `loc()` is not a parser

```java
public static ResourceLocation loc(String id) {
    return ResourceLocation.fromNamespaceAndPath(MODID, id);   // namespaces a bare path
}
```

`loc()` prepends **our** modid, so `loc("youkaisfeasts:oak_dining_table")` builds `gensokyolegacy:youkaisfeasts:oak_dining_table` — a namespace plus a path containing a colon. Foreign ids must go through `ResourceLocation.parse`.

This cost real time. The symptom was that **any** `addOptional` call in this tag provider made `runData` hang: the Registrate provider never returned, workers went idle, and the main thread sat forever in `CompletableFuture.join` with no error and no stack trace. It was bisectable only by commenting lines out, and two plausible-sounding culprits were blamed first — `addOptional` itself, then `GLDecoBlocks.WoodType.values()` class-initialising mid-provider — before the malformed id turned up. Neither was the cause: `addOptionalElement` only appends to a list, and the wood list is a literal. One bad id, and a silent hang that looks exactly like a deadlock in someone else's code.

## 2. A walk, not a box

```java
public record PartyArea(BlockPos anchor, List<BlockPos> food, List<BlockPos> seats,
        List<BlockPos> tables, int appeal) {

    /** The box each piece of furniture queues around itself: 5 x 3 x 5, centred. */
    private static final int POKE_X = 2, POKE_Y = 1, POKE_Z = 2;
    private static final int MAX_VISITED = 1 << 16;

    public static PartyArea scan(ServerLevel level, BlockPos anchor, int distance) {
        var food = new ArrayList<BlockPos>();
        var seats = new ArrayList<BlockPos>();
        var tables = new ArrayList<BlockPos>();
        int appeal = 0;
        long reach = (long) distance * distance;
        var seen = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(anchor.immutable());
        while (!queue.isEmpty() && seen.size() < MAX_VISITED) {
            var pos = queue.poll();
            if (!seen.add(pos)) continue;
            if (!within(anchor, pos, reach) || !level.isLoaded(pos)) continue;
            var state = level.getBlockState(pos);
            var furniture = false;
            if (state.is(GLTagGen.PARTY_SEAT)) { seats.add(pos); furniture = true; }
            if (state.is(GLTagGen.PARTY_TABLE)) { tables.add(pos); furniture = true; }
            // a chair or table is solid, so its box is what carries the walk past it
            if (furniture) poke(queue, pos);
            if (PartyFoodValue.isFood(level.registryAccess(), state) && isLaidOut(level, pos)) {
                int worth = PartyFoodValue.appeal(level.registryAccess(), state);
                if (worth <= 0 || !PartyFoodValue.result(state).valid()) continue;
                food.add(pos);
                appeal += worth;
            }
            if (!furniture && state.getCollisionShape(level, pos).isEmpty())
                for (var face : FACES) queue.add(pos.relative(face));
        }
        return new PartyArea(anchor.immutable(), food, seats, tables, appeal);
    }
}
```

Four rules, and together they are the whole of what counts as a party's spread:

1. **The walk passes only through space a guest could stand in** — empty collision shape. A wall ends it; a doorway does not. So a party is the *room* it was set in, not the building around it, and a table in a neighbouring hall is a different party.
2. **Furniture extends the walk by a 5×3×5 box**, centred on itself: two out sideways, one up and down. A chair cannot be walked through, only reached, so without this the walk would stop at the first chair and never find the rest of a long table. Because the box's contents are then read and walked *in the ordinary way*, the walk resumes from the air on the guest side of the chair and carries on — a whole row of seating extends the walk along its length without the rule being applied twice. It also reaches furniture the walk could never have stood near: a chair pushed into a nook, a dish tight against a wall.
3. **Food counts on a table or touching one** (`PartyArea.isLaidOut`, shared with `PartyTasks` so a guest can never be handed something the scan would not have counted). On the table is the block below; touching means the four horizontal faces plus up, but **not** down — a dish tucked under a table is not laid out for anyone.
4. **Nothing beyond `partyScanDistance` from the anchor counts**, however it was reached, and nothing in an unloaded chunk is read at all. The scan runs inside one click and must not drag a chunk in to answer a question about a party.

Two more things the walk gets right that a box sweep did not: a food block is solid, so the walk only ever meets one by reading it off a seat's or table's box, which is exactly where laid-out food is; and an unclaimed food block sitting on the floor is never reached at all.

`MAX_VISITED` is a safety valve, not the limit. The 32-block distance cap alone allows the better part of a million blocks of open air — a stall the player cannot see or stop — so the walk stops at 65536 blocks. It only ever bites in a large empty space, where there is by then nothing to find.

`PartyArea` collects tables alongside food and seats, so nothing needs a second scan of its own.

### 2.1 What a refusal still knows

`PartySend.Outcome` carries the scanned `area` even when `plan` is null. That is the interesting half of a refusal: the host wants to see what the walk did and did not take around a table that was refused, and re-scanning it would be a second walk over the same blocks. The wand renders the area whether or not the plan succeeded.

## 3. Centre, and the chunk the party belongs to

The party is owned by the chunk containing the **centre of the food**, not the chunk of the block that was clicked. A spread laid across a chunk border puts its centre in one specific chunk, and that chunk owns the party.

```java
/** The mean of the food positions; the chunk containing it owns the party. */
public static BlockPos center(List<BlockPos> food) {
    long x = 0, y = 0, z = 0;
    for (var p : food) { x += p.getX(); y += p.getY(); z += p.getZ(); }
    int n = food.size();
    return new BlockPos((int) Math.floorDiv(x, n), (int) Math.floorDiv(y, n), (int) Math.floorDiv(z, n));
}
```

`floorDiv` rather than `/` so a negative-coordinate spread does not bias the centre toward zero. The mean, not the midpoint of the bounding box, because a spread of one cake and one far-away pie should centre nearer the cake.

At `partyScanDistance = 32` the reachable area spans at most 5×5 chunks, so the set of chunks a party can possibly touch is small and bounded.

## 4. One party per area, enforced by chunk scan

A second player must not be able to send a letter at a table that is already hosting, and a spread straddling a border must not produce two independent parties over one set of tables. Both are caught by scanning every chunk the spread touches and rejecting if any of them already holds a live party.

```java
/** Every chunk the food touches. A party may not overlap another party's. */
public static ChunkPosRange touchedChunks(List<BlockPos> food) {
    if (food.isEmpty()) return null;
    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    for (var p : food) {
        minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
        minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
        minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
    }
    return ChunkPosRange.ofBlocks(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
}

/** @return the live party overlapping this spread's chunks, or null */
@Nullable
public static PartyAttachment existingParty(ServerLevel level, ChunkPosRange range) {
    PartyAttachment found = null;
    for (var cpos : range.stream().toList()) {
        // false => never force-load: an unloaded chunk cannot be holding a live party
        LevelChunk chunk = level.getChunkSource().getChunk(cpos.x, cpos.z, false);
        if (chunk == null) continue;
        var party = GLMeta.PARTY.type().getOrCreate(chunk);
        if (party.active()) found = party;
    }
    return found;
}
```

`ChunkPosRange.ofBlocks` and `.stream()` already exist (`content/attachment/area/ChunkPosRange.java`) and are exactly the shape needed. `getChunk(..., false)` returns `null` for an unloaded chunk instead of loading it, which is both correct and desirable: a party only exists while its chunk is loaded, so an unloaded chunk is by definition not hosting one.

This rule subsumes the single-anchor check. Two parties can never overlap, because any second spread sharing a table necessarily shares a chunk with the first party's centre chunk.
