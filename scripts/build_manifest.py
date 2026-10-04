#!/usr/bin/env python3
"""Build `rpg-manifest.json`: the file index the website uses to discover the RPG
datapack JSONs under `src/generated/resources`.

The page is served from the repository root of the `gh-page` branch, so it can
fetch those JSONs directly at their real paths - this script only records *where*
they are. No data file is copied, rewritten or merged.

Usage: python3 scripts/build_manifest.py [--check]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any, Iterator

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = "src/generated/resources"
SRC = ROOT / RESOURCES
MANIFEST = ROOT / "rpg-manifest.json"
NAMESPACE = "gensokyolegacy"

# Searched in order: the first hit wins. Datagen output is the primary source, but
# `zh_cn.json` is hand-authored under `src/main/resources` (see AGENTS.md).
RESOURCE_ROOTS = (RESOURCES, "src/main/resources")

# RPG datapack registries, by folder under `data/<ns>/gensokyolegacy/`.
REGISTRIES = ("quest", "dialog", "dialog_starter", "trade")

# Recipes, indexed for the item section. Datagen writes them flat under one folder,
# including the mod's own alchemy and brewing recipe types in subdirectories.
RECIPES = f"{RESOURCES}/data/{NAMESPACE}/recipe"

# Patchouli guide books: the item guide the website reads instead of writing its own.
# The book definition is datagen output under `data/<ns>/patchouli_books/`, while the
# categories and entries are hand-authored under `assets/<ns>/patchouli_books/`, so
# both resource roots are searched.
GUIDE_BOOKS = f"{RESOURCES}/data/{NAMESPACE}/patchouli_books"
GUIDE_PAGES = f"src/main/resources/assets/{NAMESPACE}/patchouli_books"
GUIDE_LOCALES = ("en_us", "zh_cn")

# Lang files the viewer resolves translation keys against.
LANG = ("en_us", "zh_cn")

# Vanilla lang tables, committed next to the page by scripts/build_vanilla_lang.py.
# They name the `minecraft:` ids the mod's own files know nothing about, so they are
# listed here only when they are actually present.
VANILLA_LANG_DIR = "assets/lang/vanilla"

# Tags the viewer needs beyond those referenced by the data.
# - `currency` backs TradeOffer.isSellOffer, which decides whether an offer is "sell to
#   the character" or "request a craft" - without it the trade list cannot be labelled.
# - `morichika_offers` is what Rinnosuke stocks his shop shelves from; nothing in the
#   RPG registries refers to it (only MorichikaEntity does), so without listing it here
#   the shop's stock would be missing from every item page.
EXTRA_ITEM_TAGS = (f"{NAMESPACE}:currency", f"{NAMESPACE}:morichika_offers")

# The data maps the viewer reads beyond what the content refers to. `morichika_offer`
# holds the price and stock range each item is shelved at - the tag above says what he
# may stock at all, this says what for. Datapacked, so it needs no Java to find.
EXTRA_DATA_MAPS = (f"{NAMESPACE}/data_maps/item/morichika_offer",)


def walk(directory: Path, prefix: str) -> list[str]:
    """Recursively lists files under `directory` as paths relative to the repo root."""
    if not directory.is_dir():
        return []
    found = []
    for entry in sorted(directory.iterdir()):
        rel = f"{prefix}/{entry.name}"
        if entry.is_dir():
            found.extend(walk(entry, rel))
        elif entry.is_file():
            found.append(rel)
    return sorted(found)


def read_json(rel: str) -> Any:
    """Reads a repo-relative JSON file, returning None when absent or malformed."""
    try:
        with open(ROOT / rel, encoding="utf-8") as handle:
            return json.load(handle)
    except FileNotFoundError:
        return None
    except json.JSONDecodeError as error:
        print(f"  ! {rel}: {error}", file=sys.stderr)
        return None


def values_at(value: Any, key: str) -> Iterator[str]:
    """Yields every string stored under `key` anywhere inside `value`."""
    if isinstance(value, list):
        for item in value:
            yield from values_at(item, key)
    elif isinstance(value, dict):
        for item in value.values():
            yield from values_at(item, key)
        if isinstance(value.get(key), str):
            yield value[key]


def collect_item_tags(value: Any, into: set[str]) -> set[str]:
    """Collects item tag references from an arbitrary value.

    Since 1.21 a `tag` field drops its leading `#`, so the field is what says a value is
    a reference rather than the spelling. The older `#ns:path` form is still accepted,
    as is the `tag:ns:path` a Patchouli spotlight uses - and a spotlight may hold a list
    of them, so the walk descends into lists rather than looking at fields alone.
    """
    if isinstance(value, str):
        if value.startswith("tag:"):
            into.add(value.removeprefix("tag:"))
        elif value.startswith("#"):
            into.add(value)
    elif isinstance(value, list):
        for item in value:
            collect_item_tags(item, into)
    elif isinstance(value, dict):
        for key, item in value.items():
            if key == "tag" and isinstance(item, str) and ":" in item:
                into.add(item.removeprefix("#"))
            else:
                collect_item_tags(item, into)
    return into


def collect_entity_refs(value: Any, into: set[str]) -> set[str]:
    """Collects namespaced ids used as `type` discriminators.

    `kill_mob` targets are either `#namespace:entity_type_tag` or a bare
    `namespace:entity_type`, so both shapes land here and are resolved against the
    entity tag directory afterwards. Ids that are not tags simply resolve to
    nothing, which the viewer already handles.
    """
    if isinstance(value, list):
        for item in value:
            collect_entity_refs(item, into)
    elif isinstance(value, dict):
        kind = value.get("type")
        if isinstance(kind, str) and ":" in kind and " " not in kind:
            into.add(kind)
        for item in value.values():
            collect_entity_refs(item, into)
    return into


def resolve_tag(ref: str, kind: str) -> str | None:
    """Maps `#ns:path` or `ns:path` to its tag file, or None when absent.

    Tags are looked up in both resource roots, since vanilla tags live in neither.
    """
    namespace, _, tail = ref.lstrip("#").partition(":")
    if not tail:
        return None
    for resource_root in RESOURCE_ROOTS:
        candidate = f"{resource_root}/data/{namespace}/tags/{kind}/{tail}.json"
        if (ROOT / candidate).is_file():
            return candidate
    return None


def find_lang() -> list[str]:
    """Locates each requested lang file, preferring datagen output over hand-authored."""
    found = {}
    for locale in LANG:
        for resource_root in RESOURCE_ROOTS:
            rel = f"{resource_root}/assets/{NAMESPACE}/lang/{locale}.json"
            if (ROOT / rel).is_file():
                found[locale] = rel
                break
        else:
            print(f"  ! missing lang file {locale}.json", file=sys.stderr)
    return [found[locale] for locale in LANG if locale in found]


def find_vanilla_lang() -> list[str]:
    """The committed vanilla tables, which live with the page rather than the mod."""
    return [f"{VANILLA_LANG_DIR}/{locale}.json" for locale in LANG if (ROOT / VANILLA_LANG_DIR / f"{locale}.json").is_file()]


def data_files(manifest: dict[str, Any]) -> list[str]:
    """Every datapack file the page fetches, which is everything the index points at.

    The two files that live with the page rather than with the datapack are left out: the
    index cannot hash itself, and the committed vanilla tables are re-read every visit
    rather than versioned, since a hand edit to one should show up without a rebuild.
    """
    files: set[str] = set()
    for registry in manifest["registries"].values():
        files.update(registry["files"])
    files.update(manifest.get("recipes", {}).get("files", []))
    files.update(manifest["lootTables"].values())
    files.update(manifest["itemTags"].values())
    files.update(manifest["entityTags"].values())
    files.update(manifest.get("dataMaps", {}).values())
    files.update(manifest["lang"])
    for book in manifest["guides"]:
        files.add(book["book"])
        for pages in book["locales"].values():
            for kind in ("categories", "entries"):
                files.update(pages[kind])
    return sorted(files)


def content_revision(manifest: dict[str, Any]) -> str:
    """An md5 of everything the page fetches, so a data URL can be versioned by it.

    A file is asked for at its own path with the revision on it as a query, and the
    browser is told to use its cached copy rather than ask again. The name therefore
    changes exactly when the content does - merging new content onto this branch moves
    it, so the next visit fetches afresh instead of trusting what it kept - and nothing
    else about the site has to be told.

    Only the bytes are hashed, never the timestamps, so a re-run that changes nothing
    leaves the number alone. A file the index lists but the tree does not hold is
    skipped, with the same warning the rest of the script gives: the page falls back to
    a derived path for it anyway.
    """
    digest = hashlib.md5()
    listed = data_files(manifest)
    hashed = 0
    for file in listed:
        path = ROOT / file
        if not path.is_file():
            print(f"  ! missing data file {file}", file=sys.stderr)
            continue
        digest.update(file.encode("utf-8"))
        digest.update(path.read_bytes())
        hashed += 1
    print(f"  revision: {digest.hexdigest()} over {hashed} files")
    return digest.hexdigest()


def find_data_maps() -> dict[str, str]:
    """The data maps listed in EXTRA_DATA_MAPS, keyed by their name in the datapack."""
    found: dict[str, str] = {}
    for ref in EXTRA_DATA_MAPS:
        rel = f"{RESOURCES}/data/{ref}.json"
        name = ref.split("/")[-1]
        if (ROOT / rel).is_file():
            found[name] = rel
        else:
            print(f"  ! missing data map {name}", file=sys.stderr)
    return found


def build_recipes(item_tags: set[str]) -> dict[str, Any]:
    """The recipe index. Ingredients name item tags like any other content, so the
    tags they reference are collected too - before the tag index is resolved."""
    files = walk(ROOT / RECIPES, RECIPES)
    for file in files:
        collect_item_tags(read_json(file), item_tags)
    return {"root": RECIPES, "files": files}


def build_guides(item_tags: set[str]) -> list[dict[str, Any]]:
    """Every Patchouli book, with its per-locale categories and entries.

    A book is one `book.json`; the pages beside it are `<locale>/categories/**` and
    `<locale>/entries/**`. Entries are named by their path, which is what the book
    refers to and what a locale-independent id is built from, since the files for the
    two locales are otherwise indistinguishable. An entry may spotlight a whole item tag,
    so its tags are collected for the index like any other content's.
    """
    books = []
    for book_file in walk(ROOT / GUIDE_BOOKS, GUIDE_BOOKS):
        if not book_file.endswith("/book.json"):
            continue
        folder = book_file[len(GUIDE_BOOKS) + 1 : -len("/book.json")]
        locales = {}
        for locale in GUIDE_LOCALES:
            pages = f"{GUIDE_PAGES}/{folder}/{locale}"
            categories = [f for f in walk(ROOT / pages / "categories", f"{pages}/categories") if f.endswith(".json")]
            entries = [f for f in walk(ROOT / pages / "entries", f"{pages}/entries") if f.endswith(".json")]
            if not categories and not entries:
                continue
            for entry in entries:
                collect_item_tags(read_json(entry), item_tags)
            locales[locale] = {"categories": categories, "entries": entries}
        if not locales:
            print(f"  ! guide book {folder} has no pages under {GUIDE_PAGES}", file=sys.stderr)
            continue
        books.append({"id": f"{NAMESPACE}:{folder}", "book": book_file, "pages": f"{GUIDE_PAGES}/{folder}", "locales": locales})
    return books


def build() -> dict[str, Any]:
    registries: dict[str, Any] = {}
    loot_tables: set[str] = set()
    item_tags: set[str] = set()
    entity_refs: set[str] = set()

    for registry in REGISTRIES:
        root = f"{RESOURCES}/data/{NAMESPACE}/gensokyolegacy/{registry}"
        files = walk(ROOT / root, root)
        for file in files:
            raw = read_json(file)
            loot_tables.update(values_at(raw, "table"))
            collect_item_tags(raw, item_tags)
            collect_entity_refs(raw, entity_refs)
        registries[registry] = {"root": root, "files": files}

    loot = {}
    for ident in sorted(loot_tables):
        namespace, _, tail = ident.partition(":")
        rel = f"{RESOURCES}/data/{namespace}/loot_table/{tail}.json"
        if not (ROOT / rel).is_file():
            print(f"  ! missing loot table {ident}", file=sys.stderr)
            continue
        loot[ident] = rel
        for name in values_at(read_json(rel), "name"):
            if isinstance(name, str) and name.startswith("#"):
                item_tags.add(name)

    def existing_tags(refs: set[str], kind: str) -> dict[str, str]:
        found = {}
        for ref in sorted(refs):
            rel = resolve_tag(ref, kind)
            if rel and (ROOT / rel).is_file():
                found[ref] = rel
        return found

    # Recipes and the guide book both contribute tags of their own, so they are indexed
    # before the tags are resolved into files.
    recipes = build_recipes(item_tags)
    guides = build_guides(item_tags)
    item_tag_files = existing_tags(set(item_tags) | set(EXTRA_ITEM_TAGS), "item")
    for ref in EXTRA_ITEM_TAGS:
        if ref not in item_tag_files:
            print(f"  ! missing tag {ref}", file=sys.stderr)

    manifest = {
        "generatedBy": "scripts/build_manifest.py",
        "namespace": NAMESPACE,
        "resources": RESOURCES,
        "registries": registries,
        "recipes": recipes,
        "guides": guides,
        "lootTables": loot,
        "itemTags": item_tag_files,
        "dataMaps": find_data_maps(),
        "entityTags": existing_tags(entity_refs, "entity_type"),
        "lang": find_lang(),
        "vanillaLang": find_vanilla_lang(),
    }
    manifest["revision"] = content_revision(manifest)
    manifest["stats"] = {name: len(reg["files"]) for name, reg in registries.items()}
    manifest["stats"]["files"] = sum(len(reg["files"]) for reg in registries.values())
    manifest["stats"]["recipes"] = len(manifest["recipes"]["files"])
    # An entry is a file per locale, so the same guide entry is counted once per locale.
    manifest["stats"]["guideEntries"] = sum(
        len(locale["entries"]) for book in manifest["guides"] for locale in book["locales"].values()
    )
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="exit 1 if the manifest is stale")
    args = parser.parse_args()

    if not SRC.is_dir():
        print(f"no datagen output at {RESOURCES} - run ./gradlew runData first", file=sys.stderr)
        return 1

    manifest = build()
    body = json.dumps(manifest, indent=2, ensure_ascii=False) + "\n"

    if args.check:
        current = MANIFEST.read_text(encoding="utf-8") if MANIFEST.is_file() else None
        stale = "manifest missing" if current is None else ("manifest is out of date" if current != body else None)
        print(stale or "up to date")
        return 1 if stale else 0

    MANIFEST.write_text(body, encoding="utf-8")
    print(f"wrote {MANIFEST.relative_to(ROOT)}: {json.dumps(manifest['stats'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
