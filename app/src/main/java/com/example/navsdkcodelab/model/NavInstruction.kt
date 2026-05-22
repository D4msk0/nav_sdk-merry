package com.example.navsdkcodelab.model

enum class NavInstructionType {
    LEFT,
    RIGHT,
    STRAIGHT,
    ARRIVE,
    UNKNOWN
}

data class NavInstruction(
    val type: NavInstructionType,
    val distanceMeters: Int,
    val rawManeuver: Int? = null
)
