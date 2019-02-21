package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class SensorClusterItem implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final MarkerItem marker;

    public SensorClusterItem(double lat, double lng, String title, String snippet, MarkerItem marker) {
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

    public MarkerItem getMarker() {
        return marker;
    }
}
