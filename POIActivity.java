package com.mmlab.n1;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


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
import com.mmlab.n1.preference.Preset;

import java.util.ArrayList;
import java.util.List;

public class POIActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_poi);

        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

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
                Toast.makeText(getApplicationContext(), mAudios.get(position), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(POIActivity.this, VideoDemoActivity.class);
                PLAYBACK.remoteUri = mAudios.get(position).replace("moe2//", "");
                startActivityForResult(intent, UPDATE);
            }

            public void onItemLongClick(View view, int position) {

            }
        });
        recyclerView_audio.setAdapter(audioAdapter);

        videoAdapter = new VideoAdapter(getApplicationContext(), mVideos);
        videoAdapter.setOnItemClickLitener(new VideoAdapter.OnItemClickLitener() {
            public void onItemClick(View view, int position) {
                Toast.makeText(getApplicationContext(), mVideos.get(position), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(POIActivity.this, VideoDemoActivity.class);
                PLAYBACK.remoteUri = mVideos.get(position).replace("moe2//", "");
                startActivityForResult(intent, UPDATE);
            }

            public void onItemLongClick(View view, int position) {

            }
        });
        recyclerView_video.setAdapter(videoAdapter);

        imageAdapter.notifyDataSetChanged();
        videoAdapter.notifyDataSetChanged();
        audioAdapter.notifyDataSetChanged();

        refresh();
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
            Intent intent = new Intent(POIActivity.this, MemberService.class);
            startService(intent);
            Intent intent1 = new Intent(POIActivity.this, MemberService.class);
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
            Intent intent = new Intent(POIActivity.this, ProxyService.class);
            startService(intent);
            Intent intent1 = new Intent(POIActivity.this, ProxyService.class);
            bindService(intent1, serverConnection, BIND_AUTO_CREATE);

            serverReceiver = new ServerReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ProxyService.CONNECT_ACTION);
            intentFilter.addAction(ProxyService.FILE_COMPLETE__ACTION);
            registerReceiver(serverReceiver, intentFilter);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poi, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            Toast.makeText(POIActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(POIActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            ProxyService.ProxyBinder mLocalBinder = (ProxyService.ProxyBinder) service;
            mServer = mLocalBinder.getProxyInstance();
        }
    };

    ServiceConnection clientConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(POIActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mClient = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(POIActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
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
                Intent intentVideo = new Intent(POIActivity.this, VideoDemoActivity.class);
                Log.d(TAG, "Broadcast : " + intent.getLongExtra("mediaLength", -1));
                intentVideo.putExtra("mediaLength", intent.getLongExtra("mediaLength", -1));
                intentVideo.putExtra("remoteUri", intent.getStringExtra("remoteUri"));
                intentVideo.putExtra("data", intent.getIntExtra("data", 0));
                startActivityForResult(intentVideo, UPDATE);
            }
        }
    };

    public static final int UPDATE = 0;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case UPDATE:
                try {
                    if (data.getIntExtra("show", Package.SHOW_NONE) == Package.SHOW_AUTO) {
                        poi = mClient.getCurPOI();
                        refresh();
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                break;
            default:
        }
    }
}
