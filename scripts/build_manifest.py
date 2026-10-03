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

# Lang files the viewer resolves translation keys against.
LANG = ("en_us", "zh_cn")

# Tags the viewer needs beyond those referenced by the data. `currency` backs
# TradeOffer.isSellOffer, which decides whether an offer is "sell to the character"
# or "request a craft" - without it the trade list cannot be labelled correctly.
EXTRA_ITEM_TAGS = (f"{NAMESPACE}:currency",)


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
    """Collects ingredient tag references from an arbitrary value.

    Since 1.21 the datapack form of a tag reference drops the leading `#`, so both
    `{"tag": "ns:path"}` and `{"tag": "#ns:path"}` are accepted.
    """
    if isinstance(value, list):
        for item in value:
            collect_item_tags(item, into)
    elif isinstance(value, dict):
        for key, item in value.items():
            if key == "tag" and isinstance(item, str) and ":" in item:
                into.add(item)
            elif key in ("item", "items") and isinstance(item, str) and item.startswith("#"):
                into.add(item)
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

    item_tag_files = existing_tags(set(item_tags) | set(EXTRA_ITEM_TAGS), "item")
    for ref in EXTRA_ITEM_TAGS:
        if ref not in item_tag_files:
            print(f"  ! missing tag {ref}", file=sys.stderr)

    manifest = {
        "generatedBy": "scripts/build_manifest.py",
        "namespace": NAMESPACE,
        "resources": RESOURCES,
        "registries": registries,
        "lootTables": loot,
        "itemTags": item_tag_files,
        "entityTags": existing_tags(entity_refs, "entity_type"),
        "lang": find_lang(),
    }
    manifest["stats"] = {name: len(reg["files"]) for name, reg in registries.items()}
    manifest["stats"]["files"] = sum(len(reg["files"]) for reg in registries.values())
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
