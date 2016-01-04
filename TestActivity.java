package com.mmlab.n1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TestActivity extends AppCompatActivity {

    Button button_wifi_enable, button_wifi_disable, button_3G_enable, button_3G_disable, button_ap_enable, button_ap_disable;

    View layout_text;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        layout_text = (View) findViewById(R.id.text);

        button_wifi_enable = (Button) findViewById(R.id.button_wifi_enable);
        button_wifi_disable = (Button) findViewById(R.id.button_wifi_disable);
        button_3G_enable = (Button) findViewById(R.id.button_3G_enable);
        button_3G_disable = (Button) findViewById(R.id.button_3G_disable);
        button_ap_enable = (Button) findViewById(R.id.button_ap_enable);
        button_ap_disable = (Button) findViewById(R.id.button_ap_disable);

        button_wifi_enable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setWiFiEnabled(getApplicationContext(), true);
            }
        });

        button_wifi_disable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setWiFiEnabled(getApplicationContext(), false);
            }
        });

        button_3G_enable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setMobileDataEnabledMethod1(getApplicationContext(), true);
            }
        });

        button_3G_disable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setMobileDataEnabledMethod2(getApplicationContext(), false);
            }
        });

        button_ap_enable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setAPEnabledMethod(getApplicationContext(), true);
            }
        });

        button_ap_disable.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setAPEnabledMethod(getApplicationContext(), false);
            }
        });
    }

    protected void onStart() {
        super.onStart();
        IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filters.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(mReceiver, filters);
    }

    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setMobileDataEnabledMethod1(final Context context, boolean enabled) {
        final ConnectivityManager conman =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(
                    iConnectivityManager.getClass().getName());

            final Method setMobileDataEnabledMethod = iConnectivityManagerClass
                    .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);

            if (enabled) {
                Snackbar snackbar = Snackbar
                        .make(layout_text, "Mobile data enabled!", Snackbar.LENGTH_LONG)
                        .setAction("DISABLE", new View.OnClickListener() {
                            public void onClick(View view) {
                                setMobileDataEnabledMethod1(context, false);
                            }
                        });

                // Changing message text color
                snackbar.setActionTextColor(Color.RED);

                // Changing action button text color
                View sbView = snackbar.getView();
                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(Color.YELLOW);

                snackbar.show();
            }
        } catch (Exception e) {
            setMobileDataEnabledMethod2(context, enabled);
            e.printStackTrace();
        }
    }

    private void setMobileDataEnabledMethod2(final Context context, boolean enabled) {

        Class[] cArg = new Class[2];
        cArg[0] = String.class;
        cArg[1] = Boolean.TYPE;
        final ConnectivityManager conman =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(
                    iConnectivityManager.getClass().getName());

            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", cArg);
            Object[] pArg = new Object[2];
            pArg[0] = context.getPackageName();
            pArg[1] = enabled;
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, pArg);

            if (enabled) {
                Snackbar snackbar = Snackbar
                        .make(layout_text, "Mobile data enabled!", Snackbar.LENGTH_LONG)
                        .setAction("DISABLE", new View.OnClickListener() {
                            public void onClick(View view) {
                                setMobileDataEnabledMethod1(context, false);
                            }
                        });

                // Changing message text color
                snackbar.setActionTextColor(Color.RED);

                // Changing action button text color
                View sbView = snackbar.getView();
                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(Color.YELLOW);

                snackbar.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isWiFiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    private void setWiFiEnabled(Context context, boolean enabled) {
        if (enabled)
            setAPEnabledMethod(context, !enabled);
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(enabled);
    }

    public static boolean isAPEnabled(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        } catch (Throwable ignored) {
        }
        return false;
    }

    // toggle wifi hotspot on or off
    public static boolean setAPEnabledMethod(Context context, boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfiguration = null;
        try {
            // if WiFi is on, turn it off
            if (enabled) {
                wifiManager.setWifiEnabled(false);
            }
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, wifiConfiguration, enabled);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                if (isAPEnabled(context)) {
                    Snackbar snackbar = Snackbar
                            .make(layout_text, "AP enabled!", Snackbar.LENGTH_LONG)
                            .setAction("DISABLE", new View.OnClickListener() {
                                public void onClick(View view) {
                                    setAPEnabledMethod(context, false);
                                }
                            });

                    // Changing message text color
                    snackbar.setActionTextColor(Color.RED);

                    // Changing action button text color
                    View sbView = snackbar.getView();
                    TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setTextColor(Color.YELLOW);

                    snackbar.show();
                }
            } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                if (isWiFiEnabled(context)) {
                    Snackbar snackbar = Snackbar
                            .make(layout_text, "WiFi enabled!", Snackbar.LENGTH_LONG)
                            .setAction("DISABLE", new View.OnClickListener() {
                                public void onClick(View view) {
                                    setWiFiEnabled(context, false);
                                }
                            });

                    // Changing message text color
                    snackbar.setActionTextColor(Color.RED);

                    // Changing action button text color
                    View sbView = snackbar.getView();
                    TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setTextColor(Color.YELLOW);

                    snackbar.show();
                }
            } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {

            }
        }
    };
}
