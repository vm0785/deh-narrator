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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;


import com.mmlab.n1.constant.IDENTITY;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.constant.PLAYBACK;
import com.mmlab.n1.fragment.VideoRetainFragment;
import com.mmlab.n1.info.*;
import com.mmlab.n1.info.Package;
import com.mmlab.n1.network.MemberService;
import com.mmlab.n1.network.ProxyService;
import com.mmlab.n1.network.VideoService;
import com.mmlab.n1.preference.Preset;

import java.io.File;

public class VideoDemoActivity extends AppCompatActivity {

    private static final String TAG = "VideoDemoActivity";

    private Toolbar toolbar = null;

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
    };
    private IntentFilter videoIntentFilter = null;

    private VideoView videoView_video;
    private TextView textView_cache;
    private String localUri;
    private ProgressDialog progressDialog = null;
    private Dialog dialog = null;
    private int isOrientation = 0;
    private int data = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_demo);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().hide();
        }

        // load Preference
        Preset.loadPreferences(getApplicationContext());

        initViews();
        initVariables();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged()...");
        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE | newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            isOrientation = 1;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_video_demo, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()...");

        mHandler.removeCallbacksAndMessages(null);

        if (MSN.identity == IDENTITY.PROXY) {
            // start service
            Intent startIntent = new Intent(VideoDemoActivity.this, ProxyService.class);
            startService(startIntent);
            // bind service
            Intent bindIntent = new Intent(VideoDemoActivity.this, ProxyService.class);
            bindService(bindIntent, connection, BIND_AUTO_CREATE);
        }
        // start video service
        Intent startVideoIntent = new Intent(VideoDemoActivity.this, VideoService.class);
        startService(startVideoIntent);
        // bind video intent
        Intent bindVideoIntent = new Intent(VideoDemoActivity.this, VideoService.class);
        bindService(bindVideoIntent, videoConnection, BIND_AUTO_CREATE);


        registerReceiver(videoServiceReceiver, videoIntentFilter);
    }

    protected void onStop() {
        super.onStop();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }

        if (MSN.identity == IDENTITY.PROXY) {
            // unbind service
            unbindService(connection);
        }
        unbindService(videoConnection);
        unregisterReceiver(videoServiceReceiver);
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
        if (MSN.identity == IDENTITY.PROXY) {
            // start service
            Intent startIntent = new Intent(VideoDemoActivity.this, ProxyService.class);
            stopService(startIntent);
        }
        // start video service
        Intent startVideoIntent = new Intent(VideoDemoActivity.this, VideoService.class);
        stopService(startVideoIntent);
    }

    private void initViews() {
        this.videoView_video = (VideoView) findViewById(R.id.videoView_video);
        this.textView_cache = (TextView) findViewById(R.id.textView_cache);
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
                    finish();
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

    private void showProgressDialog() {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(VideoDemoActivity.this);
                    progressDialog.setTitle("多媒體下載");
                    progressDialog.setMessage("加載中...");
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.setOnKeyListener(new Dialog.OnKeyListener() {
                        public boolean onKey(DialogInterface arg0, int keyCode,
                                             KeyEvent event) {
                            // TODO Auto-generated method stub
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                VideoDemoActivity.this.finish();
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
                        progressDialog = new ProgressDialog(VideoDemoActivity.this);
                        progressDialog.setTitle("多媒體下載");
                        progressDialog.setMessage("加載中...");
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(false);
                        progressDialog.setOnKeyListener(new Dialog.OnKeyListener() {
                            public boolean onKey(DialogInterface arg0, int keyCode,
                                                 KeyEvent event) {
                                // TODO Auto-generated method stub
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    VideoDemoActivity.this.finish();
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
                        dialog = new AlertDialog.Builder(VideoDemoActivity.this)
                                .setCancelable(false)
                                .setTitle("糟糕了")
                                .setMessage("與多媒體伺服器連線中斷")
                                .setOnKeyListener(new Dialog.OnKeyListener() {

                                    @Override
                                    public boolean onKey(DialogInterface arg0, int keyCode,
                                                         KeyEvent event) {
                                        // TODO Auto-generated method stub
                                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                                            VideoDemoActivity.this.finish();
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
                    VideoDemoActivity.this.setResult(RESULT_OK, i);
                    VideoDemoActivity.this.finish();
                }
            }
        }
    };
}