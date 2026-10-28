# Next-move composer refinement

2026-10-28. Baseline `19f8915`; clean main, refreshed and equal to origin/main.

The user says “Give it a little room” looks like an HTML form and wants to choose their
own minutes instead of four presets. Implement directly; no new discovery or approval stage.

1. Recompose NextMoveSheet as a native charcoal/lavender working surface: deliberate header,
   an inset action editor/source picker, a sculpted editable time control, a live placement
   receipt, progressive material/first-step details and an accessible action footer.
2. Remove the preset chips. Make exact whole minutes the main input, with numeric keyboard,
   one-minute adjustment and a full-range native slider. Keep the existing 1–180-minute
   session boundary visible. Persist raw text alongside the existing draft so blank/invalid
   input never silently commits the previous duration; preserve custom values across source
   selection, reopening/recreation and preview backup/restore. Validate again in placeMove.
3. Preserve actual source identity, material, collision checks, source freshness, start versus
   save-for-later, active-session protection and no-records preview. Add meaningful external
   tests for arbitrary durations, invalid input and placement. Run source/static checks only,
   update continuity, commit and ordinarily push main. No Android builds/tests locally.

Acceptance: enter 17/37/90 minutes directly without opening details; the exact selected time
must drive the displayed window, stored block and timer. Clearing, zero, decimal, nonnumeric
or out-of-range input blocks new work. Keyboard, narrow/large-font, scrolling, reduced motion,
source selection and footer access require external/device checks. Do not claim rendered QA.

## Implemented source

- NextMoveSheet now uses inset native action/material editors, a lit charcoal time surface
  with a large editable numeral, a searchable saved-work picker with readable source rows,
  a linked-source badge, an exact start/end receipt and a separate action footer. It is a
  native Compose sheet; no images, web shell, external framework or new dependency.
- Exact 1–180 whole minutes can be typed with a numeric keyboard, adjusted one minute at
  a time or selected over the full slider range. There are no preset-duration chips or hidden
  custom-time field. The bounds are the existing shared focus-engine limit, shown in place.
- Raw duration text is persisted in the existing draft JSON as optional durationText. Old
  drafts fall back to their stored minutes. Invalid/blank raw text stays invalid on restoration;
  the prior valid integer is retained only as draft metadata, not accepted as a new selection.
  Metadata callbacks keep the editor's current duration even if their source snapshot is stale.
  Late editor callbacks cannot reopen a closed draft. Duration state clears after placement
  or the existing explicit user wipe; the sheet cannot dismiss itself during a pending save.
- UI window/labels and the final placeMove write guard use the same parser. Blank, zero,
  negative, decimal, nonnumeric, overflow and >180 input block new work. Actual source and
  collision checks, task+block transaction, start/save distinction and explicit completion stay.
  Active work is returned to rather than replaced. Preview backup includes the custom raw text
  and restores the real source/material. Preview still creates no real task/block/history/alert.
- Custom focus lighting and detail expansions follow Reduce motion; native input, selection,
  numeric keyboard/Done, slider accessibility and labelled controls remain. The main action
  stays outside the scrolling body with IME avoidance. Its phone/large-font behavior is unverified.

## Evidence

- `python scripts/check_sources.py`: **62 Kotlin files** parse; strict UTF-8, manifest XML,
  13 routes, original action entry points and additive SQL preservation checks pass.
- `git diff --check` passes. No database/entity/migration/signing/builder/dependency changes.
  The only preference format addition is backward-compatible optional draft text.
- Inspected the exact pinned Foundation **1.7.0** BasicTextField overload and Material3
  **1.3.0** Slider/color and ModalBottomSheet parameters from official Maven source archives,
  resolved by BOM **2024.09.00**. This is source evidence, not a Kotlin compilation result.
- Six external-runner regressions are supplied, not run: five NextMoveDurationTest cases
  cover stale metadata/source/reopen/recreation, invalid-write rejection and exact preview
  duration, invalid raw restoration, real-draft preview backup and old-draft compatibility;
  an additional DayGuideTest checks exact 17/37-minute placement around a commitment.
  There are now 42 supplied V2-related methods in these groups, plus the existing tests.

No local Android compilation, Gradle, emulator or Android tests. Cloud compilation/Room/test
execution, rendering, touch feel, frame pacing, TalkBack and keyboard/font-scaling journeys
remain unverified. Device path: choose real work → type 17 → inspect exact window → save or
start → inspect block/timer; repeat with 37/90, invalid input, source changes, restart, no free
window and Preview backup. Verify title/material editing, keyboard Done, stepper/slider, scroll
and footer at narrow width/200% font size in each palette and Reduce motion.
