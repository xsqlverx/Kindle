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
| Natural access to additional tools / side panel | Desired; final navigation pattern deferred |
| Arrival flow and useful study sessions | Proposed direction grounded in the user's day; Today preview implemented |
| Per-class attendance history and duplicate protection | Proposed reliability work; aggregate records currently remain |
| Automatic activity or home detection | Not agreed or implemented |
| App blocking, online AI, credentials-based portal login | Not authorized as a chosen design |

## Reference evidence

- JPG: `C:/Users/kenaz/Downloads/879c7494ddb73a6a7270ad24427aeb3a.jpg`.
- MP4: `C:/Users/kenaz/Downloads/ux_ronasit_pindown.io_1793032755.mp4`.
- Both were inspected. Their observations are recorded in REVAMP_DIRECTION.md. Local Downloads paths are not portable repository assets.

## Read next

- STATUS.md: current implementation, latest handoff, validation, and remaining work.
- REVAMP_DIRECTION.md: discovery answers, visual interpretation, and proposed feature depth.
- TODAY_PROTOTYPE.md: prototype behavior and explicit limitations.

Update these documents when the user changes direction. Keep uncertain inputs uncertain; do not silently promote suggestions into requirements.
