package com.example.navsdkcodelab.navigation

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Waypoint

class NavigationController(
    private val activity: AppCompatActivity,
    private val navView: NavigationView,
    private val startLocation: LatLng,
    private val destinationPlaceId: String,
    private val onToast: (String) -> Unit
) {

    private var navigator: Navigator? = null
    private var arrivalListener: Navigator.ArrivalListener? = null
    private var routeChangedListener: Navigator.RouteChangedListener? = null
    private var googleMap: GoogleMap? = null

    var isSimulationMode: Boolean = false
        private set

    fun initialize(savedInstanceState: Bundle?) {
        navView.onCreate(savedInstanceState)

        NavigationApi.getNavigator(activity, object : NavigationApi.NavigatorListener {
            override fun onNavigatorReady(navigator: Navigator) {
                this@NavigationController.navigator = navigator

                val registered = navigator.registerServiceForNavUpdates(
                    activity.packageName,
                    "com.example.navsdkcodelab.NavInfoReceivingService",
                    1
                )

                if (registered) {
                    onToast("NavInfoReceivingService registered for nav updates")
                } else {
                    onToast("Failed to register NavInfoReceivingService")
                }

                registerNavigationListeners()
                navigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)
                setupCameraFollowMyLocation()

                if (isSimulationMode) {
                    navigator.simulator?.setUserLocation(startLocation)
                }

                navigateToPlace(destinationPlaceId)
            }

            override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                when (errorCode) {
                    NavigationApi.ErrorCode.NOT_AUTHORIZED -> onToast(
                        "Error loading Navigation API: Your API key is invalid or not authorized to use Navigation."
                    )
                    NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> onToast(
                        "Error loading Navigation API: User did not accept the Navigation Terms of Use."
                    )
                    else -> onToast("Error loading Navigation API: $errorCode")
                }
            }
        })
    }

    fun setSimulationMode(enabled: Boolean) {
        isSimulationMode = enabled

        navigator?.let { currentNavigator ->
            try {
                currentNavigator.stopGuidance()
                currentNavigator.clearDestinations()

                if (enabled) {
                    currentNavigator.simulator?.setUserLocation(startLocation)
                } else {
                    currentNavigator.simulator?.unsetUserLocation()
                }

                navigateToPlace(destinationPlaceId)
            } catch (error: Exception) {
                onToast("Failed to restart navigation: ${error.message}")
            }
        }
    }

    fun navigateToLatLng(latLng: LatLng) {
        val waypoint = Waypoint.builder()
            .setLatLng(latLng.latitude, latLng.longitude)
            .build()

        val pendingRoute = navigator?.setDestination(waypoint)
        pendingRoute?.setOnResultListener { code ->
            when (code) {
                Navigator.RouteStatus.OK -> {
                    activity.supportActionBar?.hide()
                    navigator?.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                    navigator?.startGuidance()
                }
                Navigator.RouteStatus.ROUTE_CANCELED -> onToast("Route guidance canceled.")
                Navigator.RouteStatus.NO_ROUTE_FOUND,
                Navigator.RouteStatus.NETWORK_ERROR -> onToast("Error starting guidance: $code")
                else -> onToast("Error starting guidance: $code")
            }
        }
    }

    fun navigateToPlace(placeId: String) {
        val waypoint = try {
            Waypoint.builder().setPlaceIdString(placeId).build()
        } catch (error: Waypoint.UnsupportedPlaceIdException) {
            onToast("Place ID was unsupported.")
            return
        }

        val pendingRoute = navigator?.setDestination(waypoint)
        pendingRoute?.setOnResultListener { code ->
            when (code) {
                Navigator.RouteStatus.OK -> {
                    activity.supportActionBar?.hide()
                    navigator?.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                    navigator?.startGuidance()

                    if (isSimulationMode) {
                        navigator?.simulator?.simulateLocationsAlongExistingRoute(
                            com.google.android.libraries.navigation.SimulationOptions().speedMultiplier(5f)
                        )
                    }
                }
                Navigator.RouteStatus.ROUTE_CANCELED -> onToast("Route guidance canceled.")
                Navigator.RouteStatus.NO_ROUTE_FOUND,
                Navigator.RouteStatus.NETWORK_ERROR -> onToast("Error starting guidance: $code")
                else -> onToast("Error starting guidance: $code")
            }
        }
    }

    private fun registerNavigationListeners() {
        arrivalListener = Navigator.ArrivalListener {
            onToast("User has arrived at the destination!")
            navigator?.clearDestinations()
        }
        navigator?.addArrivalListener(arrivalListener)

        routeChangedListener = Navigator.RouteChangedListener {
            onToast("The driver's route changed")
        }
        navigator?.addRouteChangedListener(routeChangedListener)
    }

    @SuppressLint("MissingPermission")
    private fun setupCameraFollowMyLocation() {
        navView.getMapAsync { map ->
            googleMap = map
            map.followMyLocation(GoogleMap.CameraPerspective.TILTED)

            if (!isSimulationMode) {
                map.setOnMapClickListener { latLng ->
                    onToast("Clicked map at ${latLng.latitude}, ${latLng.longitude}")
                    navigateToLatLng(latLng)
                }
            }
        }
    }

    fun onStart() {
        navView.onStart()
    }

    fun onResume() {
        navView.onResume()
    }

    fun onPause() {
        navView.onPause()
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        navView.onConfigurationChanged(newConfig)
    }

    fun onStop() {
        navView.onStop()
    }

    fun cleanup() {
        navigator?.also { currentNavigator ->
            if (arrivalListener != null) {
                currentNavigator.removeArrivalListener(arrivalListener)
            }
            if (routeChangedListener != null) {
                currentNavigator.removeRouteChangedListener(routeChangedListener)
            }
            currentNavigator.simulator?.unsetUserLocation()
            currentNavigator.unregisterServiceForNavUpdates()
            currentNavigator.cleanup()
        }

        navigator = null
        arrivalListener = null
        routeChangedListener = null
        googleMap = null
        navView.onDestroy()
    }
}
