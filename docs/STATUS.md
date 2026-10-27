# Implementation status

Last updated: 2026-10-27, Asia/Calcutta.

## Current delivery: research-driven daily use

Baseline `4951ae3`, clean main and equal to origin/main before editing. The user understood
the feature vision but could not immediately understand testing or an average day. They
prioritized reduced cognitive load and rewarding motion, and authorized research, implementation
and push without another design pitch. Primary sources and design observations are in
EXPERIENCE_RESEARCH.md. There is no claim that engagement, dopamine or cognitive load was measured.

### Implemented in source

- **One next move on Today.** Actual active work/expiry comes first, then saved class or fixed
  time, explicit home check-in, a missed-start recovery, a chosen plan block or a concrete
  source action. The task title is the main visual. Short reasons come from real priorities,
  deadlines, due revisions/retries, open coding problems and project milestones. Completed,
  deleted, archived, blocked-project and Drop-quadrant work do not become automatic busywork.
  Future revisions remain manually reachable without being proposed as due now.
- **A short, shared entry flow.** Choose an existing source or name one action, pick a duration
  and see the first free window. Material, resume point, first step and custom 1–180-minute
  duration are progressive detail. A new real action becomes a real task and linked block
  together in Room; starting now follows that successful placement. Future work is saved,
  never started early without the user's start action. Fresh source/interval checks run again
  on commit. Drafts persist, and ongoing work is not overwritten.
- **An immediately available tryout.** Today exposes **Try the daily loop · 60 sec**; Settings
  can replay it after dismissal. It uses the same chooser, real countdown, focus surface,
  capsule, pause/continue and resume-note interaction. It is labelled PREVIEW throughout,
  can run during a commitment, and creates no task, plan block, study history or timer alert.
  The result is transient UI feedback, not a study record. A pre-existing real draft is backed
  up and restored. Existing actual work must be saved before another session can start.
- **A connected focus surface.** Focus grows from the touched card or capsule in the same
  root, with actual elapsed arc, a restrained travelling light, reachable pause/finish controls,
  progressive material and an in-place resume-note step. Minimize returns to the capsule.
  Card bounds, page transitions, dock springs, scene lighting, changing card sizes, the
  sculpted three-stage ribbon and earned week traces connect actions to visible changes.
  Reduced motion removes custom travel/loops while retaining every action.
- **Real completion feedback.** Saving a real session returns to Today after Room confirms
  the record, with its actual elapsed seconds/minutes, note and a short completion bloom.
  Marking a linked task/milestone complete or a deadline submitted is a separate explicit
  action. Its confirmed state then appears. The preview has its own completion response and
  never contributes to real history or earned traces.
- **A legible day.** Today and Plan share an agenda of saved classes, confirmed anchors and
  flexible blocks. Plan offers the same next-move flow; detailed editing, future blocks,
  duration/tomorrow/removal, the full-day ribbon, bulk recovery, undo and the editable evening
  draft remain available. No approximate routine becomes a confirmed schedule automatically.
- **A useful launcher.** The centre still morphs into the library. Search, a quick next-move
  action and intent groups expose all 13 destinations. Groups affect discovery only. Every
  destination still fits any outer slot, with live preview, picker, drag, arrows and persistence.
- **Consistent follow-through.** Tasks, College deadlines, Study, projects, topics, coding
  practice and Review continuations enter the same short flow. Explicit task/submission/
  milestone changes update associated plan reservations without rewriting saved session
  snapshots. Success messages are non-blocking snackbars; errors stay visible inside an
  open work/focus surface. Old acknowledgement popups are removed.
- The first introduction lands on Today once; subsequent navigation restoration is retained.
  All original Today tools, habits, hydration, activity rings, College/attendance/exam editors,
  Fitness/sets/rest/meals/weight, task tools, Daily Reset, imports, appearance and data controls
  remain wired. V1 sample moments remain a separately labelled Settings preview.

## Data and platform boundaries

Room remains version 3 with the existing additive 2-to-3 migration and database filename.
This pass changes queries and behavior, not entity columns or schema. No destructive fallback,
startup mock seeding, heuristic record deletion, branding, package/signing or builder changes.
Unsupported version-1 installations still have no supplied migration; fail closed rather than wipe.

No automatic home/activity detection, app blocking, credential login or online AI. No invented
attendance, measurements, task completion or readiness/productivity score. Older stored data
may contain historical samples; preserve it rather than guess its provenance.

Opt-in inexact plan/timer reminders remain. Preview timers are excluded from alerts and do
not suppress real plan notifications. Delivery, permissions and device behavior are unverified.

## Verification actually performed

- `python scripts/check_sources.py`: **54 Kotlin files** pass grammar parsing. Manifest XML,
  strict documentation/source UTF-8, all 13 routes, original LifeOsViewModel action names,
  disabled mock/destructive startup paths and additive SQL declaration checks pass.
- Python SQLite executes the source-derived V2 fixture and existing migration declarations
  while preserving a legacy sentinel. This is not Room schema or Android runtime validation.
- `git diff --check`: no whitespace errors. Source inspection confirms original routes/action
  APIs remain. No Gradle, emulator, local Android compilation or Android test execution.

Cloud tests supplied, **not run here**: 15 DayGuide decision/window/continuation/preview cases,
9 navigation/interval/library cases and 3 Room migration/transaction/state cases (27 methods),
plus the existing timer/attendance tests. Device journeys are in V2_VERIFICATION.md.
No external cloud-build result, phone rendering, frame pacing, touch response, accessibility
journey or reminder delivery was observed.

## Next required evidence / remaining work

- External compilation and test execution; run the short loop and ordinary-day journeys on
  the device. Repair observed type/API, state, gesture, keyboard, large-font or animation failures.
  Source checks and a push are not build/device proof; V2 is not declared complete.
- Tune the root bounds transition, glass readability, micro motion and frame cost using actual
  rendering/frame evidence. The focus surface uses custom bounds interpolation rather than
  assuming newer experimental shared-element/Expressive APIs exist in the pinned BOM.
- Recurring commitments, undo across process death, deeper bulk management/source navigation,
  longitudinal training history and per-class attendance events/correction/duplicate protection.
- Official attendance screenshot, exact timetable, confirmed workout program and name/logo
  remain unresolved. Preserve family, relationship, coding and training commitments.

## Historical deliveries

- `6c8b447`: accepted Today prototype, explicitly excellent for V1 only.
- `40463c6`: V1 workspace continuation with the original tools preserved.
- `1d14d90`: V2 requirements and implementation handoff.
- `4951ae3`: first connected 13-space V2 foundation; source checks only, no observed cloud build.

Use the current Git log for this delivery's commit. Report an ordinary push separately from
external cloud compilation and physical-device results.
