package com.example.navsdkcodelab.bluetooth

import com.example.navsdkcodelab.model.BluetoothCommand
import com.example.navsdkcodelab.model.BluetoothCommandType
import com.example.navsdkcodelab.model.NavInstructionType

object BluetoothCommandSerializer {

    fun serialize(command: BluetoothCommand): String {
        return when (command.type) {
            BluetoothCommandType.ARRIVE -> "type=ARRIVE"
            BluetoothCommandType.IDLE -> "type=IDLE"
            BluetoothCommandType.TURN -> {
                val instruction = command.instruction ?: NavInstructionType.UNKNOWN
                "type=TURN|instruction=${instruction.name}|distanceMeters=${command.distanceMeters}"
            }
        }
    }

    fun parse(rawMessage: String): BluetoothCommand? {
        val message = rawMessage.trim()
        if (message.isEmpty()) {
            return null
        }

        return when {
            message == "type=ARRIVE" -> BluetoothCommand(BluetoothCommandType.ARRIVE)
            message == "type=IDLE" -> BluetoothCommand(BluetoothCommandType.IDLE)
            message.startsWith("type=TURN|") -> {
                val segments = message.split("|")
                var instruction: NavInstructionType? = null
                var distanceMeters = 0

                segments.drop(1).forEach { segment ->
                    when {
                        segment.startsWith("instruction=") -> {
                            instruction = runCatching { NavInstructionType.valueOf(segment.removePrefix("instruction=")) }
                                .getOrNull()
                        }
                        segment.startsWith("distanceMeters=") -> {
                            distanceMeters = segment.removePrefix("distanceMeters=").toIntOrNull() ?: 0
                        }
                    }
                }

                if (instruction == null) {
                    null
                } else {
                    BluetoothCommand(
                        type = BluetoothCommandType.TURN,
                        instruction = instruction,
                        distanceMeters = distanceMeters
                    )
                }
            }
            else -> null
        }
    }
}
