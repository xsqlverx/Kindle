# Project context and requirement record

Last updated: 2026-10-28, Asia/Calcutta.

## User intent

Revamp the entire existing Android app into a distinctive, premium personal tool that helps the user act throughout a college day. The user uses all five current areas and wants meaningful additional depth, while keeping navigation uncrowded. The user should not have to produce a perfect design brief or repeat the five discovery answers.

The recurring questions the app should answer are: what is supposed to be happening now, what should happen next, and how can the plan recover after a missed action? Attendance and seeing the workout routine are current practical advantages. Studying consistently is the most urgent missing outcome.

## Accepted decisions

- First prototype: `6c8b447`, pushed to main. The user's response was: "HOLYYY SHITTTT THIS IS INSANEEE DUDE CONTINUE THROUGH". Preserve that design direction while extending the rest of the app.
- Visual language: charcoal, smoke-violet, pale lavender, readable pale text, controlled lighting, a sculpted day ribbon, and connected motion when tools unfold.
- The supplied references are inspiration for materials, hierarchy, and interaction. The user explicitly rejected copying a reference screen and substituting their data.
- All existing features remain in scope. Naming and logo are deferred. Both current names are disliked.
- No local compilation. The user's external cloud builder consumes main; commit and push the work to main.
- Requirements and status must live in the repository, beyond chat context.

### V2 correction after device testing

The user praised the app specifically **for V1 only**. They now report that it feels disconnected and gives too little reason to return, even for testing. They explicitly request more tabs/spaces, meaningful feature density, a unified experience, richer motion/animations, liquid glass, and a much stronger experiential identity. The V1 approval is a foundation, not final acceptance or a ceiling on richness. See V2_EXPERIENCE_PLAN.md for the proposed response and the subsequently confirmed navigation contract.

### Confirmed navbar and Settings correction

Replace the current middle College button with a fixed unified centre button that expands into the destination launcher. The other **four** positions, two on each side, can each hold **any** destination from the full 12+ tab library, in **any order** selected in a redesigned Settings destination. No outer destination is permanently glued into place, and there are no category restrictions by side. College becomes assignable like every other area. Settings remains reachable when unpinned. Exact launcher geometry and final module inventory remain open. The user explicitly wants useful new capabilities, not the existing app divided into nine labels.

### Latest daily-use correction

After the first V2 source delivery (`4951ae3`), the user could not immediately understand
what to test or how an average day would work. They explicitly asked for internet research
across planner apps and life dashboards, followed by building/pushing without another pitch.
The top priorities are reduced decision work and rewarding animation, page transitions and
micro feedback. Research and applied decisions are recorded in EXPERIENCE_RESEARCH.md.
The new Today is a guided next-action surface, with a distinct optional 60-second preview
that does not create real tasks, plans or study history. Real activity keeps the existing
data/session contract. Neither cognitive-load reduction nor visual quality is called verified
without user/device evidence.

### Control design feedback, 2026-10-28

The user says most buttons look unprofessional/out of place, naming Today's Whole day and
Habits/water/everyday tools. CONTROL_DESIGN.md records the implemented shared native action
styles and targeted Today composition. Controls must belong to the charcoal/lavender surface,
show clear primary/secondary/disclosure roles, and retain tactile response and all behavior.
This visual refinement is implemented in source; device acceptance/cloud build are unverified.
No data, brand, navigation contract or feature scope change was requested.

### Next-move feedback, 2026-10-28

The user says “Give it a little room” feels like an HTML form and wants to choose exact
minutes rather than four prompts. NEXT_MOVE_REFINEMENT.md records the implemented native
composer/time surface, visible numeric input/slider/one-minute controls and 1–180-minute
boundary. Raw input persists and is validated at the write, including after restoring/changing
sources. Preserve the real-work/preview contract. Cloud/device visual acceptance is pending.

## Routine and human constraints

The full approximate weekday is in REVAMP_DIRECTION.md. Important anchors: college around 8:00 AM-1:30 PM; home around 2:30-2:50 PM; gym around 8:00-9:00 PM; family prayer around 9:30-10:30 PM; call with girlfriend around 10:30-11:45 PM. Exact boundaries remain uncertain.

The afternoon loses structure to food, scrolling, and sometimes a nap. Records and coding tend to happen late, while study is deferred. Coding, family, and relationship time are meaningful commitments; do not portray them as worthless or automatically displace them. A short morning workout is a wish rather than an established habit.

Additional reported details to preserve: alarm around 6:30-6:45 AM, actual getting up around 7:00; a desired five-minute morning workout; bus departure around 7:35-7:40 AM, arrival at college around 8:00; first period reportedly 8:00-8:45 followed by an approximately 20-minute break; another break reportedly 11:35-11:55; classes may be 45 minutes but the user was unsure. Leave college around 1:45-1:50 PM. These are recollections, not a validated timetable.

For attendance, the user chose a screenshot as the faster way to establish accurate counts. No login credentials have been supplied or recorded.

## Requirements versus proposals

| Item | Status |
| --- | --- |
| Keep all five areas and current functions | Explicit requirement |
| Extend the accepted visual direction across the app | Authorized current work |
| Know now / next / recovery | Explicit desired payoff |
| Notifications, alarms, useful follow-through | Opt-in inexact local implementation; delivery remains unverified |
| Attendance baseline from portal screenshot | Desired; screenshot not supplied |
| More quality features without filler | Explicit requirement; candidate modules need a recurring purpose |
| More tabs/spaces and greater feature density | Explicit V2 requirement after device testing |
| Unified cross-space behavior, rich motion, and liquid glass | Explicit V2 requirement; extends beyond the current visual consistency |
| Fixed expanding centre launcher plus four fully customizable outer slots | Explicit requirement; any available destination from the full 12+ library, any order |
| Redesigned Settings with navbar assignment and ordering | Explicit requirement; selections persist |
| Final launcher geometry and useful new module inventory | First 13-space implementation and grouped/searchable centre morph; device/user refinement remains |
| Arrival flow and useful study sessions | Guided live Today, explicit check-in, shared work and resume history implemented in source |
| Per-class attendance history and duplicate protection | Requested after spam/feedback complaint; dated ledger, shared controls and corrections now implemented in source; cloud/device checks pending |
| Automatic activity or home detection | Not agreed or implemented |
| App blocking, online AI, credentials-based portal login | Not authorized as a chosen design |

## Reference evidence

- JPG: `C:/Users/kenaz/Downloads/879c7494ddb73a6a7270ad24427aeb3a.jpg`.
- MP4: `C:/Users/kenaz/Downloads/ux_ronasit_pindown.io_1793032755.mp4`.
- Both were inspected. Their observations are recorded in REVAMP_DIRECTION.md. Local Downloads paths are not portable repository assets.

## Implemented V2 foundation (2026-10-27)

The first V2 source delivery now implements 13 destinations, unrestricted four-slot
configuration, the morphing centre launcher, live Today/Plan, source-linked study/session
history, Projects, Capture, Exam Prep, Coding Practice and Review. The chosen inventory is
an implementation decision grounded in the brief, not a newly obtained final design approval.
The existing College/Fitness/Tasks/Reset/Settings tools remain; the old sample moments live
under Settings > V1 design preview. Appearance now applies to live Today as well.

Additive Room 2-to-3 and 3-to-4 migrations replace destructive fallback. The latter preserves
aggregate attendance and adds a dated period ledger plus a baseline reconciliation timestamp.
Automatic startup seeding
and heuristic water deletion are disabled; existing records are preserved, including any
older samples whose provenance cannot be inferred safely. Reminders are opt-in inexact local
alarms with permission checks; delivery has not been observed. No local build/tests were run.
See V2_IMPLEMENTATION.md, V2_VERIFICATION.md and STATUS.md for exact scope and limits.

### Attendance feedback, 2026-10-27

The user reports that Present/Absent can be spammed, have no convincing press confirmation,
and do not match the visual design. Both marking entry points now use one dated-class control
with press spring/ripple/haptic, storage-confirmed feedback, explicit Change and persistent
duplicate protection. Corrections replace a class's contribution; cancelled/removed marks
contribute zero. History records only actual marks made through the new ledger. Existing
aggregate totals cannot disclose previously counted class identities and are preserved.
The user's promised current attendance screenshot/counts are still pending. Do not seed them.
See ATTENDANCE_RELIABILITY.md for scope, baseline behavior and evidence boundaries.

### Continue from these files

- NEXT_CHAT_PROMPT.md: paste-ready implementation handoff for a new chat.
- STATUS.md: current implementation, latest handoff, validation, and remaining work.
- V2_EXPERIENCE_PLAN.md: device feedback, explicit V2 goals, and proposed product/navigation/motion plan.
- REVAMP_DIRECTION.md: discovery answers, visual interpretation, and proposed feature depth.
- TODAY_PROTOTYPE.md: prototype behavior and explicit limitations.
- V2_IMPLEMENTATION.md: concise code-grounded stages and the implemented destination inventory.
- V2_VERIFICATION.md: source evidence, supplied cloud tests, device journeys and known gaps.
- EXPERIENCE_RESEARCH.md: latest daily-use feedback, primary research and applied implementation decisions.

Update these documents when the user changes direction. Keep uncertain inputs uncertain; do not silently promote suggestions into requirements.
