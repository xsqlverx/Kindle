# Floating assistant orb

2026-11-06. The user wants Assistant detached from the top bar as a Siri-like animated,
movable orb that snaps to an edge. Existing Today hero and compact header orbs are Add/log,
not Assistant; preserve Add/log, assistant data, all destinations and the launcher.

1. Replace Today's assistant toolbar button with an in-app floating Assistant orb. Tap opens
   the existing Assistant conversation; dragging follows the finger and springs to the nearest
   left/right edge, keeping a clamped vertical position. Persist edge and relative height.
   Provide accessible move actions. Respect status/navigation/dock/session space and hide the
   orb while IME, launcher or blocking surfaces are shown and while Assistant is open.
2. Draw a distinct luminous moving sphere with inexpensive Canvas layers using the existing
   lifecycle-aware, reduced-motion-aware orb clock. Animate touch and snapping; do not claim
   microphone/listening or provider activity that is not real. Keep quick entry available.
3. Add focused position/snap tests and cloud/device interaction coverage. Run source/static
   checks only locally, build/test on GitHub Actions, update handoff and ordinarily push main.
   Do not touch signing, app records, encrypted keys or the installed preview. Validate actual
   appearance, scroll/drag/accessibility, frame pacing and phone behavior separately.

## Implemented source

- The Assistant shortcut is a floating 58dp orb with animated gradient highlights and flowing
  bands. It uses the existing lifecycle/reduced-motion-aware phase clock and animates press and
  edge snapping. It does not display an invented listening or AI-processing state. Tapping it
  opens the existing Assistant, with all configured providers, chat and settings unchanged.
- Dragging clamps to usable screen bounds and snaps to the nearest side; the side and relative
  vertical position persist in workspace preferences. Accessible actions reposition it without
  dragging. It is hidden with the keyboard, centre launcher, open Assistant, session panel and
  blocking sheets/dialogs so it does not interfere with them or the bottom dock.
- Today's old Assistant header button is removed. The prior header orb was actually Add/log,
  so it becomes a clearly labelled Add/log button; the large Today continuity orb and its
  session/add behavior remain. Assistant stays in the destination library and assignable slots.
- `AssistantOrbPositionTest` covers side, clamping, resize and invalid-state arithmetic;
  `FloatingAssistantOrbTest` supplies tap/drag device coverage. UI appearance, touch, screen
  readers, keyboard, rotation and smoothness require actual device checks; no measured speedup.

## Verification

- Static checks parsed 92 Kotlin files and passed academic/project data checks plus
  `git diff --check`. No local compilation, Gradle, emulator or Android tests were run.
- GitHub Actions run `42177409393` built source `d5b84bc` and actually ran the focused
  debug unit-test task, including `AssistantOrbPositionTest`; its Android-test compilation
  then found an existing unresolved import in `WorkspaceDockTest`. Commit `b2cc768` removes
  that import. Run `42177973358` built the corrected source, passed the unit-test task from
  cache, and compiled `compileDebugAndroidTestKotlin` successfully. Compilation of the
  tap/drag instrumentation is not execution.
- The cloud artifact uses the separate ephemeral preview identity. Update the original Life
  OS via its Google AI Studio builder after verifying the signing identity; don't uninstall
  or clear the original or separate preview. Real phone orb behavior, animation quality,
  drag hit-testing, accessibility, keyboard, app lifecycle and frame pacing are unverified.
