#!/usr/bin/env python3
"""Static sanity check for the site's ES modules.

JSC on macOS cannot parse ES modules and there is no node in this environment, so
this uses tree-sitter to (a) find syntax errors, (b) verify every imported name is
actually exported by the target module, (c) report unused imports, and (d) check
that every interface string is translated in both locales.

Usage: python3 scripts/check_site.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    from tree_sitter import Language, Parser
    import tree_sitter_javascript
except ImportError:  # optional dependency
    print("tree-sitter not installed; skipping the site check")
    print("(pip install tree_sitter tree_sitter_javascript)")
    raise SystemExit(0)

ROOT = Path(__file__).resolve().parent.parent
SITE = ROOT / "assets"

parser = Parser(Language(tree_sitter_javascript.language()))


def text_of(node, source: bytes) -> str:
    return source[node.start_byte : node.end_byte].decode("utf-8")


def imports_of(source: bytes) -> list[tuple[str, set[str]]]:
    """Returns (module specifier, imported names) for each import statement."""
    found = []
    for node in walk(parser.parse(source).root_node):
        if node.type != "import_statement":
            continue
        specifier = None
        names: set[str] = set()
        for child in node.children_by_field_name("source"):
            specifier = text_of(child, source).strip("'\"")
        for child in node.named_children:
            if child.type == "import_clause":
                for item in child.named_children:
                    if item.type == "identifier":
                        names.add(text_of(item, source))
                    elif item.type == "import_specifier":
                        alias = child_by_field(item, "alias")
                        names.add(text_of(alias, source) if alias else text_of(child_by_field(item, "name"), source))
        if specifier:
            found.append((specifier, names))
    return found


def exports_of(source: bytes) -> set[str]:
    """Names a module makes available to importers."""
    names: set[str] = set()
    for node in walk(parser.parse(source).root_node):
        if node.type == "export_statement":
            for child in node.named_children:
                if child.type in ("function_declaration", "class_declaration"):
                    names.add(text_of(child_by_field(child, "name"), source))
                elif child.type == "lexical_declaration":
                    for declarator in walk(child):
                        if declarator.type == "variable_declarator":
                            target = child_by_field(declarator, "name")
                            if target is not None:
                                names.add(text_of(target, source))
                elif child.type == "export_clause":
                    for specifier in child.named_children:
                        alias = child_by_field(specifier, "alias")
                        names.add(text_of(alias, source) if alias else text_of(specifier, source))
    return names


def child_by_field(node, field):
    target = node.child_by_field_name(field)
    return target


def walk(node):
    yield node
    for child in node.named_children:
        yield from walk(child)


def identifiers_in(source: bytes) -> list[str]:
    return [text_of(node, source) for node in walk(parser.parse(source).root_node) if node.type == "identifier"]


def top_level_declarations(source: bytes) -> list[tuple[str, str]]:
    """(name, kind) for each top-level binding, so collisions can be spotted."""
    found: list[tuple[str, str]] = []
    for node in parser.parse(source).root_node.named_children:
        if node.type in ("function_declaration", "class_declaration") and node.child_by_field_name("name"):
            found.append((text_of(node.child_by_field_name("name"), source), node.type))
        elif node.type in ("lexical_declaration", "variable_declaration"):
            for child in node.named_children:
                if child.type == "variable_declarator":
                    target = child.child_by_field_name("name")
                    if target is not None:
                        found.append((text_of(target, source), node.type))
        elif node.type == "import_statement":
            for clause in node.named_children:
                if clause.type != "import_clause":
                    continue
                for specifier in clause.named_children:
                    if specifier.type == "identifier":
                        found.append((text_of(specifier, source), "import"))
                    elif specifier.type == "import_specifier":
                        alias = specifier.child_by_field_name("alias")
                        found.append((text_of(alias or specifier.child_by_field_name("name"), source), "import"))
    return found


def i18n_tables(source: bytes) -> dict[str, dict[str, str]]:
    """The string tables declared in assets/lib/i18n.js, keyed by locale."""
    tables: dict[str, dict[str, str]] = {}
    for node in walk(parser.parse(source).root_node):
        if node.type != "variable_declarator":
            continue
        name = child_by_field(node, "name")
        value = child_by_field(node, "value")
        if name is None or value is None or value.type != "object":
            continue
        locale = text_of(name, source)
        if locale not in ("en_us", "zh_cn"):
            continue
        entries: dict[str, str] = {}
        for pair in value.named_children:
            if pair.type != "pair":
                continue  # a comment inside the table
            key_node = child_by_field(pair, "key")
            text_node = child_by_field(pair, "value")
            if key_node is None or text_node is None:
                continue
            entries[text_of(key_node, source).strip("\"'")] = text_of(text_node, source)
        tables[locale] = entries
    return tables


def placeholders(text: str) -> set[str]:
    """The `{0}`, `{1}` ... a translated string expects."""
    return set(re.findall(r"\{\d+\}", text))


def used_keys(source: bytes) -> set[tuple[str, str, str]]:
    """Every i18n key a module asks for: (kind, key, origin).

    A literal argument is a single key, a template literal such as
    `character.${slug}` is a prefix that every key under it must cover, and each
    branch of a ternary contributes its own key. An argument built from a variable
    cannot be checked statically and is skipped.
    """
    used = set()
    for node in walk(parser.parse(source).root_node):
        if node.type != "call_expression":
            continue
        callee = child_by_field(node, "function")
        if callee is None or callee.type != "identifier" or text_of(callee, source) not in ("tr", "trOrNull", "trPlural"):
            continue
        arguments = node.children_by_field_name("arguments")
        first = next(iter(arguments[0].named_children), None) if arguments else None
        if first is None:
            continue
        line = source[: node.start_byte].count(b"\n") + 1
        origin = f"line {line}"
        plural = text_of(callee, source) == "trPlural"
        if first.type == "ternary_expression":
            # Only the branches name keys; the condition never does.
            candidates = [child_by_field(first, "consequence"), child_by_field(first, "alternative")]
        else:
            candidates = [first]
        for argument in candidates:
            if argument is None:
                continue
            for literal in walk(argument):
                if literal.type == "string":
                    key = text_of(literal, source).strip("\"'`")
                    for name in [f"{key}.one", f"{key}.many"] if plural else [key]:
                        used.add(("key", name, origin))
                elif literal.type == "template_string":
                    # `character.${slug}`: every key under the static head is required.
                    head = text_of(literal, source).strip("`").split("${", 1)[0]
                    used.add(("prefix", head, origin))
    return used


def markup_keys(html: str) -> set[tuple[str, str, str]]:
    """Keys referenced by the `data-i18n` hooks in index.html."""
    used = set()
    for key in re.findall(r'data-i18n="([^"]+)"', html):
        used.add(("key", key, "index.html"))
    for pair in re.findall(r'data-i18n-attr="([^"]+)"', html):
        for entry in pair.split(","):
            _, _, key = entry.partition(":")
            if key.strip():
                used.add(("key", key.strip(), "index.html"))
    return used


def check_i18n(sources: dict[Path, bytes]) -> int:
    """Every interface string must exist in both locales with matching slots."""
    failures = 0
    tables = i18n_tables(sources[SITE / "lib" / "i18n.js"])
    if set(tables) != {"en_us", "zh_cn"}:
        print("i18n: assets/lib/i18n.js does not declare both en_us and zh_cn tables")
        return 1

    used = set()
    for path, source in sources.items():
        if path.name != "i18n.js":
            used |= used_keys(source)
    used |= markup_keys((ROOT / "index.html").read_text(encoding="utf-8"))

    # A prefix stands for every key under it, so the character names built at
    # runtime are covered just as strictly as the literal ones.
    covered = {key for kind, key, _ in used if kind == "key"}
    for _, prefix, origin in sorted(item for item in used if item[0] == "prefix"):
        matches = {key for key in tables["en_us"] if key.startswith(prefix)}
        if not matches:
            failures += 1
            print(f"untranslated: {origin} asks for keys starting {prefix!r}, none exist")
        covered |= matches

    for _, key, origin in sorted(used):
        if key not in covered:
            continue
        missing = [locale for locale in ("en_us", "zh_cn") if key not in tables[locale]]
        if missing:
            failures += 1
            print(f"untranslated: {origin} uses {key!r}, missing from {', '.join(missing)}")
            continue
        slots = placeholders(tables["en_us"][key])
        if placeholders(tables["zh_cn"][key]) != slots:
            failures += 1
            print(f"placeholder mismatch: {key!r} in zh_cn against en_us {sorted(slots)}")

    for key in sorted(set(tables["en_us"]) - covered):
        failures += 1
        print(f"unused i18n key: {key!r}")

    # A key that only one locale has would silently vanish when the other is picked.
    for locale, other in (("zh_cn", "en_us"), ("en_us", "zh_cn")):
        for key in sorted(set(tables[locale]) - set(tables[other])):
            failures += 1
            print(f"locale-only key: {key!r} is in {locale} but not in {other}")

    if not failures:
        print(f"i18n: {len(covered)} interface strings, {len(tables['en_us'])} keys, both locales complete")
    return failures


# The site's logo and favicon are the mod's own Mini Hakkero texture, copied into
# assets/img so the page does not depend on a path outside the site. The copy is
# checked against the texture rather than trusted, since a retexture in the mod would
# otherwise leave the site showing the old icon forever.
LOGO = ROOT / "assets/img/hakkero.png"
LOGO_SOURCE = ROOT / "src/main/resources/assets/gensokyolegacy/textures/item/tool/mini_hakkero_prototype.png"


def check_logo() -> int:
    """The committed logo copy still matches the mod's texture."""
    if not LOGO_SOURCE.is_file():
        print(f"logo source missing: {LOGO_SOURCE.relative_to(ROOT)}")
        return 1
    if not LOGO.is_file():
        print(f"logo missing: {LOGO.relative_to(ROOT)}; copy {LOGO_SOURCE.relative_to(ROOT)} over it")
        return 1
    if LOGO.read_bytes() != LOGO_SOURCE.read_bytes():
        print(f"logo is stale: {LOGO.relative_to(ROOT)} differs from {LOGO_SOURCE.relative_to(ROOT)}")
        print("  cp " + str(LOGO_SOURCE.relative_to(ROOT)) + " " + str(LOGO.relative_to(ROOT)))
        return 1
    return 0


def main() -> int:
    files = sorted(SITE.rglob("*.js"))
    sources = {path: path.read_bytes() for path in files}
    failures = check_logo()

    for path, source in sources.items():
        tree = parser.parse(source)
        errors = [node for node in walk(tree.root_node) if node.type == "ERROR" or node.is_missing]
        for node in errors:
            failures += 1
            line = source[: node.start_byte].count(b"\n") + 1
            print(f"syntax error: {path.relative_to(ROOT)}:{line} {node.type}")

        # A name declared twice at top level is a hard parse error in a browser and
        # silently kills the whole module, so it is worth catching here.
        seen_names: dict[str, str] = {}
        for name, kind in top_level_declarations(source):
            if name in seen_names:
                failures += 1
                print(f"duplicate declaration: {path.relative_to(ROOT)} {name!r} ({seen_names[name]} + {kind})")
            seen_names[name] = kind

    exports = {path: exports_of(source) for path, source in sources.items()}
    usages = {path: identifiers_in(source) for path, source in sources.items()}

    for path, source in sources.items():
        for specifier, names in imports_of(source):
            target = (path.parent / specifier).resolve()
            if not target.exists():
                failures += 1
                print(f"missing module: {path.relative_to(ROOT)} imports {specifier}")
                continue
            for name in sorted(names):
                if name not in exports[target]:
                    failures += 1
                    print(f"missing export: {path.relative_to(ROOT)} imports {name!r} from {specifier}")
        # An imported name that never appears again usually means dead wiring.
        for specifier, names in imports_of(source):
            for name in sorted(names):
                if usages[path].count(name) < 2:
                    failures += 1
                    print(f"unused import: {path.relative_to(ROOT)} {name!r} from {specifier}")

    failures += check_i18n(sources)

    if failures:
        print(f"\n{failures} problem(s)")
    else:
        print(f"ok: {len(files)} modules parsed, imports, exports and translations resolve")
        print(f"ok: {LOGO.relative_to(ROOT)} matches the mod's texture")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
