package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterItem;

public class SensorClusterItem implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final Marker marker;

    public SensorClusterItem(double lat, double lng, String title, String snippet, Marker marker) {
        this.position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
        this.marker = marker;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    public Marker getMarker() {
        return marker;
    }
}
