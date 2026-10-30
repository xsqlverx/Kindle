# Working on this app

Read `docs/PROJECT_CONTEXT.md`, `docs/REVAMP_DIRECTION.md`, `docs/STATUS.md`, `docs/V2_EXPERIENCE_PLAN.md`, `docs/V2_IMPLEMENTATION.md`, and `docs/V2_VERIFICATION.md` before making changes. Keep those files current when decisions, implementation, or verification status change. `docs/TODAY_PROTOTYPE.md` records the historical first implementation, now an optional Settings preview.

## Established requirements

- Native Android, Kotlin, Jetpack Compose. Preserve all existing capabilities in Today, College, Fitness, Tasks, Daily Reset, and Settings.
- The user enthusiastically accepted the Today prototype at commit `6c8b447`. Continue its charcoal, smoke-violet, pale-lavender visual language and sculpted day ribbon. Extend it with distinct layouts for each activity.
- Later device feedback explicitly limits that praise to **V1**. V2 must address the reported disconnection and add more spaces, meaningful capability, unified behavior, much richer motion, and liquid glass. Do not use the earlier approval to freeze the current quiet presentation or five-destination structure. The detailed V2 plan is proposed, not an already approved final layout.
- References supply material and interaction principles; do not copy their layouts and substitute this app's data.
- The primary payoff is knowing what should happen now, the next concrete action, and how to recover after a missed start. The main failure point is the transition home from college.
- Confirmed navbar: four fully assignable outer slots around one fixed centre launcher replacing the current middle College button. Any available destination from the full 12+ tab library can occupy any outer slot in any order through a redesigned Settings destination. The centre button expands into the launcher. No permanent outer assignments or left/right category restrictions. Exact expanded geometry and module inventory remain open; the earlier nine-space split is not a locked plan. Preserve all existing areas and add genuinely new useful capabilities.
- Naming and logo come last. The user dislikes both Life OS and Kindle; do not introduce another brand without discussion.
- Approximate routine times and prototype scenarios must stay distinct from actual schedules, detected activity, or confirmed records.
- Do not invent attendance, body measurements, study progress, successful actions, alert delivery, or cloud-build results.
- Preserve user data. Do not reset the database or remove features to work around build errors.

## Build and delivery

The user explicitly requested **no local compilation** and has an external cloud builder. Do not run Gradle, local Android builds, emulators, or Android test execution unless the user changes that instruction. Perform source/static checks, supply meaningful cloud tests when necessary, and report unverified build/device behavior honestly.

The user authorized pushing this ongoing revamp to `origin/main`. Verify the checkout and remote state, commit the intended files, and use an ordinary push. Do not force push or change the cloud builder/signing setup. Push success is separate from cloud compilation success.

## Continuity

Latest entry/motion/projects request (2026-10-30): read docs/ENTRY_ORB_PROJECTS.md. The user
struggles with how to fill the app, wants easy custom projects, GitHub-informed project work,
and a prominent Today animation shared subtly elsewhere. Preserve the 12-kind guided Add/log
flow, per-kind drafts, transaction-confirmed receipts, 4→5 additive migration, editable/wipe-aware
GitHub suggestions and real-session orb. Do not infer progress or private repository access.
No local Android build/tests; source parsing is not cloud/device/visual/usability verification.

Academic input (2026-10-30): all seven listed S1 syllabus PDFs and the S1/S2 calendar have
now been supplied. Read docs/ACADEMIC_IMPORT.md. Preserve the deduplicated original assets,
source/page/hash metadata, stable IDs and non-overwriting import. The editable roadmap's
initial budget/order are suggestions, not confirmed availability, teacher portions or progress.
Keep calendar dates apart from actual subject papers/times/personal blocks; S2 syllabus,
attendance screenshot, exact timetable and completed coverage remain unknown. Keep user wipe
intent, explicit completion and source-linked session/history behavior. No local Android
execution; source checks and supplied cloud tests do not establish a build or device result.

Latest composer feedback (2026-10-28): “Give it a little room” should feel more native and
aesthetic, with exact custom minutes as the main input, not four preset prompts or a hidden
field. Read docs/NEXT_MOVE_REFINEMENT.md. Preserve raw duration drafts, validation at placement,
source identity, preview backup, session protection and collision checks. Keep 1–180 bounds
visible and do not treat source checks as cloud/native visual verification.

Latest control feedback (2026-10-28): most buttons feel unprofessional/out of place, naming
Today's Whole day and Habits/water/everyday tools. Read docs/CONTROL_DESIGN.md. Use the shared
native controls and deliberate primary/secondary/disclosure hierarchy in live UI; preserve
callbacks, data, disabled/focus/accessibility behavior and reduced motion. Source-only evidence
does not confirm native visual quality, cloud compilation or final user approval.

Latest feedback after `4951ae3`: a feature library alone did not make the average day or
testing path understandable. The user prioritized removing cognitive load and rewarding
motion, requested internet research, and explicitly asked for implementation/push without
another design pitch. Read `docs/EXPERIENCE_RESEARCH.md` and the current STATUS.md before
reworking the guided daily loop. Keep one obvious next action, progressive detail, honest
feedback and the separately labelled no-records tryout; do not replace them with a module
catalogue or invented productivity metrics.

Update the project documents as part of implementation. Preserve explicit requests, accepted visual decisions, uncertain inputs, remaining requirements, and evidence. Do not store credentials in these files. The user's attendance screenshot and exact routine/timetable have not yet been supplied.

Attendance follow-up: read docs/ATTENDANCE_RELIABILITY.md. Present/Absent spam, weak press
confirmation and mismatched styling are explicit feedback. Both marking entry points now
use the dated Room ledger and shared control; do not restore unscoped increment buttons or
replace persistent deduplication with a cooldown. The current attendance screenshot is still
pending. Preserve aggregates, reconcile explicit baseline replacements, and distinguish
source checks from the unobserved cloud build/device results.
