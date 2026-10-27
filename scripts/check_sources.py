"""Source-only checks. Does not compile Kotlin or execute Android code.

Optional grammar parser: python -m pip install --target .tools/python tree-sitter tree-sitter-kotlin
"""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import re
import sqlite3
import subprocess

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / '.tools/python'))
from tree_sitter import Language, Parser
import tree_sitter_kotlin

parser = Parser(Language(tree_sitter_kotlin.language()))
files = sorted((ROOT / 'app/src').rglob('*.kt'))
errors = []
for path in files:
    raw = path.read_bytes()
    text = raw.decode('utf-8', errors='strict')
    if '\ufffd' in text or '\x00' in text:
        errors.append(f'{path.relative_to(ROOT)}: invalid text marker')
    tree = parser.parse(raw)
    nodes = [tree.root_node]
    while nodes:
        node = nodes.pop()
        if node.type == 'ERROR' or node.is_missing:
            errors.append(f'{path.relative_to(ROOT)}:{node.start_point.row + 1}: {node.type} {node.text[:100]!r}')
        nodes.extend(node.children)

ET.parse(ROOT / 'app/src/main/AndroidManifest.xml')
for path in (ROOT / 'docs').glob('*.md'):
    path.read_bytes().decode('utf-8', errors='strict')

# Execute only SQL declarations in Python SQLite. This is not a Room migration test.
fixture = (ROOT / 'app/src/test/resources/v2-schema.sql').read_text(encoding='utf-8')
migration = (ROOT / 'app/src/main/java/com/example/data/local/DatabaseProvider.kt').read_text(encoding='utf-8')
with sqlite3.connect(':memory:') as connection:
    connection.executescript(fixture)
    connection.execute("INSERT INTO tasks VALUES (42, 'Preserve this', 'Q1_DO', 'PENDING', 'College', '', 1, 123)")
    connection.execute("INSERT INTO attendance_records VALUES (7, 'CS', 17, 20, 80)")
    declarations = re.findall(r'db\.execSQL\("((?:CREATE TABLE|ALTER TABLE)[^"\n]+)"\)', migration)
    for statement in declarations:
        connection.execute(statement)
    assert len(declarations) == 9  # seven V3 tables, one additive V4 column and one ledger
    assert connection.execute('SELECT title FROM tasks WHERE id=42').fetchone() == ('Preserve this',)
    assert connection.execute('SELECT attendedClasses, totalClasses, baselineAt FROM attendance_records WHERE id=7').fetchone() == (17, 20, 0)
    assert connection.execute("SELECT count(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").fetchone()[0] == 18

registry = (ROOT / 'app/src/main/java/com/example/data/model/Workspace.kt').read_text(encoding='utf-8')
destinations = re.findall(r'^    ([A-Z_]+)\("[a-z]+",', registry, flags=re.M)
main = (ROOT / 'app/src/main/java/com/example/MainActivity.kt').read_text(encoding='utf-8')
assert len(destinations) == 13
assert all(f'Destination.{destination} ->' in main for destination in destinations)
baseline = subprocess.check_output(['git', 'show', '1d14d90:app/src/main/java/com/example/viewmodel/LifeOsViewModel.kt'], cwd=ROOT).decode('utf-8')
current = (ROOT / 'app/src/main/java/com/example/viewmodel/LifeOsViewModel.kt').read_text(encoding='utf-8')
actions = lambda text: set(re.findall(r'^    fun (\w+)\(', text, flags=re.M))
assert actions(baseline) <= actions(current), 'Legacy action entry point removed'
repository = (ROOT / 'app/src/main/java/com/example/data/repository/LifeOsRepository.kt').read_text(encoding='utf-8')
assert repository.count('seedDefaultDataIfEmpty(') == 1, 'Example data seeding is callable at startup again'
assert 'fallbackToDestructiveMigration' not in repository + migration

print(f'Parsed {len(files)} Kotlin files; checked manifest XML and documentation UTF-8.')
print('13 destination routes, legacy action entry points, and additive SQL declarations checked.')
print('\n'.join(errors) if errors else 'No grammar or encoding errors found.')
sys.exit(bool(errors))
