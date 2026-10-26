# Working on this app

Read `docs/PROJECT_CONTEXT.md`, `docs/REVAMP_DIRECTION.md`, and `docs/STATUS.md` before making changes. Keep those files current when decisions, implementation, or verification status change. `docs/TODAY_PROTOTYPE.md` records the first implementation.

## Established requirements

- Native Android, Kotlin, Jetpack Compose. Preserve all existing capabilities in Today, College, Fitness, Tasks, Daily Reset, and Settings.
- The user enthusiastically accepted the Today prototype at commit `6c8b447`. Continue its charcoal, smoke-violet, pale-lavender visual language and sculpted day ribbon. Extend it with distinct layouts for each activity.
- References supply material and interaction principles; do not copy their layouts and substitute this app's data.
- The primary payoff is knowing what should happen now, the next concrete action, and how to recover after a missed start. The main failure point is the transition home from college.
- Keep navigation discoverable and uncrowded. All five existing areas remain accessible. A final side-panel/navigation expansion is not yet agreed.
- Naming and logo come last. The user dislikes both Life OS and Kindle; do not introduce another brand without discussion.
- Approximate routine times and prototype scenarios must stay distinct from actual schedules, detected activity, or confirmed records.
- Do not invent attendance, body measurements, study progress, successful actions, alert delivery, or cloud-build results.
- Preserve user data. Do not reset the database or remove features to work around build errors.

## Build and delivery

The user explicitly requested **no local compilation** and has an external cloud builder. Do not run Gradle, local Android builds, emulators, or Android test execution unless the user changes that instruction. Perform source/static checks, supply meaningful cloud tests when necessary, and report unverified build/device behavior honestly.

The user authorized pushing this ongoing revamp to `origin/main`. Verify the checkout and remote state, commit the intended files, and use an ordinary push. Do not force push or change the cloud builder/signing setup. Push success is separate from cloud compilation success.

## Continuity

Update the project documents as part of implementation. Preserve explicit requests, accepted visual decisions, uncertain inputs, remaining requirements, and evidence. Do not store credentials in these files. The user's attendance screenshot and exact routine/timetable have not yet been supplied.
