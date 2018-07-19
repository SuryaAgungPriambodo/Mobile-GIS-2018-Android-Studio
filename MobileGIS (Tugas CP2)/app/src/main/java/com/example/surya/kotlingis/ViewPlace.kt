package com.example.surya.kotlingis

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.surya.kotlingis.Common.Common
import com.example.surya.kotlingis.Model.PlaceDetail
import com.example.surya.kotlingis.Remote.IGoogleAPIService
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_view_place.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewPlace : AppCompatActivity() {

    internal lateinit var mService:IGoogleAPIService
    var mPlace:PlaceDetail?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_place)

        mService = Common.googleApiService

        place_name.text=""
        place_address.text=""
        place_open_hour.text=""

        btn_show_map.setOnClickListener{
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mPlace!!.result!!.url))
            startActivity(mapIntent)
        }

        btn_view_direction.setOnClickListener{
            val viewDirections = Intent(this@ViewPlace,ViewDirections::class.java)
            startActivity(viewDirections)
        }

        if(Common.currentResult!!.photos != null && Common.currentResult!!.photos!!.size > 0)
            Picasso.with(this)
                    .load(getPhotoOfPlace(Common.currentResult!!.photos!![0].photo_reference!!,1000))
                    .into(photo)

        if(Common.currentResult!!.rating != null)
            rating_bar.rating = Common.currentResult!!.rating.toFloat()
        else
            rating_bar.visibility = View.GONE

        if(Common.currentResult!!.opening_hours != null){
            if(Common.currentResult!!.opening_hours!!.open_now == true)
            {
                place_open_hour.setText("Buka")
            }else
                place_open_hour.setText("Tutup")
        }else
            place_open_hour.visibility = View.GONE

        mService.getDetailPlace(getPlaceDetailUrl(Common.currentResult!!.place_id!!))
                .enqueue(object : retrofit2.Callback<PlaceDetail>{
                    override fun onFailure(call: Call<PlaceDetail>?, t: Throwable?) {
                        Toast.makeText(baseContext,""+t!!.message,Toast.LENGTH_SHORT).show()
                    }

                    override fun onResponse(call: Call<PlaceDetail>?, response: Response<PlaceDetail>?) {
                        mPlace = response!!.body()

                        place_address.text = mPlace!!.result!!.formatted_address
                        place_name.text = mPlace!!.result!!.name

                    }

                })



    }

    private fun getPlaceDetailUrl(place_id: String): String {

        val url = StringBuilder("https://maps.googleapis.com/maps/api/place/details/json")
        url.append("?placeid=$place_id")
        url.append("&key=AIzaSyBvI6OhpkdSNRzlvSe7iWXJuyxrwxigEcg")
        return url.toString()
    }

    private fun getPhotoOfPlace(photo_reference: String, maxWidth: Int): String {

        val url = StringBuilder("https://maps.googleapis.com/maps/api/place/photo")
        url.append("?maxwidth=$maxWidth")
        url.append("&photoreference=$photo_reference")
        url.append("&key=AIzaSyBvI6OhpkdSNRzlvSe7iWXJuyxrwxigEcg")
        return url.toString()

    }
}
