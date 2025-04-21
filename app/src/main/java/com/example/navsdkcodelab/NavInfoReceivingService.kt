package com.example.navsdkcodelab

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
import com.google.android.libraries.mapsplatform.turnbyturn.TurnByTurnManager

/**
 * Receives turn-by-turn navigation information forwarded from NavSDK.
 */
class NavInfoReceivingService : Service() {

    /** The messenger used by the service to receive nav step updates. */
    private lateinit var incomingMessenger: Messenger
    private lateinit var turnByTurnManager: TurnByTurnManager

    private inner class IncomingNavStepHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == TurnByTurnManager.MSG_NAV_INFO) {
                val navInfo = turnByTurnManager.readNavInfoFromBundle(msg.data)

                // Haal de instructie of richting uit de NavInfo
//                val instruction = navInfo.currentStep.fullInstructionText
                val instruction = navInfo.currentStep.maneuver
                val distanceToNextStep = navInfo.distanceToCurrentStepMeters

                // Log de richting en de afstand naar de volgende stap
                Log.d("NavService", "Volgende richting: $instruction")
                Log.d("NavService", "Afstand tot volgende stap: $distanceToNextStep meters")
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return incomingMessenger.binder
    }

    override fun onCreate() {
        super.onCreate()
        turnByTurnManager = TurnByTurnManager.createInstance()
        val thread = HandlerThread(
            "NavInfoReceivingService",
            Process.THREAD_PRIORITY_DEFAULT
        )
        thread.start()
        incomingMessenger = Messenger(IncomingNavStepHandler(thread.looper))
    }
}