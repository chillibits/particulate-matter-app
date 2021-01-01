/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.fragment

import android.app.Application
import android.os.Bundle
import android.util.Log
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
import com.mrgames13.jimdo.feinstaubapp.model.dbo.ScrapingResultDbo
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.ui.item.ScrapingResultItem
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_local_network.view.*

class LocalNetworkFragment(
    private val application: Application,
    private val listener: LocalSearchListener
) : Fragment(), Observer<List<ScrapingResultDbo>> {

    // Variables as objects
    private lateinit var viewModel: MainViewModel
    private lateinit var scrapingResultItemAdapter: ItemAdapter<ScrapingResultItem>

    // Interfaces
    interface LocalSearchListener {
        fun onRefreshLocalSensors()
    }

    // Default constructor has to be implemented, otherwise the app crashes on configuration change
    constructor() : this(Application(), object: LocalSearchListener {
        override fun onRefreshLocalSensors() {}
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(MainViewModel::class.java)

        return inflater.inflate(R.layout.fragment_local_network, container, false).apply {
            // Initialize recycler view
            scrapingResultItemAdapter = ItemAdapter()
            scrapingResults.run {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                adapter = FastAdapter.with(scrapingResultItemAdapter)
            }

            // Initialize SwipeRefreshLayout
            refreshContainer.setOnRefreshListener {
                listener.onRefreshLocalSensors()
                refreshContainer.isRefreshing = false
            }

            // Observe live data
            viewModel.scrapingResults.observe(viewLifecycleOwner, this@LocalNetworkFragment)
        }
    }

    fun updateSearchProgress(progress: Int) {
        val progressString = getString(R.string.searching_for_sensors) + " " + String.format(getString(R.string.loading_percent), progress)
        view?.loadingText?.text = progressString
    }

    fun showSearchingScreen() {
        view?.run {
            noData.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
        }
    }

    fun hideSearchingScreen() {
        view?.run {
            if(viewModel.scrapingResults.value == null || viewModel.scrapingResults.value?.size == 0) {
                noDataText.setText(R.string.no_sensors_found_local)
                noData.visibility = View.VISIBLE
            }
            loadingContainer.visibility = View.GONE
        }
    }

    override fun onChanged(scrapingResults: List<ScrapingResultDbo>?) {
        Log.i(Constants.TAG, "Refreshing scraping results ...")
        scrapingResults?.let {
            scrapingResultItemAdapter.set(it.map { item -> ScrapingResultItem(viewModel, item) })
            view?.noData?.visibility = if(it.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}