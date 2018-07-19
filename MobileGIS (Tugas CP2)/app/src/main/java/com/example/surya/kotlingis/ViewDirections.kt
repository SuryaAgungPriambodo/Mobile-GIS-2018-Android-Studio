package com.example.surya.kotlingis

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
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
import com.example.surya.kotlingis.Helper.DirectionJSONParser
import com.example.surya.kotlingis.Remote.IGoogleAPIService
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import dmax.dialog.SpotsDialog
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewDirections : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    lateinit var mService: IGoogleAPIService

    lateinit var mCurrentMarker: Marker

    var polyLine:Polyline?=null

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback
    lateinit var mLastLocation:Location

    companion object {
        private const val MY_PERMISSION_CODE: Int = 1000
    }

    private fun checkLocationPermission():Boolean {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ), ViewDirections.MY_PERMISSION_CODE)
            else
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ), ViewDirections.MY_PERMISSION_CODE)
            return false
        }
        else
            return true

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode)
        {
            ViewDirections.MY_PERMISSION_CODE ->{
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

                val markerOptions = MarkerOptions()
                        .position(LatLng(mLastLocation.latitude,mLastLocation.longitude))
                        .title("Posisi Anda")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_man))

                mCurrentMarker = mMap!!.addMarker(markerOptions)

                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(mLastLocation.latitude,mLastLocation.longitude)))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(12.0f))

                val destinationLatlng = LatLng(Common.currentResult!!.geometry!!.location!!.lat.toDouble(),
                        Common.currentResult!!.geometry!!.location!!.lng.toDouble())

                mMap!!.addMarker(MarkerOptions().position(destinationLatlng)
                        .title(Common.currentResult!!.name)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_man_d)))

                drawPath(mLastLocation,Common.currentResult!!.geometry!!.location!!)

            }
        }

    }

    private fun drawPath(mLastLocation: Location?, location: com.example.surya.kotlingis.Model.Location) {
        if(polyLine != null)
            polyLine!!.remove()

        val origin = StringBuilder(mLastLocation!!.latitude.toString())
                .append(",")
                .append(mLastLocation!!.longitude.toString())
                .toString()

        val destination = StringBuilder(location.lat.toString()).append(",").append(location.lng.toString()).toString()

        mService.getDirections(origin,destination)
                .enqueue(object:Callback<String>{
                    override fun onFailure(call: Call<String>?, t: Throwable?) {
                        Log.d("CARIAJA",t!!.message)
                    }

                    override fun onResponse(call: Call<String>?, response: Response<String>?) {
                        ParserTask().execute(response!!.body()!!.toString())
                    }

                })

    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_directions)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mService = Common.googleApiServiceScalars

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
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->

            mLastLocation = location

            val markerOptions = MarkerOptions()
                    .position(LatLng(mLastLocation.latitude,mLastLocation.longitude))
                    .title("Posisi Anda")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_man))

            mCurrentMarker = mMap!!.addMarker(markerOptions)

            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(mLastLocation.latitude,mLastLocation.longitude)))
            mMap!!.animateCamera(CameraUpdateFactory.zoomTo(12.0f))

            val destinationLatlng = LatLng(Common.currentResult!!.geometry!!.location!!.lat.toDouble(),
                    Common.currentResult!!.geometry!!.location!!.lng.toDouble())

            mMap!!.addMarker(MarkerOptions().position(destinationLatlng)
                    .title(Common.currentResult!!.name)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_man_d)))

            drawPath(mLastLocation,Common.currentResult!!.geometry!!.location!!)

        }
    }

    inner class ParserTask:AsyncTask<String,Int,List<List<HashMap<String,String>>>> (){

        internal val waitingDialog:AlertDialog = SpotsDialog(this@ViewDirections)

        override fun onPreExecute() {
            super.onPreExecute()
            waitingDialog.show()
            waitingDialog.setMessage("Mohon Tunggu......")
        }

        override fun doInBackground(vararg params: String?): List<List<HashMap<String, String>>>? {
            val jsonObject:JSONObject
            var routes:List<List<HashMap<String, String>>>?=null
            try{
                jsonObject = JSONObject(params[0])
                val parser = DirectionJSONParser()
                routes = parser.parse(jsonObject)
            }catch (e:JSONException)
            {
                e.printStackTrace()
            }
            return routes
        }

        override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
            super.onPostExecute(result)

            var points:ArrayList<LatLng>?=null
            var polylineOptions:PolylineOptions?=null

            for(i in result!!.indices)
            {
                points = ArrayList()
                polylineOptions = PolylineOptions()

                val path = result[i]

                for(j in path.indices)
                {
                    val point = path[j]
                    val lat = point["lat"]!!.toDouble()
                    val lng = point["lng"]!!.toDouble()
                    val position = LatLng(lat,lng)

                    points.add(position)
                }
                polylineOptions.addAll(points)
                polylineOptions.width(12f)
                polylineOptions.color(Color.RED)
                polylineOptions.geodesic(true)

            }
            polyLine = mMap!!.addPolyline(polylineOptions)
            waitingDialog.dismiss()
        }
    }
}

