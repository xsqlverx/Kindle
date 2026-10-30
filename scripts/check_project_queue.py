"""Static audit of bundled public-source suggestions and entry routes; no Android execution."""
from pathlib import Path
from urllib.parse import urlparse
import json
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/com/example'
queue = json.loads((root / 'app/src/main/assets/project_queue.json').read_text(encoding='utf-8'))
rows = queue['projects']
assert queue['version'] == 'github-2026-10-30-v1'
assert len(rows) == 4 and len({r['id'] for r in rows}) == 4
assert {r['repo'] for r in rows} == {'xsqlverx/Kindle', 'xsqlverx/AURA-V2', 'xsqlverx/KronosV1', 'xsqlverx/Emerald-math'}
for row in rows:
    assert row['id'] == 'github:' + row['repo'].replace('/', ':')
    assert re.fullmatch(r'[0-9a-f]{40}', row['revision'])
    evidence = urlparse(row['evidence'])
    assert evidence.scheme == 'https' and evidence.netloc == 'github.com'
    assert evidence.path.startswith('/' + row['repo'] + '/blob/' + row['revision'] + '/')
    assert len(row['steps']) >= 2 and len(row['steps']) == len(set(row['steps']))
    assert all(row[key].strip() for key in ('name', 'why', 'goal'))
    assert all(step.strip() for step in row['steps'])
store = (src / 'data/repository/QuickEntryStore.kt').read_text(encoding='utf-8')
rules = (src / 'data/model/QuickEntry.kt').read_text(encoding='utf-8')
kinds = ['TASK', 'PROJECT', 'NOTE', 'DEADLINE', 'CLASS', 'TOPIC', 'CODING', 'HABIT', 'WATER', 'WEIGHT', 'TRAINING', 'SET']
for kind in kinds:
    assert re.search(r'\b' + kind + r'\("', rules)
    assert 'EntryKind.' + kind in store
assert 'db.withTransaction' in store and 'dao.entryReceipt(draft.id)' in store and 'dao.saveEntryReceipt' in store
assert 'prefs.edit().putString("draft", encode(next)).commit()' in store
dao = (src / 'data/local/WorkspaceDao.kt').read_text(encoding='utf-8')
for method in ('saveEntryReceipt', 'insertSuggestedProject', 'insertSuggestedStep'):
    assert re.search(r'@Insert\(onConflict = OnConflictStrategy.IGNORE\)\s+suspend fun ' + method, dao)
assert 'clearEntryReceipts()' in dao
migrations = (src / 'data/local/DatabaseProvider.kt').read_text(encoding='utf-8')
statements = re.findall(r'db\.execSQL\("((?:CREATE TABLE|ALTER TABLE)[^"\n]+)"\)', migrations)
fixture = (root / 'app/src/test/resources/v2-schema.sql').read_text(encoding='utf-8')
with sqlite3.connect(':memory:') as connection:
    connection.executescript(fixture)
    connection.execute("INSERT INTO tasks VALUES (42, 'Existing task', 'Q1_DO', 'PENDING', 'College', '', 1, 123)")
    connection.execute("INSERT INTO attendance_records VALUES (7, 'CS', 17, 20, 80)")
    for statement in statements: connection.execute(statement)
    connection.execute("INSERT INTO entry_receipts VALUES ('same', 'TASK', '42')")
    connection.execute("INSERT OR IGNORE INTO entry_receipts VALUES ('same', 'TASK', '999')")
    assert connection.execute('SELECT recordId FROM entry_receipts WHERE id = ?', ('same',)).fetchone() == ('42',)
    assert connection.execute('SELECT title FROM tasks WHERE id = 42').fetchone() == ('Existing task',)
    assert connection.execute('SELECT attendedClasses, totalClasses, baselineAt FROM attendance_records WHERE id = 7').fetchone() == (17, 20, 0)
for path in [root / 'app/src/main/assets/project_queue.json', src / 'ui/components/ContinuityOrb.kt']:
    content = path.read_text(encoding='utf-8')
    assert not any(marker in content for marker in ('\ufffd', '\x00', 'Ã', 'â€'))
print('Four GitHub suggestions: unique IDs, immutable source URLs and proposed actions checked.')
print('Twelve entry kinds, transactional receipt and draft declarations, SQLite migration preservation checked.')
print('No Android runtime, Kotlin type-check, native animation, cloud build or device usability verified.')
