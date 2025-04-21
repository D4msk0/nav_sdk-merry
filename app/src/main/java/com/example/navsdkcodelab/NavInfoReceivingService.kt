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

    /**
     * Verbindt met het ESP32-apparaat via Bluetooth.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectToEsp32() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            Log.e("NavService", "Bluetooth niet beschikbaar of niet ingeschakeld")
            return
        }

        val device: BluetoothDevice? = adapter.getRemoteDevice(esp32MacAddress)
        if (device == null) {
            Log.e("NavService", "Bluetooth-apparaat met adres $esp32MacAddress niet gevonden")
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

    /**
     * Verzendt navigatie-informatie naar het ESP32-apparaat.
     * @param instruction De instructie die naar het apparaat moet worden gestuurd.
     * @param distanceToNextStep De afstand naar de volgende stap in meters.
     */
    private fun sendNavInfoToESP32(instruction: Int, distanceToNextStep: Int) {
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

    /**
     * Verwerkt inkomende navigatie-informatie.
     */
    private inner class IncomingNavStepHandler(looper: Looper) : Handler(looper) {
        private var lastInstruction: Int? = null
        private var lastDistance: Int? = null
        private var lastTimestamp: Long = 0
        private val minIntervalMs = 1000L  // Minimum interval tussen berichten in milliseconden (bijv. 1 seconde)
        private val destinationReachedThreshold = 1 // Drempel in meters voor bestemming bereikt

        override fun handleMessage(msg: Message) {
            if (msg.what == TurnByTurnManager.MSG_NAV_INFO) {
                val navInfo = turnByTurnManager.readNavInfoFromBundle(msg.data)

                val instruction = navInfo.currentStep.maneuver
                val distanceToNextStep = navInfo.distanceToCurrentStepMeters.toInt()
                val currentTimestamp = System.currentTimeMillis()

                // Controleer of er voldoende tijd is verstreken en of er een verandering is in instructie of afstand
                if (currentTimestamp - lastTimestamp >= minIntervalMs &&
                    (instruction != lastInstruction || distanceToNextStep != lastDistance)) {

                    // Controleer of de bestemming bereikt is
                    if (distanceToNextStep <= destinationReachedThreshold) {
                        Log.d("NavService", "Bestemming bereikt!")
                        sendNavInfoToESP32(-1, 0)  // Stuur een bericht dat de bestemming is bereikt
                    } else {
                        Log.d("NavService", "Richting: $instruction")
                        Log.d("NavService", "Afstand tot volgende stap: $distanceToNextStep")

                        sendNavInfoToESP32(instruction, distanceToNextStep)
                    }

                    // Update de laatst verstuurde instructie, afstand en tijd
                    lastInstruction = instruction
                    lastDistance = distanceToNextStep
                    lastTimestamp = currentTimestamp
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate() {
        super.onCreate()
        turnByTurnManager = TurnByTurnManager.createInstance()

        // Start een nieuwe thread om berichten van de TurnByTurnManager te ontvangen
        val thread = HandlerThread("NavInfoReceivingService", Process.THREAD_PRIORITY_DEFAULT).apply { start() }
        incomingMessenger = Messenger(IncomingNavStepHandler(thread.looper))

        // Maak verbinding met het ESP32-apparaat
        connectToEsp32()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Sluit de Bluetooth-verbinding netjes af
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d("NavService", "Bluetooth verbinding gesloten.")
        } catch (e: Exception) {
            Log.e("NavService", "Fout bij sluiten van Bluetooth: ${e.message}")
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return incomingMessenger.binder
    }
}
