"""Source-authored S1 catalog. Uses supplied PDFs, never generates lessons or exam dates.

Run with the bundled Python (pypdf). Source files are copied once; subsequent builds use
the bundled originals. The duplicate Electrical/Electronics input is intentionally one PDF.
"""
from pathlib import Path
import hashlib
import json
import shutil
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets/academic'
ASSETS.mkdir(parents=True, exist_ok=True)
INPUT = Path('C:/college/00 First year syllabus')
files = {
    'electrical': (INPUT / 'BE(electrical).pdf', ['BE(electrical).pdf', 'BEC(electronics).pdf']),
    'chemistry': (INPUT / 'CHY(chem).pdf', ['CHY(chem).pdf']),
    'graphics': (INPUT / 'A105EST03-Engineering-Graphics-and-Computer-Aided-Drawing.pdf', ['A105EST03-Engineering-Graphics-and-Computer-Aided-Drawing.pdf']),
    'life_skills': (INPUT / 'A105HWT06-Life-Skills-and-Professional-Communication-1.pdf', ['A105HWT06-Life-Skills-and-Professional-Communication-1.pdf']),
    'mathematics': (INPUT / 'A105MAT01-Mathematics-for-Information-Science-1.pdf', ['A105MAT01-Mathematics-for-Information-Science-1.pdf']),
    'python': (INPUT / 'ATP(python).pdf', ['ATP(python).pdf']),
    'calendar': (Path('C:/college/Academic calender(year 1-4)/Academic-Calendar-S1-2026-27-Autonomous.pdf'), ['Academic-Calendar-S1-2026-27-Autonomous.pdf'])
}
sources = []
for key, (original, names) in files.items():
    target = ASSETS / f'{key}.pdf'
    if not target.exists():
        shutil.copyfile(original, target)
    if original.exists():
        assert original.read_bytes() == target.read_bytes(), f'Original changed: {original}'
    sources.append(dict(id=key, asset=f'academic/{key}.pdf', filenames=names,
        sha256=hashlib.sha256(target.read_bytes()).hexdigest(), pages=len(PdfReader(target).pages)))
if (INPUT / 'BEC(electronics).pdf').exists():
    assert (INPUT / 'BEC(electronics).pdf').read_bytes() == (ASSETS / 'electrical.pdf').read_bytes()

courses = []
items = []
def course(key, code, title, short, hours, credits, assessment, note=''):
    courses.append(dict(id=key, code=code, title=title, short=short, hours=hours, credits=credits,
        assessment=assessment, note=note, source=key))
def module(key, number, title, hours, page, groups, note=''):
    for slug, name, detail in groups:
        items.append(dict(id=f'aisat26:{key}:m{number}:{slug}', course=key, module=f'M{number} · {title}',
            moduleKey=f'{key}:m{number}', hours=hours, page=page, scope='THEORY', title=name,
            detail=detail + ('\nScope: ' + note if note else ''), source=key,
            firstStep=('Suggested practice: use your notes to work one example, then recall the method without looking.' if key in ('mathematics','electrical','chemistry') else
                'Suggested practice: draw one construction from your notes and explain the steps without copying.' if key == 'graphics' else
                'Suggested practice: trace a small example, then implement or explain the method at the syllabus depth.')))
def other(key, slug, title, detail, page, scope, group):
    items.append(dict(id=f'aisat26:{key}:{slug}', course=key, module=group, moduleKey=f'{key}:{scope.lower()}', hours=0,
        page=page, scope=scope, title=title, detail=detail, source=key,
        firstStep='Suggested next step: check what your teacher has assigned; prepare one small piece and save where to resume.'))

standard = 'CIE 40 / ESE 60. ESE: 2 h 30 min; Part A 8 × 3 marks, Part B 4 × 9 marks (one of two full questions per module).'
course('mathematics','A105MAT01','Mathematics for Information Science – 1','Mathematics','3:0:0:0',3,standard)
module('mathematics',1,'Single-variable calculus',9,1,[
    ('limits','Limits and continuity','Limits of function values; continuity at a point; continuous functions.'),
    ('derivatives','Derivatives and rates of change','Derivative at a point and as a function; second/higher derivatives; instantaneous rates of change.'),
    ('chain','Chain rule and implicit differentiation','Chain rule; implicit differentiation; tangents and normal lines.'),
    ('linearization','Linearization and concavity','Linearization; concavity and second-derivative test for concavity. Thomas’ Calculus sections 2.2, 2.5, 3.1–3.4, 3.6–3.7, 3.9, 4.4 cover this module.')])
module('mathematics',2,'Functions of several variables',9,1,[
    ('domains','Domains, ranges and level curves','Functions of several variables; domains/ranges; level curves of two variables.'),
    ('limits','Two-variable limits and continuity','Limits and continuity for functions of two variables.'),
    ('partials','Partial derivatives and the chain rule','Partial derivatives of functions of more than two variables; partial derivatives and continuity; second-order partials; mixed derivative theorem; chain rule for two-variable functions. Text 1 sections 14.1–14.4.')])
module('mathematics',3,'Gradient and extrema',9,2,[
    ('gradient','Directional derivatives and gradient','Chain rule for three variables; directional derivatives in the plane, interpretation and properties; gradient.'),
    ('local','Local extrema and saddle points','Relative extrema; first-derivative theorem; critical and saddle points; second-derivative test for local extreme values.'),
    ('absolute','Absolute extrema on bounded regions','Absolute maxima/minima on closed bounded regions. Text 1 sections 14.4, 14.5, 14.7.')])
module('mathematics',4,'Constrained optimization',9,2,[
    ('lagrange','Lagrange multipliers','Constrained maxima/minima; Lagrange multipliers with one and two constraints.'),
    ('descent','Steepest descent','Method of steepest descent: two variables only.'),
    ('lpp','Linear programming','LPP formulation and graphical solution. Text 1 section 14.8; Text 2 sections 22.1–22.2.')])
course('electrical','A105EST04','Introduction to Electrical and Electronics Engineering','Electrical & Electronics','4:0:0:0',4,standard,
    'Both supplied Electrical and Electronics files are identical copies of this combined course. Existing attendance entries are not merged.')
module('electrical',1,'DC and magnetic circuits',11,1,[
    ('laws','Circuit laws, division and stored energy','Current/voltage division, relative potential; capacitor/inductor V–I relations and stored energy; Ohm and Kirchhoff laws; numerical problems.'),
    ('star-delta','Resistive star–delta conversion','Resistive networks only; numerical problems; derivation not required.'),
    ('networks','Mesh and node analysis','Mesh-current and node-voltage methods; matrix representation and solution of network equations; numerical problems.'),
    ('magnetic','Magnetic circuits','MMF, field strength, flux density and reluctance; electric/magnetic comparison; series/parallel composite magnetic circuits. Numerical problems not needed.')])
module('electrical',2,'Induction and AC circuits',11,1,[
    ('induction','Electromagnetic induction','Faraday and Lenz laws; static/dynamic induced emf; self/mutual inductance and coefficient of coupling. Numerical problems not needed.'),
    ('waveforms','AC waveforms and RMS values','Generation of AC; frequency, period, average/RMS value and form factor; numerical problems.'),
    ('phasors','Phasors and single-phase circuits','Trigonometric/rectangular/polar/complex forms; pure R/L/C; reactance and impedance; RL, RC, RLC series circuits; power factor and active/reactive/apparent power; simple numerical problems. Continues on PDF page 2.'),
    ('three-phase','Balanced three-phase systems','Three-phase generation and advantages; balanced star/delta; line/phase voltage and current relations; numerical problems. PDF page 2.')])
module('electrical',3,'Electronic devices',13,2,[
    ('diodes','PN and Zener diodes','Passive/active components; PN junction working and V–I characteristics; Zener/avalanche breakdown; Zener voltage regulator basics.'),
    ('rectifiers','Power supply and rectifiers','DC supply block diagram; half-wave, full-wave and bridge rectifiers; ripple factor with/without capacitor filters.'),
    ('bjt','BJT characteristics and configurations','Construction, working, V–I characteristics; CE input/output; CE/CB/CC comparison.'),
    ('amplifiers','Biasing, switching and amplification','Biasing/load line; transistor as switch/amplifier; RC-coupled amplifier diagram and frequency response.'),
    ('fet','FET and MOSFET','Introduction to FET; construction/working of N-channel and P-channel MOSFETs.')])
module('electrical',4,'Modern electronics',9,2,[
    ('communication','Communication and receivers','Communication-system and fiber-optic block diagrams; AM/FM concepts without derivations; AM/FM superheterodyne receiver blocks.'),
    ('networks','Wired, wireless and mobile systems','Wired/wireless basics; GSM block diagram; comparison of 3G, 4G, 5G and 6G technologies.'),
    ('instruments','Electronic instruments','Instrumentation-system, digital multimeter and function-generator blocks; CRO and Lissajous patterns.'),
    ('iot','IoT applications','Smart homes, healthcare and agriculture: case study only.')])
course('chemistry','A106CYT02','Chemistry for Information Science','Chemistry','3:0:2:0',4,standard + ' CIE: attendance 5, continuous assessment 10, written internals 10+10, lab exam 5.',
    'Any 10 of the 15 listed experiments are mandatory; actual selected experiments are not supplied. Self-study batteries are excluded from ESE.')
module('chemistry',1,'Electrochemistry and corrosion',9,1,[
    ('cells','Electrochemical cells and Nernst equation','Electrode potential; Nernst equation for single electrode/cell with numericals; electrochemical series/applications.'),
    ('electrodes','Reference electrodes and measurement','SHE/calomel construction/working; glass electrode and pH; conductivity measurement using digital meter.'),
    ('energy','Li-ion battery and fuel cell','Li-ion battery and H2–O2 fuel cell with acid electrolyte: construction and working.'),
    ('corrosion','Corrosion and protection','Electrochemical mechanism in acidic/alkaline medium; galvanic series; sacrificial/impressed-current cathodic protection; copper electroplating/electroless plating.')])
module('chemistry',2,'Electronic materials',9,2,[
    ('nano','Nanomaterials and carbon structures','Classification by dimension/material; sol-gel and chemical reduction; CNT, fullerenes, graphene and carbon quantum dots: structures, properties, applications.'),
    ('polymers','Fire-retardant and conducting polymers','Halogenated/non-halogenated examples; conducting-polymer classification; polyaniline/polypyrrole synthesis, properties and applications.'),
    ('devices','Organic and emerging electronic materials','OLED/DSSC construction, working and applications; quantum-computing materials, supercapacitors and spintronics.')])
module('chemistry',3,'Spectroscopy and analysis',9,2,[
    ('spectra','Spectra and Beer–Lambert law','Types of spectra; molecular energy levels; Beer–Lambert numerical problems; electronic transitions, conjugation and absorption maxima; instrumentation/applications.'),
    ('vibrations','Vibrational spectroscopy','Principle; number of vibrational modes; CO2 and H2O modes; applications.'),
    ('analysis','Thermal and electron-microscopic analysis','DETA of polymers: working/applications. SEM: principle, instrumentation and applications.')])
module('chemistry',4,'Environmental chemistry',9,2,[
    ('water','Hardness and water treatment','Temporary/permanent hardness, disadvantages and degree-of-hardness numericals; ion exchange, reverse osmosis; chlorination/breakpoint, ozone and UV.'),
    ('sewage','Water quality and sewage','DO/BOD/COD definitions/significance; primary/secondary/tertiary sewage treatment; trickling filter and UASB flow diagrams.'),
    ('environment','E-waste, climate and sustainability','E-waste recycling/recovery/reuse; greenhouse gases, ozone depletion and introduction to sustainable development goals.')])
other('chemistry','self-batteries','Additional batteries — self study','Lead-acid, nickel-cadmium and nickel-metal-hydride batteries: construction, working and applications. Explicitly NOT included for end-semester examination.',2,'OPTIONAL','Self study · excluded from ESE')
chem_labs = ['Estimation of iron in iron ore','Estimation of copper in brass','Cell constant and conductance of solutions','pH-meter calibration and solution pH','Synthesis of urea-formaldehyde / phenol-formaldehyde polymers','Absorption maximum and colorimetric estimation of Fe3+','Molar absorptivity of KMnO4 or food colorant','IR-spectra analysis','Drug identification using TLC','Total hardness by EDTA','Dissolved oxygen by Winkler method','Calorific value by bomb calorimeter','Saponification value of vegetable oil','Acid value of vegetable oil','Verification of Nernst equation']
for n, title in enumerate(chem_labs,1):
    other('chemistry',f'lab{n:02}',f'Experiment {n} · {title}','Listed syllabus experiment. Any 10 are mandatory; teacher-selected set and performed experiments are unknown. Prepare theory/record/viva from your lab manual; follow supervised lab instructions.',6 if n <= 8 else 7,'LAB','Chemistry experiments')
course('graphics','A105EST03','Engineering Graphics and Computer Aided Drawing','Graphics','2:0:2:0',3,
    'CIE 40 / ESE 60, 2 h 30 min. PDF table offers eight questions at 15 marks, four answered (4 × 15 = 60). CIE: attendance 5, assignment+lab 10+5, internals 10+10.',
    'Introduction: no ESE questions. CAD: internal evaluation only. Exclude true-shape-given and through-hole development problems as stated.')
other('graphics','foundations','Technical drawing foundations','Relevance; types of lines; dimensioning; BIS practice. No questions for ESE from this introduction.',1,'FOUNDATION','M1 · foundations')
module('graphics',1,'Points and lines',9,1,[
    ('points','Projection of points','Points in different quadrants.'),
    ('lines','Projection of lines','Straight lines inclined to one/both planes; trace; inclination to reference planes; true length and true inclinations.')])
module('graphics',2,'Simple solids',9,1,[
    ('simple','Solids in simple position','Triangular, rectangular, square, pentagonal and hexagonal prisms/pyramids; cone and cylinder only; simple position including profile view.'),
    ('inclined','Inclined solids','Axis inclined to one or both reference planes.')])
module('graphics',3,'Sections and development',9,2,[
    ('sections','Sections and true shapes','Vertical-axis prisms, pyramids, cone and cylinder cut by different planes; true shapes. Exclude problems where the true shape is given.'),
    ('development','Development of surfaces','Solids and solids cut by section planes. Exclude through-hole problems.')])
module('graphics',4,'Isometric drawing',9,2,[
    ('scale','Isometric scale and projections','Isometric scale; view/projection of prisms, pyramids, cone, cylinder, sphere and hemisphere.'),
    ('combinations','Combinations of solids','Isometric view/projection of combinations of the listed solids.')])
other('graphics','cad','2D CAD with dimensions','Introduction, role and advantages of CAD; create a dimensioned 2D drawing using suitable software. Internal evaluation only.',2,'INTERNAL','M4 · CAD')
course('python','A105EST05','Algorithmic Thinking with Python','Python','3:0:2:0',4,standard + ' CIE: attendance 5, programming assessment 5, written internals 10+10, lab exam 10.',
    'Module 4: introductory diagrammatic/algorithmic explanations only; analysis not required. Flowcharts visualize control flow. Source lab list has 18 exercises.')
module('python',1,'Problem solving and Python basics',7,1,[
    ('strategies','Problem-solving strategies','Trial/error, heuristics, means–ends analysis and backtracking/working backward; value of multiple strategies.'),
    ('process','The problem-solving process','Computer as computation model; understand problem, formulate model, develop algorithm, program, test and evaluate.'),
    ('basics','Python essentials','Variables, numeric/string types, math module, print/input, operators and precedence.')])
module('python',2,'Algorithms and flowcharts',9,2,[
    ('pseudocode','Pseudocode constructs','Meaning/definition/reasons; sequencing, if/else and case selection; for/while/repeat-until repetition.'),
    ('flowcharts','Flowchart symbols','Start/end, calculation, I/O, decision, module call, for-loop hexagon, flow lines and connectors. For visualizing algorithm control flow only.'),
    ('examples','Algorithm sample problems','Expressions, simple interest, comparison, KTU grades with selection, 50-to-1 countdown, sum of inputs in three loop variants, factorial and largest of n.')])
module('python',3,'Control flow and modularization',10,2,[
    ('loops','Selection and iteration','if/else/elif, for/range and while loops.'),
    ('sequences','Sequences and NumPy','Lists, tuples, sets, strings, dictionaries and NumPy arrays.'),
    ('functions','Decomposition and functions','Decomposition/modularization motivation; functions and multiple returns; merge sort and top-three-integers examples.'),
    ('recursion','Recursion and the call stack','Definition/reasons, stack and circularity; Fibonacci, GCD, factorial, adding positive integers and digit sum. Continues on page 3.')])
module('python',4,'Computational approaches',10,3,[
    ('brute','Brute force','Padlock/password-guessing examples.'),
    ('divide','Divide and conquer','Merge sort; advantages/disadvantages.'),
    ('dynamic','Dynamic programming','Fibonacci; recursion versus dynamic programming.'),
    ('greedy','Greedy algorithms','Maximum completed tasks within limited time; motivations, characteristics and comparison with DP.'),
    ('randomized','Randomized approaches','Coupon-collection and random hat-return examples; motivations.')],
    'Introductory diagrammatic/algorithmic explanations only; analysis not required.')
python_labs = [
    'Five-operator desktop calculator','Strings: create, concatenate, substring','Display time and date in different formats','Create, append and remove NumPy lists','Largest of three numbers','Celsius–Fahrenheit conversion','Nested-loop star patterns','Prime numbers below N','Recursive factorial','Recursive addition of two positive numbers','Recursive multiplication of two positive numbers','Recursive GCD','Right-triangle check using functions','Fibonacci module and import','Validate a mobile number: 10 digits, first digit 7/8/9','Merge lists: sorted evens then sorted odds','16-stick game: take 1–3, last stick loses','Monty Hall switching problem']
for n,title in enumerate(python_labs,1):
    other('python',f'lab{n:02}',f'Lab {n} · {title}','Official programming exercise from the supplied lab list. Outcome remains Unattempted until you report it. ' +
        ('Use c/5 = (f−32)/9.' if n == 6 else ''),7,'CODE','Python lab exercises')
other('python','classroom','Classroom problem-solving exercises','The PDF lists 12 classroom exercises across pages 6–7, including Hanoi, recursion, array reversal, illustrative credential validation, alternate-element lists and collinearity. The credential example is sample programming data, not authorization to access any portal.',6,'INTERNAL','Classroom exercises')
course('life_skills','A105HWT06','Life Skills and Professional Communication','Life Skills','2:0:1:0',1,
    'CIE 100 / ESE 0. Activity-based individual/group assessment. Portfolio and journal are mandatory in addition to minimum marks.',
    'Weeks are relative syllabus guidance, not actual dates. Keep portfolio/journal for later seminar use. Table 2 heading says 24 marks while its rows list 24 and 4; no derived grade total is inferred.')
activities = [
    ('1.1','Group formation and self-introduction','Week 1 · group',2),('1.2','Plan the course activities','Week 1 · group',2),('1.3','Prepare the Gantt chart','Week 1 · group',2),
    ('2.1','Personality test, reflection and report','Week 1 · individual',2),('2.2','Role-storming: guardian and friend perspectives','Week 1 · individual',2),('2.3','Role-storming: teacher perspective on graduate skills','Week 1 · individual',2),('2.4','Discuss and refine the group skill list','Week 1 · group',2),('2.5','Mind map and class presentation','Week 2 · group',2),
    ('3','Empathy presentation','Weeks 2–4 · individual',3),('4.1','Connect with three professionals','Week 3 · individual; at least one connection through LinkedIn',3),('4.2','Document professional interactions','Week 3 · individual',3),('4.3','Group discussion and minutes','Week 3 · group',3),('4.4','Workplace-readiness group report','Week 4 · group',3),('4.5','Workplace role-play','Week 5 · group',3),('4.6','Undergraduate goal and action plan','Week 5 · individual',3),
    ('5.1','Select a technical problem and study materials','Week 6 · group',4),('5.2','Lecture or TED-talk one-page summary','Week 6 · individual; different resource per member',4),('5.3','Gather ideas from a technical forum','Week 6 · group',4),('5.4','Six thinking hats solution exercise','Week 7 · group',4),('5.5','Problem-solving report','Week 7 · group',4),
    ('6.1','LinkedIn profile','Week 1 · individual',4),('6.2','Prepare a resume','Week 8 · individual',4),('6.3','Self-introduction video','Week 8 · individual',4),('7','Emotional-intelligence presentation','Week 9 · individual',4),('8','Diversity video, 3–5 minutes','Week 10 · group',4),('9','Interview practice and reflection','Week 10 · individual',4),('10','Listening test and reflection','Week 11 · individual',4),
    ('11.1','Vocabulary activities','Week 8 · individual/group',4),('11.2','Identify English-usage errors','Week 9 · individual/group',4),('11.3','Spelling, pronunciation and confusing words','Week 10 · individual/group',4),('11.4','Communication-improvement reflection','Week 12 · individual',4),('11.5','Collaboration-tools and time-management presentation','Weeks 11–12 · group',4),('12.1','Kerala-context technology podcast content','Week 12 · group',5),('12.2','Publish the podcast/video','Week 12 · group',5),('12.3','Add podcast link to LinkedIn','Week 12 · group',5)]
for number,title,week,page in activities:
    other('life_skills','a'+number.replace('.','-'),f'Activity {number} · {title}',f'{week}. Relative syllabus week only; actual deadline and completion are not known. Check the source activity table for full requirements.',page,'ACTIVITY','Course activities')
other('life_skills','portfolio','Portfolio and journal audit','Mandatory portfolio file with reports/textual activity materials, and a journal. Audit what actually exists and identify the next missing item. Do not assume earlier activities are incomplete or completed.',1,'ACTIVITY','Mandatory portfolio / journal')
other('life_skills','practical','Practical engineering and language lab','Hands-on engineering sessions and reflection; language lab. Review actual assigned records and supervised sessions; do not infer performed work. See the full list and mark-table ambiguity on page 5.',5,'INTERNAL','Practical activities')

events=[]
def event(key,title,semester,kind,dates,page):
    events.append(dict(id=key,title=title,semester=semester,kind=kind,dates=dates.split(','),source='calendar',page=page))
event('s1-start','S1 classes commence',1,'MILESTONE','2026-08-10',1)
event('s1-enrol-start','S1 enrolment begins',1,'ADMIN','2026-08-17',1)
event('s1-enrol-end','S1 enrolment ends',1,'ADMIN','2026-09-02',1)
event('s1-onam','Onam vacation starts',1,'HOLIDAY','2026-08-22',1)
event('s1-reopen','Classes reopen',1,'MILESTONE','2026-08-31',1)
event('s1-internal1','First internal examination (S1)',1,'EXAM','2026-10-01,2026-10-03,2026-10-05,2026-10-06,2026-10-07',1)
event('s1-internal2','Second internal examination (S1)',1,'EXAM','2026-11-13,2026-11-16,2026-11-17,2026-11-18,2026-11-19',1)
event('s1-fees','Last date of payment of fees',1,'ADMIN','2026-10-30',1)
event('s1-class-end','S1 classes end / AISAT Techquest',1,'MILESTONE','2026-11-27',1)
event('s1-publish','S1 attendance and IA marks publication',1,'MILESTONE','2026-11-30',1)
event('s1-final','End-semester examination (S1)',1,'EXAM','2026-12-07,2026-12-09,2026-12-11,2026-12-14,2026-12-16',1)
event('s1-labs','Completion of all S1 lab exams',1,'MILESTONE','2026-12-22',1)
event('s1-christmas','Christmas vacation starts',1,'HOLIDAY','2026-12-24',1)
event('s1-results','S1 result publication',2,'MILESTONE','2027-01-11',2)
event('s2-start','S2 classes commence',2,'MILESTONE','2027-01-04',2)
event('s2-enrol-start','S2 enrolment begins',2,'ADMIN','2027-01-08',2)
event('s2-enrol-end','S2 enrolment ends',2,'ADMIN','2027-01-22',2)
event('s2-internal1','First internal examination (S2)',2,'EXAM','2027-02-11,2027-02-12,2027-02-15,2027-02-16,2027-02-17,2027-02-18',2)
event('s2-internal2','Second internal examination (S2)',2,'EXAM','2027-03-20,2027-03-22,2027-03-23,2027-03-24,2027-03-30,2027-03-31',2)
event('s2-class-end','S2 classes end',2,'MILESTONE','2027-04-09',2)
event('s2-publish','S2 attendance and IA marks publication',2,'MILESTONE','2027-04-12',2)
event('s2-final','End-semester examination (S2)',2,'EXAM','2027-04-19,2027-04-21,2027-04-23,2027-04-26,2027-04-28,2027-04-30',2)
event('s2-labs','Completion of all S2 lab exams',2,'MILESTONE','2027-05-04',2)
event('s2-results','S2 result publication',2,'MILESTONE','2027-05-15',2)
event('s2-vacation','Vacation starts',2,'HOLIDAY','2027-05-16',2)
event('s2-vacation-end','Vacation ends',2,'HOLIDAY','2027-06-16',2)
pack=dict(version='aisat-2026-s1-v1',title='AISAT · S1 2026–27',
    preparationEnd='2026-12-06',sources=sources,courses=courses,items=items,events=events,
    notices=['No subject-wise exam timetable, paper times or confirmed internal portions were supplied.',
        'Calendar dates are college milestones, not confirmed all-day personal commitments.',
        'S2 calendar is included, but no S2 syllabus has been provided.',
        'All progress starts Unrated or Unattempted; existing user data is never replaced.',
        'PDF text layers differ from the visible Graphics/Life Skills/Python code tables; visible headers were checked.'])
assert len({item['id'] for item in items}) == len(items)
(ASSETS/'catalog.json').write_text(json.dumps(pack,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'{len(courses)} courses; {len(items)} source-linked groups; {len(events)} calendar milestones; {len(sources)} unique PDFs')
