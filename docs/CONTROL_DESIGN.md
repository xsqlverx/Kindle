# Control design follow-up

2026-10-28. Baseline `fbaa1f5`; clean main, fetched and equal to origin/main.

The user reports that most buttons feel unprofessional and out of place, specifically
Today's Whole day and Habits/water/everyday tools. This is an implementation request.

Source diagnosis: GuidedTodayScreen mixes bare Material TextButtons, a bright hero action
and separately styled quick-door tiles. Other live screens use Material defaults alongside
one-off radii and label sizes. Typography does not define the app's action labels.

1. Introduce shared native Compose controls with clear primary, secondary and quiet roles,
   consistent label typography, corner/padding rules, and restrained spring press feedback.
   Keep native button semantics, ripple, focus, disabled state and the same callbacks. Use
   the current appearance palette and reduced-motion setting. No third-party UI dependency.
2. Recompose Today's Whole day disclosure and quick-access area: a visible disclosure state,
   one coherent tools surface, readable icons/labels and a full-width Everyday tools row.
   Use the same action language in the chooser, plan, learning/tools and settings flows.
   Preserve all destinations, actions, focus origins and data behavior; leave the historical
   V1 preview's layout, palette and behavior intact; action typography is shared app-wide.
3. Audit live call sites, run source/static checks and update handoff evidence. No local
   Android compilation, Gradle, emulator or Android tests. Commit and ordinarily push main.

Acceptance: Whole day still expands/collapses the same agenda; Everyday tools still opens
all habits/water/task/ring tools; all quick destinations and capture work remain reachable.
Labels must wrap rather than truncate, interactive targets remain at least 48dp where layout
permits, and motion must preserve cancellation and keyboard/TalkBack access. Actual Android
rendering, font scaling, press feel and cloud compilation need external/device evidence.

## Implemented

- WorkspaceControls defines native primary, tonal, outline and quiet buttons, icon controls,
  stateful disclosures, action rows and shortcut cells. Shared labels use 14/20sp and 12/18sp
  medium type without wide letter spacing; corners are 12–18dp for controls and 22dp for rows.
  Press scale is a restrained 0.978 spring; custom motion snaps with Reduce motion. Native
  ripple, disabled state, semantics, keyboard activation and a visible focus border remain.
- Today now has a bounded Whole day disclosure with a reversing chevron and a resizing
  agenda, one integrated College/Fitness/Capture shelf and a full-width Everyday tools row,
  an explicit Daily Reset row, and matching header/review icon controls. All original actions
  remain connected. The tools overlay has a titled header and a labelled Back control.
- Plan and the next-move chooser use the same disclosures and action rows. The live College,
  Fitness, Tasks, Reset, Settings, Study, Projects, Capture, Exam Prep, Practice and Review
  paths use shared action buttons. Existing deliberate destructive/status colors and custom
  compact shapes are retained. The attendance pair retains its own confirmed-save choreography
  and persistent deduplication; its quiet auxiliary actions use the common style.
- MoveButton delegates to the shared primary control, keeping its arrow, origin/callback
  and existing haptic. No new data writes, migrations, preference formats, destinations,
  dependency versions, signing changes or builder configuration.

## Evidence and limits

- `python scripts/check_sources.py`: 59 Kotlin files parse; strict source/docs UTF-8,
  manifest XML, 13 routes, original action APIs and additive SQL checks pass.
- A source comparison of 14 mechanically migrated files found no changes beyond imports
  and control names, preserving all callback bodies. Curated Today/Plan/chooser/header/primary
  changes were separately inspected for the same navigation, state and action callbacks.
- Forwarded native signatures and VerticalDivider were inspected in the **pinned** Material3
  1.3.0 source archive, resolved from the official Compose BOM 2024.09.00 POM. This was a
  source read, with no Android compilation or test execution. Official artifacts:
  [BOM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2024.09.00/compose-bom-2024.09.00.pom),
  [Material3 sources](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3/1.3.0/material3-1.3.0-sources.jar).
- `git diff --check` passes. These checks do not establish Kotlin type correctness, a cloud
  build, native visual quality, font scaling, touch response, TalkBack or frame pacing.
  No new Android tests were needed for this presentation-only change; existing cloud tests
  remain supplied and unrun. No local build, Gradle, emulator or Android tests were run.

External/device checks: Today Whole day/Less, all four shelf actions, Daily Reset and Review;
tools Back and habit/water/task access; primary/secondary/cancel/destructive/disabled buttons
through all spaces; keyboard/TalkBack, narrow screens, 200% fonts, each appearance and Reduce
motion. Visual acceptance remains with the user's device feedback.
