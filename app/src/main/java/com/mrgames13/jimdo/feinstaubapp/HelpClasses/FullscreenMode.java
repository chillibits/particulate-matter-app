/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.Window;

import com.mrgames13.jimdo.feinstaubapp.R;


public class FullscreenMode {
	public static void setFullscreenMode(final Window window, boolean fullscreen) {
		if(fullscreen) {
			final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

			int currentApiVersion = Build.VERSION.SDK_INT;
			if(currentApiVersion >= Build.VERSION_CODES.KITKAT) window.getDecorView().setSystemUiVisibility(flags);
		} else {
            final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

			window.getDecorView().setSystemUiVisibility(flags);
		}
        setTranslucentStatusBar(window);
	}

    public static void setTranslucentStatusBar(Window window) {
        if (window == null) return;
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= Build.VERSION_CODES.LOLLIPOP) {
            setTranslucentStatusBarLollipop(window);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void setTranslucentStatusBarLollipop(Window window) {
        window.setStatusBarColor(window.getContext().getResources().getColor(R.color.bg_dark));
    }
}