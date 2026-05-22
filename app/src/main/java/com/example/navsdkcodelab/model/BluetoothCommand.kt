package com.example.navsdkcodelab.model

enum class BluetoothCommandType {
    TURN,
    ARRIVE,
    IDLE
}

data class BluetoothCommand(
    val type: BluetoothCommandType,
    val instruction: NavInstructionType? = null,
    val distanceMeters: Int = 0
)
