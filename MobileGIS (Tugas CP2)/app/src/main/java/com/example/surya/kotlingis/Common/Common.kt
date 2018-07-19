package com.example.surya.kotlingis.Common

import com.example.surya.kotlingis.Model.Results
import com.example.surya.kotlingis.Remote.IGoogleAPIService
import com.example.surya.kotlingis.Remote.RetrofitClient
import com.example.surya.kotlingis.Remote.RetrofitScalarsClient

object Common {

    private val GOOGLE_API_URL="https://maps.googleapis.com/"

    var currentResult:Results?=null

    val googleApiService:IGoogleAPIService
        get()=RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleAPIService::class.java)

    val googleApiServiceScalars:IGoogleAPIService
        get()=RetrofitScalarsClient.getClient(GOOGLE_API_URL).create(IGoogleAPIService::class.java)
}