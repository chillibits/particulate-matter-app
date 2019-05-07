package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

public class ClusterRederer extends DefaultClusterRenderer<SensorClusterItem> {

    //Konstanten

    //Variablen als Objekte
    private StorageUtils su;

    //Variablen

    public ClusterRederer(Context context, GoogleMap map, ClusterManager<SensorClusterItem> clusterManager, StorageUtils su) {
        super(context, map, clusterManager);
        this.su = su;
    }

    @Override
    protected void onBeforeClusterItemRendered(SensorClusterItem item, MarkerOptions markerOptions) {
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(su.isFavouriteExisting(item.getTitle()) ? BitmapDescriptorFactory.HUE_RED : su.isSensorExisting(item.getTitle()) ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_BLUE));
    }
}
