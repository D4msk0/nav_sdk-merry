package com.example.navsdkcodelab

import android.Manifest
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.navsdkcodelab.bluetooth.BluetoothTransport
import com.example.navsdkcodelab.model.BluetoothCommand
import com.example.navsdkcodelab.model.BluetoothCommandType
import com.example.navsdkcodelab.navigation.NavInstructionMapper
import com.google.android.libraries.mapsplatform.turnbyturn.TurnByTurnManager

class NavInfoReceivingService : Service() {

    private lateinit var incomingMessenger: Messenger
    private lateinit var turnByTurnManager: TurnByTurnManager
    private lateinit var bluetoothTransport: BluetoothTransport

    private val esp32MacAddress = "94:E6:86:3C:70:0E"

    private inner class IncomingNavStepHandler(looper: Looper) : Handler(looper) {
        private var lastCommand: BluetoothCommand? = null
        private var lastTimestamp: Long = 0
        private val minIntervalMs = 1000L
        private val destinationReachedThreshold = 1

        override fun handleMessage(msg: Message) {
            if (msg.what != TurnByTurnManager.MSG_NAV_INFO) {
                return
            }

            val navInfo = turnByTurnManager.readNavInfoFromBundle(msg.data)
            val step = navInfo.currentStep

            if (step == null) {
                Log.w(TAG, "Received nav info without current step")
                return
            }

            val instruction = NavInstructionMapper.fromManeuver(
                maneuver = step.maneuver,
                distanceMeters = navInfo.distanceToCurrentStepMeters.toInt()
            )

            val command = if (instruction.distanceMeters <= destinationReachedThreshold) {
                BluetoothCommand(type = BluetoothCommandType.ARRIVE)
            } else {
                BluetoothCommand(
                    type = BluetoothCommandType.TURN,
                    instruction = instruction.type,
                    distanceMeters = instruction.distanceMeters
                )
            }

            val currentTimestamp = System.currentTimeMillis()
            val shouldSend = currentTimestamp - lastTimestamp >= minIntervalMs && command != lastCommand

            if (!shouldSend) {
                return
            }

            Log.d(TAG, "Sending command: $command")
            bluetoothTransport.send(command)

            lastCommand = command
            lastTimestamp = currentTimestamp
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate() {
        super.onCreate()
        turnByTurnManager = TurnByTurnManager.createInstance()
        bluetoothTransport = BluetoothTransport(esp32MacAddress)
        bluetoothTransport.connect()

        val thread = HandlerThread("NavInfoReceivingService", Process.THREAD_PRIORITY_DEFAULT).apply {
            start()
        }
        incomingMessenger = Messenger(IncomingNavStepHandler(thread.looper))
    }

    override fun onDestroy() {
        bluetoothTransport.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder = incomingMessenger.binder

    companion object {
        private const val TAG = "NavInfoReceivingService"
    }
}

