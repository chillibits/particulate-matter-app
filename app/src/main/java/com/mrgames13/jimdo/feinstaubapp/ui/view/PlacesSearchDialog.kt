/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.ui.adapter.PlaceAutocompleteAdapter
import kotlinx.android.synthetic.main.place_search_dialog.*

class PlacesSearchDialog(mContext: Context, private val listener: PlaceSelectedCallback): AppCompatDialog(mContext), PlaceAutocompleteAdapter.PlaceSelectedListener {

    // Variables as objects
    private val THRESHHOLD: Int = 1
    private lateinit var placesClient: PlacesClient
    private lateinit var h: Handler
    private var results = ArrayList<AutocompletePrediction>()

    interface PlaceSelectedCallback {
        fun onPlaceSelected(place: Place)
    }

    init {
        setContentView(R.layout.place_search_dialog)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        //LatLngBounds(LatLng(-85.0, 180.0), LatLng(85.0, -180.0))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        h = Handler()
        touchable_background.setOnClickListener {
            dismiss()
        }

        Places.initialize(context, context.getString(R.string.maps_api_key))
        placesClient = Places.createClient(context)

        initDialog()
    }

    private fun initDialog(){
        val adapter = PlaceAutocompleteAdapter(results, this)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter

        search_edit_text.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(query: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(query!!.length > THRESHHOLD) {
                    loadingIndicator.visibility = View.VISIBLE
                    recyclerFrame.visibility = View.GONE
                    val predictionRequest = FindAutocompletePredictionsRequest.builder()
                            .setQuery(query.toString())
                            .setSessionToken(AutocompleteSessionToken.newInstance())
                            .build()
                    placesClient
                            .findAutocompletePredictions(predictionRequest)
                            .addOnSuccessListener {response ->
                                if(response.autocompletePredictions.size > 0) {
                                    noResultsLayout.visibility = View.GONE

                                    results.clear()
                                    for(prediction in response.autocompletePredictions) results.add(prediction)

                                    recyclerView.adapter = PlaceAutocompleteAdapter(results, this@PlacesSearchDialog)
                                    recyclerFrame.visibility = View.VISIBLE
                                } else {
                                    noResultsLayout.visibility = View.VISIBLE
                                    recyclerFrame.visibility = View.GONE
                                }
                                loadingIndicator.visibility = View.GONE
                            }
                            .addOnFailureListener {exception ->
                                if(exception is ApiException) {
                                    Log.e("FA", "Place not found: " + exception.statusCode)
                                }
                                loadingIndicator.visibility = View.GONE
                                recyclerFrame.visibility = View.VISIBLE
                            }
                } else {
                    loadingIndicator.visibility = View.GONE
                    recyclerFrame.visibility = View.GONE
                }
            }
        })

        search_edit_text.requestFocus()
        showKeyboard()
    }

    override fun onPlaceSelected(placeId: String) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        placesClient.fetchPlace(FetchPlaceRequest.newInstance(placeId, placeFields))
                .addOnSuccessListener {response ->
                    listener.onPlaceSelected(response.place)
                    this.dismiss()
                }
                .addOnFailureListener {exception ->
                    if(exception is ApiException) {
                        Log.e("FA", "Not able to fetch place: " + exception.statusCode)
                        Toast.makeText(context, R.string.error_try_again, Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun showKeyboard(){
        this.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}