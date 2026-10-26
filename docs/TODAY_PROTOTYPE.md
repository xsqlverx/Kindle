# Today: first prototype

Historical first-pass notes for commit `6c8b447`. The user accepted that prototype and requested continuation. The shared dock, workspace palette, and task-to-focus integration have since been extended; see STATUS.md for the current implementation and checks.

## Try it

Open Today. The first-look design starts in the Home moment. Select College, Home, or Running late to explore three explicitly labelled sample situations. Sample times are suggestions based on the user's approximate weekday, not activity detection or a verified timetable.

- **College:** Open College leads to the existing timetable, assignments, and attendance tools.
- **Home:** Choose a saved task or write a small next step, then start a 25-minute study timer.
- **Running late:** Make a smaller plan replaces the missed-start outline with a shorter proposed afternoon, preserving the gym window and acknowledging evening commitments.
- **Everyday tools:** Expand the plus control for real hydration logging, a 20-minute rest timer, Daily Reset, and the full original Today tools including activity rings.
- **Tasks and habits:** Existing saved tasks can be marked complete, and habits can be toggled. Starting or finishing a timer does not mark a task complete.
- **Navigation:** All five destinations and Settings remain reachable. The new dock is scoped to Today; other destinations keep their existing interfaces.

## Visual direction

Charcoal, smoke-violet, pale lavender, a sculpted day ribbon with a moving point of light, and restrained surface lighting. The ribbon and current-action composition are derived from the user's day. The supplied references inform materials and connected expansion motion.

Today uses a consistent dark palette for this visual study. Existing theme choices remain available for the other screens. The existing app name, logo, databases, signing setup, and cloud build configuration are unchanged.

## Timer behavior

Study and rest use a dedicated persisted session, separate from gym rest and Daily Reset timers. Running sessions use a stored deadline; paused sessions retain the remaining duration. The visible countdown refreshes only while Today is started. Returning after expiry shows completion. A task remains pending until the user explicitly completes it.

This prototype has no sound, alarm, notification delivery, automatic home detection, automatic study-content selection, or attendance-baseline import. The timer sheet states the alert limitation. Real timetable/attendance controls are accessed through College; switching sample moments does not change those records.

## Validation and cloud handoff

The user explicitly requested no local compilation. No Gradle build, emulator, or Android test execution was run locally. Source and diff checks were used for this handoff. Five plain JUnit timer-boundary cases are supplied for the cloud runner; they have not been executed locally.

Cloud/device checks after building:

1. Launch Today and switch through all three moments; verify layout at narrow width and larger font sizes.
2. In Running late, choose Make a smaller plan and verify the revised steps appear.
3. Choose a saved task or custom focus, start, pause, continue, navigate away, and return. Verify the session keeps the selected title and remaining time. Also check process recreation and expiry.
4. Expand tools, log water, toggle a habit, and complete a task; confirm changes persist in the existing screens.
5. Visit all five tabs, open Settings, and open attendance details from College.

Native rendering and build success remain unverified until the cloud builder and device run complete.
