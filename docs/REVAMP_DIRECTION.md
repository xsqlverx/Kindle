# App revamp: working direction

Status: the first Today prototype was accepted for V1 only. V2 implementation now includes the connected shell, live day/session foundation and working new modules; see STATUS.md, V2_IMPLEMENTATION.md and V2_VERIFICATION.md. Cloud compilation/device verification remain unobserved. Naming and logo work come last.

Latest correction: the user could not immediately see an average day or what to test. The
research-driven pass now centers one next action, a shared short entry flow, a clear agenda,
connected focus/capsule motion and confirmed progress feedback. An optional labelled 60-second
tryout uses the interaction flow without changing real records. EXPERIENCE_RESEARCH.md records
the primary references; this is implemented source, not another proposed catalogue.

Latest attendance feedback: the user reports spammed Present/Absent totals, insufficient
press feedback and mismatched buttons. The dated ledger and shared College/detail controls
now implement one contribution per course/date/period, explicit corrections/cancellation/
removal, press response and storage-confirmed feedback in the existing palette. This replaces
the aggregate counter-button behavior described historically below. Real baseline screenshot
is still pending; no totals were guessed. See ATTENDANCE_RELIABILITY.md and STATUS.md.

## Purpose

2026-10-28 control feedback: most buttons feel unprofessional/out of place, including Whole
day and the everyday-tools entry. The native shared controls and recomposed Today tool shelf
are now implemented in source (CONTROL_DESIGN.md). Keep this consistent action hierarchy,
geometry, typography, focus/disabled states and restrained press motion across live screens.
User/device visual approval remains pending; no existing function or data is removed.

Opening the app should answer: what is supposed to be happening now, what is the next useful action, and how can the day recover when something was missed?

The main difficulty described by the user is losing structure after returning home. Attendance and workout guidance already have practical value. Study currently gets deferred beyond the user's evening commitments.

## Existing scope to preserve

Latest composer request: “Give it a little room” needs a stronger native visual design and
direct custom minutes. The source now replaces preset chips with a large numeric editor,
full-range slider and one-minute adjustment, live placement and a reachable action footer.
The input is not hidden under details. NEXT_MOVE_REFINEMENT.md records the safeguards and
unverified native/cloud boundaries; retain exact chosen time and all source/session behavior.

Keep every current capability across Today, Fitness, College, Tasks, Daily Reset, and Settings. The confirmed fixed centre launcher and four fully assignable outer slots now route the 13-space library. The expanded centre geometry is an implemented first design, pending device feedback; no side restrictions or permanent outer assignments apply.

## User's weekday: approximate, not a fixed schedule

| Window | Reported activity |
| --- | --- |
| 6:30-7:00 AM | Alarm, dozing, getting up; a short morning workout is a wish, not an established routine |
| 7:00-7:40 AM | Bath, breakfast, preparation, bus departure |
| 8:00 AM-1:30 PM | College; exact periods and breaks require the timetable |
| Around 1:45-1:50 PM | Leave college |
| Around 2:30-2:50 PM | Arrive home; afternoon structure breaks down |
| Afternoon | Food, scrolling, sometimes a nap with no reliable end time |
| Around 8:00-9:00 PM | Gym; actual start sometimes closer to 8:30 |
| Around 9:30-10:30 PM | Bath and family prayer; precise boundaries need confirmation |
| Around 10:30-11:45 PM | Call with girlfriend; preserve as a meaningful commitment |
| Later | Records and coding; study often remains undone |

Do not turn uncertain times into strict alarms. Do not infer that arriving home is detectable without an explicit check-in or an agreed mechanism. Family, relationship, and coding time should be represented honestly alongside coursework.

## Reference observations

### Supplied JPG: dark sleep-app interface

Observed: charcoal background, muted lavender, softly lit curved surfaces, pale text, a dominant central visual, compact week history, and a lower section with supporting actions. Depth comes from controlled lighting and variation in surface tone. Some small labels are faint; readability needs independent attention.

Interpretation: the desired quality includes atmosphere, tactile materials, deliberate hierarchy, and visual richness. These qualities can inform a new composition built around this app's purpose. The sleep sphere, sleep score, task arrangement, branding, and exact layout are reference-specific.

### Supplied MP4: expanding action control

Inspected using local FFprobe metadata and sequential frames across the approximately 5.5-second clip. The central purple action button rises from a recessed white dock, changes from a circle into a horizontal pill, reveals several tools, and reverses the sequence.

Interpretation: compact controls can reveal additional capability through connected, deliberate motion. A final navigation decision is still pending; the reference demonstrates an interaction principle rather than a ready-made navigation scheme for five existing areas.

## Proposed visual direction

- Deep charcoal with smoke-violet surfaces and pale, readable text.
- Soft edge lighting and restrained dimensional accents at important interaction points.
- A clear typographic hierarchy and varied compositions suited to each activity.
- A distinctive Today composition based on the day's sequence and current action. A day timeline with a travelling highlight is one candidate for exploration, not a finalized signature element.
- Motion should explain a change: an action becoming an active session, a tool unfolding from its trigger, or a missed block moving into a revised plan.
- Distinct layouts for attendance, study, workout logging, and reflection within one shared visual system.

## Proposed depth: practical additions to evaluate

1. **Arrival and recovery flow.** A quick home check-in, space for food or a bounded rest, one manageable study action, and a revised plan after a missed start. Suggested blocks stay distinct from confirmed commitments.
2. **Study execution.** Connect courses and deadlines to an actual session: chosen topic or task, relevant material, first step, a timer when useful, and a record of where to resume. The app needs user-supplied study content before it can offer subject-specific instructions.
3. **Attendance reliability.** Reconcile a dated baseline from the user's screenshot; then support present, absent, cancelled, and unmarked classes, corrections, and protection against duplicate marking. Calculations must reflect the known class counts and configured target. The attendance screenshot has not yet been supplied.
4. **Workout continuity.** Make today's routine actionable, surface previous set data and the next exercise, and preserve workout, rest, hydration, weight, and meal capabilities. Routine changes require the user's actual program.
5. **Planning and follow-through.** Give reminders useful actions such as start, delay, or revise the plan. Respect protected commitments and avoid repeatedly interrupting an active session. Keep all work, including coding and records, visible when allocating time.

These are candidates grounded in the reported day, not a requirement to add five more tabs. Additional modules should earn space through a recurring use case. No final side-panel design has been selected.

## V1 source diagnosis (historical; current V2 status is in STATUS.md)

- Native Android app using Kotlin and Jetpack Compose, with Room-backed data.
- The manifest currently declares vibration permission. No application notification/alarm scheduler, alarm receiver, or notification permission declaration was found in the checked main source.
- A timetable-derived current/next lecture display exists. A dependable whole-day plan and recovery system would require additional behavior and data.
- Existing records include tasks, habits, timetable entries, assignments, attendance totals, workout sessions and sets, weight, hydration, and daily reset entries.
- Current attendance records store aggregate counts by course. Per-class history and duplicate prevention would require a data-model change.
- Existing screenshots reveal repeated large cards, truncated labels, and severely compressed academic rows. The current runtime has not been launched or tested during this discussion.

## First design study to develop

Show the same proposed Today screen in three situations: an ordinary college day, the transition home, and a missed afternoon start. Demonstrate how the same information hierarchy and actions respond to each situation. Establish an original visual language and useful behavior before extending the direction across the full app.

## Open inputs

- Actual timetable and accurate attendance screenshot.
- Actual workout routine if changes to guidance are wanted.
- Device feedback on the workspace continuation. The first Today study was already enthusiastically accepted.
- Device refinement of the implemented expanding centre launcher; the five-position contract is confirmed.

The user has already completed the five discovery questions. Future discussion should use concrete examples and prototypes rather than repeat the intake questionnaire.
