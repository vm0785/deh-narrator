package com.mmlab.n1.network;


import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


import com.mmlab.n1.FileDownload;
import com.mmlab.n1.GPSTracker;
import com.mmlab.n1.POISearch;
import com.mmlab.n1.Utils;
import com.mmlab.n1.info.*;
import com.mmlab.n1.info.Package;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyService extends Service implements GPSTracker.onUpdateListener {

    private static final String TAG = "ProxyService";
    public static final String CONNECT_ACTION = "ProxyService.CONNECT_ACTION";
    public static final String MEMBER_ACTION = "ProxyService.MEMBER_ACTION";
    public static final String FILE_COMPLETE__ACTION = "ProxyService.FILE_COMPLETE_ACTION";

    public static final String MEMBER_LOCATION_UPDATE = "ProxyService.MEMBER_LOCATION_ACTION";
    public static final String MEMBER_JOIN_ACTION = "ProxyService.MEMBER_JOIN_ACTION";
    public static final String MEMBER_LEAVE_ACTION = "ProxyService.MEMBER_LEAVE_ACTION";
    public static final String PROXY_LOCATION_UPDATE = "ProxyService.PROXY_LOCATION_ACTION";

    private ServerThread threadServer = null;
    private HandlerThread pHandlerThread = null;

    private static List<POI> poiList = new ArrayList<POI>();
    public static String location_proxy = "";

    private VideoService videoService = null;
    private VideoService.VideoBinder videoBinder = null;
    private ServiceConnection videoConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()...");
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()...");
            videoBinder = (VideoService.VideoBinder) service;
            videoService = videoBinder.getVideoInstance();
        }
    };
    private ProxyBinder binder = new ProxyBinder();
    private GPSTracker gpsTracker = null;

    public void onCreate() {
        super.onCreate();

        pHandlerThread = new HandlerThread("Server");
        pHandlerThread.start();
        pHandler = new ProcessHandler(ProxyService.this, pHandlerThread.getLooper());

        startServer();

        gpsTracker = new GPSTracker(ProxyService.this);
        gpsTracker.setOnUpdateListener(ProxyService.this);

        // start service
        Intent startIntent = new Intent(ProxyService.this, VideoService.class);
        startService(startIntent);
        // bind service
        Intent bindIntent = new Intent(ProxyService.this, VideoService.class);
        bindService(bindIntent, videoConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "onCreate()...");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()...");
        stopServer();
        // stop service
        Intent intent = new Intent(ProxyService.this, VideoService.class);
        stopService(intent);
        // unbind service
        unbindService(videoConnection);
    }

    /**
     * START_STICKY：sticky的意思是“黏性的”。使用這個返回值時，
     * 我們啟動的服務跟應用程序"黏"在一起，如果在執行完onStartCommand後，
     * 服務被異常kill掉，系统會自動重起該服務。
     * 當再次啟動服務時，傳入的第一個参數將為null;
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()...");
        // return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public ProxyService() {

    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnBind()...");
        return super.onUnbind(intent);
    }

    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()...");
        return binder;
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged()...");
//        String msg = "New Latitude: " + location.getLatitude() + "New Longitude: " + location.getLongitude();
        String msg = location.getLatitude() + " " + location.getLongitude();
        location_proxy = msg;
        Message message = pHandler.obtainMessage();
        message.obj = msg;
        message.what = 12;
        pHandler.sendMessage(message);
    }

    public void onProviderDisabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps is turned off!! ",
                Toast.LENGTH_SHORT).show();
    }

    public void onProviderEnabled(String provider) {
        Toast.makeText(getBaseContext(), "Gps is turned on!! ",
                Toast.LENGTH_SHORT).show();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public class ProxyBinder extends Binder {
        public ProxyService getProxyInstance() {
            return ProxyService.this;
        }
    }

    public ConcurrentHashMap<String, Profile> getProfiles() {
        Log.d(TAG, "ppppp : " + threadServer.profiles.size());
        if (threadServer.profiles != null)
            return threadServer.profiles;
        return new ConcurrentHashMap<>();
    }

    public void sendCommandPlayback(long mediaLength, String remoteUri) {
        Message message = pHandler.obtainMessage(7);
        Bundle bundle = new Bundle();
        bundle.putLong("mediaLength", mediaLength);
        bundle.putString("remoteUri", remoteUri);
        message.setData(bundle);
        pHandler.sendMessage(message);
    }

    public void sendResponsePlayback(String host, long mediaLength, String remoteUri) {
        Message message = pHandler.obtainMessage(8);
        Bundle bundle = new Bundle();
        bundle.putString("host", host);
        bundle.putLong("mediaLength", mediaLength);
        bundle.putString("remoteUri", remoteUri);
        message.setData(bundle);
        pHandler.sendMessage(message);
    }

    public class ServerThread extends Thread {

        private static final String TAG = "Server";
        private static final int PORT = 9001;

        private ServerSocket serverSocket = null;
        private Socket socket = null;
        private ConcurrentHashMap<Socket, ObjectOutputStream> members = new ConcurrentHashMap<>();
        private ConcurrentHashMap<String, Profile> profiles = new ConcurrentHashMap<>();

        public void addProfile(Profile profile) {
            profiles.put(profile.IP_ADDRESS, profile);
            Log.d(TAG, "addProfile : " + profiles.size());
        }

        public void removeProfile(String key) {
            if (profiles.containsKey(key))
                profiles.remove((String) key);
            Log.d(TAG, "removeProfile key : " + key);
            Log.d(TAG, "removeProfile : " + profiles.size());
        }

        public void modifyProfile(String key, String lat, String lon) {
            if (profiles.containsKey(key)) {
                Profile profile = profiles.get(key);
                profile.latitude = Double.parseDouble(lat);
                profile.longitude = Double.parseDouble(lon);
                addProfile(profile);
            }
        }

        public void modifyProfile(String key, boolean connected) {
            if (profiles.containsKey(key)) {
                Profile profile = profiles.get(key);
                profile.isConnected = connected;
                addProfile(profile);
            }
        }

        public ServerThread() {

        }


        /**
         * 關閉ServerSocket，block在accept method的地方會throw a SocketException
         */
        public void interrupt() {
            super.interrupt();
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Iterator<Socket> iterator = members.keySet().iterator();
            while (iterator.hasNext()) {
                Socket member = iterator.next();
                try {
                    member.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        public void run() {
            serverSocket = null;

            try {
                serverSocket = new ServerSocket(PORT);
                Log.i(TAG, "The server is running");

                // Accept the incoming socket connection
                while (!Thread.currentThread().isInterrupted()) {

                    socket = serverSocket.accept();
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    sendConfirmPackage(objectOutputStream);
                    members.put(socket, objectOutputStream);
                    new ServerHandler(socket).start();
                    Log.i(TAG, "accept client");
                }
            } catch (IOException e) {
                Log.i(TAG, "ServerSocket accept fail: " + e.toString());
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.i(TAG, "SocketServer close fail: " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
            Log.i(TAG, "close server socket successfully");
        }

        public void sendCommandPlayback(long mediaLength, String remoteUri) {

            if (serverSocket != null && !serverSocket.isClosed()) {
                Log.d(TAG, "Number of Member : " + members.size());
                // 確認當前還有多少成員維持連線
                Iterator<Socket> iterator = members.keySet().iterator();
                while (iterator.hasNext()) {
                    Socket member = iterator.next();
                    try {
                        ObjectOutputStream writer = members.get(member);
                        writer.write(1);
                        writer.writeObject(new Package(Package.TAG_COMMAND, Package.TYPE_NONE, "", Utils.stringToByteArray("start video" + " " + String.valueOf(mediaLength) + " " + remoteUri)));
                    } catch (IOException e) {
                        members.remove(member);
                        e.printStackTrace();
                    }
                }
            }
        }

        public void sendResponsePlayback(String host, long mediaLength, String remoteUri) {
            Log.d(TAG, "Number of Member : " + members.size());
            // 確認當前還有多少成員維持連線
            Iterator<Socket> iterator = members.keySet().iterator();
            while (iterator.hasNext()) {
                Socket member = iterator.next();
                Log.d(TAG, "remote socket address : " + member.getRemoteSocketAddress().toString());
                if (member.getRemoteSocketAddress().toString().equals(host))
                    try {
                        ObjectOutputStream writer = members.get(member);
                        writer.write(1);
                        writer.writeObject(new Package(Package.TAG_COMMAND, Package.TYPE_NONE, "", Utils.stringToByteArray("request video" + " " + String.valueOf(mediaLength) + " " + remoteUri)));
                    } catch (IOException e) {
                        members.remove(member);
                        e.printStackTrace();
                    }
            }
        }

        public void sendPackageToClient(int cnt, Package[] packages) {
            if (serverSocket != null && !serverSocket.isClosed()) {
                Log.d(TAG, "Number of Member : " + members.size());
                // 確認當前還有多少成員維持連線
                Iterator<Socket> iterator = members.keySet().iterator();
                while (iterator.hasNext()) {
                    Socket member = iterator.next();
                    try {
                        ObjectOutputStream writer = members.get(member);
                        writer.write(cnt);
                        // Check whether the data length is correct
                        if (cnt == packages.length) {
                            for (int i = 0; i < cnt; ++i) {
                                writer.writeObject(packages[i]);
                            }
                        }
                    } catch (IOException e) {
                        members.remove(member);
                        e.printStackTrace();
                    }
                }
            }
        }

        public void sendPackageToClient(ArrayList<Package> packages) {

            if (serverSocket != null && !serverSocket.isClosed()) {
                Log.d(TAG, "Number of Member : " + members.size());
                // 確認當前還有多少成員維持連線
                Iterator<Socket> iterator = members.keySet().iterator();
                while (iterator.hasNext()) {
                    Socket member = iterator.next();
                    try {
                        ObjectOutputStream writer = members.get(member);
                        writer.write(packages.size());
                        // Check whether the data length is correct
                        for (int i = 0; i < packages.size(); ++i) {
                            writer.writeObject(packages.get(i));
                        }
                    } catch (IOException e) {
                        members.remove(member);
                        e.printStackTrace();
                    }
                }
            }
        }

        private void sendConfirmPackage(ObjectOutputStream writer) {

            if (serverSocket != null && !serverSocket.isClosed())
                try {
                    writer.write(1);
                    writer.writeObject(new Package(Package.TAG_NONE, Package.TYPE_NONE, "", Utils.stringToByteArray("Hello this is server")));

                    Log.i(TAG, "Send package successfully");
                } catch (IOException e) {
                    Log.i(TAG, "Send package fail: " + e.toString());
                    e.printStackTrace();
                }
        }

        private class ServerHandler extends Thread {

            private static final String TAG = "ServerHandler";
            private Socket socket;
            private ObjectInputStream objectInputStream;

            public ServerHandler(Socket socket) {
                this.socket = socket;
            }

            public void run() {
                Log.i(TAG, "ServerHandler is running");
                try {

                    objectInputStream = new ObjectInputStream(socket.getInputStream());
                    while (!Thread.currentThread().isInterrupted()) {
                        int objectNumber = objectInputStream.read();
                        if (objectNumber < 0) break;


                        // Read the location information from client and send  to the web server
                        for (int i = 0; i < objectNumber; ++i) {
                            try {
                                Package pack = (Package) objectInputStream.readObject();

                                switch (pack.tag) {
                                    case Package.TAG_COMMAND:
                                        break;
                                    case Package.TAG_DATA:
                                        if (pack.type == Package.TYPE_LOCATION) {
                                            Message message = pHandler.obtainMessage();
                                            message.what = 9;
                                            message.obj = socket.getRemoteSocketAddress().toString().substring(0, socket.getRemoteSocketAddress().toString().lastIndexOf(":")) + " " + Utils.byteArrayToString(pack.payload);
                                            pHandler.sendMessage(message);
                                        } else if (pack.type == Package.TYPE_PROFILE) {
                                            Message message = pHandler.obtainMessage();
                                            message.what = 10;
                                            message.obj = Utils.byteArrayToString(pack.payload);
                                            pHandler.sendMessage(message);
                                        }
                                        break;
                                    case Package.TAG_NONE:
                                        break;
                                }
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (SocketException e) {
                    Log.i(TAG, "Socket Exception: " + e.toString());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.i(TAG, "input fail: " + e.toString());
                    e.printStackTrace();
                } finally {
                    if (objectInputStream != null) {
                        try {
                            objectInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (socket != null) {
                        Message message = pHandler.obtainMessage();
                        message.what = 11;
                        message.obj = socket.getRemoteSocketAddress().toString().substring(0, socket.getRemoteSocketAddress().toString().lastIndexOf(":"));
                        pHandler.sendMessage(message);
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.i(TAG, "Socket close fail: " + e.toString());
                            e.printStackTrace();
                        }
                    }
                    Log.i(TAG, "close serverHandler properly");
                }
            }
        }
    }

    private Handler pHandler = null;

    /**
     * 處理費時任務
     */
    private class ProcessHandler extends Handler {

        private WeakReference<Service> weakReference = null;

        public ProcessHandler(Service service, Looper looper) {
            super(looper);
            weakReference = new WeakReference<Service>((ProxyService) service);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    // 開始TCP Server
                    break;
                case 1:
                    // 結束TCP Server

                    break;
                case 2:
                    // 搜尋附近景點
                    new POISearch(weakReference.get()).execute();
                    break;
                case 3:
                    // 處理點資訊
                    poiList = Utils.parsePoisJSONObject((String) msg.obj);
                    Intent intent = new Intent();
                    intent.setAction(CONNECT_ACTION);
                    intent.putExtra("status", "running");
                    if (weakReference.get() != null)
                        weakReference.get().sendBroadcast(intent);
                    break;
                case 4:
                    // 傳送單個景點資訊給所有成員， 並下載圖片、影片和聲音
                    if (weakReference.get() != null) {
                        POI poi = poiList.get((Integer) msg.obj);
                        Package[] sendPack = new Package[1];
                        sendPack[0] = new Package(Package.TAG_DATA, Package.TYPE_POI_SIN, "",
                                Utils.stringToByteArray(poi.getJsonObject().toString()));
                        sendPack[0].show = Package.SHOW_AUTO;
                        ((ProxyService) weakReference.get()).threadServer.sendPackageToClient(1, sendPack);
                        String[] array = new String[poi.getUrl().size()];
                        Log.d(TAG, "poi url size : " + poi.getUrl().size());
                        poi.getUrl().toArray(array);
                        Log.d(TAG, poi.getJsonObject().toString());
                        new FileDownload(weakReference.get(), FileDownload.POST_ALL).execute(array);
                    }
                    break;
                case 5:
                    // 傳送下載完的檔案給所有成員
                    Log.d(TAG, "傳送下載完的檔案給所有成員");
                    Package[] sendPack = new Package[1];
                    File file = new File((String) msg.obj);
                    byte[] data = new byte[(int) file.length()];
                    try {
                        new FileInputStream(file).read(data);
                        sendPack[0] = new Package(Package.TAG_DATA, Package.TYPE_IMAGE, file.getName(), data);
                        ((ProxyService) weakReference.get()).threadServer.sendPackageToClient(1, sendPack);

                        // 檔案下載完成，傳送完成訊息給有註冊監聽器的Activity
                        Intent intent1 = new Intent();
                        intent1.putExtra("file", file.getName()); // 下載完的檔案名稱
                        Log.d(TAG, "file name : " + file.getName());
                        intent1.setAction(FILE_COMPLETE__ACTION);
                        if (weakReference.get() != null)
                            weakReference.get().sendBroadcast(intent1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 6:
                    // 檔案下載完成，傳送完成訊息給有註冊監聽器的Activity
                    Intent intent1 = new Intent();
                    intent1.putExtra("file", (String) msg.obj); // 下載完的檔案名稱
                    intent1.setAction(FILE_COMPLETE__ACTION);
                    if (weakReference.get() != null)
                        weakReference.get().sendBroadcast(intent1);
                    break;
                case 7:
                    ((ProxyService) weakReference.get()).
                            threadServer.sendCommandPlayback(msg.getData().getLong("mediaLength", -1), msg.getData().getString("remoteUri"));
                    break;
                case 8:
                    ((ProxyService) weakReference.get()).
                            threadServer.sendResponsePlayback(msg.getData().getString("host"), msg.getData().getLong("mediaLength", -1), msg.getData().getString("remoteUri"));
                    break;
                case 9:
                    // 成員位置更新
                    String[] splited = ((String) msg.obj).split("\\s+");
                    if (splited.length < 3) return;
                    threadServer.modifyProfile(splited[0], splited[1], splited[2]);
                    Intent locationIntent = new Intent();
                    locationIntent.setAction(ProxyService.MEMBER_LOCATION_UPDATE);
                    locationIntent.putExtra("location", (String) msg.obj);
                    ProxyService.this.sendBroadcast(locationIntent);
                    break;
                case 10:
                    // 成員加入
                    Profile profile = new Profile();
                    profile.setProfile((String) msg.obj);
                    threadServer.addProfile(profile);
                    Log.d(TAG, "profile : " + profile.IP_ADDRESS);
                    Intent addMemberIntent = new Intent();
                    addMemberIntent.setAction(ProxyService.MEMBER_JOIN_ACTION);
                    sendBroadcast(addMemberIntent);
                    break;
                case 11:
                    // 成員離開
                    // threadServer.removeProfile((String) msg.obj);
                    threadServer.modifyProfile((String) msg.obj, false);
                    Intent leaveMemberIntent = new Intent();
                    leaveMemberIntent.setAction(ProxyService.MEMBER_LEAVE_ACTION);
                    sendBroadcast(leaveMemberIntent);
                case 12:
                    // 導覽員位置更新
                    locationIntent = new Intent();
                    locationIntent.setAction(ProxyService.PROXY_LOCATION_UPDATE);
                    locationIntent.putExtra("location", (String) msg.obj);
                    ProxyService.this.sendBroadcast(locationIntent);
                    break;
                default:
            }
        }
    }

    class CreateGroup extends AsyncTask<String, Void, Void> {

        private WeakReference<Service> weakReference = null;
        private HttpURLConnection connection = null;
        private InputStream inputStream = null;

        public CreateGroup(Service service) {
            weakReference = new WeakReference<Service>(service);
        }

        protected Void doInBackground(String... strings) {

            return null;
        }
    }

    public void POISearch() {
        pHandler.sendEmptyMessage(2);
    }

    public void setPoiList(String result) {
        pHandler.sendMessage(pHandler.obtainMessage(3, result));
    }

    public List<POI> getPOIList() {
        return this.poiList;
    }

    public void sendSinglePOI(int position) {
        pHandler.sendMessage(pHandler.obtainMessage(4, position));
    }

    protected void startServer() {
        if (threadServer == null) {
            threadServer = new ServerThread();
            threadServer.start();
        } else {
            threadServer.interrupt();
            threadServer = new ServerThread();
            threadServer.start();
        }
    }

    public void stopProxyService() {
        videoService.stopProxyService();
    }

    protected void stopServer() {
        if (threadServer != null)
            threadServer.interrupt();
    }

    public Handler getpHandler() {
        return pHandler;
    }

    public void setpHandler(Handler pHandler) {
        this.pHandler = pHandler;
    }
}
