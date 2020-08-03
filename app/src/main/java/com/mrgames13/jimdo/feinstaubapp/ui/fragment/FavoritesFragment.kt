/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.dbo.SensorDbo
import com.mrgames13.jimdo.feinstaubapp.ui.item.SensorItem
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_favorites.*
import kotlinx.android.synthetic.main.fragment_favorites.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment(), Observer<List<SensorDbo>> {

    // Variables as objects
    private lateinit var rootView: View
    private lateinit var viewModel: MainViewModel
    private lateinit var favoritesAdapter: ItemAdapter<SensorItem>

    // Default constructor has to be implemented, otherwise the app crashes on configuration change
    //constructor() : this()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)).get(
            MainViewModel::class.java)

        return inflater.inflate(R.layout.fragment_favorites, container, false).run {
            // Initialize RecyclerView
            favoritesAdapter = ItemAdapter()
            favorites.run {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                adapter = FastAdapter.with(favoritesAdapter)
            }

            // Initialize SwipeRefreshLayout
            refreshContainer.setOnRefreshListener { refreshManually() }

            // Observer live data
            viewModel.sensors.observe(viewLifecycleOwner, this@FavoritesFragment)

            this@FavoritesFragment.rootView = this
            this
        }
    }

    override fun onStart() {
        super.onStart()
        refreshManually()
    }

    private fun refreshManually() {
        refreshContainer.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.manuallyRefreshSensors()
            withContext(Dispatchers.Main) {
                refreshContainer.isRefreshing = false
            }
        }
    }

    override fun onChanged(sensors: List<SensorDbo>?) {
        // Hide no data container
        noData.visibility = if(sensors?.size == 0) View.VISIBLE else View.GONE
        // Add new data to the recycler view
        favoritesAdapter.clear()
        sensors?.let {
            favoritesAdapter.add(sensors.map { program ->
                SensorItem(requireContext(), program)
            })
        }
    }
}