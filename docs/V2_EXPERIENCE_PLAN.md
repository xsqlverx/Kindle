# V2: a connected daily experience

Recorded 2026-10-27, Asia/Calcutta.

Status: the V2 goals and five-position navigation contract below are explicit requirements. A first implementation of the inventory, centre expansion and connected flows is now in source. V2_IMPLEMENTATION.md records implementation choices; STATUS.md and V2_VERIFICATION.md distinguish source checks from unobserved cloud/device behavior. These choices are not final user design approval.

## Feedback that changes the brief

The user tested the app. They called it excellent **for a V1 prototype only**, but report that it feels disconnected and gives them too little reason to open it even for testing. They explicitly want more tabs/spaces, more meaningful capability, a unified experience, richer animations and motion, liquid glass, and a much more compelling visual experience for an app intended to organize their whole life.

The V1 approval establishes a foundation. It must not be interpreted as an instruction to freeze the current quiet surfaces, navigation, feature set, or interaction depth. Preserve the original requirement to derive an original experience rather than copy supplied references.

## Source-grounded diagnosis before V2 implementation

- Today still defaults to the Home sample moment. Its proposed steps are static examples selected through College/Home/Running late controls.
- The focus timer persists its current session, but there is no lasting study-session history or shared full-day plan. Dismissing the timer resets its session state.
- The app has a task-to-focus link and common styling, but no common activity lifecycle spanning class, study, projects, workouts, and review.
- Notifications and alarms are not wired. The manifest currently declares vibration permission only.
- The current glass treatment uses tint, gradients, borders, and sheen; it does not yet provide backdrop-aware glass across a coordinated layered scene.

These explain plausible causes of the reported disconnection. Retention and engagement have not been measured.

## Earlier module proposal: not a locked inventory

The user clarified that the goal is not to split the current app into the nine destinations below. The expanded library should contain 12+ meaningful destinations with genuinely new capabilities for a B.Tech CSE student who is serious about training and improving their life. Existing features stay available. The table is an earlier discussion aid, not a final count, an approved partition, or permission to make filler tabs.

| Space | Repeated job and connection |
| --- | --- |
| Now | Actual current context, next action, and continuation of active work |
| Plan | Fixed commitments, flexible time blocks, free windows, and recovery |
| College | Timetable, course context, attendance, assignments, and deadlines |
| Study | Chosen topic/material, focus sessions, resume notes, and study history |
| Fitness | Routine execution, sessions, rest, previous sets, nutrition, hydration, and weight |
| Tasks | Captured actions, priorities, scheduling, and completion |
| Projects | Coding and other multi-step deliverables, milestones, and next actions |
| Capture | Quick notes, ideas, and links that can become a task or project input |
| Review | Daily Reset and actual day/week progress from recorded actions |

All existing capabilities remain. Daily Reset remains directly reachable from Now and Review. These are proposed destinations; final names and placement are not settled branding decisions.

## Confirmed navigation contract

- The navbar retains five positions: `[slot 1] [slot 2] [centre launcher] [slot 3] [slot 4]`.
- Replace the current middle College button with a fixed, unified centre button. Its final name is undecided.
- Clicking that centre button expands the button itself into access to the complete destination library, including genuinely new essential tabs. The expansion's exact geometry remains open; do not lock in the earlier side-panel proposal.
- **All four outer positions are fully configurable.** Any available destination from the full 12+ library can occupy any outer position, in any order chosen by the user. These are four total slots, two physically on each side, with no left/right category restrictions or permanently assigned destinations.
- College is an ordinary assignable destination after its current centre position is replaced. Today/Now, Fitness, Tasks, Reset, Settings, and future modules must not be arbitrarily excluded from assignment.
- The centre launcher is the only fixed control. It remains available regardless of the four chosen tabs, so unpinned destinations remain reachable.
- The inventory must grow through useful new functionality, not relabelling or fragmenting existing screens solely to reach a count.
- The current activity remains accessible through one persistent session surface across destinations.

## Required Settings revamp

Settings becomes a properly designed destination that matches the V2 material and motion system. Navigation customization is a complete experience within it, not an incidental four-item text menu.

- Show a live five-position navbar preview with the fixed centre clearly represented.
- Let the user select any of the four outer slots and assign any available destination from the full library.
- Let the user arrange those four chosen destinations in any order; provide an accessible alternative to drag reordering.
- Show clear names and icons when browsing destinations. The current assignments should be understandable at a glance.
- Persist assignments and ordering across navigation, process recreation, and app restarts.
- Keep Settings accessible through the launcher even when it is not pinned. Retain existing appearance, imports, targets, and data controls.

## Shared behavior

1. A single plan combines user-confirmed fixed commitments and flexible work. Now reads that same plan; it stops using sample moments as its primary experience.
2. Tasks, deadlines, projects, and study sessions link to their originating records. Scheduling a task, starting it, pausing it, and reviewing it preserve identity and context.
3. Starting work creates an active activity. Finishing it saves a real history entry and a resume point. Task completion remains an explicit choice.
4. Reminders offer useful actions such as Start, Delay, and Replan. Quiet/protected windows include classes, active work, family prayer, and relationship time.
5. Time and confirmed actions inform the interface. An explicit home check-in can drive the afternoon transition; automatic location/activity detection remains unagreed.
6. After a missed start, flexible blocks move around fixed commitments. The revised plan is visible and reversible.

## Material and motion proposal

- Keep the accepted charcoal/lavender foundation while adding depth, varied compositions, meaningful artwork, and richer visualized information.
- Establish a background scene that can be perceived through translucent dock/panel surfaces, with backdrop softening where supported, controlled edge lighting, and distinct surface depths.
- A source card expands into its detail/session surface, keeping its identity and position legible through the transition.
- A focus action becomes an active-session surface that can contract into the persistent capsule and expand again from another space.
- Replanning visibly moves flexible blocks along the day timeline while fixed anchors hold their places.
- The dock responds continuously to touch; the fixed centre button expands into the destination launcher with coordinated depth and haptics.
- Completing real work updates a visible activity trail and relevant progress visual, with proportionate completion feedback.
- Reading surfaces settle. Full-session motion should support interaction without delaying the primary action.

The motion and material system belong in the first V2 implementation, alongside working behavior. They are not a final decorative pass after static screens have been built.

## Reasons to return

Each opening should offer a continuation, a decision made easier, or visible evidence of progress: an exact resume point, a usable free window, a clear next set, a meaningful attendance projection, a project milestone, or an honest day/week history. Progress visuals must come from real records; no fabricated readiness scores or completed work.

## Build sequence and acceptance

1. **Connected shell:** expanded navigation, the shared day/activity model, persistent activity surface, and the first glass/shared-motion transitions. Existing areas stay functional.
2. **Real afternoon journey:** home check-in, a task placed in a feasible study window, focus with pause/continue, recorded session and resume note, plan/history updates, useful reminders, and preserved evening commitments. Sample moments move into an optional design-preview area.
3. **Broader depth:** connect College and Fitness to the same plan/history, add useful Projects and Capture flows, and build Review from actual records. Avoid publishing empty destination shells as finished features.
4. **Device refinement:** inspect complete journeys for animation continuity, touch response, glass readability, frame pacing, accessibility, and state recovery. Ship useful behavior and motion together throughout the sequence.

The next meaningful prototype should let the user travel through a connected afternoon and multiple spaces, with visible state continuity. A successful test is whether the user can answer what now/what next, resume their work without reconstruction, and perceive the app as one coherent experience. This does not establish long-term retention by itself.

## Technical feasibility and constraints

Native Compose supports shared bounds/elements; see https://developer.android.com/develop/ui/compose/animation/shared-elements . Blur capabilities vary by Android/Compose version; see https://developer.android.com/develop/ui/compose/graphics/images/customize . The project pins an older Compose BOM, so implementation must check available APIs before adopting current documentation examples. Blurring a foreground composable alone is not equivalent to capturing/softening a backdrop. Material effects need device verification.

Continue the delivery restrictions: no local Android compilation or emulator; preserve data/features and use the external cloud builder for main. The first implementation now replaces the primary sample selector with a real shared day and sessions, adds the 13-space library and working modules, and introduces opt-in reminders. Source checks do not establish a successful build or delivered alert. Exact attendance/timetable/workout inputs and name/logo remain pending.
