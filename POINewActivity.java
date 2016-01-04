package com.mmlab.n1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.mmlab.n1.adapter.AudioAdapter;
import com.mmlab.n1.adapter.ImageAdapter;
import com.mmlab.n1.adapter.VideoAdapter;
import com.mmlab.n1.constant.IDENTITY;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.constant.PLAYBACK;
import com.mmlab.n1.info.*;
import com.mmlab.n1.info.Package;
import com.mmlab.n1.network.MemberService;
import com.mmlab.n1.network.ProxyService;
import com.mmlab.n1.network.VideoService;
import com.mmlab.n1.preference.Preset;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class POINewActivity extends AppCompatActivity {

    private static final String TAG = "POIACtivity";
    private Toolbar toolbar = null;
    private ActionBar actionBar = null;

    private POI poi = null;

    private TextView textView_title = null;
    private TextView textView_theme = null;
    private TextView textView_address = null;
    private TextView textView_period = null;
    private TextView textView_description = null;

    private MemberService mClient = null;
    private ClientReceiver clientReceiver = null;

    private ProxyService mServer = null;
    private ServerReceiver serverReceiver = null;

    private RecyclerView recyclerView_image = null;
    private RecyclerView recyclerView_video = null;
    private RecyclerView recyclerView_audio = null;

    private ImageAdapter imageAdapter = null;
    private VideoAdapter videoAdapter = null;
    private AudioAdapter audioAdapter = null;

    private List<String> mImages = new ArrayList<String>();
    private List<String> mVideos = new ArrayList<String>();
    private List<String> mAudios = new ArrayList<String>();

    private TextView textView_imageNone = null;
    private TextView textView_audioNone = null;
    private TextView textView_videoNone = null;

    private IntentFilter intentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poinew);

//        toolbar = (Toolbar) findViewById(R.id.tool_bar);
//        setSupportActionBar(toolbar);

        poi = (POI) getIntent().getSerializableExtra("poi");

        textView_title = (TextView) findViewById(R.id.textView_title);
        textView_theme = (TextView) findViewById(R.id.textView_theme);
        textView_address = (TextView) findViewById(R.id.textView_address);
        textView_period = (TextView) findViewById(R.id.textView_period);
        textView_description = (TextView) findViewById(R.id.textView_description);
        textView_imageNone = (TextView) findViewById(R.id.imageView_imageNone);
        textView_videoNone = (TextView) findViewById(R.id.imageView_videoNone);
        textView_audioNone = (TextView) findViewById(R.id.imageView_audioNone);


        LinearLayoutManager imageLinearLayout = new LinearLayoutManager(getApplicationContext());
        imageLinearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView_image = (RecyclerView) findViewById(R.id.recyclerView_image);
        recyclerView_image.setLayoutManager(imageLinearLayout);
        recyclerView_image.setItemAnimator(new DefaultItemAnimator());

        LinearLayoutManager videoLinearLayout = new LinearLayoutManager(getApplicationContext());
        videoLinearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView_video = (RecyclerView) findViewById(R.id.recyclerView_video);
        recyclerView_video.setLayoutManager(videoLinearLayout);
        recyclerView_video.setItemAnimator(new DefaultItemAnimator());

        LinearLayoutManager audioLinearLayout = new LinearLayoutManager(getApplicationContext());
        audioLinearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView_audio = (RecyclerView) findViewById(R.id.recyclerView_audio);
        recyclerView_audio.setLayoutManager(audioLinearLayout);
        recyclerView_audio.setItemAnimator(new DefaultItemAnimator());


        imageAdapter = new ImageAdapter(getApplicationContext(), mImages);
        imageAdapter.setOnItemClickLitener(new ImageAdapter.OnItemClickLitener() {
            public void onItemClick(View view, int position) {

            }

            public void onItemLongClick(View view, int position) {

            }
        });
        recyclerView_image.setAdapter(imageAdapter);

        audioAdapter = new AudioAdapter(getApplicationContext(), mAudios);
        audioAdapter.setOnItemClickLitener(new AudioAdapter.OnItemClickLitener() {
            public void onItemClick(View view, int position) {
//                Toast.makeText(getApplicationContext(), mAudios.get(position), Toast.LENGTH_SHORT).show();
//
//                Intent intent = new Intent(POINewActivity.this, VideoDemoActivity.class);
//                PLAYBACK.remoteUri = mAudios.get(position).replace("moe2//", "");
//                startActivityForResult(intent, UPDATE);
            }

            public void onItemLongClick(View view, int position) {

            }
        });
        recyclerView_audio.setAdapter(audioAdapter);

        videoAdapter = new VideoAdapter(getApplicationContext(), mVideos);
        videoAdapter.setOnItemClickLitener(new VideoAdapter.OnItemClickLitener() {
            public void onItemClick(View view, int position) {
                Toast.makeText(getApplicationContext(), mVideos.get(position), Toast.LENGTH_SHORT).show();

//                Intent intent = new Intent(POINewActivity.this, VideoDemoActivity.class);
//                PLAYBACK.remoteUri = mVideos.get(position).replace("moe2//", "");
//                startActivityForResult(intent, UPDATE);
                mHandler.removeCallbacksAndMessages(null);
                PLAYBACK.remoteUri = mVideos.get(position);
                if (MSN.identity == IDENTITY.PROXY) {
                    localUri = ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(PLAYBACK.remoteUri);
                } else {
                    localUri = ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(PLAYBACK.remoteUri);
                }
                if (MSN.identity == IDENTITY.PROXY) {
                    //  videoService.setRemoteUri(remoteUri);
                    //  videoService.startTakeThread(VideoDemoActivity.this, remoteUri);

                    videoService.startVideoPlayback(PLAYBACK.remoteUri, isOrientation);
                } else {
                    // videoService.setMediaLength(mediaLength);
                    //videoService.setRemoteUri(remoteUri);
                    if (data == 0)
                        videoService.startReceiveThread(PLAYBACK.remoteUri, PLAYBACK.mediaLength, isOrientation);
                    else
                        videoService.startMemberRequest(PLAYBACK.remoteUri, PLAYBACK.mediaLength, isOrientation);
                }
            }

            public void onItemLongClick(View view, int position) {

            }
        });
        recyclerView_video.setAdapter(videoAdapter);

        imageAdapter.notifyDataSetChanged();
        videoAdapter.notifyDataSetChanged();
        audioAdapter.notifyDataSetChanged();

        refresh();

        /** V2 Start **/
        initViews();
        initVariables();
        /** V2 End **/
    }

    protected void onStart() {
        super.onStart();
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        Preset.loadPreferences(getApplicationContext());

        if (MSN.identity == IDENTITY.MEMBER) {
            Intent intent = new Intent(POINewActivity.this, MemberService.class);
            startService(intent);
            Intent intent1 = new Intent(POINewActivity.this, MemberService.class);
            bindService(intent1, clientConnection, BIND_AUTO_CREATE);

            clientReceiver = new ClientReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MemberService.CONNECT_ACTION);
            intentFilter.addAction(MemberService.FILE_COMPLETE__ACTION);
            registerReceiver(clientReceiver, intentFilter);
            // register
            intentFilter = new IntentFilter();
            intentFilter.addAction(MemberService.VIDEO_START_ACTION);
            registerReceiver(serviceReceiver, intentFilter);
        } else if (MSN.identity == IDENTITY.PROXY) {
            Intent intent = new Intent(POINewActivity.this, ProxyService.class);
            startService(intent);
            Intent intent1 = new Intent(POINewActivity.this, ProxyService.class);
            bindService(intent1, serverConnection, BIND_AUTO_CREATE);

            serverReceiver = new ServerReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ProxyService.CONNECT_ACTION);
            intentFilter.addAction(ProxyService.FILE_COMPLETE__ACTION);
            registerReceiver(serverReceiver, intentFilter);
        }

        /**
         * V2 Start
         */
        mHandler.removeCallbacksAndMessages(null);

        // start video service
        Intent startVideoIntent = new Intent(POINewActivity.this, VideoService.class);
        startService(startVideoIntent);
        // bind video intent
        Intent bindVideoIntent = new Intent(POINewActivity.this, VideoService.class);
        bindService(bindVideoIntent, videoConnection, BIND_AUTO_CREATE);

        registerReceiver(videoServiceReceiver, videoIntentFilter);
        /** V2 End **/
    }


    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged()...");
        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE | newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            isOrientation = 1;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MSN.identity == IDENTITY.MEMBER) {
            unbindService(clientConnection);
            unregisterReceiver(clientReceiver);
            // unregister
            unregisterReceiver(serviceReceiver);
        } else if (MSN.identity == IDENTITY.PROXY) {
            unbindService(serverConnection);
            unregisterReceiver(serverReceiver);
        }

        /** V2 Start **/
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }


        unbindService(videoConnection);
        unregisterReceiver(videoServiceReceiver);
        /** V2 End **/
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()...");
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }

        // start video service
        Intent stopVideoIntent = new Intent(POINewActivity.this, VideoService.class);
        stopService(stopVideoIntent);
    }

    public void refresh() {
        textView_title.setText(poi.getName());
        textView_theme.setText(poi.getSubject());
        textView_address.setText(poi.getAddress());
        textView_period.setText(poi.getPeriod());
        textView_description.setText(poi.getDescript());

        mImages.clear();
        if (poi.getImgUrl() != null) {
            mImages.addAll(poi.getImgUrl());
        }
        imageAdapter.notifyDataSetChanged();
        mVideos.clear();
        if (poi.getVideoUrl() != null) {
            mVideos.addAll(poi.getVideoUrl());
            videoAdapter.notifyDataSetChanged();
        }
        mAudios.clear();
        if (poi.getAudioUrl() != null) {
            mAudios.addAll(poi.getAudioUrl());
            audioAdapter.notifyDataSetChanged();
        }

        if (mImages.size() > 0) {
            textView_imageNone.setVisibility(View.GONE);
            recyclerView_image.setVisibility(View.VISIBLE);
        } else {
            textView_imageNone.setVisibility(View.VISIBLE);
            recyclerView_image.setVisibility(View.GONE);
        }
        if (mVideos.size() > 0) {
            textView_videoNone.setVisibility(View.GONE);
            recyclerView_video.setVisibility(View.VISIBLE);
        } else {
            textView_videoNone.setVisibility(View.VISIBLE);
            recyclerView_video.setVisibility(View.GONE);
        }
        if (mAudios.size() > 0) {
            textView_audioNone.setVisibility(View.GONE);
            recyclerView_audio.setVisibility(View.VISIBLE);
        } else {
            textView_audioNone.setVisibility(View.VISIBLE);
            recyclerView_audio.setVisibility(View.GONE);
        }
    }

    public void updateImagesList() {
        mImages.clear();
        mImages.addAll(poi.getImgUrl());
        imageAdapter.notifyDataSetChanged();
    }

    ServiceConnection serverConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(POINewActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(POINewActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            ProxyService.ProxyBinder mLocalBinder = (ProxyService.ProxyBinder) service;
            mServer = mLocalBinder.getProxyInstance();
        }
    };

    ServiceConnection clientConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(POINewActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mClient = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(POINewActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            MemberService.MemberBinder mLocalBinder = (MemberService.MemberBinder) service;
            mClient = mLocalBinder.getMemberInstance();
        }
    };

    public class ServerReceiver extends BroadcastReceiver {

        public ServerReceiver() {

        }

        public void onReceive(Context context, Intent intent) {
            if (ProxyService.CONNECT_ACTION.equals(intent.getAction())) {

            } else if (ProxyService.MEMBER_ACTION.equals(intent.getAction())) {

            } else if (ProxyService.FILE_COMPLETE__ACTION.equals(intent.getAction())) {
                updateImagesList();
                Toast.makeText(getApplicationContext(), "Proxy" + intent.getStringExtra("file"), Toast.LENGTH_SHORT).show();
            }

        }
    }

    public class ClientReceiver extends BroadcastReceiver {

        public ClientReceiver() {

        }

        public void onReceive(Context context, Intent intent) {
            if (MemberService.CONNECT_ACTION.equals(intent.getAction())) {
                if (intent.getIntExtra("show", Package.SHOW_NONE) == Package.SHOW_AUTO) {
                    poi = mClient.getCurPOI();
                    refresh();
                }
            } else if (MemberService.FILE_COMPLETE__ACTION.equals(intent.getAction())) {
                updateImagesList();
                Toast.makeText(getApplicationContext(), "Member" + intent.getStringExtra("file"), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 處理來自Service的broadcast訊息
     * 交由pHandler處理費時任務和mHandler更新UI介面
     * 避免阻塞主線程
     */
    public BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MemberService.VIDEO_START_ACTION)) {
                Intent intentVideo = new Intent(POINewActivity.this, VideoDemoActivity.class);
                Log.d(TAG, "Broadcast : " + intent.getLongExtra("mediaLength", -1));
                intentVideo.putExtra("mediaLength", intent.getLongExtra("mediaLength", -1));
                intentVideo.putExtra("remoteUri", intent.getStringExtra("remoteUri"));
                intentVideo.putExtra("data", intent.getIntExtra("data", 0));
                // startActivityForResult(intentVideo, UPDATE);
            }
        }
    };

    public static final int UPDATE = 0;

    /**
     * V2 Start
     **/

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

    private IntentFilter videoIntentFilter = null;

    private VideoView videoView_video;
    private TextView textView_cache;
    private String localUri;
    private ProgressDialog progressDialog = null;
    private Dialog dialog = null;
    private int isOrientation = 0;
    private int data = 0;


    private void showProgressDialog() {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(POINewActivity.this);
                    progressDialog.setTitle("多媒體下載");
                    progressDialog.setMessage("加載中...");
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.setOnKeyListener(new Dialog.OnKeyListener() {
                        public boolean onKey(DialogInterface arg0, int keyCode,
                                             KeyEvent event) {
                            // TODO Auto-generated method stub
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                POINewActivity.this.finish();
                                progressDialog.dismiss();
                            }
                            return true;
                        }
                    });
                    progressDialog.show();
                }
            }
        });
    }

    private void dismissProgressDialog() {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            }
        });
    }

    private void initViews() {
        this.videoView_video = (VideoView) findViewById(R.id.videoView_video);
        this.textView_cache = (TextView) findViewById(R.id.textView_cache);

        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
    }

    private void initVariables() {
        Intent intent = getIntent();

        videoIntentFilter = new IntentFilter();
        videoIntentFilter.addAction(VideoService.VIDEO_STATE_UPDATE);
        videoIntentFilter.addAction(VideoService.VIDEO_STATE_START);
        videoIntentFilter.addAction(VideoService.CACHE_VIDEO_END);
        videoIntentFilter.addAction(VideoService.CACHE_VIDEO_READY);
        videoIntentFilter.addAction(VideoService.CACHE_VIDEO_UPDATE);
        videoIntentFilter.addAction(VideoService.SHOW_PROGRESS_DIALOG);
        videoIntentFilter.addAction(VideoService.DISMISS_PROGRESS_DIALOG);
        videoIntentFilter.addAction(MemberService.VIDEO_START_ACTION);
        videoIntentFilter.addAction(MemberService.CONNECT_ACTION);
        videoIntentFilter.addAction(VideoService.VIDEO_SERVER_DISCONNECTED);

        data = intent.getIntExtra("data", 0);

        if (MSN.identity == IDENTITY.PROXY) {
            localUri = ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(PLAYBACK.remoteUri);
        } else {
            localUri = ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(PLAYBACK.remoteUri);
        }

        MediaController mediaController = new MediaController(this) {
            public void show() {
                super.show();
                if (getSupportActionBar() != null) getSupportActionBar().show();
                textView_cache.setVisibility(View.GONE);
            }

            public void hide() {
                super.hide();
                if (getSupportActionBar() != null) getSupportActionBar().hide();
                textView_cache.setVisibility(View.VISIBLE);
            }
        };
        Log.d(TAG, "background : " + mediaController.getBackground());

        videoView_video.setMediaController(mediaController);

        videoView_video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            public void onPrepared(MediaPlayer mediaplayer) {
                dismissProgressDialog();
                videoView_video.seekTo(PLAYBACK.currentPosition);

                if (!PLAYBACK.isError && PLAYBACK.isReady) {
                    mediaplayer.start();
                } else {
                    videoView_video.pause();
                    showProgressDialog();
                }
            }
        });

        videoView_video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            public void onCompletion(MediaPlayer mediaplayer) {
                if (PLAYBACK.isDownloaded) {
                    PLAYBACK.currentPosition = 0;
                }
                videoView_video.pause();
                dismissProgressDialog();
            }
        });

        videoView_video.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            public boolean onError(MediaPlayer mediaplayer, int i, int j) {
                Log.d(TAG, "onError()...");
                PLAYBACK.isError = true;
                PLAYBACK.errorCount++;
                videoView_video.pause();
                showProgressDialog();
                return true;
            }
        });
    }

    public final static int VIDEO_STATE_START = 4;
    public final static int VIDEO_STATE_UPDATE = 0;
    public final static int CACHE_VIDEO_READY = 1;
    public final static int CACHE_VIDEO_UPDATE = 2;
    public final static int CACHE_VIDEO_END = 3;
    public final static int SHOW_PROGRESS_DIALOG = 5;
    public final static int DISMISS_PROGRESS_DIALOG = 6;
    public final static int VIDEO_SERVER_DISCONNECTED = 7;

    private final Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case VIDEO_STATE_START:
                    videoView_video.setVideoPath(ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(PLAYBACK.remoteUri));
                    videoView_video.start();
                    break;
                case VIDEO_STATE_UPDATE:
                    double cachePercent = PLAYBACK.readSize * 100.00 / PLAYBACK.mediaLength * 1.0;
                    String info = String.format("下載進度: %.2f%%", cachePercent);

                    if (videoView_video.isPlaying()) {
                        PLAYBACK.currentPosition = videoView_video.getCurrentPosition();
                        int duration = videoView_video.getDuration();
                        duration = duration == 0 ? 1 : duration;

                        double playPercent = PLAYBACK.currentPosition * 100.00 / duration * 1.0;

                        int i = PLAYBACK.currentPosition / 1000;
                        int hour = i / (60 * 60);
                        int minute = i / 60 % 60;
                        int second = i % 60;

                        // info += String.format(" 播放: %02d:%02d:%02d (%.2f%%)", hour, minute, second, playPercent);
                        info += String.format(" 播放進度: %.2f%%", playPercent);
                    }

                    textView_cache.setText(info);

                    mHandler.sendEmptyMessageDelayed(VIDEO_STATE_UPDATE, 1000);
                    break;

                case CACHE_VIDEO_READY:
                    PLAYBACK.isReady = true;
                    videoView_video.setVideoPath(localUri);
                    videoView_video.start();
                    break;

                case CACHE_VIDEO_UPDATE:
                    if (PLAYBACK.isError) {
                        videoView_video.setVideoPath(localUri);
                        videoView_video.start();
                        PLAYBACK.isError = false;
                    }
                    break;
                case CACHE_VIDEO_END:
                    videoView_video.setVideoPath(localUri);
                    videoView_video.start();
                    PLAYBACK.isError = false;
                    PLAYBACK.isDownloaded = true;
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    break;
                case SHOW_PROGRESS_DIALOG:
                    if (progressDialog == null) {
                        progressDialog = new ProgressDialog(POINewActivity.this);
                        progressDialog.setTitle("多媒體下載");
                        progressDialog.setMessage("加載中...");
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(false);
                        progressDialog.setOnKeyListener(new Dialog.OnKeyListener() {
                            public boolean onKey(DialogInterface arg0, int keyCode,
                                                 KeyEvent event) {
                                // TODO Auto-generated method stub
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    POINewActivity.this.finish();
                                    progressDialog.dismiss();
                                }
                                return true;
                            }
                        });
                        progressDialog.show();
                    }
                    break;
                case DISMISS_PROGRESS_DIALOG:
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    break;
                case VIDEO_SERVER_DISCONNECTED:
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    if (videoView_video.isPlaying()) {
                        videoView_video.pause();
                    }

                    if (dialog == null) {
                        dialog = new AlertDialog.Builder(POINewActivity.this)
                                .setCancelable(false)
                                .setTitle("糟糕了")
                                .setMessage("與多媒體伺服器連線中斷")
                                .setOnKeyListener(new Dialog.OnKeyListener() {

                                    @Override
                                    public boolean onKey(DialogInterface arg0, int keyCode,
                                                         KeyEvent event) {
                                        // TODO Auto-generated method stub
                                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                                            POINewActivity.this.finish();
                                            dialog.dismiss();
                                        }
                                        return true;
                                    }
                                })
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // continue with delete
                                        finish();
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                    break;
                default:
            }

            super.handleMessage(msg);
        }
    };


    /**
     * 處理來自VideoService的broadcast訊息
     * 交由pHandler處理費時任務和mHandler更新UI介面
     * 避免阻塞主線程
     */
    public BroadcastReceiver videoServiceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(VideoService.VIDEO_STATE_START)) {
                mHandler.sendEmptyMessage(VIDEO_STATE_START);
                mHandler.sendEmptyMessage(VIDEO_STATE_UPDATE);
                Log.d(TAG, "VIDEO_STATE_START");
            } else if (intent.getAction().equals(VideoService.VIDEO_STATE_UPDATE)) {
                mHandler.sendEmptyMessage(VIDEO_STATE_UPDATE);
                Log.d(TAG, "VIDEO_STATE_UPDATE");
            } else if (intent.getAction().equals(VideoService.CACHE_VIDEO_UPDATE)) {
                mHandler.sendEmptyMessage(CACHE_VIDEO_UPDATE);
                Log.d(TAG, "CACHE_VIDEO_UPDATE");
            } else if (intent.getAction().equals(VideoService.CACHE_VIDEO_READY)) {
                mHandler.sendEmptyMessage(CACHE_VIDEO_READY);
                Log.d(TAG, "CACHE_VIDEO_READY");
            } else if (intent.getAction().equals(VideoService.CACHE_VIDEO_END)) {
                mHandler.sendEmptyMessage(CACHE_VIDEO_END);
                Log.d(TAG, "CACHE_VIDEO_END");
            } else if (intent.getAction().equals(VideoService.SHOW_PROGRESS_DIALOG)) {
                mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);
                // showProgressDialog();
                Log.d(TAG, "SHOW_PROGRESS_DIALOG");
            } else if (intent.getAction().equals(VideoService.DISMISS_PROGRESS_DIALOG)) {
                mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);
                // dismissProgressDialog();
                Log.d(TAG, "DISMISS_PROGRESS_DIALOG");
            } else if (intent.getAction().equals(VideoService.VIDEO_SERVER_DISCONNECTED)) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.sendEmptyMessage(VIDEO_SERVER_DISCONNECTED);
                Log.d(TAG, "VIDEO_SERVER_DISCONNECTED");
            } else if (intent.getAction().equals(MemberService.VIDEO_START_ACTION)) {
                Log.d(TAG, "VIDEO_START_ACTION");
                mHandler.removeCallbacksAndMessages(null);
                PLAYBACK.mediaLength = intent.getLongExtra("mediaLength", -1);
                PLAYBACK.remoteUri = intent.getStringExtra("remoteUri");
                localUri = ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(PLAYBACK.remoteUri);
                PLAYBACK.readSize = 0;
                PLAYBACK.isReady = false;
                PLAYBACK.isError = false;
                PLAYBACK.isDownloaded = false;
                PLAYBACK.errorCount = 0;
                PLAYBACK.currentPosition = 0;
                videoView_video.pause();
                dismissProgressDialog();
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                if (intent.getIntExtra("data", 0) == 0)
                    videoService.startReceiveThread(PLAYBACK.remoteUri, PLAYBACK.mediaLength, isOrientation);
                else {
                    videoService.startMemberRequest(PLAYBACK.remoteUri, PLAYBACK.mediaLength, isOrientation);
                }
            } else if (MemberService.CONNECT_ACTION.equals(intent.getAction())) {
                if (intent.getIntExtra("show", Package.SHOW_NONE) == Package.SHOW_AUTO) {
                    Intent i = new Intent();
                    i.putExtra("show", Package.SHOW_AUTO);
                    POINewActivity.this.setResult(RESULT_OK, i);
                    POINewActivity.this.finish();
                }
            }
        }
    };
    /** V2 End **/
}
