# V2 verification and cloud/device handoff

2026-10-27. No local Android compilation, Gradle, emulator or Android test execution.

## Source evidence

Run `python scripts/check_sources.py` after installing the optional tree-sitter packages
into `.tools/python` as described in that script. The directory is ignored by Git.

The source check parses all Kotlin files, strictly decodes UTF-8, parses the manifest,
checks all 13 destination routes and preservation of original LifeOsViewModel action names,
and executes the old/new SQL declarations in Python SQLite with a preservation sentinel.
This last check validates SQL syntax and additive behavior only, **not Room's schema validation**.
`git diff --check` checks whitespace. Neither establishes Android types or runtime behavior.

The project still pins Compose BOM `2024.09.00`. The new source uses 1.7-era GraphicsLayer
record/replay and standard animation APIs; no dependency upgrade, signing or builder change.
API references inspected: [GraphicsLayer](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/layer/GraphicsLayer)
and [graphics modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers).

## Supplied tests for the external runner (not run locally)

- `WorkspaceModelTest`: all 13 destinations in each of four slots, assignment swaps, corrupt
  layout repair/round trip, exact interval boundaries, ongoing and overlapping commitments,
  lack of space and invalid duration/window. Eight test methods.
- `WorkspaceDatabaseTest`: build a version-2 fixture derived from the baseline entities,
  migrate through Room, validate the new schema and preserve task/attendance/weight records;
  idempotent capture conversion, idempotent session archive and unchanged task completion.
  Two test methods. This fixture is source-derived, not a copy of the user's database.
- Keep the existing timer and attendance tests. Existing screenshot/sample tests are not V2
  acceptance evidence; add/update screenshots using real rendered results from the cloud/device.

## Device acceptance journeys

1. Upgrade an installed V1 with real data. Verify timetable, assignments, attendance, workouts,
   sets/rest, meals, hydration, weight, habits, tasks and Reset records remain. Fresh storage
   must stay empty; startup must not seed sample measurements, completions or schedules.
2. Open the centre launcher on every destination. All 13 spaces must be reachable. Open,
   close, reverse quickly, choose a space, tap scrim and use system Back. The centre surface
   must visibly unfold and return, without a separate unrelated panel appearing.
3. Pin Settings/College/each new destination in each slot. Swap by picker, long-press drag and
   arrows. Unpin the active destination, rotate, kill/reopen and restore. Validate safe fallback
   from unknown/duplicate preference IDs. No outer slot or side is reserved.
4. Check in at home. Add explicit evening commitments using actual times; none are inserted
   automatically from the approximate routine. Review/edit the optional evening draft and
   explicitly confirm today's times. Cancelling must create no records. Link a task or deadline to a feasible 25-minute window, add
   material and a first step. Today and Study must read that exact block and source ID.
5. Miss a start. Recover around confirmed commitments and saved classes; inspect moving
   timeline rows. Undo, shorten, remove or move to tomorrow. When there is no room, the
   original block stays visible and the app asks for a smaller/later window. Recovery undo
   is currently retained for the active ViewModel session, not across process death.
6. Start focus from Tasks, College, Plan, Study, Projects, Exam Prep and Practice. Navigate
   through spaces, pause, background, rotate and recreate the process. Resume notes persist.
   Continue must refuse a collision caused by a late pause. Expiry never completes a task.
7. Save/end the session; inspect Review and Study. One history row per session even after
   repeated save attempts or interruption. If storage fails, retain the paused session and note.
   Continue the same source with its material/resume point. Source records can be completed
   explicitly in their own spaces. Deleting a source must not destroy its historical snapshot.
   Simulate termination after Room archive but before preference clearing: retry must preserve
   the first elapsed-time snapshot and revised note, and Continue must clear the stale timer.
8. Build a project, add/check milestones, edit a blocker, focus on the next step and archive/
   restore. Capture a link, edit it, convert twice to a task, and find the original in Processed.
9. Add real exam topics, set self-rated confidence and 1/3/7-day revision dates, open material
   and study. Add a coding problem, report its outcome, save a lesson and schedule a retry.
   Due retries must reflect the chosen dates, with no inferred mastery or solved counts.
10. Log an actual training duration and note from Fitness. Review must label it self reported.
    Existing routine checkboxes are not dated workout history or proof of training today.
11. Enable reminders in Settings; grant/deny/revoke Android permission. Test plan Start,
    Delay 10m and Replan on cold and warm launches, timer review alerts, edited/deleted blocks,
    reboot, time changes and protected periods. Delivery uses **inexact** alarms and may be
    delayed or suppressed by Android. Quiet-period alerts are skipped; recovery stays in app.
12. Inspect the dock/launcher/capsule over changing content on Android 12+ and the denser
    fallback on older Android. Check readability, clipping, stale frames, flicker, GPU frame
    pacing, large fonts, landscape, TalkBack traversal and drag alternatives. Reduced motion
    and system animator settings must keep all actions available without decorative delay.
13. Exercise the existing imports/targets/appearance/data tools. Clearing tracked records is
    an explicit confirmed action and now includes V2 records; its text names retained data.
    The button is unavailable while a session archive is in progress.

## Known boundaries / next stages

- V2 is **not declared complete**. Visual quality, frame pacing, Room migration correctness,
  permissions, reminder delivery and complete journeys still require cloud/device evidence.
- No per-class attendance events/undo/duplicate protection yet; existing aggregate tools remain.
- No official attendance screenshot, exact timetable or confirmed workout program supplied.
- No automatic home detection, blocking, credential login, online AI or new branding.
- Planning uses explicit dated blocks plus the existing saved timetable. There is no recurring
  commitment editor yet. Existing legacy records may originate from older sample seeding;
  they are preserved rather than automatically classified, removed or called verified facts.
- Source-card-to-session shared-element geometry, richer in-page artwork and full motion
  refinement remain; the first connected motion is the centre morph, dock press, session
  capsule and animated recovery rows. Cards within the recorded page use tint/edge lighting;
  the dock/launcher/capsule alone replay the softened backdrop. This avoids recursive capture.
- Active focus still uses the existing preference store with a stable UUID; history/plan are
  committed together in Room. Timer archive is deduplicated if interrupted before prefs clear.
- New-topic/attempt editing and project archive are implemented; richer bulk management,
  source deep links from every history entry and longitudinal per-set training history remain.

Push success is a repository delivery result. Record cloud compilation and device results
separately when observed; no successful build or delivered alert is assumed here.
