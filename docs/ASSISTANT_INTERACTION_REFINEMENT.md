# Assistant response and keyboard feedback

2026-11-06, baseline `2d366ee`. User screenshots show a large raw Markdown table, a
hallucinated full-day timetable in answer to “what's next in my day”, and the composer/dock
displaced when the keyboard opens. The user wants expressive native motion, tappable pieces
and substantially less text overload. Screenshots are device evidence of the current preview.

1. Route explicit current-day/next-step questions to a deterministic local snapshot of saved
   commitments, active focus and actual chosen work. Label suggestions and known schedule
   separately; no invented times. Preserve source identity and require confirmation before
   placing work. Other questions retain the configured provider and full existing history.
2. Parse ordinary assistant answers into readable, animated blocks (including lists and
   Markdown tables) with expandable detail, per-item editable next moves, and a safe fallback
   for code or malformed content. Preserve full original answers and existing stored chats.
   Treat provider-generated schedules as suggestions, never as saved commitments.
3. Rebuild the composer as a tactile native surface; when typing, eliminate duplicate keyboard
   insets and hide the dock until the keyboard closes. Keep focus/session/launcher behavior,
   accessibility and reduced motion. Do not erase drafts, records or encrypted provider keys.
4. Static/source checks and cloud tests; ordinary push to main. No local Gradle, Android builds,
   emulator or Android test execution. Cloud and physical device results must be distinguished.
   The existing Assistant Preview was signed with an ephemeral Actions key; a new build cannot
   overwrite that installed preview without the matching key. Never uninstall or clear it.

## Source implementation

- Explicit current-day/next-step questions and The briefing use `AssistantDay` locally, even
  without a key. The snapshot labels actual timetable entries and saved work; suggestions have
  explicit reasons and hand off existing source IDs to the editable next-move composer. A live
  animated orb and progress ring derive from the current saved commitment, not inferred presence.
  Earlier local snapshots stay in chat; the latest live view updates with the actual day. The
  most recent historical AI answer to the same question also opens as a live saved-day view,
  with the old provider response retained behind an explicit disclosure.
- `AssistantAnswer` segments provider prose, headings, lists, Markdown tables and fenced code.
  Compose surfaces reveal short previews, expandable/tappable cards, item-level planning and
  a full-original option for expanded responses. AI table times are labelled suggestions, never
  stored schedule. Stored historical messages retain their original text. New provider replies
  record the provider's truncation signal, and prompt instructions forbid fabricated routines.
- Local saved-day snapshots are excluded from subsequent provider conversation history.
  Existing provider keys, encrypted chat/memory and explicitly controlled context sharing stay.
- Assistant typing hides the dock and focus capsule while releasing their reserved space;
  the composer owns keyboard padding and uses a compact, tactile input. Other destinations,
  launcher search, active sessions and reduced motion remain available.
- `AssistantPresentationTest` covers the reported Markdown table, code and arithmetic text,
  explicit local intent and real-day provenance; routing tests cover excluding local snapshots.
  Static checks and cloud build/test results must be recorded separately from device usability.

Cloud Actions run `42099774239` compiled commit `bdf7444` and successfully ran the routing
and presentation unit-test task. The later historical-answer disclosure change requires its
own cloud build. Neither run verifies phone motion, IME geometry or real provider behavior.
