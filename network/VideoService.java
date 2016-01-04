package com.mmlab.n1.network;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mmlab.n1.ExternalStorage;
import com.mmlab.n1.Utils;
import com.mmlab.n1.constant.IDENTITY;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.constant.PLAYBACK;
import com.mmlab.n1.preference.Preset;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

public class VideoService extends Service {

    private static final String TAG = "VideoService";

    private VideoBinder vBinder = new VideoBinder();

    private ProxyService proxyService = null;
    private ProxyService.ProxyBinder proxyBinder = null;
    private ServiceConnection connection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()...");
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()...");
            proxyBinder = (ProxyService.ProxyBinder) service;
            proxyService = proxyBinder.getProxyInstance();
        }
    };

    private Handler cHandler = null;
    private HandlerThread cHandlerThread;
    private VideoThread videoThread = null;

    public VideoService() {

    }

    public void stopProxyService() {
        if (MSN.identity == IDENTITY.PROXY) {
            // unbind service
            unbindService(connection);
            if (MSN.identity == IDENTITY.PROXY) {
                // stop service
                Intent stopIntent = new Intent(VideoService.this, ProxyService.class);
                stopService(stopIntent);
            }
            stopSelf();
        }
    }

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()...");
        Preset.loadPreferences(getApplicationContext());

        if (MSN.identity == IDENTITY.PROXY) {
            // start service
            Intent startIntent = new Intent(VideoService.this, ProxyService.class);
            startService(startIntent);
            // bind service
            Intent bindIntent = new Intent(VideoService.this, ProxyService.class);
            bindService(bindIntent, connection, BIND_AUTO_CREATE);
            // start Video Thread
            videoThread = new VideoThread();
            videoThread.start();
        }

        cHandlerThread = new HandlerThread("Main");
        cHandlerThread.start();
        cHandler = new ControlHandler(cHandlerThread.getLooper(), VideoService.this);
    }

    /**
     * START_STICKY：sticky的意思是“黏性的”。使用這個返回值時，
     * 我們啟動的服務跟應用程序"黏"在一起，如果在執行完onStartCommand後，
     * 服務被異常kill掉，系统會自動重啟該服務。
     * 當再次啟動服務時，傳入的第一個参數將為null;
     *
     * @param intent  intent
     * @param flags   flags
     * @param startId startId
     * @return int
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()...");
        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()...");

        if (videoThread != null) {
            videoThread.interrupt();
        }

        if (cHandler != null) {
            cHandler.removeCallbacksAndMessages(null);
        }
    }

    public IBinder onBind(Intent intent) {
        return vBinder;
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()...");
        return super.onUnbind(intent);
    }

    public class VideoBinder extends Binder {
        public VideoService getVideoInstance() {
            return VideoService.this;
        }
    }

    /* 處理連線共享資料相關 */
    public final static String VIDEO_STATE_START = "VideoService.VIDEO_STATE_START";
    public final static String VIDEO_STATE_UPDATE = "VideoService.VIDEO_STATE_UPDATE";
    public final static String CACHE_VIDEO_READY = "VideoService.CACHE_VIDEO_READY";
    public final static String CACHE_VIDEO_UPDATE = "VideoService.CACHE_VIDEO_UPDATE";
    public final static String CACHE_VIDEO_END = "VideoService.CACHE_VIDEO_END";
    public final static String SHOW_PROGRESS_DIALOG = "VideoService.SHOW_PROGRESS_DIALOG";
    public final static String DISMISS_PROGRESS_DIALOG = "VideoService.DISMISS_PROGRESS_DIALOG";
    public final static String VIDEO_SERVER_DISCONNECTED = "VideoService.VIDEO_SERVER_DISCONNECTED";

    public static final int READY_BUFF = 2000 * 1024;
    public static final int CACHE_BUFF = 500 * 1024;
    public static final int VIDEO_PORT = 6666;

    /**
     * 處理每個成員的要求
     */
    class VideoThread extends Thread {

        private ServerSocket serverSocket = null;
        private HashSet<Socket> memebrs = new HashSet<>();
        private Socket socket = null;

        public VideoThread() {

        }

        public void run() {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(VIDEO_PORT));
                //  the socket will block waiting to accept a connection in 3000 milliseconds before throwing an InterruptedIOException
                // serverSocket.setSoTimeout(3000);
//                proxyService.sendTest();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            try {
                Log.d(TAG, "VideoThread start");
                while (!Thread.currentThread().isInterrupted()) {
                    socket = serverSocket.accept();
                    memebrs.add(socket);
                    PLAYBACK.mediaLength = Preset.loadFilePreferences(getApplicationContext(), Utils.urlToFilename(PLAYBACK.remoteUri));
                    new GiveThread(socket, PLAYBACK.remoteUri, PLAYBACK.mediaLength).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "VideoThread is in finally clause");
            }
            Log.d(TAG, "VideoThread  is closed properly");
        }

        public void interrupt() {
            super.interrupt();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (Socket member : memebrs) {
                try {
                    member.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 讀檔傳送給成員
     */
    class GiveThread extends Thread {

        private RandomAccessFile randomAccessFile = null;
        private Socket socket = null;
        private OutputStream outputStream = null;
        private InputStream inputStream = null;
        private String pRemoteUri = null;
        private long pMediaLength = -1;

        public GiveThread(Socket socket, String pRemoteUri, long pMediaLength) {
            this.socket = socket;
            this.pMediaLength = pMediaLength;
            this.pRemoteUri = pRemoteUri;
        }

        public void run() {
            int curPos = 0;
            int readBytes = 0;
            byte[] bytes = new byte[4 * 1024];
            try {
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                readBytes = inputStream.read(bytes);
                pRemoteUri = Utils.byteArrayToString(Arrays.copyOfRange(bytes, 0, readBytes));
                Log.d(TAG, "GiveThread remoteUri : " + pRemoteUri);
                pMediaLength = Preset.loadFilePreferences(getApplicationContext(), Utils.urlToFilename(pRemoteUri));

                randomAccessFile = new RandomAccessFile(ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(pRemoteUri), "rwd");
                while (!Thread.currentThread().isInterrupted()) {

                    if (pMediaLength == curPos) break;

                    try {
                        readBytes = randomAccessFile.read(bytes, 0, bytes.length);
                        if (readBytes >= 0) {
                            curPos = curPos + readBytes;
                            // Log.d(TAG, "readBytes : " + readBytes);
                        } else {
                            if (Preset.loadFilePreferences(getApplicationContext(), Utils.urlToFilename(pRemoteUri)) == -1) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        // 檔案下載中斷，停止當前給成員的多媒體檔案
                        if (Preset.loadFilePreferences(getApplicationContext(), Utils.urlToFilename(pRemoteUri)) == -1) {
                            break;
                        }
                        // e.printStackTrace();
                    }

                    if (readBytes > 0) {
                        outputStream.write(bytes, 0, readBytes);
                    }
                }

                // Log.d(TAG, "curPos : " + curPos);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "GiveThread is in finally clause");
            }
            Log.d(TAG, "GiveThread is closed properly");
        }

        public void interrupt() {
            super.interrupt();
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
    public void startTakeThread(Activity activity, String pRemoteUri) {
        new TakeThread(activity, pRemoteUri).start();
    }
*/

    public void startReceiveThread(String pRemoteUri, long pMediaLength, int isOrientatioin) {
        Log.d(TAG, "startReceiveThread()...");
        Message message = cHandler.obtainMessage(MEMBER_UPDATE_CURRENT_MULTIMEDIA, pRemoteUri);
        Bundle bundle = new Bundle();
        bundle.putLong("mediaLength", pMediaLength);
        bundle.putInt("isOrientation", isOrientatioin);
        message.setData(bundle);
        cHandler.sendMessage(message);
    }

    public void startMemberRequest(String pRemoteUri, long pMediaLength, int isOrientatioin) {
        Log.d(TAG, "startMemberRequest");
        Message message = cHandler.obtainMessage(MEMBER_UPDATE_REQUEST_MULTIMEDIA, pRemoteUri);
        Bundle bundle = new Bundle();
        bundle.putLong("mediaLength", pMediaLength);
        bundle.putInt("isOrientation", isOrientatioin);
        message.setData(bundle);
        cHandler.sendMessage(message);
    }

    public void startVideoPlayback(String pRemoteUri, int isOrientatioin) {
        Log.d(TAG, "startVideoPlayback()...");
        Message message = cHandler.obtainMessage(PROXY_UPDATE_CURRENT_MULTIMEDIA, pRemoteUri);
        Bundle bundle = new Bundle();
        bundle.putInt("isOrientation", isOrientatioin);
        message.setData(bundle);
        cHandler.sendMessage(message);
    }

    public void startRequestPlayback(String pHost, String pRemoteUri) {
        Message message = cHandler.obtainMessage(PROXY_UPDATE_REQUEST_MULTIMEDIA, pRemoteUri);
        Bundle bundle = new Bundle();
        bundle.putString("host", pHost);
        message.setData(bundle);
        cHandler.sendMessage(message);
    }

    /**
     * 下載寫檔
     */
    class TakeThread extends Thread {
        private Intent intent = null;
        private String pRemoteUri = null;
        private String pLocalUri = null;
        private File cacheFile = null;
        private HttpURLConnection pConnection;
        private long pMediaLength = -1;
        private long pSession = 0;
        private long readSize = 0;

        public TakeThread(String pRemoteUri, long pSession, HttpURLConnection pConnection) {
            this.pRemoteUri = pRemoteUri;
            this.pSession = pSession;
            this.pConnection = pConnection;
        }

        public void run() {

            if (pRemoteUri == null) return;

            if (pSession == PLAYBACK.session) {
                intent = new Intent();
                intent.setAction(SHOW_PROGRESS_DIALOG);
                sendBroadcast(intent);
            }
            FileOutputStream out = null;
            InputStream inputStream = null;
            InputStream is = null;
            //HttpURLConnection httpConnection = null;

            try {
                if (pLocalUri == null) {
                    pLocalUri = ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(pRemoteUri);
                }

                Log.d(TAG, "localUrl: " + pLocalUri);

                cacheFile = new File(pLocalUri);

                if (!cacheFile.exists()) {
                    cacheFile.getParentFile().mkdirs();
                    cacheFile.createNewFile();
                }

                Log.d(TAG, PLAYBACK.remoteUri + "   " + pRemoteUri);
                // readSize = cacheFile.length();
                readSize = 0;
                if (pSession == PLAYBACK.session) {
                    PLAYBACK.readSize = readSize;
                }
                Log.d(TAG, "ReadSize : " + readSize);
                out = new FileOutputStream(cacheFile, true);

                // httpConnection.setRequestMethod("GET");
                // httpConnection.setRequestProperty("User-Agent", "NetFox");
                // httpConnection.setRequestProperty("RANGE", "bytes=" + readSize + "-");

                //  is = httpConnection.getInputStream();
                inputStream = pConnection.getInputStream();

                // pMediaLength = httpConnection.getContentLength();
                pMediaLength = pConnection.getContentLength();
                Log.d(TAG, "MediaLength : " + pMediaLength);
                if (pMediaLength == -1) {
                    return;
                }
                pMediaLength += readSize;

                byte buf[] = new byte[4 * 1024];
                int size;
                long lastReadSize = 0;

                if (pSession == PLAYBACK.session) {
                    intent = new Intent();
                    intent.setAction(VIDEO_STATE_UPDATE);
                    sendBroadcast(intent);
                }
                while ((size = inputStream.read(buf)) != -1 && !Thread.currentThread().isInterrupted()) {

                    // write to file
                    try {
                        out.write(buf, 0, size);
                        readSize += size;
                        if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                            PLAYBACK.readSize = readSize;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Thread.sleep(20, 0);

                    if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                        boolean isReady = PLAYBACK.isReady;
                        if (!isReady) {
                            if ((readSize - lastReadSize) > READY_BUFF) {
                                lastReadSize = readSize;
                                intent = new Intent();
                                intent.setAction(CACHE_VIDEO_READY);
                                sendBroadcast(intent);
                            }
                        } else {
                            int errorCount = PLAYBACK.errorCount;
                            if ((readSize - lastReadSize) > CACHE_BUFF
                                    * (errorCount + 1)) {
                                lastReadSize = readSize;
                                intent = new Intent();
                                intent.setAction(CACHE_VIDEO_UPDATE);
                                sendBroadcast(intent);
                            }
                        }
                    }
                }
                if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                    intent = new Intent();
                    intent.setAction(CACHE_VIDEO_END);
                    sendBroadcast(intent);
                }

                Log.d(TAG, "after size : " + readSize);
            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        //
                    }
                }

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        //
                    }
                }

                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (pConnection != null) pConnection.disconnect();

//
//                if (httpConnection != null) {
//                    httpConnection.disconnect();
//                }

                if (readSize == pMediaLength) {
                    // 下載完畢
                    if (pSession == PLAYBACK.session) {
                        PLAYBACK.isDownloaded = true;
                        PLAYBACK.isReady = true;
                        PLAYBACK.isError = false;
                    }
                } else {
                    // 下載檔案長度不一樣， 將當前的檔案取消下載，從裝置中和下載清單中移除
                    if (cacheFile.exists())
                        cacheFile.delete();
                    Preset.removeFilePreferences(getApplicationContext(), Utils.urlToFilename(pRemoteUri));
                    if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                        intent = new Intent();
                        intent.setAction(VIDEO_SERVER_DISCONNECTED);
                        sendBroadcast(intent);
                    }
                }
                Log.d(TAG, "Session : " + pSession + "    " + PLAYBACK.session);
                if (pSession == PLAYBACK.session) {
                    intent = new Intent();
                    intent.setAction(DISMISS_PROGRESS_DIALOG);
                    sendBroadcast(intent);
                }
                Log.d(TAG, "Take Thread is in finally clause");
            }
            Log.d(TAG, "Take Thread is closed safely");
        }

        public void interrupt() {
            super.interrupt();
        }
    }

    /**
     * 從Proxy讀取播放
     */
    class ReceiveThread extends Thread {

        private Socket socket = null;

        private Intent intent = null;
        private InputStream inputStream = null;
        private OutputStream outputStream = null;
        private FileOutputStream fileOutputStream = null;
        private File cacheFile = null;
        private long readSize = 0;
        private long pMediaLength;
        private String pRemoteUri;
        private String pLocalUri;
        private long pSession = 0;

        public ReceiveThread(String pRemoteUri, long pSession, long pMediaLength) {
            this.pRemoteUri = pRemoteUri;
            this.pSession = pSession;
            this.pMediaLength = pMediaLength;
        }

        public void run() {
            readSize = 0;
            try {
                InetAddress inetAddress = InetAddress.getByName(getServerIpAddress());
                socket = new Socket(inetAddress, VIDEO_PORT);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                // 傳送當前要下載的多媒體檔案名稱給proxy
                byte[] buffer = pRemoteUri.getBytes();
                outputStream.write(buffer, 0, buffer.length);

                if (pSession == PLAYBACK.session) {
                    intent = new Intent();
                    intent.setAction(SHOW_PROGRESS_DIALOG);
                    sendBroadcast(intent);
                }

                pLocalUri = ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(pRemoteUri);
                Log.d(TAG, "localUrl: " + pLocalUri);

                cacheFile = new File(pLocalUri);

                if (!cacheFile.exists()) {
                    cacheFile.getParentFile().mkdirs();
                    cacheFile.createNewFile();
                } else {
                    cacheFile.delete();
                }
                fileOutputStream = new FileOutputStream(cacheFile);
                byte[] bytes = new byte[4 * 1024];
                int readBytes;
                long lastReadSize = 0;
                boolean isReady;

                if (pSession == PLAYBACK.session) {
                    intent = new Intent();
                    intent.setAction(VIDEO_STATE_UPDATE);
                    sendBroadcast(intent);
                }
                while (!Thread.currentThread().isInterrupted() && !((readBytes = inputStream.read(bytes, 0, bytes.length)) < 0)) {
                    // Log.d(TAG, "socket receive length : " + readBytes + " " + readSize + " " + PLAYBACK.mediaLength);
                    readSize += readBytes;
                    fileOutputStream.write(bytes, 0, readBytes);
                    if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                        PLAYBACK.readSize = readSize;
                    }

                    if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                        isReady = PLAYBACK.isReady;
                        if (!isReady) {
                            if ((readSize - lastReadSize) > READY_BUFF) {
                                lastReadSize = readSize;
                                intent = new Intent();
                                intent.setAction(CACHE_VIDEO_READY);
                                sendBroadcast(intent);
                            }
                        } else {
                            int errorCount = PLAYBACK.errorCount;
                            if ((readSize - lastReadSize) > CACHE_BUFF
                                    * (errorCount + 1)) {
                                lastReadSize = readSize;
                                intent = new Intent();
                                intent.setAction(CACHE_VIDEO_UPDATE);
                                sendBroadcast(intent);
                            }
                        }
                    }
                }

                // Log.d(TAG, "mediaLength : " + pMediaLength + " readSize : " + readSize);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (pMediaLength != readSize) {
                    if (cacheFile.exists())
                        cacheFile.delete();
                    Preset.removeFilePreferences(getApplicationContext(), Utils.urlToFilename(pRemoteUri));
                    if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                        intent = new Intent();
                        intent.setAction(VIDEO_SERVER_DISCONNECTED);
                        sendBroadcast(intent);
                    }
                } else {

                    // 下載完畢
                    if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                        PLAYBACK.isDownloaded = true;
                        PLAYBACK.isReady = true;
                        PLAYBACK.isError = false;
                    }

                    if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                        intent = new Intent();
                        intent.setAction(CACHE_VIDEO_END);
                        sendBroadcast(intent);
                    }
                }
                Log.d(TAG, "VideoReceiverThread is in finally clause");
            }

            Log.d(TAG, "VideoReceiverThread is closed properly");
        }

        public void interrupt() {
            super.interrupt();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 取得DHCP伺服器的IP_ADDRESS
     *
     * @return string of DHCP IP_ADDRESS
     */
    public String getServerIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        int serverAddress = wifiManager.getDhcpInfo().serverAddress;
        return (serverAddress & 0xFF) + "." + ((serverAddress >> 8) & 0xFF) + "."
                + ((serverAddress >> 16) & 0xFF) + "." + ((serverAddress >> 24) & 0xFF);
    }

    public void fromLocal(String pRemoteUri, long pMediaLength, long pSession) {
        if (PLAYBACK.session == pSession) {
            if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                Intent intent = new Intent();
                intent.setAction(VIDEO_STATE_START);
                intent.putExtra("mediaLength", pMediaLength);
                intent.putExtra("remoteUri", pRemoteUri);
                sendBroadcast(intent);
            }
        }
    }

    public void fromMemberLocal(String pRemoteUri, long pMediaLength, long pSession) {
        if (PLAYBACK.session == pSession) {
            if (pRemoteUri.equals(PLAYBACK.remoteUri)) {
                Intent intent = new Intent();
                intent.setAction(VIDEO_STATE_START);
                intent.putExtra("mediaLength", pMediaLength);
                intent.putExtra("remoteUri", pRemoteUri);
                intent.putExtra("data", 1);
                sendBroadcast(intent);
            }
        }
    }

    public void fromNet(String pRemoteUri, long pMediaLength, long pSession, HttpURLConnection
            pConnection) {
        new TakeThread(pRemoteUri, pSession, pConnection).start();
    }

    public void fromProxy(String pRemoteUri, long pMediaLength, long pSession) {
        new ReceiveThread(pRemoteUri, pSession, pMediaLength).start();
    }

    public static final int PROXY_UPDATE_CURRENT_MULTIMEDIA = 0;
    public static final int PROXY_UPDATE_REQUEST_MULTIMEDIA = 1;
    public static final int MEMBER_UPDATE_CURRENT_MULTIMEDIA = 2;
    public static final int MEMBER_UPDATE_REQUEST_MULTIMEDIA = 3;

    /**
     * 處理費時任務
     */
    private static class ControlHandler extends Handler {

        VideoService service = null;

        public ControlHandler(Looper looper, Service service) {
            super(looper);
            this.service = (VideoService) service;
        }

        public void handleMessage(Message msg) {
            String tmpRemoteUri = "";
            long tmpMediaLength;
            long tmpSession;
            HttpURLConnection connection = null;

            switch (msg.what) {
                case PROXY_UPDATE_CURRENT_MULTIMEDIA:
                    tmpRemoteUri = (String) msg.obj;
                    tmpSession = System.currentTimeMillis();
                    // 檢查檔案是否已經下載過了
                    if ((tmpMediaLength = Preset.loadFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri))) == -1) {
                        try {
                            Log.d("cHandler", "fromeNet()...");
//                        File netFile = new File(Environment.getExternalStorageDirectory()
//                                .getAbsolutePath()
//                                + "/NetCache/"
//                                + Utils.urlToFilename(tmpRemoteUri));
                            // 獲取網址有誤，做前置處理
                            tmpRemoteUri = tmpRemoteUri.replace("moe2//", "");

                            // 確認網路連線
                            URL url = new URL(tmpRemoteUri);
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");
                            connection.connect();
                            int responseCode = connection.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                tmpMediaLength = connection.getContentLength();
                                if (tmpMediaLength < 0) {
                                    Intent intent = new Intent();
                                    intent.setAction(VIDEO_SERVER_DISCONNECTED);
                                    service.sendBroadcast(intent);
                                    return;
                                }
                            } else {
                                Intent intent = new Intent();
                                intent.setAction(VIDEO_SERVER_DISCONNECTED);
                                service.sendBroadcast(intent);
                                return;
                            }

                            // 更新當前要播放多媒體的資訊
                            PLAYBACK.remoteUri = tmpRemoteUri;
                            PLAYBACK.mediaLength = tmpMediaLength;
                            PLAYBACK.session = tmpSession;
                            PLAYBACK.isReady = false;
                            PLAYBACK.isDownloaded = false;
                            PLAYBACK.readSize = 0;
                            PLAYBACK.errorCount = 0;
                            PLAYBACK.currentPosition = 0;
                            PLAYBACK.isError = false;

                            // 儲存將要開始下載的多媒體檔案資訊
                            Preset.saveFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri), tmpMediaLength);
                            Log.d("cHandler", "load : " + Preset.loadFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri)));
                            // 開始下載
                            service.fromNet(tmpRemoteUri, tmpMediaLength, tmpSession, connection);
                            // 通知群組成員當前播放的多媒體檔案名稱和長度
                            service.proxyService.sendCommandPlayback(tmpMediaLength, tmpRemoteUri);
                        } catch (IOException e) {
                            if (connection != null) connection.disconnect();

                            Intent intent = new Intent();
                            intent.setAction(VIDEO_SERVER_DISCONNECTED);
                            service.sendBroadcast(intent);

                            e.printStackTrace();
                        }
                    } else {
                        Log.d("cHandler", "fromeLocal()..." + tmpRemoteUri + " " + tmpMediaLength);
                        // 更新當前要播放多媒體的資訊
                        PLAYBACK.remoteUri = tmpRemoteUri;
                        PLAYBACK.mediaLength = tmpMediaLength;
                        PLAYBACK.session = tmpSession;
                        PLAYBACK.isReady = true;
                        PLAYBACK.isDownloaded = true;
                        PLAYBACK.readSize = tmpMediaLength;
                        PLAYBACK.errorCount = 0;
                        // 檢查是否是裝置螢幕學轉而產生的請求
                        if (msg.getData().getInt("isOrientation", 0) == 0)
                            PLAYBACK.currentPosition = 0;
                        PLAYBACK.isError = false;
                        service.fromLocal(tmpRemoteUri, tmpMediaLength, tmpSession);
                        // 通知群組成員當前播放的多媒體檔案名稱和長度
                        if (msg.getData().getInt("isOrientation", 0) == 0)
                            service.proxyService.sendCommandPlayback(tmpMediaLength, tmpRemoteUri);
                    }


                    break;
                case PROXY_UPDATE_REQUEST_MULTIMEDIA:
                    tmpRemoteUri = (String) msg.obj;
                    tmpSession = System.currentTimeMillis();
                    if ((tmpMediaLength = Preset.loadFilePreferences(service.getApplication(), Utils.urlToFilename(tmpRemoteUri))) == -1) {
                        try {
                            Log.d("cHandler", "REQUEST fromeNet()...");
                            tmpRemoteUri = tmpRemoteUri.replace("moe2//", "");

                            URL url = null;

                            url = new URL(tmpRemoteUri);

                            connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");
                            connection.connect();
                            int responseCode = connection.getResponseCode();
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                tmpMediaLength = connection.getContentLength();
                                if (tmpMediaLength < 0) return;
                            } else {
                                return;
                            }
                            // 儲存開始下載多媒體檔案資訊
                            Preset.saveFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri), tmpMediaLength);
                            Log.d("cHandler", "load : " + Preset.loadFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri)));
                            service.fromNet(tmpRemoteUri, tmpMediaLength, tmpSession, connection);
                            // 通知群組成員當前播放的多媒體檔案名稱和長度
                            service.proxyService.sendResponsePlayback(msg.getData().getString("host"), tmpMediaLength, tmpRemoteUri);
                        } catch (IOException e) {
                            if (connection != null) connection.disconnect();
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("cHandler", "fromeLocal()..." + tmpRemoteUri + " " + tmpMediaLength);
                        service.fromLocal(tmpRemoteUri, tmpMediaLength, tmpSession);
                        // 通知群組成員當前播放的多媒體檔案名稱和長度
                        if (msg.getData().getInt("isOrientation", 0) == 0)
                            service.proxyService.sendResponsePlayback(msg.getData().getString("host"), tmpMediaLength, tmpRemoteUri);
                    }
                    break;
                case MEMBER_UPDATE_CURRENT_MULTIMEDIA:
                    tmpRemoteUri = (String) msg.obj;
                    tmpSession = System.currentTimeMillis();
                    if ((tmpMediaLength = Preset.loadFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri))) == -1) {
                        Log.d("cHandler", "fromeProxy()...");
                        PLAYBACK.remoteUri = tmpRemoteUri;
                        PLAYBACK.mediaLength = msg.getData().getLong("mediaLength", -1);
                        PLAYBACK.session = tmpSession;
                        PLAYBACK.isReady = false;
                        PLAYBACK.isDownloaded = false;
                        PLAYBACK.readSize = 0;
                        PLAYBACK.errorCount = 0;
                        PLAYBACK.currentPosition = 0;
                        PLAYBACK.isError = false;
                        // 儲存開始下載多媒體檔案資訊
                        Preset.saveFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri), PLAYBACK.mediaLength);
                        Log.d("cHandler", "load : " + Preset.loadFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri)));
                        service.fromProxy(tmpRemoteUri, PLAYBACK.mediaLength, tmpSession);
                    } else {
                        Log.d("cHandler", "fromeLocal()..." + tmpRemoteUri + " " + tmpMediaLength);
                        PLAYBACK.remoteUri = tmpRemoteUri;
                        PLAYBACK.mediaLength = tmpMediaLength;
                        PLAYBACK.session = tmpSession;
                        PLAYBACK.isReady = true;
                        PLAYBACK.isDownloaded = true;
                        PLAYBACK.readSize = tmpMediaLength;
                        PLAYBACK.errorCount = 0;
                        if (msg.getData().getInt("isOrientation", 0) == 0)
                            PLAYBACK.currentPosition = 0;
                        PLAYBACK.isError = false;
                        service.fromLocal(tmpRemoteUri, tmpMediaLength, tmpSession);
                    }
                    break;
                case MEMBER_UPDATE_REQUEST_MULTIMEDIA:
                    tmpRemoteUri = (String) msg.obj;
                    tmpSession = System.currentTimeMillis();
                    if ((tmpMediaLength = Preset.loadFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri))) == -1) {
                        Log.d("cHandler", "fromeProxy()...");
                        PLAYBACK.remoteUri = tmpRemoteUri;
                        PLAYBACK.mediaLength = msg.getData().getLong("mediaLength", -1);
                        PLAYBACK.session = tmpSession;
                        PLAYBACK.isReady = false;
                        PLAYBACK.isDownloaded = false;
                        PLAYBACK.readSize = 0;
                        PLAYBACK.errorCount = 0;
                        PLAYBACK.currentPosition = 0;
                        PLAYBACK.isError = false;
                        // 儲存開始下載多媒體檔案資訊
                        Preset.saveFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri), PLAYBACK.mediaLength);
                        Log.d("cHandler", "load : " + Preset.loadFilePreferences(service.getApplicationContext(), Utils.urlToFilename(tmpRemoteUri)));
                        service.fromProxy(tmpRemoteUri, PLAYBACK.mediaLength, tmpSession);
                    } else {
                        Log.d("cHandler", "fromeMemberLocal()..." + tmpRemoteUri + " " + tmpMediaLength);
                        PLAYBACK.remoteUri = tmpRemoteUri;
                        PLAYBACK.mediaLength = tmpMediaLength;
                        PLAYBACK.session = tmpSession;
                        PLAYBACK.isReady = true;
                        PLAYBACK.isDownloaded = true;
                        PLAYBACK.readSize = tmpMediaLength;
                        PLAYBACK.errorCount = 0;
                        if (msg.getData().getInt("isOrientation", 0) == 0)
                            PLAYBACK.currentPosition = 0;
                        PLAYBACK.isError = false;
                        service.fromMemberLocal(tmpRemoteUri, tmpMediaLength, tmpSession);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
