# Implementation status

Last updated: 2026-10-27, Asia/Calcutta.

## Current delivery: first connected V2 implementation

V2 is in implementation, **not declared complete**. This delivery started from clean
`main` at `1d14d90`, refreshed and equal to origin/main. The implementation plan is
V2_IMPLEMENTATION.md; the precise evidence and acceptance journeys are V2_VERIFICATION.md.

Implemented in source:

- A stable 13-destination library: Today, Plan, College, Study, Fitness, Tasks, Projects,
  Capture, Exam Prep, Coding Practice, Review, Daily Reset and Settings. These are working
  flows, with seven new areas alongside the six preserved areas.
- A fixed centre surface that expands from the dock button into the full scrollable library
  and reverses on close. The four outer slots accept any destination in any order. Settings
  includes a live preview, full picker, swap assignment, long-press drag, accessible arrows,
  immediate preference persistence and repair of unknown/duplicate saved IDs. Active unpinned
  destinations stay active and reachable; Back closes the launcher before returning to Today.
- Live Today reads shared dated blocks and the active session. Explicit home check-in is
  dated and persisted. The sculpted ribbon reflects the actual saved day and clock. V1 sample
  moments remain available only in Settings > V1 design preview; original everyday tools,
  water, habits and activity rings remain reachable from live Today.
- Plan stores fixed commitments and flexible work with source IDs, material and first step.
  Gap finding includes saved timetable intervals, other blocks and the active session.
  The optional evening draft uses explicitly labelled approximate routine times, editable
  before confirmation; it creates no commitments until the user confirms today's times.
  Recovery moves missed flexible blocks around anchors; undo is available in the active
  ViewModel session. Duration edits, removal and tomorrow moves are implemented. No routine
  times are automatically inserted or promoted into confirmed commitments.
- One shared focus session retains its source ID, material and resume note across spaces,
  backgrounding and process recreation. A global capsule expands into controls. Late resume
  checks for collisions. Save/end archives actual elapsed time and reviews the linked plan
  block transactionally before clearing preferences. Stable session IDs deduplicate retry.
  Expiry never completes a task, milestone or submission.
  Live Today also retains a bounded 20-minute rest action after home check-in. A stale paused
  timer already archived before process death cannot resume into the same history ID.
- Study selects a task/deadline/topic/project milestone/problem or custom action and resumes
  saved threads. Tasks and College deadlines start the same shared activity. Session source
  links survive title edits until explicitly unlinked.
- Projects stores goals, blockers, ordered milestones and archive state. Checkboxes record
  explicit completion; the next unfinished milestone leads into focus.
- Capture stores notes/links and processed state. Conversion creates one real task in the
  Schedule quadrant, transactionally preserving the original note and preventing duplicate
  conversion on repeated taps.
- Exam Prep stores real course/topic/material input, self-rated confidence and revision dates.
  Coding Practice stores problem/link/difficulty, reported outcome, lesson and retry dates.
  Both lead into focus and provide honest empty states. Material links can be opened.
- Review reads actual saved session snapshots, seven-day recorded minutes and clearly labelled
  stored legacy counts. Fitness adds explicit self-reported training duration/note history;
  routine set checks are not represented as a dated workout history.
- Opt-in plan and timer-review reminders use inexact local alarms. Settings requests Android
  notification permission when needed. Plan notifications expose Start, Delay 10m and Replan
  through the activity, rechecking current plan state. Protected blocks, saved classes and
  active work suppress relevant alerts. Reboot/time-change rescheduling is wired. Delivery
  remains unverified and can be delayed/skipped by Android.
- Glass overlays replay a separate content-only GraphicsLayer with softening on Android 12+,
  denser fallback below it, readable tint and edge lighting. In-page cards use surface lighting
  to avoid recursive capture. Coordinated centre morph, tactile dock press, session capsule,
  moving recovery rows and reduced-motion controls are implemented. Font-scaled dock height
  and measured capsule reservation reduce overlap risk; actual rendering is unverified.
- Settings retains imports, exam target, appearance and data tools. Confirmed tracked-record
  clearing includes V2 records; copy explicitly names retained timetable/targets/Reset records
  and preferences. Clearing is disabled while a session archive is in progress.

## Data preservation

Room advances from version 2 to 3 through seven additive tables in WORKSPACE_MIGRATION_2_3.
The database filename, application ID, package identity, signing and cloud-builder setup
are unchanged. Destructive fallback is removed. Unsupported older schemas fail closed rather
than wiping data; an upgrade from version 1 has no supplied migration and needs investigation
if such an installation exists.

Startup mock seeding and heuristic deletion of water records are disabled. Existing records
are never automatically classified or removed, even if they resemble older sample data.
No attendance, body measurements, completed work or schedule was manufactured in this delivery.

## Verification actually performed

- `python scripts/check_sources.py`: all 48 Kotlin source/test files parse with tree-sitter;
  manifest XML and documentation UTF-8 pass; all 13 destination routes are present; original
  LifeOsViewModel action names remain; additive SQL declarations execute in Python SQLite
  while preserving a legacy-row sentinel.
- `git diff --check`: no whitespace errors.
- Source comparison confirms the existing College, Fitness, Tasks, Reset, Today tools and
  settings imports/targets remain wired. This is source evidence, not end-to-end evidence.

No local compilation, Gradle, emulator or Android test execution. Ten new cloud test methods
are supplied (eight model/interval/navigation cases and two Room migration/transaction cases),
alongside the existing timer/attendance tests. They have **not** been executed here.
No cloud compilation, physical-device rendering, alert delivery or frame pacing was observed.

## Remaining work and unresolved decisions

- External cloud compilation and supplied test execution, followed by complete device
  journeys in V2_VERIFICATION.md. Fix any type/API, migration, layout, motion or reminder
  failure based on that evidence. The 13-space inventory and expanded geometry are first
  implementation choices, not final user acceptance.
- Richer source-card-to-session shared geometry, artwork, full motion continuity and device
  tuning. The source implements real backdrop replay only for dock/launcher/capsule.
- Recurring commitment editing; recovery undo across process death; stronger source deep
  links and wider editing/bulk-management tools; longitudinal per-set training records.
- Per-class attendance events, cancellation, correction/undo and duplicate prevention.
  Existing aggregate correction and target projections remain.
- Exact timetable, official attendance screenshot and confirmed workout program remain
  unsupplied. Automatic detection, app blocking, credential login and online AI remain
  unselected. Naming and logo remain last.

## Historical deliveries

- `6c8b447`: the accepted **V1-only** Today prototype with three sample moments and a
  persisted focus/rest timer. The user later explicitly requested substantially richer V2.
- `40463c6`: V1 workspace continuation preserving editors, attendance projection/correction,
  priority matrix, fitness/rest/nutrition, Reset drafts/packing confirmation and appearance.
- `1d14d90`: requirements and implementation handoff, before this V2 source delivery.

Push success must be reported separately from cloud build or device success. Consult the
current Git log for this delivery's commit; no unobserved builder result is recorded.
