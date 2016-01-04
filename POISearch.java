package com.mmlab.n1;

/**
 * Created by mmlab on 2015/9/23.
 */

import android.app.Service;
import android.os.AsyncTask;
import android.util.Log;


import com.mmlab.n1.info.POI;
import com.mmlab.n1.network.ProxyService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Using AsyncTask to avoid UI thread have been block when sending a httpRequest
 */
public class POISearch extends
        AsyncTask<Void, Void, Void> {

    private static final String TAG = "POISearch";

    /**
     * Download POI attributes
     */
    private static final String API_GET_NEARBY_POI = "http://deh.csie.ncku.edu.tw/dehencode/json/nearbyPOIs";
    private String latitude = "22.9972479";
    private String longitude = "120.2186137";
    private WeakReference<Service> weakReference = null;

    public POISearch(Service service) {
        weakReference = new WeakReference<Service>(service);
    }

    @Override
    protected Void doInBackground(Void... params) {

        ArrayList<POI> list = new ArrayList<POI>();
        ArrayList<Package> packages = new ArrayList<Package>();

        try {
            String result = sentHttpRequest(API_GET_NEARBY_POI + "?"
                    + "lat=" + latitude + "&lng=" + longitude
                    + "&dist=2000");

            list = Utils.parsePoisJSONObject(result);
            Log.d(TAG, "POI number: " + list.size());

            if (weakReference.get() != null) {
                ((ProxyService) weakReference.get()).setPoiList(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String sentHttpRequest(String url) throws Exception {
        Log.i(TAG, "sentHttpRequest: " + url);

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String result = "";

        try {
            // 建立連線
            URL link = new URL(url);
            connection = (HttpURLConnection) link.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 讀取資料
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer sb = new StringBuffer("");
                String line = "";
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                result = sb.toString();
            }
        } catch (Exception e) {
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        Log.i(TAG, "received: " + result);
        return result;
    }
}

