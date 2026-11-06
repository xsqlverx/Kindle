# Startup and next-move interruption

2026-11-06. Recording `WhatsApp Video 2026-11-06 at 11.50.20 AM.mp4` shows the app
starting with a restored next-move sheet covering the page; some first destination entries
pause, while other switches appear smooth. No frame timings or specific bottleneck are proven.

1. Add a brief, reduced-motion-aware first composition entrance; keep destination transitions
   and saved state. Avoid initiating Assistant storage/network setup when another screen opens,
   and load its encrypted state off the UI thread without allowing saves before loading.
2. A next-move request/restored draft appears as a centered in-app icon that expands into a
   compact persistent prompt. Expand opens the existing composer; Dismiss reverses the prompt
   animation and keeps the draft recoverable. No OS notification, automatic work or data loss.
   Dismissing a draft must not automatically reopen it on every process restart; explicitly
   choosing a next move must still recover it. Respect Back, preview backup, session and IME.
3. Add focused cloud regressions for draft suppression/recovery and retain duration tests;
   source/static checks only locally. Build/test in GitHub Actions, distinguish that from the
   Google AI Studio signing identity and actual phone/frame/usability verification.

## Source implementation

- The first composed workspace fades and rises into place once per activity, respecting the
  app's Reduce motion setting; existing destination transitions and saved state are retained.
- Assistant initialization now starts only when that destination is composed. Keystore reads
  run on IO; a loading surface prevents edits until the existing data has been read, and a
  failed read remains non-overwriting. This removes that work from startup but is not a
  measured frame-time improvement and first-entry/provider behavior needs phone testing.
- Next-move requests and previously open drafts use a centered in-app prompt which grows
  from an icon. Expand opens the existing full editor; Dismiss/Back collapse it, retain the
  existing draft, and silence automatic reopening after a process restart. Explicitly making
  a move recovers the exact source, text and raw minutes. Preview backup is retained. There
  are no OS notifications or automatic placement. Prompt animation respects Reduce motion.
- `NextMoveDurationTest` adds restart/dismiss/reopen and preview-backup coverage. New
  `NextMovePromptTest` covers both actions with reduced motion on an external device runner.
  Phone layout, transitions, large fonts, system animation settings, IME behavior and actual
  cold/warm frame pacing remain unverified; the recording cannot identify a root bottleneck.

## Verification and installed-app identity

- Source checks parsed 88 Kotlin files; academic catalog and project queue checks and
  `git diff --check` passed. No local Gradle, Android build, emulator or Android tests ran.
- GitHub Actions run `42174843876` compiled commit `3bbb11b` and executed the focused
  debug unit-test task including Assistant routing/presentation and NextMoveDurationTest.
  `NextMovePromptTest` is an instrumentation test supplied for a future cloud/device runner;
  it was not run. GitHub Actions signs a side-by-side preview with a temporary key, not an
  update to the user's original Life OS.
- ADB showed the original package `com.aistudio.lifeos.vkmqzt` updated at 11:41:30 on
  2026-11-06. Its currently installed certificate SHA-256 matched the original captured APK
  certificate `229e59aa2647a24bc961c25483b9e496ab8808488f47b99d214e6de4ddfcffc0`. The
  recording subsequently shows the interactive Assistant in that app. Exact source revision
  and complete data equivalence were not checked. Both original and old separate preview
  remain installed; their data do not sync. Never uninstall the preview without a verified
  migration/explicit informed authorization. The new startup changes need a fresh original-
  identity AI Studio APK and matching-signature verification before phone installation.
- Visual behavior, actual frame times, live provider flow, touch and accessibility are not
  established by compilation or unit tests. Cold versus warm device measurements remain open.
