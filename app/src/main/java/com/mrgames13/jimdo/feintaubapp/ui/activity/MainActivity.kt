/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.activity

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.fxn.OnBubbleClickListener
import com.google.zxing.integration.android.IntentIntegrator
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.ui.adapter.viewpager.ViewPagerAdapterMain
import com.mrgames13.jimdo.feintaubapp.ui.dialogs.showImportExportDialog
import com.mrgames13.jimdo.feintaubapp.ui.dialogs.showRatingDialog
import com.mrgames13.jimdo.feintaubapp.ui.dialogs.showRecommendationDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : AppCompatActivity() {

    // Variables as objects
    private var searchMenuItem: MenuItem? = null

    // Variables
    private var previousPage = 1

    companion object {
        // Constants
        const val REQ_SCAN_WEB = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize toolbar
        toolbar.setTitle(R.string.app_name)
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
        viewPager.offscreenPageLimit = 4
        viewPager.adapter = ViewPagerAdapterMain(supportFragmentManager)
        viewPager.currentItem = 1 // Start on the map
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
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
                previousPage = pos
            }
        })

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
        tabBar.setupBubbleTabBar(viewPager)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        searchMenuItem = menu?.findItem(R.id.action_search)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_search -> item.expandActionView()
            R.id.action_import_export -> showImportExportDialog()
            R.id.action_rate -> showRatingDialog()
            R.id.action_settings -> startActivity(Intent())
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
                REQ_SCAN_WEB -> initializeWebConnection(resultCode, data)
            }
        } else outputErrorMessage()
    }

    private fun switchToFavoritesPage() {
        // Hide the fab
        if(fabAddSearch.isOrWillBeShown) fabAddSearch.hide()
    }

    private fun switchToAllSensorsPage() {
        // Show and animate the fab
        if(!fabAddSearch.isOrWillBeShown) fabAddSearch.show()
        if(previousPage == 0 || previousPage == 2) {
            fabAddSearch.setImageResource(R.drawable.fab_anim_add_to_search)
            if (fabAddSearch.drawable is Animatable) (fabAddSearch.drawable as Animatable).start()
        }
    }

    private fun switchToOwnSensorsPage() {
        // Show and animate the fab
        if(!fabAddSearch.isOrWillBeShown) fabAddSearch.show()
        if(previousPage != 2) {
            fabAddSearch.setImageResource(R.drawable.fab_anim_search_to_add)
            if (fabAddSearch.drawable is Animatable) (fabAddSearch.drawable as Animatable).start()
        }
    }

    private fun switchToLocalNetworkPage() {
        // Same actions, so we can execute this method
        switchToAllSensorsPage()
    }

    private fun openQRScanner() {
        IntentIntegrator(this).run {
            setRequestCode(REQ_SCAN_WEB)
            setOrientationLocked(true)
            setBeepEnabled(false)
            setPrompt(getString(R.string.scan_prompt))
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            initiateScan()
        }
    }

    private fun openFAQPage() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(getString(R.string.faq_url))
        startActivity(i)
    }

    private fun initializeWebConnection(resultCode: Int, data: Intent?) {
        try{
            // Extract SyncKey out of QR-Code
            val syncKey = IntentIntegrator.parseActivityResult(resultCode, data).contents
            // Check key for validity
            if(syncKey.length == 25 && !syncKey.startsWith("http")) {
                // Start WebSyncService

                // Display message, that the connection is established
                Toast(this).run {
                    setGravity(Gravity.CENTER, 0, 0)
                    duration = Toast.LENGTH_LONG
                    view = layoutInflater.inflate(R.layout.sync_success, null)
                    show()
                }
            }
        } catch (e: Exception) {
            outputErrorMessage()
        }
    }

    private fun outputErrorMessage() {
        Toast.makeText(this, R.string.error_try_again, Toast.LENGTH_SHORT).show()
    }
}
