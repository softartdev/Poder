package com.softartdev.poder.ui.main.map

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.softartdev.poder.R
import kotlinx.android.synthetic.main.fragment_map.*

class MapFragment : Fragment(), OnMapReadyCallback {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var map: GoogleMap? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity as Activity)
        map_view?.onCreate(savedInstanceState)
        map_view?.getMapAsync(this)
        map_fab?.setOnClickListener { activity?.let { startActivity(Intent(it, MapsActivity::class.java)) } }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val grantedFine = context?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED }
        if (grantedFine == true) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener(activity as Activity, { location ->
                if (location != null) {
                    val current = LatLng(location.latitude, location.longitude)
                    map?.addMarker(MarkerOptions().position(current))
                    map?.moveCamera(CameraUpdateFactory.newLatLng(current))
                }
            })
            map?.isMyLocationEnabled = true
        } else {
            activity?.let { startActivity(Intent(it, MapsActivity::class.java)) }
        }
    }

    override fun onStart() {
        super.onStart()
        map_view?.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view?.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view?.onLowMemory()
    }
}
