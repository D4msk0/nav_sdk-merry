package com.example.navsdkcodelab.navigation

import com.example.navsdkcodelab.model.NavInstruction
import com.example.navsdkcodelab.model.NavInstructionType

object NavInstructionMapper {

    fun fromManeuver(maneuver: Int, distanceMeters: Int): NavInstruction {
        val normalizedDistance = distanceMeters.coerceAtLeast(0)

        val instructionType = when (maneuver) {
            6 -> NavInstructionType.LEFT
            7 -> NavInstructionType.RIGHT
            0, 1, 2, 3, 4, 5, 8, 9, 10 -> NavInstructionType.STRAIGHT
            -1 -> NavInstructionType.ARRIVE
            else -> NavInstructionType.UNKNOWN
        }

        return NavInstruction(
            type = instructionType,
            distanceMeters = normalizedDistance,
            rawManeuver = maneuver
        )
    }
}
