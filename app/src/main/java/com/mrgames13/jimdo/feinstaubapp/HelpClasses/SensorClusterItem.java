package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class SensorClusterItem implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;

    public SensorClusterItem(double lat, double lng, String title, String snippet) {
        this.position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
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
}
