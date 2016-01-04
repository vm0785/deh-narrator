package com.mmlab.n1.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;


import com.mmlab.n1.ExternalStorage;
import com.mmlab.n1.MainActivity;
import com.mmlab.n1.R;
import com.mmlab.n1.Utils;
import com.mmlab.n1.adapter.MainAdapter;
import com.mmlab.n1.constant.HTTPREQUEST;
import com.mmlab.n1.constant.IDENTITY;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.decoration.DividerItemDecoration;
import com.mmlab.n1.info.Group;
import com.mmlab.n1.network.ConnectionManager;
import com.mmlab.n1.network.NetworkManager;
import com.mmlab.n1.network.WirelessManager;
import com.mmlab.n1.preference.Preset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class GroupFragment extends Fragment {

    public static final String TAG = "GroupFragment";

    private RecyclerView mRecyclerView = null;
    private static List<Group> mGroups = new ArrayList<Group>();

    private static List<ScanResult> mResults = new ArrayList<ScanResult>();
    private static List<Group> tmpGroups = new ArrayList<Group>();
    private static MainAdapter mAdapter = null;

    private static WirelessManager mWirelessManager = null;
    private static NetworkManager networkManager = null;
    private static HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
    private IntentFilter intentFilter = null;

    private HandlerThread pHandlerThread = null;

    private AlertDialog alertDialog = null;
    private View layout_group = null;
    private SwipeRefreshLayout swipeRefreshLayout = null;

    private boolean isStart = false;

    public GroupFragment() {
        // Required empty public constructor
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "onCreate()...");
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(broadcastReceiver, intentFilter);
        Log.d(TAG, "onStart()...");
    }

    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d(TAG, "onHiddenChange()...");
        if (!hidden) {
            getActivity().registerReceiver(broadcastReceiver, intentFilter);
        } else {
            try {
                getActivity().unregisterReceiver(broadcastReceiver);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d(TAG, "setUserVisibleHint()..." + isVisibleToUser);
        if (isStart) {
            if (isVisibleToUser) {
                if (broadcastReceiver != null)
                    getActivity().registerReceiver(broadcastReceiver, intentFilter);
            } else {
                try {
                    if (broadcastReceiver != null)
                        getActivity().unregisterReceiver(broadcastReceiver);
                } catch (IllegalArgumentException e) {
                }
            }
        }
    }

    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()...");
        try {
            getActivity().unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
        Preset.savePreferences(getActivity().getApplicationContext(), MSN.identity);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()...");
        // Inflate the layout for this fragment
        layout_group = inflater.inflate(R.layout.fragment_group, container, false);

        Preset.clearPreferences(getActivity().getApplicationContext());
        clearFileDirectory(new File(ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY));
        clearFileDirectory(new File(ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY));


        mWirelessManager = new WirelessManager(getActivity().getApplicationContext());
        networkManager = new NetworkManager(getActivity().getApplicationContext());
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        pHandlerThread = new HandlerThread("Main");
        pHandlerThread.start();
        pHandler = new ProcessHandler(pHandlerThread.getLooper());


        mAdapter = new MainAdapter(getActivity().getApplicationContext(), mGroups);
        mRecyclerView = (RecyclerView) layout_group.findViewById(R.id.recyclerView);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                swipeRefreshLayout.setEnabled(linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0);
            }
        });
        // 設置ClickListener
        // 根據每個Group的連現狀態會有不同的Dialog
        mAdapter.setOnItemClickLitener(new MainAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                if (alertDialog != null) alertDialog.dismiss();
                if (MSN.identity == IDENTITY.MEMBER && mGroups.size() > position && position >= 0) {
                    WifiConfiguration existingConfig = mWirelessManager.isExsits(mGroups.get(position).SSID);
                    if (existingConfig != null && existingConfig.status == 0) {
                        // current connected device
                        alertDialog = ConnectionManager.createConnectedDialog(mWirelessManager, getActivity(), mGroups.get(position));
                        alertDialog.show();
                    } else if (existingConfig != null) {
                        // is int the configured list
                        alertDialog = ConnectionManager.createConfiguredDialog(mWirelessManager, getActivity(), mGroups.get(position));
                        alertDialog.show();
                    } else if (existingConfig == null) {
                        // not in the configured list
                        alertDialog = ConnectionManager.createConfiguredDialog(mWirelessManager, getActivity(), mGroups.get(position));
                        alertDialog.show();
                    }
                    Log.i(TAG, "member");
                } else {
                    Log.i(TAG, "proxy");
                    // proxy 自己創建的群組
                    alertDialog = ConnectionManager.createConnectedDialog(mWirelessManager, getActivity(), mGroups.get(position));
                    alertDialog.show();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        // 設置Adapter
        mRecyclerView.setAdapter(mAdapter);

        /** V2 Start **/
        swipeRefreshLayout = (SwipeRefreshLayout) layout_group.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        /** V2 End **/

        mWirelessManager.startScan();

        isStart = true;

        return layout_group;
    }


    /**
     * 處理來自網路狀態改變的broadcast訊息
     * 交由pHandler處理費時任務和mHandler更新UI介面
     * 避免阻塞主線程
     */
    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive()...");
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)
                    || WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)
                    || ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                Log.d("WifiReceiver", ">>>>NETWORK_STATE_CHANGED_ACTION<<<<<<");
                WifiInfo connectionInfo = mWirelessManager.getConnectionInfo();
                String ssid = "";
                if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID()) && networkManager.isConnectedWifi()) {
                    ssid = Utils.unornamatedSsid(connectionInfo.getSSID());
                    if (!ssid.equals(MSN.proxy_ssid)) {
                        Log.d(TAG, "ssid : " + ssid + " " + connectionInfo.getSSID() + " proxy_ssid" + MSN.proxy_ssid);
                        mWirelessManager.clear(ssid);
                        return;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("SSID", ssid);
                    bundle.putString("STATUS", "Connected");
                    Message msg = Message.obtain();
                    msg.setData(bundle);
                    msg.what = 0;
                    pHandler.sendMessage(msg);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("SSID", "");
                    bundle.putString("STATUS", "");
                    Message msg = Message.obtain();
                    msg.setData(bundle);
                    msg.what = 0;
                    pHandler.sendMessage(msg);
                }
            } else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                Log.d("WifiReceiver", ">>>>SUPPLICANT_STATE_CHANGED_ACTION<<<<<<");
                SupplicantState supl_state = ((SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
                WifiInfo connectionInfo = mWirelessManager.getConnectionInfo();
                String ssid = "";
                switch (supl_state) {
                    case ASSOCIATED:
                        Log.i("SupplicantState", "ASSOCIATED");
                        break;
                    case ASSOCIATING:
                        Log.i("SupplicantState", "ASSOCIATING");
                        break;
                    case AUTHENTICATING:
                        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                            ssid = connectionInfo.getSSID();
                            Bundle bundle = new Bundle();
                            bundle.putString("SSID", ssid);
                            bundle.putString("STATUS", "Authenticating");
                            Message msg = Message.obtain();
                            msg.setData(bundle);
                            msg.what = 0;
                            pHandler.sendMessage(msg);
                        }
                        Log.i("SupplicantState", "Authenticating : " + ssid);
                        break;
                    case COMPLETED:
                        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                            ssid = connectionInfo.getSSID();
                            Bundle bundle = new Bundle();
                            bundle.putString("SSID", ssid);
                            bundle.putString("STATUS", "Connected");
                            Message msg = Message.obtain();
                            msg.setData(bundle);
                            msg.what = 0;
                            pHandler.sendMessage(msg);
                        }
                        Log.i("SupplicantState", "Connected : " + ssid);
                        break;
                    case DISCONNECTED:
                        Log.i("SupplicantState", "Disconnected");
                        break;
                    case DORMANT:
                        Log.i("SupplicantState", "DORMANT");
                        break;
                    case FOUR_WAY_HANDSHAKE:
                        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                            ssid = connectionInfo.getSSID();
                            Bundle bundle = new Bundle();
                            bundle.putString("SSID", ssid);
                            bundle.putString("STATUS", "Connecting");
                            Message msg = pHandler.obtainMessage();
                            msg.setData(bundle);
                            msg.what = 0;
                            pHandler.sendMessage(msg);
                        }
                        Log.i("SupplicantState", "FOUR_WAY_HANDSHAKE");
                        break;
                    case GROUP_HANDSHAKE:
                        if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                            ssid = connectionInfo.getSSID();
                            Bundle bundle = new Bundle();
                            bundle.putString("SSID", ssid);
                            bundle.putString("STATUS", "Connecting");
                            Message msg = Message.obtain();
                            msg.setData(bundle);
                            msg.what = 0;
                            pHandler.sendMessage(msg);
                        }
                        Log.i("SupplicantState", "GROUP_HANDSHAKE");
                        break;
                    case INACTIVE:
                        Log.i("SupplicantState", "INACTIVE");
                        break;
                    case INTERFACE_DISABLED:
                        Log.i("SupplicantState", "INTERFACE_DISABLED");
                        break;
                    case INVALID:
                        Log.i("SupplicantState", "INVALID");
                        break;
                    case SCANNING:
                        Log.i("SupplicantState", "SCANNING");
                        break;
                    case UNINITIALIZED:
                        Log.i("SupplicantState", "UNINITIALIZED");
                        break;
                    default:
                        Log.i("SupplicantState", "Unknown");
                        break;

                }
                int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                if (supl_error == WifiManager.ERROR_AUTHENTICATING) {
                    Log.i("ERROR_AUTHENTICATING", "ERROR_AUTHENTICATING!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                }
            }
        }
    };

    /**
     * 處理來自Service的broadcast訊息
     * 交由pHandler處理費時任務和mHandler更新UI介面
     * 避免阻塞主線程
     */
    public BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

        }
    };

    private static Handler mHandler = new UIHandler();
    private Handler pHandler = null;

    /**
     * 更新UI介面
     */
    private static class UIHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    // 無連網，更新掃描SSID資訊列表
                    mGroups.clear();
                    mGroups.addAll(new ArrayList<Group>(tmpGroups));
                    mAdapter.notifyDataSetChanged();
                    Log.d(TAG, "size : " + tmpGroups.size());
                    break;
                default:
            }
        }
    }

    /**
     * 處理費時任務
     */
    private static class ProcessHandler extends Handler {

        public ProcessHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    String ssid = Utils.unornamatedSsid(msg.getData().getString("SSID"));
                    String status = msg.getData().getString("STATUS");
                    Log.d(TAG, "SSID SSID " + ssid);
                    Log.d(TAG, "STATUS STATUS " + status);
                    if (MSN.identity == IDENTITY.MEMBER) {
                        mResults = mWirelessManager.getScanResults();
                        tmpGroups.clear();

                        if (mResults != null)
                            for (int i = 0; i < mResults.size(); ++i) {
                                ScanResult result = (ScanResult) mResults.get(i);
                                String key = result.SSID;
                                if (!hashMap.containsKey(key)) {
                                    hashMap.put(key, tmpGroups.size());
                                    if (mWirelessManager.getCurrentGroup().SSID.equals(result.SSID)
                                            && !mWirelessManager.getCurrentGroup().SSID.equals("")) {
                                        if (result.SSID.equals(ssid))
                                            tmpGroups.add(new Group("", "", result.SSID, "", status, result.level, true));
                                        else
                                            tmpGroups.add(new Group("", "", result.SSID, "", result.capabilities, result.level, true));
                                    } else {
                                        if (result.SSID.equals(ssid))
                                            tmpGroups.add(new Group("", "", result.SSID, "", status, result.level, false));
                                        else
                                            tmpGroups.add(new Group("", "", result.SSID, "", result.capabilities, result.level, false));
                                    }
                                } else {
                                    int position = hashMap.get(key);
                                    Group updateItem = tmpGroups.get(position);
                                    if (mWirelessManager.calculateSignalStength(updateItem.level) <
                                            mWirelessManager.calculateSignalStength(result.level)) {
                                        if (mWirelessManager.getCurrentGroup().SSID.equals(result.SSID)
                                                && !mWirelessManager.getCurrentGroup().SSID.equals("")) {
                                            if (result.SSID.equals(ssid))
                                                tmpGroups.set(position, new Group("", "", result.SSID, "", status, result.level, true));
                                            else
                                                tmpGroups.set(position, new Group("", "", result.SSID, "", result.capabilities, result.level, true));
                                        } else {
                                            if (result.SSID.equals(ssid))
                                                tmpGroups.set(position, new Group("", "", result.SSID, "", status, result.level, false));
                                            else
                                                tmpGroups.set(position, new Group("", "", result.SSID, "", result.capabilities, result.level, false));
                                        }
                                    }
                                }
                            }
                        hashMap.clear();
                    } else {
                        mResults.clear();
                        tmpGroups.clear();
                        mGroups.clear();
                        hashMap.clear();
                    }
                    mHandler.sendEmptyMessage(0);
                    break;
                default:
            }
        }
    }

    public void clearFileDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                clearFileDirectory(child);

        fileOrDirectory.delete();
    }


    class CreateGroup extends AsyncTask<String, Void, Void> {

        private static final String TAG = "CreateGroup";

        private WeakReference<Activity> weakReference = null;
        private HttpURLConnection httpURLConnection = null;
        private BufferedReader bufferedReader = null;
        private OutputStreamWriter outputStreamWriter = null;

        public CreateGroup(Activity activity) {
            weakReference = new WeakReference<Activity>(activity);
        }

        protected Void doInBackground(String... params) {

            String url = HTTPREQUEST.HTTP_REQUEST;
            try {

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", "createGroup");
                jsonObject.put("content", new JSONObject().put("name", params[0]).put("FBID", params[1]).put("description", params[2]));
                Log.d(TAG, "request : " + url);
                Log.d(TAG, "data : " + jsonObject.toString());

                URL obj = new URL(url);
                httpURLConnection = (HttpURLConnection) obj.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestProperty("User-Agent", HTTPREQUEST.USER_AGENT);
                httpURLConnection.connect();

                outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream());
                outputStreamWriter.write("request=" + jsonObject.toString());
                outputStreamWriter.flush();

                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String line;
                    StringBuffer stringBuffer = new StringBuffer();
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuffer.append(line);
                    }

                    Log.d(TAG, "receive : " + stringBuffer.toString());
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (outputStreamWriter != null) {
                    try {
                        outputStreamWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }
    }

    class FirstUsage extends AsyncTask<String, Void, Void> {
        private static final String TAG = "FirstUsage";

        private WeakReference<Activity> weakReference = null;
        private HttpURLConnection httpURLConnection = null;
        private BufferedReader bufferedReader = null;
        private OutputStreamWriter outputStreamWriter = null;
        private List<String> friends = new ArrayList<>();

        public FirstUsage(Activity activity, List<String> friends) {
            weakReference = new WeakReference<Activity>(activity);
            if (friends != null) {
                this.friends = friends;
            }
        }

        protected Void doInBackground(String... params) {

            String url = HTTPREQUEST.HTTP_REQUEST;
            try {

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", "firstUsage");
                jsonObject.put("content", new JSONObject()
                        .put("FBID", params[0])
                        .put("SSID", params[1])
                        .put("SSIDpwd", params[2])
                        .put("name", params[3])
                        .put("share", params[4])
                        .put("encrypt", params[5])
                        .put("list", new JSONArray(friends)));
                Log.d(TAG, "request : " + url);
                Log.d(TAG, "data : " + jsonObject.toString());

                URL obj = new URL(url);
                httpURLConnection = (HttpURLConnection) obj.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestProperty("User-Agent", HTTPREQUEST.USER_AGENT);
                httpURLConnection.connect();

                outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream());
                outputStreamWriter.write("request=" + jsonObject.toString());
                outputStreamWriter.flush();

                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String line;
                    StringBuffer stringBuffer = new StringBuffer();
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuffer.append(line);
                    }

                    Log.d(TAG, "receive : " + stringBuffer.toString());
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (outputStreamWriter != null) {
                    try {
                        outputStreamWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }
    }

    class FindPassword extends AsyncTask<String, Void, Void> {
        private static final String TAG = "FindPassword";

        private WeakReference<Activity> weakReference = null;
        private HttpURLConnection httpURLConnection = null;
        private BufferedReader bufferedReader = null;
        private OutputStreamWriter outputStreamWriter = null;
        private List<String> SSID = new ArrayList<>();

        public FindPassword(Activity activity, List<String> SSID) {
            weakReference = new WeakReference<Activity>(activity);
            if (SSID != null) {
                this.SSID = SSID;
            }
        }

        protected Void doInBackground(String... params) {

            String url = HTTPREQUEST.HTTP_REQUEST;
            try {

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", "findPassword");
                jsonObject.put("content", new JSONObject()
                        .put("FBID", params[0])
                        .put("SSIDlist", new JSONArray(SSID)));
                Log.d(TAG, "request : " + url);
                Log.d(TAG, "data : " + jsonObject.toString());

                URL obj = new URL(url);
                httpURLConnection = (HttpURLConnection) obj.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestProperty("User-Agent", HTTPREQUEST.USER_AGENT);
                httpURLConnection.connect();

                outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream());
                outputStreamWriter.write("request=" + jsonObject.toString());
                outputStreamWriter.flush();

                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String line;
                    StringBuffer stringBuffer = new StringBuffer();
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuffer.append(line);
                    }

                    Log.d(TAG, "receive : " + stringBuffer.toString());
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (outputStreamWriter != null) {
                    try {
                        outputStreamWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    public void onDetach() {
        super.onDetach();
    }
}
