Continue implementing Kindle V2 in C:\Kindle (https://github.com/xsqlverx/Kindle). This is an implementation request: inspect the existing app, make the work concrete, and build it rather than returning another generic brainstorm.

First read AGENTS.md and these repository documents:
- docs/PROJECT_CONTEXT.md
- docs/V2_EXPERIENCE_PLAN.md
- docs/REVAMP_DIRECTION.md
- docs/STATUS.md
- docs/TODAY_PROTOTYPE.md (historical V1 notes)
- docs/V2_IMPLEMENTATION.md
- docs/V2_VERIFICATION.md

Inspect Git status and the actual source. Preserve existing local changes. V1 includes commits 6c8b447 and 40463c6, followed by the V2 delivery described in STATUS.md and the current Git log. The first V2 source now has 13 routed working destinations, the expanding launcher, persistent four-slot editor, shared plan/session history, opt-in local reminders and new learning/project modules. Do not rebuild another registry or shell. Continue from its concrete gaps and external build/device feedback; do not claim the unobserved cloud tests passed.

WHAT I WANT

I am a B.Tech CSE student serious about academics, coding, working out, and improving my daily life. V1 looks excellent for a prototype, but the app feels disconnected and gives me too little reason to return. V2 needs genuinely useful new capabilities, feature density, unified behavior, richer visuals, tactile motion, and convincing liquid glass. My most important payoff is opening the app and knowing what should happen now, what to do next, and how to recover after missing something.

Keep the existing charcoal/smoke-violet/lavender identity as a foundation. The V1 approval is not a ceiling on richness. Create an original experience; do not copy a reference app or reskin the same cards across every screen. Inspect the local references listed in PROJECT_CONTEXT.md if available. Do not block on them if their files are unavailable; their observed qualities are documented.

NON-NEGOTIABLE NAVIGATION

[ Any tab ] [ Any tab ] [ FIXED CENTRE LAUNCHER ] [ Any tab ] [ Any tab ]

1. Replace the current middle College button with a unified centre button. Its final name is undecided.
2. Clicking it expands the button itself into the complete destination launcher with connected, reversible motion. Do not assume the old side-panel proposal is mandatory; design the expansion around this centre control.
3. ALL FOUR outer slots can hold ANY available destination from the full 12+ tab library, in ANY order. No permanently pinned Today, College, Fitness, or other destination; no category restrictions on either side.
4. College becomes assignable like every other destination. Settings and all unpinned areas remain reachable through the centre launcher.
5. Redesign Settings as a polished destination with a live navbar preview, full-library tab selection, assignment to each outer slot, reordering, and persistence across restarts. Provide an accessible alternative to drag reordering. Retain the current settings tools.
6. Use stable destination identities and safe saved-layout validation. Correctly handle active unpinned destinations, system Back, restored state, and layout edits during an active session.

GENUINE DEPTH AND CONTINUITY

The 12+ destinations must add useful capability for my actual life. Do not manufacture the count by splitting existing screens, publishing Coming soon pages, or inventing metrics. The earlier nine-space table is not an approved inventory.

Briefly define a coherent destination inventory in the repo: each area's recurring purpose, actual functionality, stored data, and connections to the shared day. Use judgment to choose worthwhile additions. Planning, real study sessions/history, exam preparation, coding/project execution, capture, and recovery/routines are candidate capabilities, not a locked list. Then implement in cohesive stages rather than stopping at the inventory.

Connect the areas through a shared day plan and activity history. A task or deadline should lead into actual work, preserve its identity, remember where I stopped, and feed the next action and review. An active session should remain accessible across destinations. Finishing a timer must not silently complete a task.

Build a real afternoon flow: explicit home check-in, a feasible study window, a chosen task/material, focus, a saved resume point, useful reminders, and recovery when a start slips. Respect fixed commitments. My approximate routine and uncertain inputs are documented. Do not assume automatic home detection or attendance. Move sample moments out of the primary live-day experience.

Introduce glass and motion alongside functionality: backdrop-aware depth, coordinated surface transitions, a centre control that unfolds and returns, responsive dock interaction, persistent session transitions, and a timeline that visibly changes when the plan changes. Keep text readable, support reduced motion, and check the project's pinned Compose APIs before using newer examples. A blur modifier on foreground content alone is not proof of backdrop glass.

Preserve ALL current features and data: timetable/deadline editing, attendance, exams, workouts/sets/rest, meals, water, weight, tasks/priorities, habits, Daily Reset, imports, and settings. Use honest empty states when my real inputs are missing. The attendance screenshot and exact timetable/workout confirmation have not been supplied. App blocking, portal credential login, automatic tracking, and online AI have not been selected as requirements. Naming and logo come last.

EXECUTION AND DELIVERY

- Start with a concise code-grounded plan, write implementation stages and acceptance checks into the repo, then begin coding. Do not repeat the discovery questionnaire or stop at a proposal.
- Inspect the implemented destination architecture, launcher, Settings, plan/session foundation and modules first. Continue with cloud/device repair, richer source-to-session transitions, recurring commitments, deeper longitudinal training/academic history and any observed journey failures. Preserve the working modules; a registry or attractive shell alone does not fulfil V2. V2_VERIFICATION.md records the exact remaining boundaries.
- Do not compile the Android app locally, run Gradle, launch emulators, or execute Android tests locally. I have an external cloud builder.
- Perform source/static checks and add meaningful tests for cloud execution where needed. Use explicit UTF-8 for file reads/writes; an earlier pass caught Windows text-encoding corruption.
- Preserve the native Kotlin/Compose app and existing user data. Do not alter signing/cloud-build configuration or remove features to avoid build errors. Use proper migrations for any schema changes.
- Keep requirements, progress, unresolved decisions, and verification evidence updated in the project documents as you work. The implementation must survive context-window loss.
- Commit and push intended work to origin/main with ordinary pushes. You have my authorization; do not ask again for routine implementation choices or the push. Preserve unrelated changes and never force push.
- Report the commit, working flows, remaining work, and actual verification limits. Source parsing, a successful push, cloud compilation, and device testing are different evidence; do not claim unobserved success.

Deliver a connected, materially richer experience whose capabilities work together. Do not call V2 complete while required destinations are placeholders or the live day is still a static scenario selector.
