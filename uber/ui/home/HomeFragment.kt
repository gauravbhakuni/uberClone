package com.miniproject.uber.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.miniproject.uber.Model.DriversLocation
import com.miniproject.uber.R

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: com.google.android.gms.location.LocationCallback

    private lateinit var database: FirebaseDatabase
    private lateinit var driverLocationRef:DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onDestroy() {
        auth.currentUser?.let { user ->
            database.reference.child("DriverLocation").child(user.uid).removeValue()
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Request location permissions if not granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        driverLocationRef = database.reference.child("DriverLocations")

        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseApp.getApps(requireContext()).isEmpty()) {
            FirebaseApp.initializeApp(requireContext())
        }

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun updateCurrentUserLocation(location: Location){
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userLocation = DriversLocation(userId,location.latitude,location.longitude)
            driverLocationRef.child(userId).setValue(userLocation)
        }
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap

        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Enable the location button since permission is granted
            googleMap.isMyLocationEnabled = true

            // Call the function to get and display the current location
            showCurrentLocation()
        } else {
            // Permission is not granted, handle accordingly
            // You might want to request permission again or show a message to the user
            Log.e("MapPermission", "Location permission not granted")
            Toast.makeText(
                requireContext(),
                "Location permission is required to show your location on the map",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showCurrentLocation() {
        if (::googleMap.isInitialized) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            updateCurrentUserLocation(location)

                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        }
                        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

                        // Display the current user UID in a Toast
                        Toast.makeText(
                            requireContext(),
                            "Current User UID: $currentUserUid",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
        else{
            Log.e("MapInitialization", "Map not initialized")
            Toast.makeText(
                requireContext(),
                "Map is not initialized",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Handle the result of the permission request
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, enable location features
                    googleMap.isMyLocationEnabled = true
                    showCurrentLocation()
                } else {
                    // Permission denied, handle accordingly (e.g., show a message)
                    Log.e("MapPermission", "Location permission denied by the user")
                    Toast.makeText(
                        requireContext(),
                        "Location permission is required to show your location on the map",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release resources related to location updates if any
    }

}
