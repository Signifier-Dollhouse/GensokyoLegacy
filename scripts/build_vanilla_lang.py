#!/usr/bin/env python3
"""Collect the vanilla translations the RPG content actually references.

The site resolves content strings from the mod's own lang files, which of course
carry no vanilla text: every `minecraft:` item, block, mob, advancement and effect
would fall back to a prettified id and stay English on the Chinese page. This pulls
those strings straight out of the game data - `minecraft_*_client.jar` for `en_us`
and NeoForm's asset cache for every other locale - and keeps only the keys the
datapack refers to, so the file stays small enough to read.

The output is committed, because the published site reads it at a fixed path and a
visitor has no Minecraft installation to pull translations from.

`--check` needs no game data: it verifies that every id the content names can be
labelled in both locales, from the mod's lang files or from this one.

Usage:
    python3 scripts/build_vanilla_lang.py            # regenerate assets/lang/vanilla/*.json
    python3 scripts/build_vanilla_lang.py --check    # verify the committed files (CI)
"""

from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MANIFEST = ROOT / "rpg-manifest.json"
OUT_DIR = ROOT / "assets" / "lang" / "vanilla"
LOCALES = ("en_us", "zh_cn")

NEOFORM = Path.home() / ".gradle" / "caches" / "neoformruntime"
CLIENT_JARS = sorted(NEOFORM.glob("artifacts/minecraft_*_client.jar")) + sorted(
    (ROOT / "build" / "moddev" / "artifacts").glob("*minecraft-resources.jar")
)
ASSET_INDEX_DIR = NEOFORM / "assets" / "indexes"

# Only `minecraft` belongs in this file: the mod's own namespaces are covered by the
# mod's lang files, and anything else is left to the prettified-id fallback.
VANILLA_NAMESPACE = "minecraft"


# ---------------------------------------------------------------------------
# what the datapack refers to
# ---------------------------------------------------------------------------


class Refs:
    """Ids grouped by the lang key family that names them."""

    def __init__(self) -> None:
        self.items: set[str] = set()
        self.fluids: set[str] = set()
        self.entities: set[str] = set()
        self.advancements: set[str] = set()
        self.effects: set[str] = set()
        self.enchantments: set[str] = set()
        self.tags: set[str] = set()

    def item(self, value: object) -> None:
        """An item id. A `#` prefix still means a tag, since some writers keep it."""
        if isinstance(value, str):
            (self.tags if value.startswith("#") else self.items).add(value.lstrip("#"))

    def fluid(self, value: object) -> None:
        """A fluid id, which the game names in its own `fluid.` key family."""
        if isinstance(value, str):
            self.fluids.add(value.lstrip("#"))

    def tag(self, value: object) -> None:
        """A tag id. Since 1.21 the datapack form drops the leading `#`."""
        if isinstance(value, str):
            self.tags.add(value.lstrip("#"))

    def target(self, value: object) -> None:
        """An `EntityPredicate` type: one entity id, or a `#tag` of them."""
        if isinstance(value, str):
            (self.tags if value.startswith("#") else self.entities).add(value.lstrip("#"))


def read_json(path: str | Path) -> dict:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def collect_refs(manifest: dict) -> Refs:
    """Every item, mob, advancement, effect and enchantment the content names."""
    refs = Refs()

    def ingredients(container: object) -> None:
        """`{item}`, `{tag}` and NeoForge's `{type: neoforge:components, items}`."""
        for entry in container if isinstance(container, list) else [container]:
            if not isinstance(entry, dict):
                continue
            refs.item(entry.get("item"))
            refs.item(entry.get("items"))
            refs.tag(entry.get("tag"))

    def effects(node: object) -> None:
        """Mob effects are named by dialog actions, which nest them freely."""
        if isinstance(node, dict):
            for key, value in node.items():
                if key == "effect" and isinstance(value, str):
                    refs.effects.add(value)
                effects(value)
        elif isinstance(node, list):
            for value in node:
                effects(value)

    def deep_ingredients(node: object) -> None:
        """Every `{item}`, `{items}` and `{tag}` anywhere inside `node`.

        Recipes spell their inputs a dozen different ways - a shaped key map, an
        `ingredient` field, `extra` lists - so a walk beats enumerating them.
        """
        if isinstance(node, dict):
            refs.item(node.get("item"))
            refs.item(node.get("items"))
            refs.tag(node.get("tag"))
            for value in node.values():
                deep_ingredients(value)
        elif isinstance(node, list):
            for value in node:
                deep_ingredients(value)

    for registry in ("quest", "trade", "dialog_starter", "dialog"):
        for file in manifest["registries"][registry]["files"]:
            data = read_json(file)
            ingredients(data.get("ingredients"))
            if isinstance(data.get("result"), dict):
                refs.item(data["result"].get("id"))
            for requirement in (data.get("requirements") or {}).values():
                ingredients(requirement.get("ingredients"))
                refs.target((requirement.get("target") or {}).get("type"))
            for condition in data.get("conditions") or []:
                if isinstance(condition.get("advancement"), str):
                    refs.advancements.add(condition["advancement"])
            effects(data)

    # Recipes, including the mod's own alchemy and brewing types, whose output is a
    # fluid rather than an item. `inputFluid` is a list in one recipe type and a
    # single fluid in the others.
    for file in manifest.get("recipes", {}).get("files", []):
        recipe = read_json(file)
        deep_ingredients(recipe)
        if isinstance(recipe.get("result"), dict):
            refs.item(recipe["result"].get("id"))
        for key in ("resultFluid", "inputFluid"):
            value = recipe.get(key)
            for fluid in value if isinstance(value, list) else [value]:
                if isinstance(fluid, dict):
                    refs.fluid(fluid.get("fluid"))

    # The guide books spotlight the items they document, and those names show up in
    # the item section whether or not the item has a recipe. A spotlight names one item
    # or tag, or a list of them; a tag is spelled `tag:namespace:path`.
    for book in manifest.get("guides", []):
        for locale in book["locales"].values():
            for file in locale["entries"]:
                entry = read_json(file)
                refs.item(entry.get("icon"))
                for page in entry.get("pages") or []:
                    for value in page.get("item") if isinstance(page.get("item"), list) else [page.get("item")]:
                        if isinstance(value, str) and value.startswith("tag:"):
                            refs.tag(value.removeprefix("tag:"))
                        else:
                            refs.item(value)

    for file in manifest["lootTables"].values():
        for pool in read_json(file).get("pools") or []:
            for entry in pool.get("entries") or []:
                refs.item(entry.get("name"))
            for function in pool.get("functions") or []:
                for enchantment in function.get("enchantments") or []:
                    refs.enchantments.add(enchantment.get("enchantment"))
                for nested in function.get("functions") or []:
                    for enchantment in nested.get("enchantments") or []:
                        refs.enchantments.add(enchantment.get("enchantment"))

    for file in manifest["itemTags"].values():
        for value in read_json(file).get("values") or []:
            refs.item(value)

    for file in manifest["entityTags"].values():
        for value in read_json(file).get("values") or []:
            refs.target(value)

    return refs


def wanted_keys(refs: Refs) -> dict[str, set[str]]:
    """Every referenced id, mapped to the lang keys that could name it.

    Lang keys are dotted paths rather than namespaced ids, and an id sometimes has
    more than one spelling: an item is also a block, and an advancement is keyed
    with its namespace only when it is not vanilla. Any one candidate is enough,
    which is what `--check` asserts against the merged mod + vanilla tables.
    """
    wanted: dict[str, set[str]] = {}

    def add(ident: str, *keys: str) -> None:
        wanted.setdefault(ident, set()).update(keys)

    def key(family: str, ident: str) -> str:
        # `minecraft:zombie_head` is `item.minecraft.zombie_head`.
        return f"{family}.{ident.replace(':', '.', 1)}"

    for item in sorted(refs.items):
        add(item, key("item", item), key("block", item))
    # A fluid is its own key family, but the item and block spellings are tried too,
    # since vanilla names water and lava as blocks rather than as fluids.
    for fluid in sorted(refs.fluids):
        add(fluid, key("fluid", fluid), key("item", fluid), key("block", fluid))
    for entity in sorted(refs.entities):
        add(entity, key("entity", entity))
    for effect in sorted(refs.effects):
        add(effect, key("effect", effect))
    for enchantment in sorted(refs.enchantments):
        add(enchantment, key("enchantment", enchantment))
    for advancement in sorted(refs.advancements):
        namespace, _, path = advancement.partition(":")
        dotted = path.replace("/", ".")
        # Vanilla advancements drop the namespace, the way the game asks for them:
        # `advancements.nether.root.title`.
        add(advancement, f"advancements.{dotted}.title", f"advancements.{namespace}.{dotted}.title")
    return wanted


# ---------------------------------------------------------------------------
# game data
# ---------------------------------------------------------------------------


def client_jar() -> Path:
    for jar in CLIENT_JARS:
        with zipfile.ZipFile(jar) as archive:
            if "assets/minecraft/lang/en_us.json" in archive.namelist():
                return jar
    raise SystemExit(
        "no Minecraft client jar found; looked for:\n  "
        + "\n  ".join(str(jar) for jar in CLIENT_JARS)
        + "\nRun a Gradle task once so NeoForm downloads it."
    )


def jar_lang(jar: Path, locale: str) -> dict:
    with zipfile.ZipFile(jar) as archive:
        return json.loads(archive.read(f"assets/minecraft/lang/{locale}.json"))


def asset_lang(locale: str) -> dict:
    """A locale from NeoForm's asset cache: only the client jar is unpacked there."""
    if not ASSET_INDEX_DIR.is_dir():
        raise SystemExit(f"no asset index at {ASSET_INDEX_DIR}; run a Gradle task once")
    for index in sorted(ASSET_INDEX_DIR.glob("*.json")):
        entry = read_json(index)["objects"].get(f"minecraft/lang/{locale}.json")
        if not entry:
            continue
        blob = NEOFORM / "assets" / "objects" / entry["hash"][:2] / entry["hash"]
        if blob.exists():
            return json.loads(blob.read_bytes())
    raise SystemExit(f"{locale} is not in the NeoForm asset cache ({ASSET_INDEX_DIR})")


def game_lang(locale: str) -> dict:
    """The game's own strings for a locale."""
    jar = client_jar()
    # Only the client jar is unpacked, so it holds en_us; every other locale comes
    # from the downloaded asset objects.
    return jar_lang(jar, locale) if locale == "en_us" else asset_lang(locale)


# ---------------------------------------------------------------------------
# writing and checking
# ---------------------------------------------------------------------------


def vanilla_keys(wanted: dict[str, set[str]], locale: str) -> dict[str, str]:
    """The first candidate key the game defines, per id. Only `minecraft:` ids end
    up in the file: another namespace belongs to the mod's own lang files."""
    source = game_lang(locale)
    table: dict[str, str] = {}
    for ident, candidates in wanted.items():
        if not ident.startswith(f"{VANILLA_NAMESPACE}:"):
            continue
        found = next((key for key in sorted(candidates) if key in source), None)
        if found is not None:
            table[found] = source[found]
    return table


def write(tables: dict[str, dict[str, str]]) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for locale, table in tables.items():
        path = OUT_DIR / f"{locale}.json"
        path.write_text(json.dumps(table, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(f"wrote {path.relative_to(ROOT)} ({len(table)} keys)")


def merged_tables(manifest: dict, vanilla: dict[str, dict[str, str]]) -> dict[str, dict[str, str]]:
    """Everything the site can resolve against, per locale.

    The mod's own lang files are the primary source; the vanilla files fill the gap
    for `minecraft:` ids, which the mod of course says nothing about.
    """
    tables = {}
    for locale in LOCALES:
        mod_file = next(file for file in manifest["lang"] if file.endswith(f"{locale}.json"))
        tables[locale] = {**read_json(mod_file), **vanilla[locale]}
    return tables


def coverage(wanted: dict[str, set[str]], tables: dict[str, dict[str, str]]) -> dict[str, set[str]]:
    """Which locales can name each id; an empty set means no file names it."""
    return {
        ident: {locale for locale in LOCALES if any(key in tables[locale] for key in candidates)}
        for ident, candidates in wanted.items()
    }


def check(manifest: dict, wanted: dict[str, set[str]]) -> int:
    """Verify the committed files: every referenced id nameable in every locale."""
    failures = 0
    vanilla: dict[str, dict[str, str]] = {}
    for locale in LOCALES:
        path = OUT_DIR / f"{locale}.json"
        if not path.exists():
            print(f"missing {path.relative_to(ROOT)}; run: python3 scripts/build_vanilla_lang.py")
            return 1
        vanilla[locale] = read_json(path)

    for ident, locales in sorted(coverage(wanted, merged_tables(manifest, vanilla)).items()):
        if len(locales) == len(LOCALES):
            continue
        if locales:
            failures += 1
            missing = ", ".join(locale for locale in LOCALES if locale not in locales)
            print(f"untranslated: {ident} has no name in {missing}")
        else:
            # Nothing upstream names this id - a third party item, say - so the
            # prettified id remains the fallback. Worth knowing, not worth failing.
            print(f"note: {ident} is named by no lang file; it falls back to a prettified id")

    needed = {key for candidates in wanted.values() for key in candidates}
    for locale in LOCALES:
        for key in sorted(set(vanilla[locale]) - needed):
            failures += 1
            print(f"stale: {key!r} in {locale}.json is no longer referenced")
    for key in sorted(set(vanilla["en_us"]) ^ set(vanilla["zh_cn"])):
        failures += 1
        print(f"locale mismatch: {key!r} is not in both vanilla files")

    if not failures:
        print(f"vanilla lang: {len(vanilla['en_us'])} referenced keys, every id nameable in {', '.join(LOCALES)}")
    return failures


def report(refs: Refs, wanted: dict[str, set[str]], tables: dict[str, dict[str, str]]) -> None:
    """What the file covers, and what no lang file names."""
    print(f"referenced: {len(refs.items)} items, {len(refs.entities)} mobs, "
          f"{len(refs.advancements)} advancements, {len(refs.effects)} effects, "
          f"{len(refs.enchantments)} enchantments, {len(refs.tags)} tags")

    unnamed = sorted(ident for ident, locales in coverage(wanted, tables).items() if not locales)
    if unnamed:
        print(f"named by no lang file ({len(unnamed)}), they fall back to a prettified id:")
        print("  " + ", ".join(unnamed))
    if refs.tags:
        print(f"tags ({len(refs.tags)}), which Minecraft does not translate:")
        print("  " + ", ".join(f"#{tag}" for tag in sorted(refs.tags)))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="verify the committed files instead of regenerating")
    args = parser.parse_args()

    manifest = read_json(MANIFEST)
    refs = collect_refs(manifest)
    wanted = wanted_keys(refs)

    if args.check:
        return 1 if check(manifest, wanted) else 0

    print(f"reading vanilla strings from {client_jar().name}")
    tables = {}
    for locale in LOCALES:
        if locale == "en_us":
            print(f"reading {locale} from the client jar")
        else:
            print(f"reading {locale} from the NeoForm asset cache")
        tables[locale] = vanilla_keys(wanted, locale)
    write(tables)
    report(refs, wanted, merged_tables(manifest, tables))
    return 0


if __name__ == "__main__":
    sys.exit(main())