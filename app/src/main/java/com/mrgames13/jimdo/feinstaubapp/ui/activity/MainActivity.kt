/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.activity

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
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
import androidx.core.view.marginTop
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.viewpager2.widget.ViewPager2
import com.chillibits.simplesettings.clicklistener.LibsClickListener
import com.chillibits.simplesettings.clicklistener.PlayStoreClickListener
import com.chillibits.simplesettings.clicklistener.WebsiteClickListener
import com.chillibits.simplesettings.core.SimpleSettings
import com.chillibits.simplesettings.core.SimpleSettingsConfig
import com.fxn.OnBubbleClickListener
import com.google.android.libraries.places.api.model.Place
import com.google.zxing.integration.android.IntentIntegrator
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.databinding.ActivityMainBinding
import com.mrgames13.jimdo.feinstaubapp.model.dbo.ScrapingResultDbo
import com.mrgames13.jimdo.feinstaubapp.shared.*
import com.mrgames13.jimdo.feinstaubapp.task.SensorIPSearchTask
import com.mrgames13.jimdo.feinstaubapp.ui.adapter.viewpager.ViewPagerAdapterMain
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.*
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.AllSensorsFragment
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.FavoritesFragment
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.LocalNetworkFragment
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.OwnSensorsFragment
import com.mrgames13.jimdo.feinstaubapp.ui.view.closeActivityWithRevealAnimation
import com.mrgames13.jimdo.feinstaubapp.ui.view.openActivityWithRevealAnimation
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.place_search_dialog.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.view_main_sidebar.*

class MainActivity : AppCompatActivity(), AllSensorsFragment.OnAdapterEventListener, PlacesSearchDialog.PlaceSelectedCallback,
    LocalNetworkFragment.LocalSearchListener, SimpleSettingsConfig.PreferenceCallback {

    // Constants
    private var isTablet = false

    // Variables as objects
    private var searchMenuItem: MenuItem? = null
    private lateinit var searchTask: SensorIPSearchTask
    private lateinit var viewpagerAdapter: ViewPagerAdapterMain
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    // Fragments for tablets
    private lateinit var fragmentFavorites: FavoritesFragment
    private lateinit var fragmentOwnSensors: OwnSensorsFragment
    lateinit var fragmentLocalNetwork: LocalNetworkFragment

    // Variables
    private var pressedOnce = false
    private var isFullscreen = false
    private var exitedFullscreenOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isTablet = resources.getBoolean(R.bool.isTablet)

        // Initialize data binding and view model
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(MainViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Apply window insets
        applyWindowInsets()

        // Initialize toolbar
        setSupportActionBar(toolbar)

        // Initialize components, depending on screen size
        if(isTablet) initializeTabletComponents() else initializePhoneComponents()
    }

    private fun initializePhoneComponents() {
        // Initialize ViewPager
        viewpagerAdapter =
            ViewPagerAdapterMain(application, this, this, supportFragmentManager, lifecycle)
        viewPager.run {
            offscreenPageLimit = viewpagerAdapter.itemCount
            isUserInputEnabled = false
            adapter = viewpagerAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    // Close searchView
                    if (searchView.isSearchOpen) searchView.closeSearch()

                    // perform actions, depending on selected page
                    when (pos) {
                        0 -> switchToFavoritesPage()
                        1 -> switchToAllSensorsPage()
                        2 -> switchToOwnSensorsPage()
                        3 -> switchToLocalNetworkPage()
                    }
                    viewModel.selectedPage.postValue(pos)
                    tabBar.setSelected(pos)
                }
            })
            setCurrentItem(viewModel.selectedPage.value!!, false) // Start on the map
        }

        // Initialize BubbleTabBar
        tabBar.addBubbleListener(object : OnBubbleClickListener {
            override fun onBubbleClick(id: Int) {
                // Tell viewPager which page to show
                viewPager.currentItem = when (id) {
                    R.id.item_favorites -> 0
                    R.id.item_all_sensors -> 1
                    R.id.item_own_sensors -> 2
                    R.id.item_local_network -> 3
                    else -> viewModel.selectedPage.value!!
                }
            }
        })

        // Initialize AddSearchFab
        fabAddSearch.fab.setOnClickListener {
            when (viewModel.selectedPage.value) {
                1 -> openPlacesSearch(container)
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

    private fun initializeTabletComponents() {
        // Initialize AddSearchFab
        fabAddSearch.fab.setOnClickListener {
            when (viewModel.selectedPage.value) {
                2 -> openAddSensorActivity()
                3 -> startLocalNetworkSearch()
            }
        }

        // Initialize all fragments
        fragmentFavorites = FavoritesFragment()
        fragmentOwnSensors = OwnSensorsFragment()
        fragmentLocalNetwork = LocalNetworkFragment(application, this)

        // Apply page from ViewModel
        when (viewModel.selectedPage.value) {
            0, 1 -> showPage(menuItemFavorites)
            2 -> showPage(menuItemOwnSensors)
            3 -> showPage(menuItemLocalNetwork)
        }
    }

    private fun applyWindowInsets() = window.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decorView.setOnApplyWindowInsetsListener { _, insets ->
                val systemBarInsets = insets.getInsets(WindowInsets.Type.systemBars())
                toolbar.setPadding(0, systemBarInsets.top, 0, 0)
                if(isTablet) {
                    viewContainer.setPadding(0, 0, 0, systemBarInsets.bottom)
                } else {
                    tabBar.setPadding(0, 0, 0, systemBarInsets.bottom)
                }
                insets
            }
            setDecorFitsSystemWindows(false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            decorView.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                if(isTablet) {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    viewContainer.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                } else {
                    decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    tabBar.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                }
                insets
            }
        }
    }

    private fun loadClientPropertiesFromServer() {

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        if(!isTablet) {
            searchMenuItem = menu?.findItem(R.id.action_search)
            searchView.setMenuItem(searchMenuItem)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_search -> item.expandActionView()
            R.id.action_import_export -> showImportExportDialog()
            R.id.action_account -> signInOrAccountDetails()
            R.id.action_settings -> openSettings(container)
            R.id.action_rate -> showRatingDialog()
            R.id.action_recommend -> showRecommendationDialog()
            R.id.action_web -> openQRScanner()
            R.id.action_help -> openFAQPage()
            R.id.action_quit -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.REQ_ADD_SENSOR -> {
                try {
                    closeActivityWithRevealAnimation(this, fabAddSearch.fab, revealSheet)
                } catch (e: IllegalStateException) {}
            }
            Constants.REQ_SCAN_WEB -> {
                if(resultCode == Activity.RESULT_OK) initializeWebConnection(resultCode, data)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(isFullscreen) {
                onToggleFullscreen()
            } else if(!isTablet && searchView.isSearchOpen) {
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

    private fun signInOrAccountDetails() {
        if(viewModel.users.value.isNullOrEmpty()) {
            // Not signed in
            SignInDialog(this, viewModel).show()
        } else {
            // Already signed in
            showAccountDialog(viewModel.users.value?.maxByOrNull { it -> it.lastSignIn }!!)
        }
    }

    private fun switchToFavoritesPage() { // page index 0
        // Hide the fab
        if(fabAddSearch.fab.isOrWillBeShown) fabAddSearch.fab.hide()
        // Show search item
        searchMenuItem?.isVisible = true
    }

    private fun switchToAllSensorsPage() { // page index 1
        // Show and animate the fab
        if(!fabAddSearch.fab.isOrWillBeShown) fabAddSearch.fab.show()
        if(viewModel.selectedPage.value == 0 || viewModel.selectedPage.value == 2) {
            fabAddSearch.fab.setImageResource(R.drawable.fab_anim_add_to_search)
            if (fabAddSearch.fab.drawable is Animatable) (fabAddSearch.fab.drawable as Animatable).start()
        }
        // Hide search item
        searchMenuItem?.isVisible = false
    }

    private fun switchToOwnSensorsPage() { // page index 2
        // Show and animate the fab
        if(!fabAddSearch.fab.isOrWillBeShown) fabAddSearch.fab.show()
        fabAddSearch.fab.setImageResource(R.drawable.fab_anim_search_to_add)
        if (fabAddSearch.fab.drawable is Animatable) (fabAddSearch.fab.drawable as Animatable).start()
        // Show search item
        searchMenuItem?.isVisible = true
    }

    private fun switchToLocalNetworkPage() { // page index 3
        // Show and animate the fab
        if(!fabAddSearch.fab.isOrWillBeShown) fabAddSearch.fab.show()
        if(viewModel.selectedPage.value == 0 || viewModel.selectedPage.value == 2) {
            fabAddSearch.fab.setImageResource(R.drawable.fab_anim_add_to_search)
            if (fabAddSearch.fab.drawable is Animatable) (fabAddSearch.fab.drawable as Animatable).start()
        }
        // Show search item
        searchMenuItem?.isVisible = true
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
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(getString(R.string.url_faq))
        })
    }

    fun openSettings(view: View) {
        val config = SimpleSettingsConfig().apply {
            showResetOption = true
            preferenceCallback = this@MainActivity
        }
        SimpleSettings(this, config).show(R.xml.pref)
    }

    override fun onPreferenceClick(context: Context, key: String): Preference.OnPreferenceClickListener? {
        return when(key) {
            "clearSensorData" -> Preference.OnPreferenceClickListener {
                context.showClearSensorDataDialog()
                true
            }
            "openSource" -> WebsiteClickListener(this, getString(R.string.url_github))
            "openSourceLicenses" -> LibsClickListener(this)
            "appVersion" -> PlayStoreClickListener(this)
            "developers" -> WebsiteClickListener(this, getString(R.string.url_homepage))
            "moreApps" -> WebsiteClickListener(this, getString(R.string.url_store_developer_site))
            else -> super.onPreferenceClick(context, key)
        }
    }

    private fun initializeWebConnection(resultCode: Int, data: Intent?) {
        try {
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

    fun openPlacesSearch(view: View) {
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

    fun showPage(view: View) {
        when(view.id) {
            R.id.menuItemFavorites -> {
                supportFragmentManager.beginTransaction().replace(R.id.mainLeftFragment, fragmentFavorites).commit()
                switchToFavoritesPage()
                viewModel.selectedPage.postValue(0)
                moveShifter(0)
            }
            R.id.menuItemOwnSensors -> {
                supportFragmentManager.beginTransaction().replace(R.id.mainLeftFragment, fragmentOwnSensors).commit()
                switchToOwnSensorsPage()
                viewModel.selectedPage.postValue(2)
                moveShifter(1)
            }
            R.id.menuItemLocalNetwork -> {
                supportFragmentManager.beginTransaction().replace(R.id.mainLeftFragment, fragmentLocalNetwork).commit()
                switchToLocalNetworkPage()
                viewModel.selectedPage.postValue(3)
                moveShifter(2)
            }
        }
    }

    private fun moveShifter(pos: Int) {
        val itemHeight = menuItemFavorites.measuredHeight
        ValueAnimator.ofInt(shifter.marginTop, dpToPx(10) + pos * (itemHeight + dpToPx(17))).apply {
            duration = 300
            addUpdateListener { valueAnimator ->
                println(valueAnimator.animatedValue)
                val params = shifter.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = valueAnimator.animatedValue as Int
                shifter.layoutParams = params
            }
        }.start()
    }

    private fun openAddSensorActivity() {
        val intent = Intent(this, AddSensorActivity::class.java)
        openActivityWithRevealAnimation(
            this,
            fabAddSearch.fab,
            revealSheet,
            intent,
            Constants.REQ_ADD_SENSOR
        )
    }

    private fun startLocalNetworkSearch() {
        // Setup searching task
        searchTask = SensorIPSearchTask(
            this,
            object : SensorIPSearchTask.OnSearchEventListener {
                override fun onProgressUpdate(progress: Int) {
                    if(isTablet)
                        fragmentLocalNetwork.updateSearchProgress(progress)
                    else
                        viewpagerAdapter.localNetworkFragment.updateSearchProgress(progress)
                    fabAddSearch.setCurrentProgress(if(progress < 100) progress else 0, false)
                }

                override fun onSensorFound(result: ScrapingResultDbo?) {
                    if(result != null) viewModel.addScrapingResult(result)
                }

                override fun onSearchFinished(results: ArrayList<ScrapingResultDbo>) { finishLocalNetworkSearch() }
            },
            0
        )
        searchTask.execute()
        // Show searching screen
        if(isTablet)
            fragmentLocalNetwork.showSearchingScreen()
        else
            viewpagerAdapter.localNetworkFragment.showSearchingScreen()
        fabAddSearch.fab.isEnabled = false
    }

    private fun finishLocalNetworkSearch() {
        fabAddSearch.fab.isEnabled = true
        fabAddSearch.setIcon(getDrawable(R.drawable.done_white))
        if(isTablet)
            fragmentLocalNetwork.hideSearchingScreen()
        else
            viewpagerAdapter.localNetworkFragment.hideSearchingScreen()

        Handler().postDelayed({
            try {
                fabAddSearch.setIcon(getDrawable(R.drawable.search_white))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 1500)
    }

    override fun onToggleFullscreen() {
        isFullscreen = !isFullscreen
        // Show/hide toolbar, tabBar, etc.
        if(isFullscreen) enterFullscreen() else exitFullscreen()
        // Enter/exit fullscreen mode
        setFullscreenMode(window, isFullscreen)
    }

    private fun enterFullscreen() {
        val statusBarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) 0 else getStatusBarHeight(this)
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
        val possibleOffset = if (exitedFullscreenOnce) 0 else statusBarHeight
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
        fabAddSearch.fab.hide()
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
        fabAddSearch.fab.show()
        exitedFullscreenOnce = true
    }

    override fun onPlaceSelected(place: Place) {
        place.latLng?.let {
            if(isTablet) {

            } else
                viewpagerAdapter.allSensorsFragment.applyPlaceSearch(it)
        }
    }

    override fun onRefreshLocalSensors() = startLocalNetworkSearch()
}