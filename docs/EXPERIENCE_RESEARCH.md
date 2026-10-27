# Daily-use redesign: research and implementation record

2026-10-27. Baseline `4951ae3`; clean main, equal to origin/main. The user cannot immediately
tell what to test or how an ordinary day works. They explicitly requested research followed
by implementation and push, without a design pitch or another approval plan. Main priorities:
less decision work, immediately legible daily use, rewarding motion and tactile feedback.

## Primary sources inspected

- [Structured](https://structured.app/) and [Replan](https://help.structured.app/en/articles/4526850):
  one visible day timeline, current position, short actions for unfinished work. Inspected its
  official phone screenshot. Apply legible time/next-action relationships, not its layout.
- [Sunsama daily planning](https://www.sunsama.com/daily-planning) and
  [manual](https://help.sunsama.com/docs/usage-guides/daily-planning/): guided choice, realistic
  duration and tasks beside commitments. Inspected its official planning screenshot. The
  current manual also describes beta agentic planning; no AI or connected accounts are adopted.
- [Things scheduling](https://culturedcode.com/things/support/articles/2803579/): Today curates
  attention; intended start and actual deadline are distinct. Avoid dumping every backlog item
  into the home screen. Keep the user's unrestricted navbar contract rather than copying Things.
- [Amie tasks](https://amie.so/documentation/features/tasks): capture, duration and calendar
  placement share one task identity; rescheduling should manipulate the same object.
- [TickTick focus](https://help.ticktick.com/articles/7055782010496745472): begin work directly
  from the task context rather than reconstructing it inside a separate timer.
- [Notion calm dashboard](https://www.notion.com/templates/life-os-a-calm-life-dashboard),
  [Action Zone](https://www.youtube.com/watch?v=lK_Ysf0Ko9E) creator description, and
  [LifeOS product](https://lifeos.builtbysahil.com/): a single daily landing surface can connect
  multiple domains. Feature inventories, sample scores and navigation grids do not explain a day
  by themselves. These are product descriptions, not independently established success evidence.
- [Finch mission](https://help.finchcare.com/hc/en-us/articles/37935610353293-Our-Mission):
  reinforce small chosen actions without guilt. Reward real records, not invented streaks.
- [Apple motion guidance](https://developer.apple.com/design/human-interface-guidelines/motion)
  and [Material motion physics](https://m3.material.io/styles/motion/overview/how-it-works):
  gesture continuity, responsive interruption, concise feedback, optional motion. Use available
  Compose springs; do not import newer Expressive APIs into the pinned 2024.09 BOM.

These sources suggest design mechanisms; they do not prove retention, dopamine changes or
cognitive-load reduction for this app. Those outcomes require user/device evidence.

## Code diagnosis and build scope

The baseline Today presents several equal-weight cards and navigation instructions. Creating
work requires choosing a module and completing a multi-field dialog. A successful write opens
an acknowledgement dialog. The timer is a plain sheet, and the launcher is a long undifferentiated
grid. These choices make the user assemble the workflow mentally.

Implement one dominant next move from actual stored context, a short choose-duration-place
flow, a directly accessible first lap, a readable agenda, a purposeful focus surface, a saved
progress response and non-blocking feedback. Keep all 13 destinations, the assignable slots,
existing data and tools. No fabricated metrics, sample schedules or automatic completion.

Acceptance: from Today, choose or capture one action, see why it fits, begin, minimize, resume,
save a continuation and see the real result. A short first lap must exercise those same paths.
Recovery should offer a smaller feasible restart in place. Motion should connect these state
changes and remain interruptible/readable with reduced motion and large fonts.

## Implemented decisions

- Today now makes the real action title dominant and gives one contextual primary action.
  The local ordering follows active work, saved anchors, explicit arrival, missed work and
  actual priorities/deadlines/revisions/projects. It gives a reason, not an opaque score.
- Choosing work and finding a feasible window share one short surface; extra fields are
  optional. Existing source IDs/material/resume points travel with the action. New real work
  becomes a task and plan block in one transaction. Fresh snapshots validate the commit.
- The first-lap entry is a clearly labelled optional PREVIEW. It uses the actual timer and
  visual surfaces but bypasses task/plan/history writes and alerts; a transient result explains
  the real loop. It can be tried during a busy period without displacing commitments. An
  existing real draft is backed up and restored. The primary day itself remains live data.
- Source/capsule bounds grow into a focused working surface. Actual elapsed time drives its
  arc, while a restrained light freezes on pause. Confirmed saves produce the progress bloom
  and earned week trace; task completion remains a distinct explicit choice.
- Shared agendas, progressively revealed plan controls, searchable intent groups, a quick
  next-move action, spring dock feedback and non-blocking messages reduce module hunting.
  All 13 destinations and all four unrestricted pins remain.
- Existing work is preserved. Task/submission/milestone completion releases reservations
  without rewriting session snapshots; deleted/archived work does not become an automatic
  recommendation. Future revision and blocked/drop work stay manually reachable.

No external reference screenshot ships as an app asset. Native Compose code implements
the design; there is no replacement web shell or new framework dependency.

Verification remains source/static plus supplied cloud tests. No local compilation, Gradle,
emulator or Android test execution. Device animation, rendering and external build results
must be reported separately. Current progress and exact gaps belong in STATUS.md.
