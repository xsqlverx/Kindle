package com.example.ui.screens.today

/** Explicit sample moments: these are suggestions, never detected activity or real class times. */
enum class DayMoment(
    val label: String,
    val sampleTime: String,
    val heading: String,
    val caption: String,
    val position: Float
) {
    COLLEGE("College", "10:15 AM", "Be here.\nStay a step ahead.", "A little clarity between classes.", 0.18f),
    HOME("Home", "2:45 PM", "Come home.\nKeep your momentum.", "There's room for rest. And a fresh start.", 0.43f),
    BEHIND("Running late", "5:10 PM", "Start here.\nThe day is still yours.", "One useful block can change the afternoon.", 0.64f)
}

data class SuggestedDayStep(val time: String, val title: String, val detail: String, val isAnchor: Boolean = false)

fun suggestedSteps(moment: DayMoment, recovered: Boolean): List<SuggestedDayStep> = when (moment) {
    DayMoment.COLLEGE -> listOf(
        SuggestedDayStep("NOW", "Stay with your class", "Open College for your saved timetable"),
        SuggestedDayStep("2:45", "Arrive & settle in", "Food, water, a little breathing room"),
        SuggestedDayStep("3:15", "One focused study block", "25 minutes with today's notes"),
        SuggestedDayStep("8:00", "Gym", "Your evening training window", true)
    )
    DayMoment.HOME -> listOf(
        SuggestedDayStep("NOW", "Eat. Put your bag down.", "Give yourself 30 minutes to land"),
        SuggestedDayStep("3:15", "Open today's notes", "One topic. One example. 25 minutes."),
        SuggestedDayStep("3:40", "Take a proper break", "Then decide what comes next"),
        SuggestedDayStep("8:00", "Gym", "Keep your evening training window", true)
    )
    DayMoment.BEHIND -> if (recovered) listOf(
        SuggestedDayStep("5:20", "One small study block", "25 minutes on your chosen task"),
        SuggestedDayStep("5:45", "Step away for a break", "Leave room to eat and get ready"),
        SuggestedDayStep("8:00", "Gym", "Your training window stays in place", true)
    ) else listOf(
        SuggestedDayStep("MISSED", "The afternoon start slipped", "You can choose a smaller next step"),
        SuggestedDayStep("NEXT", "Make room for 25 minutes", "Tap Make a smaller plan to try it"),
        SuggestedDayStep("8:00", "Gym", "Your training window stays in place", true)
    )
}
