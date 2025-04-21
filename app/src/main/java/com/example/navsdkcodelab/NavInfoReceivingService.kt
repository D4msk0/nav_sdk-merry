package com.example.navsdkcodelab

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.libraries.mapsplatform.turnbyturn.TurnByTurnManager
import java.io.OutputStream
import java.util.*

class NavInfoReceivingService : Service() {

    private lateinit var incomingMessenger: Messenger
    private lateinit var turnByTurnManager: TurnByTurnManager

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val esp32MacAddress = "94:E6:86:3C:E3:AE"
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectToEsp32() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            Log.e("NavService", "Bluetooth niet beschikbaar of niet ingeschakeld")
            return
        }

        val device: BluetoothDevice? = adapter.getRemoteDevice(esp32MacAddress)
        if (device == null) {
            Log.e("NavService", "Bluetooth-apparaat niet gevonden")
            return
        }

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            Log.d("NavService", "Bluetooth verbonden met ESP32")
        } catch (e: Exception) {
            Log.e("NavService", "Bluetooth connectie mislukt: ${e.message}")
        }
    }

    private fun sendNavInfoToESP32(instruction: String, distanceToNextStep: Int) {
        if (outputStream == null) {
            Log.w("NavService", "Bluetooth niet verbonden, kan niks versturen")
            return
        }

        val message = "Instructie: $instruction, Afstand: ${distanceToNextStep.toInt()}m\n"
        try {
            outputStream?.write(message.toByteArray())
            Log.d("NavService", "Data naar ESP32: $message")
        } catch (e: Exception) {
            Log.e("NavService", "Fout bij verzenden via Bluetooth: ${e.message}")
        }
    }

    private inner class IncomingNavStepHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == TurnByTurnManager.MSG_NAV_INFO) {
                val navInfo = turnByTurnManager.readNavInfoFromBundle(msg.data)

//                val instruction = navInfo.currentStep.maneuver
                val instruction = navInfo.currentStep.fullInstructionText
                val distanceToNextStep = navInfo.distanceToCurrentStepMeters

                Log.d("NavService", "Richting: $instruction")
                Log.d("NavService", "Afstand tot volgende stap: $distanceToNextStep")

                sendNavInfoToESP32(instruction, distanceToNextStep)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        turnByTurnManager = TurnByTurnManager.createInstance()

        val thread = HandlerThread("NavInfoReceivingService", Process.THREAD_PRIORITY_DEFAULT)
        thread.start()
        incomingMessenger = Messenger(IncomingNavStepHandler(thread.looper))

        connectToEsp32()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.e("NavService", "Fout bij sluiten van Bluetooth: ${e.message}")
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return incomingMessenger.binder
    }
}
