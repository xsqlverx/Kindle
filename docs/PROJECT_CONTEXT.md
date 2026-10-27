# Project context and requirement record

Last updated: 2026-10-27, Asia/Calcutta.

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
| Notifications, alarms, useful follow-through | Desired; implementation still outstanding |
| Attendance baseline from portal screenshot | Desired; screenshot not supplied |
| More quality features without filler | Explicit requirement; candidate modules need a recurring purpose |
| More tabs/spaces and greater feature density | Explicit V2 requirement after device testing |
| Unified cross-space behavior, rich motion, and liquid glass | Explicit V2 requirement; extends beyond the current visual consistency |
| Fixed expanding centre launcher plus four fully customizable outer slots | Explicit requirement; any available destination from the full 12+ library, any order |
| Redesigned Settings with navbar assignment and ordering | Explicit requirement; selections persist |
| Final launcher geometry and useful new module inventory | Still to design; the earlier nine-space partition is not approved |
| Arrival flow and useful study sessions | Proposed direction grounded in the user's day; Today preview implemented |
| Per-class attendance history and duplicate protection | Proposed reliability work; aggregate records currently remain |
| Automatic activity or home detection | Not agreed or implemented |
| App blocking, online AI, credentials-based portal login | Not authorized as a chosen design |

## Reference evidence

- JPG: `C:/Users/kenaz/Downloads/879c7494ddb73a6a7270ad24427aeb3a.jpg`.
- MP4: `C:/Users/kenaz/Downloads/ux_ronasit_pindown.io_1793032755.mp4`.
- Both were inspected. Their observations are recorded in REVAMP_DIRECTION.md. Local Downloads paths are not portable repository assets.

## Read next

- NEXT_CHAT_PROMPT.md: paste-ready implementation handoff for a new chat.
- STATUS.md: current implementation, latest handoff, validation, and remaining work.
- V2_EXPERIENCE_PLAN.md: device feedback, explicit V2 goals, and proposed product/navigation/motion plan.
- REVAMP_DIRECTION.md: discovery answers, visual interpretation, and proposed feature depth.
- TODAY_PROTOTYPE.md: prototype behavior and explicit limitations.

Update these documents when the user changes direction. Keep uncertain inputs uncertain; do not silently promote suggestions into requirements.
