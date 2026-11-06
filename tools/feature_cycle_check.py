"""Reproduces FeatureSorter's feature-order cycle detection.

FeatureSorter links each biome's decoration features into a graph, one edge per pair of consecutive
features (including across step boundaries), keyed by (feature, step). Two biomes that list the same
features in a different relative order produce a cycle and crash chunk generation.

It is called once per chunk generator, over `BiomeSource.possibleBiomes()` — that one generator's biomes
and nothing else. Vanilla's own biomes are therefore never in the same graph as this mod's: the Gensokyo
dimension's multi_noise source lists 23 `gensokyolegacy` biomes and no vanilla one, and `EmptyChunkGenerator`
holds a FixedBiomeSource with a single biome. So the biomes are grouped by namespace and each group is
checked on its own; one graph over all of them invents cycles that no generator can ever hit.

usage: python3 tools/feature_cycle_check.py <vanilla resources jar> <mod resources dir>
"""
import json
import sys
import zipfile
from pathlib import Path

DECORATION_STEP = 9


def load_vanilla(jar: Path):
    with zipfile.ZipFile(jar) as archive:
        for name in sorted(archive.namelist()):
            if name.startswith("data/minecraft/worldgen/biome/") and name.endswith(".json"):
                yield "minecraft:" + Path(name).stem, json.loads(archive.read(name)).get("features", [])


def load_mod(directory: Path):
    for path in sorted(directory.glob("data/*/worldgen/biome/*.json")):
        ns = path.parts[-4]
        yield f"{ns}:{path.stem}", json.loads(path.read_text()).get("features", [])


def find_cycle(biomes):
    # node identity in FeatureSorter is (feature, step), since the feature index is a bijection of the
    # placed feature itself
    graph = {}

    def node(feature, step):
        return (feature, step)

    for features in biomes.values():
        flat = [(feature, step) for step, step_features in enumerate(features) for feature in step_features]
        for current, following in zip(flat, flat[1:]):
            graph.setdefault(current, set()).add(following)
    return cycle(graph)


def cycle(graph):
    done, on_stack = set(), {}

    def visit(vertex):
        state = on_stack.get(vertex)
        if state == "done":
            return None
        if state == "open":
            return [vertex]
        on_stack[vertex] = "open"
        for following in sorted(graph.get(vertex, ())):
            found = visit(following)
            if found:
                return [vertex] + found
        on_stack[vertex] = "done"
        return None

    for vertex in sorted(graph):
        found = visit(vertex)
        if found:
            return found
    return None


def main():
    vanilla_jar, mod_dir = Path(sys.argv[1]), Path(sys.argv[2])
    groups = {}
    for source in (load_vanilla(vanilla_jar), load_mod(mod_dir)):
        for name, features in source:
            groups.setdefault(name.split(":")[0], {})[name] = features
    for namespace in sorted(groups):
        found = find_cycle(groups[namespace])
        print(f"{namespace}: checked {len(groups[namespace])} biomes")
        if not found:
            print(f"{namespace}: no feature order cycle")
            continue
        print(f"{namespace}: feature order cycle:")
        for feature, step in found:
            print(f"  {feature} @ decoration step {step}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
