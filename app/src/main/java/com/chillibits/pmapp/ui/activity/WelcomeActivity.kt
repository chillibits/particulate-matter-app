/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import com.mrgames13.jimdo.feinstaubapp.R
import com.stephentuso.welcome.BasicPage
import com.stephentuso.welcome.TitlePage
import com.stephentuso.welcome.WelcomeActivity
import com.stephentuso.welcome.WelcomeConfiguration

class WelcomeActivity : WelcomeActivity() {

    override fun configuration(): WelcomeConfiguration {
        return WelcomeConfiguration.Builder(this)
			.bottomLayout(WelcomeConfiguration.BottomLayout.BUTTON_BAR)
            .defaultBackgroundColor(R.color.colorPrimary)
			.page(
				TitlePage(R.drawable.app_icon, getString(R.string.app_name))
			)
			.page(
				BasicPage(R.drawable.star_border, "Header", "More text.")
					.background(R.color.colorAccent)
			)
			.page(
				BasicPage(R.drawable.search_white, "Lorem ipsum", "dolor sit amet.")
			)
			.canSkip(true)
			.backButtonSkips(true)
            .build()
    }
}
