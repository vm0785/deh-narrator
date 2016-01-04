package com.mmlab.n1;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Network;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;


import com.mmlab.n1.constant.IDENTITY;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.fragment.ExitDialog;
import com.mmlab.n1.fragment.GroupDialog;
import com.mmlab.n1.fragment.GroupFragment;
import com.mmlab.n1.fragment.IdentityDialog;
import com.mmlab.n1.fragment.NetworkDialog;
import com.mmlab.n1.fragment.SettingsFragment;
import com.mmlab.n1.fragment.SiteFragment;
import com.mmlab.n1.info.*;
import com.mmlab.n1.info.Package;
import com.mmlab.n1.network.MemberService;
import com.mmlab.n1.network.NetWorkUtils;
import com.mmlab.n1.network.ProxyService;
import com.mmlab.n1.preference.Preset;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GroupDialog.GroupListener {

    private static final String TAG = "MainActivity";
    // private static final CharSequence[] choose_identity = {"導覽員", "成員"};
    public static final int PAGE_POI = 1;
    private Toolbar toolbar = null;
    private TabLayout tabLayout = null;
    /**
     * V2 Start
     */
    private View layout_disconnect = null;
    private Button button_reconnect = null;
    /**
     * V2 End
     **/
    private ViewPager viewPager = null;
    private int[] tabIcons = {
            R.drawable.ic_group,
            R.drawable.ic_site,
            R.drawable.ic_settings
    };
    private int[] toolbarLayout = {
            R.layout.toolbar_group,
            R.layout.toolbar_site,
            R.layout.toolbar_settings
    };

    private ImageButton imageButton_identity = null;
    private ImageButton imageButton_network = null;
    private ImageButton imageButton_add_group = null;
    private ImageButton imageButton_search = null;

    private ProxyService mServer = null;
    private MemberService mClient = null;

    private ServerReceiver serverReceiver = null;
    private ClientReceiver clientReceiver = null;

    private Intent intentService = null;
    private Intent intentClient = null;

    private android.app.FragmentManager fragmentManager = null;

    public void sendSinglePOI(int position) {
        mServer.sendSinglePOI(position);
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getFragmentManager();

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Drawable icon = getResources().getDrawable(tabIcons[tab.getPosition()]);
                icon.setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
                tabLayout.getTabAt(tab.getPosition()).setIcon(icon);

                viewPager.setCurrentItem(tab.getPosition());

                // change the menu
                View view = toolbar.findViewById(R.id.bottom_toolbar);
                int index = toolbar.indexOfChild(view);
                toolbar.removeView(view);
                toolbar.addView(getLayoutInflater().inflate(toolbarLayout[tab.getPosition()], toolbar, false), index);

                setUpMenu(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                Drawable icon = getResources().getDrawable(tabIcons[tab.getPosition()]);
                icon.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
                tabLayout.getTabAt(tab.getPosition()).setIcon(icon);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        setupTabIcons();

        /**
         * V2
         */
        layout_disconnect = (View) findViewById(R.id.layout_disconnect);
        button_reconnect = (Button) layout_disconnect.findViewById(R.id.button_reconnect);
        button_reconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClient != null) {
                    onDisConnectMessage(false);
                    mClient.stopClient();
                    mClient.startClient();
                }
            }
        });

        MSN.identity = Preset.loadPreferences(getApplicationContext());

    }

    /**
     * V2 Start
     */

    public void onDisConnectMessage(final boolean enabled) {
        Log.d(TAG, "onDisConnectMessage()...");
        runOnUiThread(new Runnable() {
            public void run() {
                if (enabled) {
                    layout_disconnect.setVisibility(View.VISIBLE);
                } else {
                    layout_disconnect.setVisibility(View.GONE);
                }
            }
        });
    }

    public void exitDEH() {
        if (MSN.identity == IDENTITY.PROXY) {
            mServer.stopProxyService();
        }
        finish();
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()...");
        if (MSN.identity == IDENTITY.PROXY) {
            try {
                unregisterReceiver(serverReceiver);
            } catch (Exception e) {
            }
            try {
                unbindService(serverConnection);
            } catch (Exception e) {

            }
        } else {
            try {
                unregisterReceiver(clientReceiver);
            } catch (Exception e) {

            }
            try {
                unbindService(clientConnection);
            } catch (Exception e) {

            }
        }
    }

    public void onRegisteProxyReceiver() {
        serverReceiver = new ServerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ProxyService.CONNECT_ACTION);
        intentFilter.addAction(ProxyService.FILE_COMPLETE__ACTION);
        registerReceiver(serverReceiver, intentFilter);
    }

    public void onRegisteMemberReceiver() {
        clientReceiver = new ClientReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MemberService.CONNECT_ACTION);
        intentFilter.addAction(MemberService.VIDEO_START_ACTION);
        intentFilter.addAction(MemberService.CONNECT_TO_PROXY);
        intentFilter.addAction(MemberService.DISCONNECT_FROM_PROXY);
        registerReceiver(clientReceiver, intentFilter);
    }

    /**
     * V2 End
     */

    protected void onStart() {
        super.onStart();
        MSN.identity = Preset.loadPreferences(getApplicationContext());

        /** V2 Start **/
        if (MSN.identity == IDENTITY.PROXY) {
            try {
                layout_disconnect.setVisibility(View.GONE);
                stopMemberService();
                startProxyService();
            } catch (Exception e) {
            }
        } else {
            try {
                layout_disconnect.setVisibility(View.GONE);
                stopProxyService();
                startMemberService();
            } catch (Exception e) {

            }
        }
        /** V2 End **/
    }

    protected void onDestroy() {
        super.onDestroy();
        stopProxyService();
        stopMemberService();
    }

    public void onBackPressed() {
        ExitDialog dialog = new ExitDialog();
        dialog.show(getFragmentManager(), "exitDialog");
    }

    private void setUpMenu(int position) {
        switch (position) {
            case 0:
                imageButton_network = (ImageButton) toolbar.findViewById(R.id.imageButton_network);
                imageButton_network.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        NetworkDialog dialog = new NetworkDialog();
                        dialog.show(getFragmentManager(), "networkDialog");
                    }
                });
                imageButton_add_group = (ImageButton) toolbar.findViewById(R.id.imageButton_add_group);
                imageButton_add_group.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
//                        GroupDialog dialog = new GroupDialog();
//                        dialog.show(getFragmentManager(), "groupDialog");
                        if (MSN.identity == IDENTITY.PROXY) {
                            Intent intent = new Intent(MainActivity.this, InformationActivity.class);
                            startActivity(intent);
                        }
                    }
                });
                imageButton_identity = (ImageButton) toolbar.findViewById(R.id.imageButton_identity);
                imageButton_identity.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        IdentityDialog dialog = new IdentityDialog();
                        dialog.show(getFragmentManager(), "identityDialog");
                    }
                });
                break;
            case 1:
                imageButton_search = (ImageButton) toolbar.findViewById(R.id.imageButton_search);
                imageButton_search.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if ((MSN.identity == IDENTITY.PROXY)) {
                            // 判斷當前的fragment是哪種，向DEH Servre請求資訊
                            if (viewPager.getCurrentItem() == PAGE_POI)
                                mServer.POISearch();
                            else
                                Log.d(TAG, "other pages");
                        } else if (MSN.identity == IDENTITY.MEMBER) {
                            // 向Proxy發送周邊景點資訊請求
                        }
                    }
                });
                break;
            case 2:
                break;
            default:
        }
    }

    private void setupTabIcons() {
        Drawable icon = getResources().getDrawable(tabIcons[tabLayout.getSelectedTabPosition()]);
        icon.setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
        tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).setIcon(icon);
        setUpMenu(tabLayout.getSelectedTabPosition());

        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new GroupFragment(), "GROUP");
        adapter.addFragment(new SiteFragment(), "SITE");
        adapter.addFragment(new SettingsFragment(), "SETTINGS");
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);
    }

    public void onGroupCreate(String group_name, String group_description) {
        Toast.makeText(this, "群組名稱：" + group_name + ",  群組描述 :" + group_description,
                Toast.LENGTH_SHORT).show();
    }


    public void startProxyService() {
        // NetWorkUtils.setAPEnabledMethod(getApplicationContext(), true);
        onDisConnectMessage(false);

        try {
            unregisterReceiver(serverReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        onRegisteProxyReceiver();

        Intent intent = new Intent(MainActivity.this, ProxyService.class);
        startService(intent);
        Intent intent1 = new Intent(MainActivity.this, ProxyService.class);
        bindService(intent1, serverConnection, BIND_AUTO_CREATE);

    }

    public void startMemberService() {
        NetWorkUtils.setWiFiEnabled(getApplicationContext(), true);

        try {
            unregisterReceiver(clientReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        onRegisteMemberReceiver();

        Intent intent = new Intent(MainActivity.this, MemberService.class);
        startService(intent);
        Intent intent1 = new Intent(MainActivity.this, MemberService.class);
        bindService(intent1, clientConnection, BIND_AUTO_CREATE);

    }

    public void stopProxyService() {
        try {
            if (mServer != null) mServer.stopProxyService();
            Intent intent = new Intent(MainActivity.this, ProxyService.class);
            stopService(intent);
            unbindService(serverConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            unregisterReceiver(serverReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMemberService() {
        try {
            Intent intent = new Intent(MainActivity.this, MemberService.class);
            stopService(intent);
            unbindService(clientConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            unregisterReceiver(clientReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ServiceConnection serverConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            ProxyService.ProxyBinder mLocalBinder = (ProxyService.ProxyBinder) service;
            mServer = mLocalBinder.getProxyInstance();
        }
    };

    ServiceConnection clientConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mClient = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            MemberService.MemberBinder mLocalBinder = (MemberService.MemberBinder) service;
            mClient = mLocalBinder.getMemberInstance();
        }
    };

    public class ServerReceiver extends BroadcastReceiver {

        public ServerReceiver() {

        }

        public void onReceive(Context context, Intent intent) {
            if (ProxyService.CONNECT_ACTION.equals(intent.getAction())) {
                if (viewPager.getCurrentItem() == PAGE_POI) {
                    Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem());
                    ((SiteFragment) page).updateSites(mServer.getPOIList());
                }
            }
            if (ProxyService.MEMBER_ACTION.equals(intent.getAction())) {

            }
        }
    }

    public class ClientReceiver extends BroadcastReceiver {

        public ClientReceiver() {

        }

        public void onReceive(Context context, Intent intent) {
            if (MemberService.CONNECT_ACTION.equals(intent.getAction())) {
                if (viewPager.getCurrentItem() == PAGE_POI) {
                    Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem());
                    ((SiteFragment) page).updateSites(mClient.getPOIList());
                }
                if (intent.getIntExtra("show", Package.SHOW_NONE) == Package.SHOW_AUTO) {
                    Log.d(TAG, "show auto");
                    Intent intent1 = new Intent(MainActivity.this, POIActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("poi", mClient.getCurPOI());
                    intent1.putExtras(bundle);
                    startActivity(intent1);
                }
            } else if (MemberService.VIDEO_START_ACTION.equals(intent.getAction())) {
//                Intent intentVideo = new Intent(GroupActivity.this, VideoDemoActivity.class);
//                Log.d(TAG, "Broadcast : " + intent.getLongExtra("mediaLength", -1));
//                intentVideo.putExtra("mediaLength", intent.getLongExtra("mediaLength", -1));
//                intentVideo.putExtra("remoteUri", intent.getStringExtra("remoteUri"));
//                intentVideo.putExtra("data", intent.getIntExtra("data", 0));
//                startActivity(intentVideo);
            } else if (MemberService.DISCONNECT_FROM_PROXY.equals(intent.getAction())) {
                onDisConnectMessage(true);
            } else if (MemberService.CONNECT_TO_PROXY.equals(intent.getAction())) {
                onDisConnectMessage(false);
            }
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public CharSequence getPageTitle(int position) {
            // return null to display only the icon
            return null;
        }
    }
}
