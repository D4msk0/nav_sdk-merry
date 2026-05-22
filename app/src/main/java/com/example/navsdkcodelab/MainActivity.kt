// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.example.navsdkcodelab

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.navsdkcodelab.navigation.NavigationController
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity() {

    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Bluetooth permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_LONG).show()
            }
        }

    private var restoredState: Bundle? = null
    private var isSimulationMode: Boolean = false

    private lateinit var simulationSwitch: Switch
    private lateinit var navigationController: NavigationController

    companion object {
        const val SPLASH_SCREEN_DELAY_MILLIS = 1000L
        val START_LOCATION = LatLng(51.44440676010944, 5.46379722510205)
        const val END_LOCATION = "ChIJH8FSaBzZxkcRZWlh32Nlj40"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoredState = savedInstanceState
        setContentView(R.layout.activity_main)

        simulationSwitch = findViewById(R.id.simulation_mode_switch)
        simulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            isSimulationMode = isChecked
            navigationController.setSimulationMode(isChecked)
            showToast("Simulation: ${if (isChecked) "On" else "Off"}")
        }

        navigationController = NavigationController(
            activity = this,
            navView = findViewById(R.id.navigation_view),
            startLocation = START_LOCATION,
            destinationPlaceId = END_LOCATION,
            onToast = ::showToast
        )

        checkBluetoothPermission()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestAccessPermissions()
    }

    private fun checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED -> Unit

                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) -> {
                    Toast.makeText(
                        this,
                        "Bluetooth permission is required to communicate with the ESP32",
                        Toast.LENGTH_LONG
                    ).show()
                    requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }

                else -> requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    private fun requestAccessPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.any { !checkPermissionGranted(it) }) {
            val permissionsLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionResults ->
                if (permissionResults.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
                    onLocationPermissionGranted()
                } else {
                    finish()
                }
            }

            permissionsLauncher.launch(permissions)
        } else {
            android.os.Handler(Looper.getMainLooper())
                .postDelayed({ onLocationPermissionGranted() }, SPLASH_SCREEN_DELAY_MILLIS)
        }
    }

    private fun checkPermissionGranted(permissionToCheck: String): Boolean =
        ContextCompat.checkSelfPermission(this, permissionToCheck) == PackageManager.PERMISSION_GRANTED

    private fun onLocationPermissionGranted() {
        navigationController.initialize(restoredState)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        navigationController.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigationController.onResume()
    }

    override fun onPause() {
        navigationController.onPause()
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        navigationController.onConfigurationChanged(newConfig)
    }

    override fun onStop() {
        navigationController.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        navigationController.cleanup()
        super.onDestroy()
    }
}

