/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.activity

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.fxn.OnBubbleClickListener
import com.google.android.libraries.places.api.model.Place
import com.google.zxing.integration.android.IntentIntegrator
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.model.io.ScrapingResult
import com.mrgames13.jimdo.feintaubapp.shared.*
import com.mrgames13.jimdo.feintaubapp.ui.adapter.viewpager.ViewPagerAdapterMain
import com.mrgames13.jimdo.feintaubapp.ui.closeActivityWithRevealAnimation
import com.mrgames13.jimdo.feintaubapp.ui.dialog.PlacesSearchDialog
import com.mrgames13.jimdo.feintaubapp.ui.dialog.showImportExportDialog
import com.mrgames13.jimdo.feintaubapp.ui.dialog.showRatingDialog
import com.mrgames13.jimdo.feintaubapp.ui.dialog.showRecommendationDialog
import com.mrgames13.jimdo.feintaubapp.ui.fragment.AllSensorsFragment
import com.mrgames13.jimdo.feintaubapp.ui.openActivityWithRevealAnimation
import com.mrgames13.jimdo.feintaubapp.ui.task.SensorIPSearchTask
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.place_search_dialog.*
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : AppCompatActivity(), AllSensorsFragment.OnAdapterEventListener, PlacesSearchDialog.PlaceSelectedCallback {

    // Variables as objects
    private var searchMenuItem: MenuItem? = null
    private lateinit var searchTask: SensorIPSearchTask
    private lateinit var viewpagerAdapter: ViewPagerAdapterMain

    // Variables
    private var selectedPage = 1
    private var pressedOnce = false
    private var isFullscreen = false
    private var exitedFullscreenOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize toolbar
        setSupportActionBar(toolbar)

        // Apply window insets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                tabBar.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                insets
            }
        }

        // Initialize ViewPager
        viewpagerAdapter = ViewPagerAdapterMain(supportFragmentManager, lifecycle, this)
        viewPager.run {
            offscreenPageLimit = 3
            isUserInputEnabled = false
            adapter = viewpagerAdapter
            setCurrentItem(1, false) // Start on the map
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    // Close searchView
                    if(searchView.isSearchOpen) searchView.closeSearch()

                    // perform actions, depending on selected page
                    when(pos) {
                        0 -> switchToFavoritesPage()
                        1 -> switchToAllSensorsPage()
                        2 -> switchToOwnSensorsPage()
                        3 -> switchToLocalNetworkPage()
                    }
                    selectedPage = pos
                    tabBar.setSelected(pos)
                }
            })
        }

        // Initialize BubbleTabBar
        tabBar.addBubbleListener(object : OnBubbleClickListener {
            override fun onBubbleClick(id: Int) {
                // Tell viewPager which page to show
                viewPager.currentItem = when(id) {
                    R.id.item_favorites -> 0
                    R.id.item_all_sensors -> 1
                    R.id.item_own_sensors -> 2
                    R.id.item_local_network -> 3
                    else -> 1
                }
            }
        })

        // Initialize AddSearchFab
        fabAddSearch.setOnClickListener {
            when(selectedPage) {
                1 -> openPlacesSearch()
                2 -> openAddSensorActivity()
                3 -> startLocalNetworkSearch()
            }
        }

        // Initialize SearchView
        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {

                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {

                return true
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        searchMenuItem = menu?.findItem(R.id.action_search)
        searchView.setMenuItem(searchMenuItem)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_search -> item.expandActionView()
            R.id.action_import_export -> showImportExportDialog()
            R.id.action_rate -> showRatingDialog()
            R.id.action_settings -> {
                availableSoon()
                LibsBuilder()
                    .withActivityTitle(getString(R.string.pref_open_source_t))
                    .withAboutAppName(getString(R.string.app_name))
                    .withAboutDescription(getString(R.string.app_description))
                    .withAboutVersionShownCode(false)
                    .withEdgeToEdge(true)
                    .withLicenseDialog(true)
                    .withLicenseShown(true)
                    .start(this)
                return true
            }
            R.id.action_recommend -> showRecommendationDialog()
            R.id.action_web -> openQRScanner()
            R.id.action_help -> openFAQPage()
            R.id.action_quit -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                Constants.REQ_SCAN_WEB -> initializeWebConnection(resultCode, data)
                Constants.REQ_ADD_SENSOR -> refresh()
            }
        } else {
            when(requestCode) {
                Constants.REQ_ADD_SENSOR -> closeActivityWithRevealAnimation(this, fabAddSearch, revealSheet)
                else -> outputErrorMessage()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(isFullscreen) {
                toggleFullscreen()
            } else if(searchView.isSearchOpen) {
                searchView.closeSearch()
            } else if(!pressedOnce) {
                pressedOnce = true
                Toast.makeText(this, R.string.tap_again_to_exit_app, Toast.LENGTH_SHORT).show()
                Handler().postDelayed({ pressedOnce = false }, 2500)
            } else {
                pressedOnce = false
                onBackPressed()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun switchToFavoritesPage() { // page index 0
        // Hide the fab
        if(fabAddSearch.isOrWillBeShown) fabAddSearch.hide()
        // Show search item
        searchMenuItem?.isVisible = true
    }

    private fun switchToAllSensorsPage() { // page index 1
        // Show and animate the fab
        if(!fabAddSearch.isOrWillBeShown) fabAddSearch.show()
        if(selectedPage == 0 || selectedPage == 2) {
            fabAddSearch.setImageResource(R.drawable.fab_anim_add_to_search)
            if (fabAddSearch.drawable is Animatable) (fabAddSearch.drawable as Animatable).start()
        }
        // Hide search item
        searchMenuItem?.isVisible = false
    }

    private fun switchToOwnSensorsPage() { // page index 2
        // Show and animate the fab
        if(!fabAddSearch.isOrWillBeShown) fabAddSearch.show()
        fabAddSearch.setImageResource(R.drawable.fab_anim_search_to_add)
        if (fabAddSearch.drawable is Animatable) (fabAddSearch.drawable as Animatable).start()
        // Show search item
        searchMenuItem?.isVisible = true
    }

    private fun switchToLocalNetworkPage() { // page index 3
        // Show and animate the fab
        if(!fabAddSearch.isOrWillBeShown) fabAddSearch.show()
        if(selectedPage == 0 || selectedPage == 2) {
            fabAddSearch.setImageResource(R.drawable.fab_anim_add_to_search)
            if (fabAddSearch.drawable is Animatable) (fabAddSearch.drawable as Animatable).start()
        }
        // Show search item
        searchMenuItem?.isVisible = true
    }

    private fun refresh() {
        // Refresh all pages

    }

    private fun openQRScanner() {
        IntentIntegrator(this).run {
            setRequestCode(Constants.REQ_SCAN_WEB)
            setOrientationLocked(true)
            setBeepEnabled(false)
            setPrompt(getString(R.string.scan_prompt))
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            initiateScan()
        }
    }

    private fun openFAQPage() {
        Intent(Intent.ACTION_VIEW).run {
            data = Uri.parse(getString(R.string.url_faq))
            startActivity(this)
        }
    }

    private fun initializeWebConnection(resultCode: Int, data: Intent?) {
        try{
            // Extract SyncKey out of QR-Code
            val syncKey = IntentIntegrator.parseActivityResult(resultCode, data).contents
            // Check key for validity
            if(syncKey.length == 25 && !syncKey.startsWith("http")) {
                // Start WebSyncService
                availableSoon()
                // Display message, that the connection is established
                Toast(this).run {
                    setGravity(Gravity.CENTER, 0, 0)
                    duration = Toast.LENGTH_LONG
                    view = LayoutInflater.from(this@MainActivity).inflate(R.layout.sync_success, null)
                    show()
                }
            }
        } catch (e: Exception) {
            outputErrorMessage()
        }
    }

    private fun openPlacesSearch() {
        PlacesSearchDialog(this@MainActivity, this@MainActivity).run {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            if (isNightModeEnabled()) {
                (searchEditText.parent as View).setBackgroundColor(ContextCompat.getColor(context, R.color.blackLight))
                recyclerFrame.setBackgroundColor(ContextCompat.getColor(context, R.color.blackLight))
            }
            show()
        }
    }

    private fun openAddSensorActivity() {
        val intent = Intent(this, AddSensorActivity::class.java)
        openActivityWithRevealAnimation(this, fabAddSearch, revealSheet, intent, Constants.REQ_ADD_SENSOR)
    }

    private fun startLocalNetworkSearch() {
        // Setup searching task
        searchTask = SensorIPSearchTask(this, object: SensorIPSearchTask.OnSearchEventListener {
            override fun onProgressUpdate(progress: Int) {

            }

            override fun onSensorFound(sensor: ScrapingResult?) {}

            override fun onSearchFinished(sensorList: ArrayList<ScrapingResult>) {

            }

            override fun onSearchFailed() {

            }
        }, 0)
        searchTask.execute()
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        // Show/hide toolbar, tabBar, etc.
        if(isFullscreen) enterFullscreen() else exitFullscreen()
        // Enter/exit fullscreen mode
        setFullscreenMode(window, isFullscreen)
    }

    private fun enterFullscreen() {
        val statusBarHeight =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                0
            else
                getStatusBarHeight(this)
        val navigationBarHeight =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> 0
                exitedFullscreenOnce -> getNavigationBarHeight(this)
                else -> getNavigationBarHeight(this) * 2
            }
        toolbar.animate()
            .translationY((-toolbar.measuredHeight).toFloat())
            .setDuration(500L)
            .start()
        viewPager.animate()
            .translationY((-toolbar.measuredHeight).toFloat())
            .setDuration(500L)
            .start()
        val possibleOffset =
            if (exitedFullscreenOnce)
                0
            else
                statusBarHeight
        val layoutParams = container.layoutParams as FrameLayout.LayoutParams
        val to = container.measuredHeight + tabBar.measuredHeight + toolbar.measuredHeight + possibleOffset + navigationBarHeight
        ValueAnimator.ofInt(container.measuredHeight, to).run {
            duration = 500L
            addUpdateListener { animation ->
                layoutParams.height = animation.animatedValue as Int
                container.layoutParams = layoutParams
            }
            start()
        }
        fabAddSearch.hide()
    }

    private fun exitFullscreen() {
        val navigationBarHeight =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> 0
                exitedFullscreenOnce -> getNavigationBarHeight(this)
                else -> 0
            }
        toolbar.animate()
            .translationY(0f)
            .setDuration(250L)
            .start()
        toolbar.setPadding(0, getStatusBarHeight(this), 0, 0)
        viewPager.animate()
            .translationY(0f)
            .setDuration(250L)
            .start()
        val layoutParams = container.layoutParams as FrameLayout.LayoutParams
        val to = container.measuredHeight - tabBar.measuredHeight - toolbar.measuredHeight - navigationBarHeight
        ValueAnimator.ofInt(container.measuredHeight, to).run {
            duration = 250L
            addUpdateListener { animation ->
                layoutParams.height = animation.animatedValue as Int
                container.layoutParams = layoutParams
            }
            start()
        }
        fabAddSearch.show()
        exitedFullscreenOnce = true
    }

    override fun onPlaceSelected(place: Place) {
        place.latLng?.let { viewpagerAdapter.allSensorsFragment.applyPlaceSearch(it) }
    }
}