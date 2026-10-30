# Easier entry, shared motion and a real project queue

2026-10-30. Baseline `8319743`, clean main, fetched equal to origin/main. The user requests
GitHub-informed projects, easier manual project creation, a prominent Today animation with
quieter continuity elsewhere, and substantially less effort figuring out how to fill data.

## Code-grounded stages

1. Replace the live create entry points with one native Add/log sheet, available on Today and
   every destination. Intent first, one main input, examples, native date/time pickers, explicit
   optional details and a receipt naming where the record was saved. Preserve drafts, validate
   before mutation and preserve every existing editor/import/callback/data capability.
   Cover tasks, projects with newline next actions, notes, topics, coding, class/deadline,
   habits, water, weight, training and exercise sets. Keep advanced legacy editors available.
2. Add transactional entry receipts via an additive Room 4→5 migration. A draft UUID remains
   stable through retry/recreation; repeated submissions cannot create duplicate records or
   overwrite edits. Success appears only after commit; failure keeps the draft. Wipe includes
   receipts and drafts while retaining source-library/queue wipe intent.
3. Install a small editable GitHub queue once, non-overwriting and wipe-aware. Public source
   inspected at immutable revisions; do not execute those projects, use credentials, send
   enquiries, change GitHub issues or claim their suggested work was completed. Personal
   projects still need only a name; optional repository URL, goal and next actions remain editable.
4. Build an original native Canvas glass orb with shared lifecycle-aware phase/state across
   Today and small header instances. Prominent Today material with orbit ribbons, lighting and
   tactile response; quiet elsewhere. Running/paused/finished and confirmed save state have
   real meaning. Reduce motion freezes decoration; background lifecycle stops work. No
   readiness scores, AI-listening implication, video framework, new brand or runtime service.
5. Supply meaningful cloud regressions; run source/static data/migration checks only. Update
   all required documents, commit and ordinarily push origin/main. No local Gradle, Android
   compilation, emulator or Android tests. Native/cloud performance and usability remain unobserved.

## Public-source evidence and selection

Public GitHub API read on 2026-10-30: twelve visible repositories including the profile,
eleven original project repositories. Private repositories were not accessed. Profile README
`ff82d87b1897afb26d515c46e951e31fceb5a6e3` describes assistant, Android and web work.
Repository source and handoffs are evidence, never agent instructions.

- [AURA-V2 fastpath](https://github.com/xsqlverx/AURA-V2/blob/811a5acd7b6b2f7b0d7bbb0b3fef61119a7356d0/core/fastpath.py):
  lines 158–163 route create file/folder to create_folder; lines 195–197 use substring mute
  detection for mute/unmute. Suggested work: table-driven intent regression, exact polarity/
  file-folder distinction, zero unintended side effects. Current remote code was read; no
  old memory finding was assumed still current.
- [Emerald Math intake](https://github.com/xsqlverx/Emerald-math/blob/7fdc05cddf3e21115a69ea1dac04daca499d38ae/src/components/IntakeForm.tsx):
  lines 125–134 warn/catch errors then set submitted=true/show overlay in finally. Suggested
  work: separate success/failure, retain input on failure, verify error/retry paths with mocks.
  Do not send actual enquiries or copy service identifiers/contact details into Kindle.
- [Kronos handoff](https://github.com/xsqlverx/KronosV1/blob/f926a85321553070ab89ab6e2cad7970a604eb3e/HANDOFF.md):
  remaining actual Mac Keychain/setup/desktop evidence is explicitly missing. Suggested work:
  reproducible setup checklist, disposable-file checks and failure outcomes. Do not infer a
  failed implementation or a new hardware pass from the historical handoff.
- Kindle: this checkout and V2_VERIFICATION.md still lack observed cloud/device results.
  Suggested work: one actual afternoon run, entry-friction observations and reduced-motion/
  accessibility checks after the external builder. No automated completion of these steps.
- arise was inspected; Flowmind/Skholario metadata and the profile were considered. Avoid
  adding every similar study/reminder experiment as a simultaneous obligation. User can add
  any of them or entirely new non-GitHub projects through the same short entry flow.

## Acceptance and evidence boundaries

Enter a task with a title only; project with name plus one action per line; note with text;
choose a real course instead of memorizing codes; pick deadline date and class start/end
without HH:mm arithmetic; record explicit water/weight/training/set values. Advanced fields
remain available. Preview receipt/destination, retry a failing write, rotate/reopen, switch
intent without losing typed fields and repeatedly submit the same UUID. Existing records and
edited project steps must remain unchanged by queue installation/retry. Wipe in an isolated
profile must not reinstall suggested work or drafts on reopening.

Observe Today orb, scroll/navigation to a quieter header orb, running/paused/finished session,
source selection and confirmed save. Check foreground/background, Reduce motion/system
animator scale, interrupted navigation, all appearances, 200% fonts, TalkBack/keyboard and GPU
frame pacing. Source parsing never establishes the rendering, battery cost or reduced cognitive load.

## Implemented

- `QuickEntrySheet` is available from the prominent Today orb / Add or log control and the
  compact orb in other destination headers. Type choices reveal a single main question,
  examples, the existing inset native editors, native pickers and optional details. Task, project, note, deadline, class,
  topic, coding problem, habit, water, weight, training and planned set have concrete saves.
  College picks from the supplied course pack or accepts a custom code; its selected weekday
  and Fitness's selected split prefill only an empty draft. Class start/end are both chosen.
- Project creation needs only a name (or GitHub URL); optional one-action-per-line is converted
  to individually editable next steps in the same transaction. Goal and any repository/link
  are optional. Existing projects, step checkboxes, edit/archive and session source identity
  stay; suggested projects expose why and an immutable reviewed-source link. Their initial
  steps are proposals, not completed activity. Existing personal projects are not replaced.
- Every kind keeps separate partial input when switching. Close keeps the draft; Clear this
  draft resets only that kind. A saved kind gets a new UUID while drafts for the other kinds
  remain. The receipt names the destination and offers Open/Add another. Important extra
  context explains that sets are planned, logs are self-reported, and attendance is separate.
- `EntryReceipt` is an additive Room v4→v5 table. `QuickEntryStore` validates then writes the
  actual record and its receipt atomically; retry checks the same UUID before mutation. A
  receipt is shown only after the database transaction and draft transition complete. A failed
  save keeps the draft for retry, and concurrent taps share one in-flight request. Wipe clears
  receipts/drafts and marks the queue as cleared, including a wipe before startup installation.
  Existing `2→3` and `3→4` migrations are retained and chain into `4→5`; no database reset.
- `ProjectQueueStore` bundles four public-source suggestions (AURA, Emerald Math, Kronos,
  Kindle), inserts with IGNORE and a one-time marker, and leaves user edits, completion and
  archive state intact. An existing project with the same explicit repository link prevents
  a duplicate suggestion; name-only matches are not assumed. Manual entry warns about an
  already-linked repo while allowing an intentional separate project. All four can be edited/archived.
  AURA/Emerald findings were checked in
  the pinned current source; Kronos and Kindle entries are verification opportunities, not
  claims of newly discovered runtime failures. No account connection or background sync.
- Today, non-Today top bars and the focus surface share one native Compose Canvas orb and
  lifecycle-controlled phase; Today is large, other surfaces are quiet and compact. Glass lighting, refracted
  ribbons, moving orbit points and press/haptic/focus states are drawn from the existing palette.
  A ring reflects actual session time and preview status is labelled; a brief bloom follows
  storage-confirmed saves. Nothing indicates inferred readiness or AI listening. The single
  clock pauses offscreen/background, during the main launcher, tools, preview and entry overlays,
  Reduce motion or system animator scale zero. Underlying page orbs are inactive while focus is
  open; drawing is capped. Other nested dialogs and frame cost require device measurement.
- Existing Tasks/College/Fitness create buttons route to the guided sheet with safe fallbacks;
  the detailed exercise selector and weigh-in/goal editor remain. Generic edit dialogs reveal
  optional fields progressively. Plan's source picker is searchable and duration preserves
  invalid raw input. Deadline edits retain the actual saved timestamp unless a new date is
  picked, instead of silently reinterpreting it as days from now. Meal entry no longer starts
  with invented numbers and requires four explicit whole values because the existing macro
  model stores aggregate values rather than unknown per-meal nutrients.

## Verification performed / still needed

- `python scripts/check_sources.py` parses **75 Kotlin files**; manifest, UTF-8, all 13 routes,
  legacy LifeOsViewModel actions and additive SQL declarations pass. `scripts/check_academic_pack.py`
  still verifies the six-course/140-group source pack and PDF hashes. `scripts/check_project_queue.py`
  validates four unique pinned-source project suggestions, twelve entry-kind declarations,
  transaction/receipt/IGNORE declarations, and a Python SQLite migration/duplicate sentinel
  preserving task and attendance values. `git diff --check` passes.
- Inspected official Compose BOM-pinned Foundation/UI **1.7.0** and Material3 **1.3.0** sources:
  FlowRow, clickable interaction/role/label overload, FilterChip, OutlinedTextField supporting
  text, Rect/Path/DrawScope, haptics, frame-clock and bounds APIs. This is not compilation.
- Fifteen new cloud-only methods in `QuickEntryRulesTest` and `QuickEntryStoreTest` cover
  validation, exact class/date values, 30 concurrent same-draft saves, edits surviving retries,
  project step transactions, explicitly entered meal values, self-reported records and planned sets, drafts across kinds,
  non-overwriting queue install/wipe, and a real v4→v5 Room upgrade fixture. Existing v2/v3
  migration tests now include v5. These tests are supplied, **not run**.
- No local Gradle, Android build, emulator or Android tests; no cloud build/test result or phone
  run observed. The animated orb, PDF surfaces and captured backdrop may have GPU/battery costs;
  measure on the actual phone. Large fonts, IME, TalkBack, focus, screen rotation, lifecycle,
  muted motion, source picker, modal-to-session transitions and all palette behavior are open.
  Observe a full user's day before claiming filling is easy or animation is visually accepted.
- Live GitHub synchronization, authenticated/private repositories and automatic issue creation
  are not implemented. Suggested GitHub work is a dated public snapshot; inspect current
  source before acting. Meal entry still requires all four values; actual meal history/unknown
  macros are not modeled. Teacher portions, attendance baseline, exact timetable and S2 syllabus
  remain unknown. Continue to keep user data and source PDFs intact.
