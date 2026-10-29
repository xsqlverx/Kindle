"""Source/data checks only: no Kotlin compilation, Room runtime, Android or PDF rendering."""
from collections import Counter
from datetime import date
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
pack = json.loads((ASSETS / 'academic/catalog.json').read_text(encoding='utf-8'))
sources = {s['id']: s for s in pack['sources']}
courses = {c['id']: c for c in pack['courses']}
items = {i['id']: i for i in pack['items']}
events = {e['id']: e for e in pack['events']}
assert len(sources) == len(pack['sources']) == 7
assert len(courses) == len(pack['courses']) == 6
assert len(items) == len(pack['items']) == 140
assert len(events) == len(pack['events']) == 26
assert date.fromisoformat(pack['preparationEnd']) == date(2026, 12, 6)
for source in sources.values():
    path = ASSETS / source['asset']
    assert path.resolve().is_relative_to(ASSETS.resolve())
    assert path.read_bytes().startswith(b'%PDF-')
    assert hashlib.sha256(path.read_bytes()).hexdigest() == source['sha256'], path
    assert source['pages'] > 0
assert sources['electrical']['filenames'] == ['BE(electrical).pdf', 'BEC(electronics).pdf']
assert sources['electrical']['sha256'].startswith('93b7a25d2e3a6c1b')
assert {key: courses[key]['code'] for key in courses} == dict(mathematics='A105MAT01',
    electrical='A105EST04', chemistry='A106CYT02', graphics='A105EST03', python='A105EST05', life_skills='A105HWT06')
for item in items.values():
    assert item['id'].startswith('aisat26:')
    assert item['course'] in courses and item['source'] in sources
    assert 1 <= item['page'] <= sources[item['source']]['pages']
    assert all(item[key].strip() for key in ('title', 'detail', 'firstStep', 'module', 'moduleKey'))
    assert item['firstStep'].startswith('Suggested ')
    if item['scope'] == 'THEORY':
        assert item['hours'] > 0 and item['moduleKey'].split(':')[-1] in ['m1', 'm2', 'm3', 'm4']
assert sum(i['scope'] == 'CODE' for i in items.values()) == 18
assert sum(i['scope'] == 'LAB' for i in items.values()) == 15
assert sum(i['course'] == 'life_skills' and i['id'].split(':')[-1].startswith('a') for i in items.values()) == 35
assert all(i['scope'] != 'THEORY' for i in items.values() if i['course'] == 'life_skills')
assert 'ESE 0' in courses['life_skills']['assessment']
assert 'mandatory' in items['aisat26:life_skills:portfolio']['detail'].lower()
assert items['aisat26:graphics:cad']['scope'] == 'INTERNAL'
assert any(i['scope'] == 'OPTIONAL' and 'excluded' in (i['detail'] + i['module']).lower() for i in items.values() if i['course'] == 'chemistry')
for course in courses:
    if course != 'life_skills':
        assert {i['moduleKey'] for i in items.values() if i['course'] == course and i['scope'] == 'THEORY'} == {f'{course}:m{m}' for m in range(1, 5)}
for event in events.values():
    assert event['source'] == 'calendar' and event['semester'] in (1, 2)
    assert event['page'] == event['semester'] and event['dates'] == sorted(set(event['dates']))
    for value in event['dates']:
        assert date.fromisoformat(value).year == (2026 if event['semester'] == 1 else 2027)
    assert 'course' not in event and 'time' not in event  # calendar does not give paper assignments
assert events['s1-internal2']['dates'] == ['2026-11-13','2026-11-16','2026-11-17','2026-11-18','2026-11-19']
assert events['s1-final']['dates'] == ['2026-12-07','2026-12-09','2026-12-11','2026-12-14','2026-12-16']
assert events['s2-internal1']['dates'] == ['2027-02-11','2027-02-12','2027-02-15','2027-02-16','2027-02-17','2027-02-18']
assert events['s2-internal2']['dates'] == ['2027-03-20','2027-03-22','2027-03-23','2027-03-24','2027-03-30','2027-03-31']
assert events['s2-final']['dates'] == ['2027-04-19','2027-04-21','2027-04-23','2027-04-26','2027-04-28','2027-04-30']
dao = (ROOT / 'app/src/main/java/com/example/data/local/WorkspaceDao.kt').read_text(encoding='utf-8')
assert re.search(r'@Insert\(onConflict = OnConflictStrategy.IGNORE\)\s+suspend fun insertAcademicTopic', dao)
assert re.search(r'@Insert\(onConflict = OnConflictStrategy.IGNORE\)\s+suspend fun insertAcademicAttempt', dao)
for path in [ASSETS / 'academic/catalog.json', ROOT / 'scripts/build_academic_pack.py']:
    content = path.read_text(encoding='utf-8')
    assert not any(marker in content for marker in ('\ufffd', '\x00', 'Ã', 'â€', 'calոմel'))
print('Academic catalog: 6 courses, 140 groups, 26 milestones, 7 byte/hash-checked PDF assets.')
print('Scopes:', dict(Counter(i['scope'] for i in items.values())))
print('Unique PDF payload:', sum((ASSETS / s['asset']).stat().st_size for s in sources.values()), 'bytes.')
print('Source pages/IDs, exclusions, module coverage, exam date lists and non-overwriting insert declarations pass.')
print('No Android runtime, cloud build, native PDF viewer or visual result verified.')
