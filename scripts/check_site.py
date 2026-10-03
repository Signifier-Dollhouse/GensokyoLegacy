#!/usr/bin/env python3
"""Static sanity check for the site's ES modules.

JSC on macOS cannot parse ES modules and there is no node in this environment, so
this uses tree-sitter to (a) find syntax errors, (b) verify every imported name is
actually exported by the target module, and (c) report unused imports.

Usage: python3 scripts/check_site.py
"""

from __future__ import annotations

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


def main() -> int:
    files = sorted(SITE.rglob("*.js"))
    sources = {path: path.read_bytes() for path in files}
    failures = 0

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

    if failures:
        print(f"\n{failures} problem(s)")
    else:
        print(f"ok: {len(files)} modules parsed, imports and exports resolve")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
