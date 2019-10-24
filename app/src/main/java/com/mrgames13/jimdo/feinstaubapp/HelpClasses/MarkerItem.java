/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import com.google.android.gms.maps.model.LatLng;

public class MarkerItem {

    // Variables as objects
    private LatLng position;

    // Variables
    private String title;
    private String snippet;
    private String tag;

    public MarkerItem(String title, String snippet, LatLng position) {
        this.title = title;
        this.snippet = snippet;
        this.position = position;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSnippet() {
        return snippet;
    }

    public LatLng getPosition() {
        return position;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
