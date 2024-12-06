package com.capztone.fishfy.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.capztone.admin.utils.FirebaseAuthUtil
import com.capztone.fishfy.R
import com.capztone.fishfy.databinding.ActivityMapsBinding
import com.capztone.fishfy.ui.activities.fragments.CurrentLocationBottomSheet
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: DatabaseReference
    private var pinnedAddress: String? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Firebase Auth
auth = FirebaseAuthUtil.auth

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check location services
        if (!isLocationEnabled()) {
            showLocationServicesDialog()
        } else {
            checkLocationPermissions()
        }

        // Set up the SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchLocation(it)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // Set up the arrow icon click event
        binding.arrowIcon.setOnClickListener {
            val query = binding.searchView.query.toString()
            if (query.isNotEmpty()) {
                searchLocation(query)
            }
        }

        binding.buttonConfirmLocation.setOnClickListener {
            if (pinnedAddress != null) {
                showCurrentLocationBottomSheet(pinnedAddress!!)
            } else {
                getCurrentAddressAndShowBottomSheet()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check location permissions
        checkLocationPermissions()

        mMap.setOnMapClickListener { latLng ->
            marker?.remove()
            val markerIcon = BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.map)
            )
            marker = mMap.addMarker(
                MarkerOptions().position(latLng).draggable(true).icon(markerIcon)
            )
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                pinnedAddress = addresses[0].getAddressLine(0)
            } else {
                pinnedAddress = "Address not found"
            }
        }

        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                this@MapsActivity.marker?.position = marker.position
                val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(marker.position.latitude, marker.position.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    pinnedAddress = addresses[0].getAddressLine(0)
                } else {
                    pinnedAddress = "Address not found"
                }
            }
        })
    }

    private fun showCurrentLocationBottomSheet(address: String) {
        val bottomSheetFragment = CurrentLocationBottomSheet()

        // Pass the address data to the fragment
        val bundle = Bundle()
        bundle.putString("address", address)
        bottomSheetFragment.arguments = bundle

        // Adjust the bottom sheet behavior
        bottomSheetFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)

        bottomSheetFragment.show(supportFragmentManager, "CurrentLocationBottomSheet")
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the missing permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val locality = addresses!![0].locality // Assuming you want the locality name

                val currentLocation = LatLng(location.latitude, location.longitude)
                val markerIcon = BitmapDescriptorFactory.fromBitmap(
                    BitmapFactory.decodeResource(resources, R.drawable.map)
                )
                marker = mMap.addMarker(
                    MarkerOptions().position(currentLocation).draggable(true).icon(markerIcon)
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))

                // Save location to Firebase
            }
        }
    }

    private fun getCurrentAddressAndShowBottomSheet() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val currentAddress = addresses[0].getAddressLine(0)
                        showCurrentLocationBottomSheet(currentAddress)
                    } else {
                        showCurrentLocationBottomSheet("Current address not found")
                    }
                } catch (e: IOException) {
                    showCurrentLocationBottomSheet("Error fetching current address: ${e.message}")
                }
            } else {
                showCurrentLocationBottomSheet("Unable to get current location")
            }
        }
    }

    private fun searchLocation(location: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(location, 1)
        if (addresses!!.isNotEmpty()) {
            val address = addresses[0]
            val latLng = LatLng(address.latitude, address.longitude)
            mMap.clear()
            val markerIcon = BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.map)
            )
            marker = mMap.addMarker(
                MarkerOptions().position(latLng).draggable(true).icon(markerIcon)
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
            pinnedAddress = address.getAddressLine(0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (isLocationEnabled()) {
                    getCurrentLocation()
                }
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showLocationServicesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Services Required")
            .setMessage("Please turn on location services to use this feature.")
            .setPositiveButton("Turn On") { dialog, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                // Handle cancellation if needed
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            if (isLocationEnabled()) {
                checkLocationPermissions()
            } else {
                showLocationServicesDialog()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val LOCATION_SETTINGS_REQUEST_CODE = 2
    }
}
