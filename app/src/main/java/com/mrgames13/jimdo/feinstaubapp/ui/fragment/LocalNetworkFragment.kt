/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.fragment

import android.app.Application
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
import com.mrgames13.jimdo.feinstaubapp.model.db.ScrapingResultDbo
import com.mrgames13.jimdo.feinstaubapp.ui.item.ScrapingResultItem
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_local_network.view.*

class LocalNetworkFragment(
    private val application: Application,
    private val listener: LocalSearchListener
) : Fragment(), Observer<List<ScrapingResultDbo>> {

    // Variables as objects
    private lateinit var rootView: View
    private lateinit var viewModel: MainViewModel
    private lateinit var scrapingResultItemAdapter: ItemAdapter<ScrapingResultItem>

    // Interfaces
    interface LocalSearchListener {
        fun onRefreshLocalSensors()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(MainViewModel::class.java)

        return inflater.inflate(R.layout.fragment_local_network, container, false).run {
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

            this@LocalNetworkFragment.rootView = this
            this
        }
    }

    fun updateSearchProgress(progress: Int) {
        val progressString = getString(R.string.searching_for_sensors) + " " +
                String.format(getString(R.string.loading_percent), progress)
        rootView.loadingText.text = progressString
    }

    fun showSearchingScreen() {
        rootView.run {
            noData.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
        }
    }

    fun hideSearchingScreen() {
        rootView.run {
            if(viewModel.scrapingResults.value == null || viewModel.scrapingResults.value?.size == 0) {
                noDataText.setText(R.string.no_sensors_found_local)
                noData.visibility = View.VISIBLE
            }
            loadingContainer.visibility = View.GONE
        }
    }

    override fun onChanged(scrapingResults: List<ScrapingResultDbo>?) {

    }
}