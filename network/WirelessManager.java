package com.mmlab.n1.network;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;


import com.mmlab.n1.constant.ENCRYPT;
import com.mmlab.n1.info.Group;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mmlab on 2015/9/17.
 */

public class WirelessManager extends NetworkManager {
    /*
     * This class provides a WiFi interface plus AP functionality.
     * Note: enabling/disabled WiFi or AP is blocking which means the functions
     * will wait TIMEOUT seconds until the desired state is in effect.
     * Because we are doing these things in a background thread, it's the
     * most convenient way for the application.
     */
    final static String TAG = "WifiExtendedManager";

    final static int TIMEOUT = 30;

    private WifiManager wifiManager;
    private static final Map<String, Method> methodMap = new HashMap<String, Method>();
    boolean isHtc = false;

    public WirelessManager(Context context) {
        super(context);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        // 檢查是否是HTC裝置
        try {
            Field field = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
            isHtc = field != null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Method method = WifiManager.class.getMethod("getWifiApState");
            methodMap.put("getWifiApState", method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        try {
            Method method = WifiManager.class.getMethod("getWifiApConfiguration");
            methodMap.put("getWifiApConfiguration", method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        try {
            Method method = WifiManager.class.getMethod(getSetWifiApConfigName(), WifiConfiguration.class);
            methodMap.put("setWifiApConfiguration", method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        try {
            Method method = WifiManager.class.getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            methodMap.put("setWifiApEnabled", method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取得當前所連熱點的local address
     * Reference from
     * http://stackoverflow.com/questions/23396582/get-local-addressserver-of-wifi-hotspot-after-getting-connected-to-it
     *
     * @return
     */
    public String getServerIpAddress() {
        int serverAddress = wifiManager.getDhcpInfo().serverAddress;
        return (serverAddress & 0xFF) + "." + ((serverAddress >> 8) & 0xFF) + "."
                + ((serverAddress >> 16) & 0xFF) + "." + ((serverAddress >> 24) & 0xFF);
    }

    public WifiInfo getConnectionInfo(){
        return wifiManager.getConnectionInfo();
    }

    /**
     * 檢查Wifi是否開啟
     *
     * @return
     */
    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    /**
     * 開啟Wifi並等待一段時間避免Crash
     *
     * @param enabled
     * @return
     */
    public boolean setWifiEnabled(boolean enabled) {
        wifiManager.setWifiEnabled(enabled);
        //若在wifi還沒打開就進行下面的連接，系统會出错，所以要先判斷wifi狀態，直到是WIFI＿STATE_ENABLED才能進行連接
        for (int i = 0; i < TIMEOUT; i++) {
            if (isWifiEnabled() == enabled)
                break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return isWifiEnabled() == enabled;
    }

    /**
     * 掃描周遭熱點裝置
     *
     * @return
     */
    public boolean startScan() {
        if (isWifiEnabled()) {
            return wifiManager.startScan();
        } else {
            return false;
        }
    }

    /**
     * 取得周遭熱點裝置資訊列表
     *
     * @return
     */
    public List<ScanResult> getScanResults() {
        if (isWifiEnabled()) {
            return wifiManager.getScanResults();
        } else {
            return new ArrayList<ScanResult>();
        }
    }

    private WifiConfiguration getHtcWifiApConfiguration(WifiConfiguration standard) {
        WifiConfiguration htcWifiConfig = standard;
        try {
            Object mWifiApProfileValue = getFieldValue(standard, "mWifiApProfile");
            if (mWifiApProfileValue != null)
                htcWifiConfig.SSID = (String) getFieldValue(mWifiApProfileValue, "SSID");
        } catch (Exception e) {
        }
        return htcWifiConfig;
    }

    public WifiConfiguration getWifiApConfiguration() {
        WifiConfiguration configuration = null;
        try {
            Method method = methodMap.get("getWifiApConfiguration");
            configuration = (WifiConfiguration) method.invoke(wifiManager);
            if (isHtc)
                configuration = getHtcWifiApConfiguration(configuration);
        } catch (Exception e) {
        }
        return configuration;
    }

    private void setupHtcWifiConfiguration(WifiConfiguration config) {
        try {
            Object mWifiApProfileValue = getFieldValue(config, "mWifiApProfile");

            if (mWifiApProfileValue != null) {
                setFieldValue(mWifiApProfileValue, "SSID", config.SSID);
                setFieldValue(mWifiApProfileValue, "BSSID", config.BSSID);
            }
        } catch (Exception e) {
        }
    }

    public boolean setWifiApConfiguration(WifiConfiguration config) {
        boolean result = false;
        try {
            if (isHtc)
                setupHtcWifiConfiguration(config);

            Method method = methodMap.get("setWifiApConfiguration");

            if (isHtc) {
                int value = (Integer) method.invoke(wifiManager, config);
                result = value > 0;
            } else {
                result = (Boolean) method.invoke(wifiManager, config);
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return result;
    }

    private String getSetWifiApConfigName() {
        return isHtc ? "setWifiApConfig" : "setWifiApConfiguration";
    }

    public WifiManager wifiManager() {
        return wifiManager;
    }

    private Object getFieldValue(Object object, String propertyName)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = object.getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        return field.get(object);
    }

    private void setFieldValue(Object object, String propertyName, Object value)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = object.getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        field.set(object, value);
    }

    /**
     * Start AccessPoint mode with the specified
     * configuration. If the radio is already running in
     * AP mode, update the new configuration
     * Note that starting in access point mode disables station
     * mode operation
     *
     * @param wifiConfig SSID, security and channel details as part of WifiConfiguration
     * @return {@code true} if the operation succeeds, {@code false} otherwise
     */
    public boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        try {
            if (enabled) { // disable WiFi in any case
                wifiManager.setWifiEnabled(false);
            }

            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean) method.invoke(wifiManager, wifiConfig, enabled);
        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            return false;
        }
    }

    /**
     * Gets the Wi-Fi enabled state.
     *
     * @return {@link WIFI_AP_STATE}
     * @see #isWifiApEnabled()
     */
    public WIFI_AP_STATE getWifiApState() {
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");

            int tmp = ((Integer) method.invoke(wifiManager));

            // Fix for Android 4
            if (tmp >= 10) {
                tmp = tmp - 10;
            }

            return WIFI_AP_STATE.class.getEnumConstants()[tmp];
        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
        }
    }

    /**
     * Return whether Wi-Fi AP is enabled or disabled.
     *
     * @return {@code true} if Wi-Fi AP is enabled
     * @hide Dont open yet
     * @see #getWifiApState()
     */
    public boolean isWifiApEnabled() {
        return getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
    }

    /**
     * Gets a list of the clients connected to the Hotspot, reachable timeout is 300
     *
     * @param onlyReachables  {@code false} if the list should contain unreachable (probably disconnected) clients, {@code true} otherwise
     * @param finishListener, Interface called when the scan method finishes
     */
    public void getClientList(boolean onlyReachables, FinishScanListener finishListener) {
        getClientList(onlyReachables, 300, finishListener);
    }

    /**
     * Gets a list of the clients connected to the Hotspot
     *
     * @param onlyReachables   {@code false} if the list should contain unreachable (probably disconnected) clients, {@code true} otherwise
     * @param reachableTimeout Reachable Timout in miliseconds
     * @param finishListener,  Interface called when the scan method finishes
     */
    public void getClientList(final boolean onlyReachables, final int reachableTimeout, final FinishScanListener finishListener) {


        Runnable runnable = new Runnable() {
            public void run() {

                BufferedReader br = null;
                final ArrayList<ClientScanResult> result = new ArrayList<ClientScanResult>();

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if ((splitted != null) && (splitted.length >= 4)) {
                            // Basic sanity check
                            String mac = splitted[3];

                            if (mac.matches("..:..:..:..:..:..")) {
                                boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(reachableTimeout);

                                if (!onlyReachables || isReachable) {
                                    result.add(new ClientScanResult(splitted[0], splitted[3], splitted[5], isReachable));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(this.getClass().toString(), e.toString());
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.e(this.getClass().toString(), e.getMessage());
                    }
                }

                // Get a handler that can be used to post to the main thread
                Handler mainHandler = new Handler(context.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        finishListener.onFinishScan(result);
                    }
                };
                mainHandler.post(myRunnable);
            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    /**
     * 計算訊號強度
     *
     * @param level
     * @return
     */
    public int calculateSignalStength(int level) {
        return wifiManager.calculateSignalLevel(level, 5) + 1;
    }


    /**
     * 取得當前所連無線網路SSID
     */
    public Group getCurrentGroup() {
        Group group = new Group();
        if (isConnectedWifi()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                group.SSID = ssid;
            }
        }

        return group;
    }

    /**
     * 提供一個外部接口，傳入要連接的尚未認證或想更改認證的無線網絡
     * 回傳值為真，只能說明密碼沒有輸錯，並且網路可用，但不一定連接上
     */
    public boolean connectUnconfigured(String SSID, String Password, int Type) {

        if (!this.isWifiEnabled()) {
            return false;
        }
        //若在wifi還沒打開就進行下面的連接，系统會出错，所以要先判斷wifi狀態，直到是WIFI＿STATE_ENABLED才能進行連接
        while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            try {
                Thread.currentThread();
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
        }
        WifiConfiguration wifiConfig = this.createWifiInfo(SSID, Password, Type);
        //
        if (wifiConfig == null) {
            Log.i(TAG, "wifiConfig is null");
            return false;
        }

        // 更新認證的狀態
        WifiConfiguration tempConfig = this.isExsits(SSID);
        if (tempConfig != null) {
            wifiManager.removeNetwork(tempConfig.networkId);
        }

        int netID = wifiManager.addNetwork(wifiConfig);

        /*  有認證過的裝置和沒有認證過的裝置連線的步驟有差異 */
        if (tempConfig == null && wifiManager.enableNetwork(netID, false) && wifiManager.saveConfiguration() && wifiManager.reconnect()) {
            return true;
        } else if (tempConfig != null && wifiManager.enableNetwork(netID, false) && wifiManager.reconnect()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 提供一個外部接口，傳入已經認證的無線網絡
     * enableNetwork()方法回傳值為真，只能說明密碼沒有輸錯，並且網路可用，但不一定連接上
     * 最後需要在呼叫reconnect()連接上次關聯的無線網絡
     */
    public boolean connectConfigured(String SSID) {
        WifiConfiguration tempConfig = this.isExsits(SSID);
        if (tempConfig != null) {
            Log.i(TAG, "connectConfigured success");
            return (wifiManager.enableNetwork(tempConfig.networkId, false) && wifiManager.reconnect());
        } else {
            Log.i(TAG, "connectConfigured fail");
            return false;
        }
    }

    /**
     * 清除當前所傳入的連線
     */
    public boolean clear(String SSID) {
        if(SSID.equals("") && SSID == null) return false;
        WifiConfiguration tempConfig = this.isExsits(SSID);

        if (tempConfig != null) {
            wifiManager.disableNetwork(tempConfig.networkId);
            return wifiManager.removeNetwork(tempConfig.networkId);
        } else {
            return false;
        }
    }

    /**
     * 查看以前是否也配置過這個網絡
     */
    public WifiConfiguration isExsits(String SSID) {

        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();

        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {


                if (existingConfig.SSID != null && existingConfig.SSID.equals("\"" + SSID + "\"")) {

                    return existingConfig;
                }
            }
        }
        return null;

    }

    public boolean disableNetwork() {
        int netId = wifiManager.getConnectionInfo().getNetworkId();
        if (netId >= 0) {
            return wifiManager.disableNetwork(netId);
        } else {
            return true;
        }
    }

    public boolean disconnect() {
        if (wifiManager.isWifiEnabled()) {
            return wifiManager.disconnect();
        } else {
            return true;
        }
    }

    private WifiConfiguration createWifiInfo(String SSID, String Password,
                                             int Type) {

        WifiConfiguration config = new WifiConfiguration();

        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        config.SSID = "\"" + SSID + "\"";
        config.status = WifiConfiguration.Status.DISABLED;
        config.priority = 40;

        if (Type == ENCRYPT.NOPASS) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
            return config;
        } else if (Type == ENCRYPT.WEP) {
            config.wepKeys[0] = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.wepTxKeyIndex = 0;

            return config;
        } else if (Type == ENCRYPT.WPA) {

            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.status = WifiConfiguration.Status.ENABLED;
            return config;
        } else {
            return null;
        }
    }

}