package com.softartdev.poder.ui.main.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.softartdev.poder.R
import com.softartdev.poder.ui.base.BaseActivity
import com.tbruyelle.rxpermissions.RxPermissions

class MapsActivity(override val layout: Int = R.layout.activity_maps_sample) : BaseActivity(), OnMapReadyCallback, DialogInterface.OnClickListener {

    private var rxPermissions: RxPermissions? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rxPermissions = RxPermissions(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        showCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    private fun showCurrentLocation() {
        if (rxPermissions!!.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener(this, { location ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    // Logic to handle location object
                    val current = LatLng(location.latitude, location.longitude)
                    map?.moveCamera(CameraUpdateFactory.newLatLng(current))
                }
            })
            map?.isMyLocationEnabled = true
        } else {
            rxPermissions!!.request(Manifest.permission.ACCESS_FINE_LOCATION)
                    .subscribe { granted ->
                        if (granted) {
                            showCurrentLocation()
                        } else {
                            showRepeatableErrorWithSettings()
                        }
                    }
        }
    }

    private fun showRepeatableErrorWithSettings() {
        AlertDialog.Builder(this)
                .setMessage(R.string.rationale_location_permission)
                .setNegativeButton(R.string.dialog_action_cancel, this)
                .setPositiveButton(R.string.retry, this)
                .setNeutralButton(R.string.settings, this)
                .setCancelable(false)
                .show()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_NEGATIVE -> {
                dialog.cancel()
                finish()
            }
            DialogInterface.BUTTON_POSITIVE -> {
                dialog.cancel()
                showCurrentLocation()
            }
            DialogInterface.BUTTON_NEUTRAL -> {
                dialog.cancel()
                val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))
                startActivityForResult(appSettingsIntent, REQUEST_PERMISSION_LOCATION)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PERMISSION_LOCATION -> showCurrentLocation()
        }
    }

    companion object {
        internal val REQUEST_PERMISSION_LOCATION = 1003
    }
}