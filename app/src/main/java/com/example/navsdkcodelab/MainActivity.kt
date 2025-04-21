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
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint


class MainActivity : AppCompatActivity() {
    private var mNavigator: Navigator? = null
    private lateinit var navView: NavigationView
    private var arrivalListener: Navigator.ArrivalListener? = null
    private var routeChangedListener: Navigator.RouteChangedListener? = null
    private val isSimulationMode get() = true

    companion object {
        const val SPLASH_SCREEN_DELAY_MILLIS = 1000L
//        val startLocation = LatLng(49.2847001, -123.1145098)
        val START_LOCATION = LatLng(51.44440676010944, 5.46379722510205)
        const val END_LOCATION = "ChIJH8FSaBzZxkcRZWlh32Nlj40"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        navView = findViewById(R.id.navigation_view)
        navView.onCreate(savedInstanceState)
        requestAccessPermissions()
    }

    private fun requestAccessPermissions() {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        if (permissions.any { !checkPermissionGranted(it) }) {
            if (permissions.any { shouldShowRequestPermissionRationale(it) }) {
                // Display a dialogue explaining the required permissions.
            }
            val permissionsLauncher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                        permissionResults ->
                    if (
                        permissionResults.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
                    ) {
                        onLocationPermissionGranted()
                    } else {
                        finish()
                    }
                }
            permissionsLauncher.launch(permissions)
        } else {
            android.os
                .Handler(Looper.getMainLooper())
                .postDelayed({ onLocationPermissionGranted() }, SPLASH_SCREEN_DELAY_MILLIS)
        }
    }

    private fun checkPermissionGranted(permissionToCheck: String): Boolean =
        ContextCompat.checkSelfPermission(this, permissionToCheck) == PackageManager.PERMISSION_GRANTED

    private fun onLocationPermissionGranted() {
        initializeNavigationApi()
    }

    @SuppressLint("MissingPermission")
    private fun initializeNavigationApi() {
        val listener =
            object : NavigationApi.NavigatorListener {
                override fun onNavigatorReady(navigator: Navigator) {
                    mNavigator = navigator

                    val registered = mNavigator?.registerServiceForNavUpdates(
                        packageName,
                        NavInfoReceivingService::class.java.name,
                        1 // of 2, afhankelijk van hoeveel stappen je vooruit wilt ontvangen
                    )

                    if (registered == true) {
                        showToast("NavInfoReceivingService geregistreerd voor nav-updates")
                    } else {
                        showToast("Registratie van NavInfoReceivingService is mislukt")
                    }


                    registerNavigationListeners()
                    navigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)
                    setupCameraFollowMyLocation()
                    if (isSimulationMode) {
                        mNavigator?.simulator?.setUserLocation(START_LOCATION)
                    }
                    navigateToPlace(END_LOCATION)
                }

                override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                    when (errorCode) {
                        NavigationApi.ErrorCode.NOT_AUTHORIZED -> {
                            // Note: If this message is displayed, you may need to check that
                            // your API_KEY is specified correctly in AndroidManifest.xml
                            // and is been enabled to access the Navigation API
                            showToast(
                                "Error loading Navigation API: Your API key is " +
                                        "invalid or not authorized to use Navigation."
                            )
                        }
                        NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> {
                            showToast(
                                "Error loading Navigation API: User did not " +
                                        "accept the Navigation Terms of Use."
                            )
                        }
                        else -> showToast("Error loading Navigation API: $errorCode")
                    }
                }
            }
        NavigationApi.getNavigator(this, listener)
    }

    private fun showToast(errorMessage: String) {
        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun registerNavigationListeners() {
        arrivalListener =
            Navigator.ArrivalListener { // Show an onscreen message
                showToast("User has arrived at the destination!")
                mNavigator?.clearDestinations()
            }
        mNavigator?.addArrivalListener(arrivalListener)
        routeChangedListener =
            Navigator.RouteChangedListener { // Show an onscreen message when the route changes
                showToast("onRouteChanged: the driver's route changed")
            }
        mNavigator?.addRouteChangedListener(routeChangedListener)
    }

    @SuppressLint("MissingPermission")
    private fun setupCameraFollowMyLocation() {
        navView.getMapAsync { googleMap ->
            googleMap.followMyLocation(GoogleMap.CameraPerspective.TILTED)
        }
    }

    override fun onStart() {
        super.onStart()
        navView.onStart()
    }

    override fun onResume() {
        super.onResume()
        navView.onResume()
    }

    override fun onPause() {
        navView.onPause()
        super.onPause()
    }

    override fun onConfigurationChanged(configuration: Configuration) {
        super.onConfigurationChanged(configuration)
        navView.onConfigurationChanged(configuration)
    }

    override fun onStop() {
        navView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        navView.onDestroy()
        mNavigator?.also { navigator ->
            if (arrivalListener != null) {
                navigator.removeArrivalListener(arrivalListener)
            }
            if (routeChangedListener != null) {
                navigator.removeRouteChangedListener(routeChangedListener)
            }
            navigator.simulator?.unsetUserLocation()

            mNavigator?.unregisterServiceForNavUpdates()

            navigator.cleanup()
        }
        mNavigator = null
        super.onDestroy()
    }

    private fun navigateToPlace(placeId: String) {
        val waypoint: Waypoint? =
            try {
                Waypoint.builder().setPlaceIdString(placeId).build()
            } catch (e: Waypoint.UnsupportedPlaceIdException) {
                showToast("Place ID was unsupported.")
                return
            }
        val pendingRoute = mNavigator?.setDestination(waypoint)
        pendingRoute?.setOnResultListener { code ->
            when (code) {
                Navigator.RouteStatus.OK -> {
                    supportActionBar?.hide()
                    mNavigator?.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                    mNavigator?.startGuidance()

                    if (isSimulationMode) {
                        mNavigator
                            ?.simulator
                            ?.simulateLocationsAlongExistingRoute(SimulationOptions().speedMultiplier(5f))
                    }
                }
                Navigator.RouteStatus.ROUTE_CANCELED -> showToast("Route guidance canceled.")
                Navigator.RouteStatus.NO_ROUTE_FOUND,
                Navigator.RouteStatus.NETWORK_ERROR -> showToast("Error starting guidance: $code")
                else -> showToast("Error starting guidance: $code")
            }
        }
    }
}
