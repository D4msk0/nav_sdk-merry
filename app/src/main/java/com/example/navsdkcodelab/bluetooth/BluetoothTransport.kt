package com.example.navsdkcodelab.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.navsdkcodelab.model.BluetoothCommand
import java.io.OutputStream
import java.util.UUID

class BluetoothTransport(
    private val macAddress: String,
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
) {

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(): Boolean {
        disconnect()

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            Log.e(TAG, "Bluetooth adapter unavailable or disabled")
            return false
        }

        val device: BluetoothDevice? = adapter.getRemoteDevice(macAddress)
        if (device == null) {
            Log.e(TAG, "Device not found for address $macAddress")
            return false
        }

        return try {
            adapter.cancelDiscovery()
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            outputStream = socket?.outputStream
            Log.d(TAG, "Bluetooth connected to $macAddress")
            true
        } catch (error: Exception) {
            Log.e(TAG, "Bluetooth connection failed: ${error.message}")
            disconnect()
            false
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
        } catch (error: Exception) {
            Log.w(TAG, "Failed to close output stream: ${error.message}")
        }

        try {
            socket?.close()
        } catch (error: Exception) {
            Log.w(TAG, "Failed to close Bluetooth socket: ${error.message}")
        }

        outputStream = null
        socket = null
    }

    fun send(command: BluetoothCommand): Boolean {
        val stream = outputStream ?: run {
            Log.w(TAG, "Bluetooth not connected, dropping command $command")
            return false
        }

        return try {
            val payload = BluetoothCommandSerializer.serialize(command) + "\n"
            stream.write(payload.toByteArray(Charsets.UTF_8))
            stream.flush()
            Log.d(TAG, "Sent Bluetooth payload: $payload")
            true
        } catch (error: Exception) {
            Log.e(TAG, "Failed to send Bluetooth payload: ${error.message}")
            false
        }
    }

    fun isConnected(): Boolean = outputStream != null

    companion object {
        private const val TAG = "BluetoothTransport"
    }
}
