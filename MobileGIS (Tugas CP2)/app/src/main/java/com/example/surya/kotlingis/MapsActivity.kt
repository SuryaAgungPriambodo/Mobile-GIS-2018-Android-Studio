package com.example.surya.kotlingis

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteFullException
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.example.surya.kotlingis.Common.Common
import com.example.surya.kotlingis.Model.MyPlaces
import com.example.surya.kotlingis.Remote.IGoogleAPIService
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private var latitude:Double=0.toDouble()
    private var longitude:Double=0.toDouble()

    private lateinit var mLastLocation:Location
    private var mMarker: Marker?=null

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    companion object {
        private const val MY_PERMISSION_CODE: Int = 1000
    }


    lateinit var mService:IGoogleAPIService

    internal lateinit var currentPlace:MyPlaces


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mService = Common.googleApiService

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationPermission()) {
                buildLocationRequest();
                buildLocationCallBack();

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        }
        else{
            buildLocationRequest();
            buildLocationCallBack();

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        }

        bottom_navigation_view.setOnNavigationItemSelectedListener { item->
            when(item.itemId)
            {
                R.id.action_hospital -> nearByPlace("hospital")
                R.id.action_police -> nearByPlace("police")
                R.id.action_gas -> nearByPlace("gas_station")
                R.id.action_bank -> nearByPlace("atm")
                R.id.action_pos -> nearByPlace("post_office")
            }
            true
        }
    }

    private fun nearByPlace(typePlace: String) {

        mMap.clear()

        val url = getUrl(latitude,longitude,typePlace)

        mService.getNearbyPlaces(url)
                .enqueue(object : Callback<MyPlaces>{
                    override fun onResponse(call: Call<MyPlaces>?, response: Response<MyPlaces>?) {

                        currentPlace = response!!.body()!!

                        if(response!!.isSuccessful)
                        {
                            for(i in 0 until response!!.body()!!.results!!.size)
                            {
                                val latLngi = LatLng(latitude,longitude)
                                val markerOptions = MarkerOptions()
                                        .position(latLngi)
                                        .title("Posisi Anda")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_man))
                                mMarker = mMap!!.addMarker(markerOptions)

                                val googlePlace = response.body()!!.results!![i]
                                val lat = googlePlace.geometry!!.location!!.lat
                                val lng = googlePlace.geometry!!.location!!.lng
                                val placeName = googlePlace.name
                                val latLng = LatLng(lat, lng)

                                markerOptions.position(latLng)
                                markerOptions.title(placeName)
                                if(typePlace.equals("hospital"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_hospital))
                                else if(typePlace.equals("police"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_police))
                                else if(typePlace.equals("gas_station"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_gas))
                                else if(typePlace.equals("atm"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bank))
                                else if(typePlace.equals("post_office"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pos))
                                else
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_man))

                                markerOptions.snippet(i.toString())

                                mMap!!.addMarker(markerOptions)
                                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))
                            }
                        }
                    }

                    override fun onFailure(call: Call<MyPlaces>?, t: Throwable?) {
                        Toast.makeText(baseContext, ""+t!!.message,Toast.LENGTH_SHORT).show()
                    }

                })
    }

    private fun getUrl(latitude: Double, longitude: Double, typePlace: String): String {

        val googlePlaceUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=10000")
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&key=AIzaSyBvI6OhpkdSNRzlvSe7iWXJuyxrwxigEcg")

        Log.d("URL_DEBUG",googlePlaceUrl.toString())
        return googlePlaceUrl.toString()

    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f

    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                mLastLocation = p0!!.lastLocation

                if (mMarker != null)
                {
                    mMarker!!.remove()
                }

                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude

                val latLng = LatLng(latitude,longitude)
                val markerOptions = MarkerOptions()
                        .position(latLng)
                        .title("Posisi Anda")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_man))
                mMarker = mMap!!.addMarker(markerOptions)

                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))

            }
        }

    }


    private fun checkLocationPermission():Boolean {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ),MY_PERMISSION_CODE)
            else
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ),MY_PERMISSION_CODE)
            return false
        }
        else
            return true

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode)
        {
            MY_PERMISSION_CODE->{
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        if(checkLocationPermission()) {
                            buildLocationRequest();
                            buildLocationCallBack();

                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

                            mMap!!.isMyLocationEnabled=true
                        }
                }
                else{
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap!!.isMyLocationEnabled = true
            }
        }
        else
            mMap!!.isMyLocationEnabled = true

        mMap.uiSettings.isZoomControlsEnabled = false

        mMap!!.setOnMarkerClickListener { marker ->
            if (marker.snippet != null) {
                Common.currentResult = currentPlace!!.results!![Integer.parseInt(marker.snippet)]

                startActivity(Intent(this@MapsActivity, ViewPlace::class.java))
            }
            true
        }
    }
}
