#!/usr/bin/env python3
"""Remove jar-in-jar payload from a mod jar, so its nested libs come from the host mod instead.

The dev-only jars under `libs/` are real published builds, so they carry
`META-INF/jarjar/` — `metadata.json` plus a copy of every library the upstream
project bundles. Keeping that payload means the same library lands on the
classpath twice: once from our own `jarJar(...)` declarations and once nested
inside the dependency, which is the JPMS split-package failure the dev launcher
cannot work around from any dependency scope.

Stripping is the fix, but it is not self-announcing: the jar stays a valid zip
and `neoforge.mods.toml` keeps declaring the same `[[dependencies]]` blocks, so a
mistaken strip looks exactly like a successful one until runtime. Hence the
verification pass — see `verify()`.

    python3 tools/stripjarjar.py libs/youkaisfeasts-1.1.0.jar   # rewrite in place
    python3 tools/stripjarjar.py libs/*.jar --dry-run           # just report
    python3 tools/stripjarjar.py libs/x.jar --backup            # keep the original

Re-exporting from the upstream project restores the payload, so re-run this
after every sync.
"""
import os
import shutil
import sys
import tempfile
import zipfile

PREFIX = 'META-INF/jarjar/'
# Files whose text is scanned for leftover references; a jar that still mentions
# jarJar after stripping was probably built with a nested-deps scheme we missed.
SCANNED = ('.json', '.toml', '.cfg', '.MF')


class Bad(Exception):
    pass


def payload(zf):
    """Entries belonging to the jarJar payload, with their sizes."""
    return [(i.filename, i.file_size) for i in zf.infolist()
            if i.filename == PREFIX.rstrip('/') or i.filename.startswith(PREFIX)]


def leftovers(zf):
    """Metadata files still mentioning jarJar after the prefix is gone."""
    hits = []
    for i in zf.infolist():
        if i.filename.endswith(SCANNED) and b'jarjar' in zf.read(i.filename).lower():
            hits.append(i.filename)
    return hits


def strip_one(path, backup=False):
    with zipfile.ZipFile(path) as zf:
        doomed = payload(zf)
        if not doomed:
            print('%s: already clean' % path)
            return True
        for name, size in doomed:
            print('  - %s (%d bytes)' % (name, size))
        # Rewrite rather than mutate in place: zipfile cannot delete entries, so a
        # new archive is built from everything that survives, keeping each entry's
        # original compression so deflated resources stay deflated.
        fd, tmp = tempfile.mkstemp(suffix='.jar', dir=os.path.dirname(path) or '.')
        os.close(fd)
        try:
            with zipfile.ZipFile(tmp, 'w') as out:
                out.comment = zf.comment
                for i in zf.infolist():
                    if i.filename.startswith(PREFIX):
                        continue
                    out.writestr(i, zf.read(i.filename), i.compress_type)
            with zipfile.ZipFile(tmp) as check:
                if check.testzip() is not None:
                    raise Bad('%s: rebuilt archive is corrupt' % path)
                residue = payload(check) + [(n, 0) for n in leftovers(check)]
                nested = [i.filename for i in check.infolist()
                          if i.filename.endswith('.jar') and not i.is_dir()]
                if residue:
                    raise Bad('%s: jarJar survived the rewrite: %s' % (path, residue))
                if nested:
                    # A nested jar elsewhere in the archive is not something this
                    # tool knows how to judge; surface it rather than guess.
                    print('%s: note - nested jar outside the prefix: %s' % (path, nested))
            if backup:
                shutil.copy2(path, path + '.bak')
                print('  original kept as %s.bak' % path)
            # mkstemp creates 0600; a jar nothing but the owner can read is a
            # confusing failure later, so carry the original mode over.
            shutil.copymode(path, tmp)
            shutil.move(tmp, path)
            tmp = None
        finally:
            if tmp and os.path.exists(tmp):
                os.unlink(tmp)
    print('%s: stripped %d entries' % (path, len(doomed)))
    return True


def verify(path):
    """Report the jar's jarJar state without touching it."""
    with zipfile.ZipFile(path) as zf:
        found = payload(zf)
        refs = leftovers(zf)
        total = len(zf.infolist())
    print('%s: %d entries, %d jarJar entries, %d total nested bytes'
          % (path, total, len(found), sum(s for _, s in found)))
    for name, size in found:
        print('  + %s (%d bytes)' % (name, size))
    for name in refs:
        print('  ! %s still mentions jarJar' % name)
    return not found and not refs


if __name__ == '__main__':
    args = sys.argv[1:]
    dry = '--dry-run' in args
    backup = '--backup' in args
    args = [a for a in args if a not in ('--dry-run', '--backup')]
    if not args:
        print('usage: stripjarjar.py <jar>... [--dry-run] [--backup]')
        sys.exit(2)
    ok = True
    for a in args:
        try:
            if dry:
                ok &= verify(a)
            else:
                ok &= strip_one(a, backup)
        except (Bad, zipfile.BadZipFile, OSError) as e:
            print('BAD:', e)
            ok = False
    sys.exit(0 if ok else 1)
