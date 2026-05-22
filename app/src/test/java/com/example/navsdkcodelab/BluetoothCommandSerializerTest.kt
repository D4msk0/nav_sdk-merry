package com.example.navsdkcodelab

import com.example.navsdkcodelab.bluetooth.BluetoothCommandSerializer
import com.example.navsdkcodelab.model.BluetoothCommand
import com.example.navsdkcodelab.model.BluetoothCommandType
import com.example.navsdkcodelab.model.NavInstructionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BluetoothCommandSerializerTest {

    @Test
    fun serializeAndParseTurnCommand() {
        val command = BluetoothCommand(
            type = BluetoothCommandType.TURN,
            instruction = NavInstructionType.LEFT,
            distanceMeters = 42
        )

        val serialized = BluetoothCommandSerializer.serialize(command)
        val parsed = BluetoothCommandSerializer.parse(serialized)

        assertNotNull(parsed)
        assertEquals(command, parsed)
    }

    @Test
    fun serializeAndParseArrivalCommand() {
        val command = BluetoothCommand(type = BluetoothCommandType.ARRIVE)

        val serialized = BluetoothCommandSerializer.serialize(command)
        val parsed = BluetoothCommandSerializer.parse(serialized)

        assertNotNull(parsed)
        assertEquals(command, parsed)
    }
}
