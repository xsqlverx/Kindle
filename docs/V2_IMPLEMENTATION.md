# V2 implementation stages

2026-10-27. Source baseline: `1d14d90`; clean `main`, refreshed against origin/main.

## Code-grounded approach

`MainActivity.kt` currently routes five enum tabs, Settings is a separate overlay, and
`TodayPrototypeViewModel` persists only the latest timer in preferences. Room is version 2
with a destructive fallback. Keep the original screens and action APIs; extend routing with
stable IDs, add an explicit 2-to-3 migration, and replace the live Today entry point while
keeping the prototype available as a labelled design preview.

1. **Connected shell.** Thirteen stable destinations; four unrestricted persistent slots;
   a centre surface morphing from button into a scrollable library and reversing on close.
   Settings provides a live preview, all-library picker, swap-on-assignment, and left/right
   accessible reorder controls. Back closes overlays before returning to Today. Slot edits
   never navigate away from an active unpinned destination.
2. **Shared afternoon.** Explicit dated home check-in, user-confirmed commitments, timetable
   collision checks, flexible task/deadline/project blocks, gap finding and reversible recovery.
   Live Today reads these records. A single persistent session capsule spans destinations;
   sessions retain source identity, material, resume note and actual elapsed time. Timer expiry
   requires review and never completes a task. Save history before clearing the current session.
3. **Working depth.** Projects with milestones and blockers; Capture with conversion to a real
   task; Exam Prep with topics, confidence and revision dates; Coding Practice with problem,
   outcome, solution notes and retry queue. Review reads real sessions and existing records.
   No placeholder destinations. Study provides source selection and history-based continuation.
4. **Follow-through and verification.** Opt-in local reminder delivery/actions, lifecycle and
   permission handling; source checks, cloud test definitions and device acceptance script.
   Refine glass and motion with physical-device evidence after the external builder.

## Destination inventory

| Destination | Recurring job / stored records / connection |
| --- | --- |
| Today | Now, next, explicit home check-in, recovery; shared plan and current session |
| Plan | Dated fixed/flexible blocks, source links, undo recovery; informs Today and Study |
| College | Existing timetable, attendance, assignments, exams; assignments lead to work |
| Study | Material, first step, timed work, resume notes; source links and session history |
| Fitness | Existing workouts, sets/rest, meals, water, weight; retained in full |
| Tasks | Existing priority matrix, filters, CRUD; focus retains task ID |
| Daily Reset | Existing triage, mood, packing and notes; retained in full |
| Settings | Navigation layout, motion, appearance and existing import/data tools |
| Projects | Project goal, blocker and ordered milestones; next milestone leads to focus |
| Capture | Notes/links, inbox and processed state; idempotent conversion to a real task |
| Exam Prep | Course/topic, self-rated confidence, revision due date; study and review |
| Coding Practice | Problem/link, difficulty, outcome, notes, retry due date; focus and review |
| Review | Actual session trail and saved resume points, existing task/reset/workout data |

No approximate routine becomes a confirmed commitment automatically. Existing timetable data
is user-editable, not verified against the absent official timetable. Naming/logo stay deferred.

## Acceptance / evidence boundaries

- Put Settings, College, and any new destination in each of the four slots; reorder, restart,
  corrupt saved IDs, and unpin the active destination. The launcher always exposes all 13.
- Open/close via centre, scrim, Back and selection; rapid reversals, large fonts, TalkBack,
  narrow/landscape screens, reduced motion and system animator scale require device checks.
- Check in, select a task and material, schedule around a confirmed commitment, miss a start,
  recover/undo, start/pause/resume across destinations and process recreation, save a resume
  note, inspect history, continue the same source. Task completion remains explicit.
- Migrate an existing version-2 database with populated tables; preserve every old row.
- Exercise every legacy screen/import/data tool. No signing or cloud-builder changes.
- No local compilation, Gradle, emulator or Android test execution. Source checks cannot
  establish Android type correctness, cloud compilation, touch response or rendering.

Progress and actual verification are recorded in STATUS.md; this document is the stage map.

The subsequent daily-use pass starts from `4951ae3`; EXPERIENCE_RESEARCH.md records the latest
feedback and primary research. It implements the guided chooser/window/focus/continuation loop,
safe tryout, agenda, grouped searchable launcher and richer motion. This stage map's baseline
diagnosis above is historical; do not rebuild the original five-tab shell.

Current first delivery: stages 1–3 have working source implementations; stage 4 includes
opt-in plan/timer reminders, source checks and ten supplied cloud test methods. Cloud
compilation, Android test execution, rendering, delivery and device refinement remain open.
