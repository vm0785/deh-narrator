package com.mmlab.n1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

/**
 * Created by mmlab on 2015/12/9.
 */
public class GPSTracker implements LocationListener {

    private onUpdateListener mCallback = null;
    private LocationManager locationManager = null;
    private Context mContext = null;
    private String bestProvider = LocationManager.NETWORK_PROVIDER;

    public GPSTracker(Context context) {
        mContext = context;
        init();
    }

    public interface onUpdateListener {
        void onLocationChanged(Location location);

        void onProviderDisabled(String provider);

        void onProviderEnabled(String provider);

        void onStatusChanged(String provider, int status, Bundle extras);

    }

    @SuppressLint("NewApi")
    public void init() {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
            if (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        locationManager.requestLocationUpdates(bestProvider, 2000, 1, this);
    }

    /**
     * 檢查定位是否開啟
     *
     * @return
     */
    public boolean isProviderEnable() {
        return !(locationManager == null || !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }

    public void setOnUpdateListener(onUpdateListener callback) {
        mCallback = callback;
    }

    public void onLocationChanged(Location location) {
        mCallback.onLocationChanged(location);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        mCallback.onStatusChanged(provider, status, extras);
    }

    public void onProviderEnabled(String provider) {
        mCallback.onProviderEnabled(provider);
    }

    public void onProviderDisabled(String provider) {
        mCallback.onProviderDisabled(provider);
    }

    /**
     * lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)
     * lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)
     * unit = the unit you desire for results
     * where: 'M' is statute miles (default)
     * 'K' is kilometers
     * 'N' is nautical miles
     */
    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    /**
     * This function converts decimal degrees to radians
     *
     * @param deg
     * @return
     */
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /**
     * This function converts radians to decimal degrees
     */
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}
