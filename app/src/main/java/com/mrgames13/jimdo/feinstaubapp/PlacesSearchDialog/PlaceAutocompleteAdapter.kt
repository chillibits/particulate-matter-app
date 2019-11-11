/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.PlacesSearchDialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.place_search_item.view.*

class PlaceAutocompleteAdapter(private val results: ArrayList<AutocompletePrediction>, private val listener: PlaceSelectedListener) : RecyclerView.Adapter<PlaceAutocompleteAdapter.PlacesViewHolder>() {

    // Interfaces
    interface PlaceSelectedListener {
        fun onPlaceSelected(placeId: String)
    }

    inner class PlacesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacesViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.place_search_item, parent, false)
        return PlacesViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PlacesViewHolder, pos: Int) {
        val prediction = results[pos]
        holder.itemView.primaryText.text = prediction.getPrimaryText(null).toString()
        holder.itemView.secondaryText.text = prediction.getSecondaryText(null).toString()
        holder.itemView.setOnClickListener {
            listener.onPlaceSelected(prediction.placeId)
        }
    }

    override fun getItemCount(): Int {
        return results.size
    }
}