package com.mmlab.n1.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mmlab.n1.InformationActivity;
import com.mmlab.n1.R;
import com.mmlab.n1.info.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MemberMapFragment extends Fragment {

    private static final String TAG = "MemberMapFragment";
    private MapView mapView;
    private GoogleMap map;
    private View layout_member;
    private Marker pMarker = null;

    private boolean isStart = false;

    private static List<Profile> mMembers = new ArrayList<Profile>();

    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d(TAG, "setUserVisibleHint()..." + isVisibleToUser);
       updateView(isVisibleToUser);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        layout_member = inflater.inflate(R.layout.fragment_member_map, container, false);

        mapView = (MapView) layout_member.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();// needed to get the map to display immediately

        MapsInitializer.initialize(getActivity());

        map = mapView.getMap();

        isStart = true;
        return layout_member;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateView(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public void updateView(boolean isVisibleToUser){
        if (isStart && isVisibleToUser) {
            InformationActivity activity = (InformationActivity) getActivity();
            if (activity.mServer != null) {
                updateProxy(activity.mServer.location_proxy);
                updateMembers(activity.mServer.getProfiles());
            }
        }
    }

    public void updateMembers(ConcurrentHashMap<String, Profile> hashMap) {
        List<Profile> list = new ArrayList<Profile>(hashMap.values());
        mMembers.clear();
        mMembers.addAll(list);
        map.clear();

        for (Profile p : mMembers) {
            if (Math.abs(p.latitude) < 1 && Math.abs(p.longitude) < 1) continue;
            LatLng latLng = new LatLng(p.latitude, p.longitude);
            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            options.title(p.FB_NAME);
            options.snippet("成員");
            map.addMarker(options);
        }
        Log.d(TAG, "updateMembers : " + mMembers.size());

        if (pMarker != null) {
            pMarker = map.addMarker(new MarkerOptions().position(pMarker.getPosition()).title(pMarker.getTitle()).snippet(pMarker.getSnippet()));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pMarker.getPosition(), 16));
        }
    }

    public void updateProxy(String location) {
        Log.d(TAG, "updateProxy()...");
        String[] splited = location.split("\\s+");
        if (splited.length < 2) return;
        double lat = Double.parseDouble(splited[0]);
        double lon = Double.parseDouble(splited[1]);
        if (Math.abs(lat) < 1 && Math.abs(lon) < 1) return;
        if (pMarker != null) pMarker.remove();
        LatLng latLng = new LatLng(lat, lon);
        MarkerOptions options = new MarkerOptions();
        options.position(latLng);
        options.title("導覽員");
        options.snippet("這是導覽員");
        pMarker = map.addMarker(options);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
    }
}
