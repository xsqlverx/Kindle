# Attendance reliability implementation

2026-10-27. Baseline `56f8d3a`, clean main, refreshed and equal to origin/main.

The user reports spammed Present/Absent counters, weak press/saved feedback, and button
styling that does not match the app. Their current attendance screenshot/counts are pending.
Do not guess, seed or replace those totals.

1. Add a dated class ledger with a unique course/date/time identity. Commit the ledger and
   aggregate delta in one Room transaction. Repeat marking must be idempotent across screens,
   restarts and concurrent calls. Corrections replace the existing mark; cancelled/unmarked
   classes contribute zero. Keep existing totals and use an additive version 3-to-4 migration.
2. Replace both counter-button paths with one charcoal/lavender control. Show the selected
   class/date, responsive press motion, saving state, and a confirmed saved indicator. Lock
   saved choices behind an explicit Change action. Keep reduced motion and accessible targets.
3. Preserve total correction, clearly reconcile existing marks into a replacement baseline,
   and retain dated records. Supply external-runner tests for migration, duplicates, correction,
   cancellation/undo and failure rollback. Run source/static checks only, update handoff docs,
   commit and ordinarily push to origin/main.

Acceptance: repeated taps never increase one class more than once; correcting Present to
Absent changes attended only; undo reverses that class once; separate periods stay separate;
both screens show the same persisted result. No future classes or missing-baseline guesses.
Existing aggregate rows cannot reveal which historical classes were previously counted.

Implementation and evidence will be recorded below. No local Android compilation or tests.

## Implemented source and boundaries

- `AttendanceLedger` performs compare-and-change writes in a Room transaction with the
  aggregate delta. A primary course/date/start/end key survives identical timetable re-import.
  Duplicate requests are no-ops; stale corrections fail. Cancellation and removal contribute
  zero. Removed marks keep a tombstone. A new mark requires a saved scheduled class, an
  existing real baseline and a class that has started. Each scheduled period counts once;
  multi-hour contact-hour weighting has not been supplied or inferred.
- Additive Room 3-to-4 migration creates attendance_events and adds baselineAt default zero.
  Existing totals and all other tables remain. Totals correction explicitly replaces a snapshot
  through now, retains/locks existing history and prevents earlier periods being counted again.
  Exact historical cutoff imports from an old screenshot are not implemented; inspect the
  screenshot/date before adding its real baseline. An unsupported version-1 upgrade still
  fails closed. No destructive fallback, data deletion or sample attendance import.
- `AttendanceControl` supplies shared dated navigation, class selection, immediate press
  spring/ripple/haptic, in-flight disabling, saved-row pulse and a confirmed haptic only after
  the transaction. Saved marks require Change before correction; cancellation/removal remain
  available. Course history contains only actual new-ledger records. Reduced motion snaps
  custom motion; standard system components follow system animation settings.
- College now exposes real-total entry even for a timetable course without an aggregate row.
  Existing nonzero totals retain their unknown historical provenance. This fix cannot recover
  counts inflated by the old buttons; reconcile from the promised screenshot. No real totals
  have been supplied or modified in this checkout.

## Actual verification

`python scripts/check_sources.py`: 58 Kotlin files parse, manifest/docs UTF-8 and all 13 routes
pass; original ViewModel action entry points remain. Python SQLite executes the seven V3
CREATE statements plus V4 ALTER/CREATE, preserving task and attendance sentinels with the
new default timestamp. `git diff --check` passes. This is SQL/grammar evidence, not Room or
Kotlin type validation.

Nine meaningful cloud-runner regression methods are supplied in AttendanceLedgerTest, and
the existing V2 fixture now migrates to V4. They cover concurrency, corrections, cancellation,
removal, re-import identities, missing/zero baselines, historical locking, invalid/stale writes,
transaction rollback, direct V3 upgrade and reopening persisted marks. No Android tests,
local compilation, Gradle or emulators were run. Cloud build, Room execution, phone animation,
touch response, haptics, TalkBack and large fonts remain unverified. See V2_VERIFICATION.md.
