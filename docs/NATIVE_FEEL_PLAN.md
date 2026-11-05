# Native feel and responsiveness: device-feedback follow-up

2026-11-06. Baseline `5e229fc`; recording made 2026-11-02. The user reports that the entire
app feels web-like and laggy, especially the centre launcher. The recording shows repeated
launcher opening, crowded navigation and a low-contrast coding reference; it does not show
every destination or establish frame timing. The installed build may predate the current
orb/entry changes; exact APK revision is unknown.

## Stages and acceptance

1. Rework the centre launcher so its full-size content is not relaid out at every frame;
   preserve the fixed centre origin, interruption, search, all 13 destinations, Back and
   reduced motion. Avoid replaying live blurred content through a changing rounded clip.
2. Reduce unnecessary backdrop work on the rest of the app while preserving the glass
   language, readability, active session, orb and all data. Prefer localized, bounded drawing
   and native-feeling press/transition responses over heavy whole-screen work.
3. Address recording-specific friction: legible coding references, compact clear dock labels,
   launcher hierarchy, academic dates and the next-move sheet's scroll/keyboard reachability.
   Keep individual actual exam dates and custom minutes; do not guess or change records.
4. Run source/static checks only. Provide a cloud/phone acceptance path covering repeated
   opening, scroll, interruption, TalkBack, reduced motion, font sizes, data preservation and
   comparative frame timing. No local Gradle, Android build, emulator or Android tests.
   Record changes and limits in the canonical handoff files; ordinary push to origin/main.

## Open evidence

The screen recording alone cannot identify a CPU/GPU bottleneck or prove the installed APK
revision. Performance, visual feel, Android compilation and behavior remain unverified until
an external build and matched phone checks. AI is an exploratory future capability only; no
provider access, data export, remote fallback or credentials are authorized by this review.

## Implemented source pass

- The dock and launcher use dense tinted, edge-lit glass instead of sampling the live blurred
  page. The page draws directly when the session capsule is absent; live backdrop capture is
  retained for the visible capsule. The changed glass treatment needs visual review; this is
  not a measured speedup.
- The centre panel uses fixed layout bounds and scales from the centre control's bottom anchor;
  alpha and transform are read in graphics layers. Search results use a lazy list. Press
  feedback, coordinated close, reduced motion and all 13 spaces remain. Keyboard-visible
  layout reserves more room for search results.
- Shared headings are more compact, Coding has a shorter dock label, the day timeline displays
  one time label, calendar dates remain individually listed in readable form, and bare web
  domains are linkable with readable fallback text. The next-move sheet has added scroll
  clearance; the footer was already outside its scrolling column. Syllabus suggestions are
  remembered by date and source data rather than recalculated every second.
- Added `WorkspaceDockTest` for cloud/device execution: search an unpinned destination,
  navigate, reopen and close. No Room schema, stored records, reminders or builder config changed.

## Verification and remaining work

`check_sources.py` parsed 76 Kotlin files and checked routes, legacy actions, UTF-8 and SQL
sentinels. `check_academic_pack.py` and `check_project_queue.py` passed source/data audits;
`git diff --check` passed. None type-checks or runs Android. The new instrumentation test
is supplied but unrun. No cloud build, frame trace, device, accessibility or visual result
is available. Continue auditing all destinations with the user: the short video cannot cover
the entire app, and matched device measurements are required before calling lag fixed.
