package com.mmlab.n1;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.mmlab.n1.constant.IDENTITY;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.fragment.MemberListFragment;
import com.mmlab.n1.fragment.MemberMapFragment;
import com.mmlab.n1.network.MemberService;
import com.mmlab.n1.network.NetWorkUtils;
import com.mmlab.n1.network.ProxyService;

import java.util.ArrayList;
import java.util.List;

public class InformationActivity extends AppCompatActivity {

    private static final String TAG = "InformationActivity";
    private TabLayout tabLayout = null;
    private ViewPager viewPager = null;
    private int[] tabIcons = {
            R.drawable.ic_map,
            R.drawable.ic_list
    };
    private FragmentManager fragmentManager;

    public ProxyService mServer = null;
    public MemberService mClient = null;

    private ServerReceiver serverReceiver = null;
    private ClientReceiver clientReceiver = null;

    public static final int PAGE_MAP = 0;
    public static final int PAGE_LIST = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        fragmentManager = getFragmentManager();

        if (MSN.identity == IDENTITY.PROXY) {
            stopMemberService();
            startProxyService();
        } else {
            stopProxyService();
            startMemberService();
        }

//        Button button_proxy = (Button) findViewById(R.id.button_proxy);
//        button_proxy.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                stopMemberService();
//                startProxyService();
//            }
//        });

//        Button button_member = (Button) findViewById(R.id.button_member);
//        button_member.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                stopProxyService();
//                startMemberService();
//            }
//        });

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.test);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()...");
        if (MSN.identity == IDENTITY.PROXY) {
            try {
                onRegisteProxyReceiver();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                onRegisteMemberReceiver();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void onStop() {
        super.onStop();
        if (MSN.identity == IDENTITY.PROXY) {
            try {
                unregisterReceiver(serverReceiver);
                unbindService(serverConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                unregisterReceiver(clientReceiver);
                unbindService(clientConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()...");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_information, menu);
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
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupTabIcons() {
        Drawable icon = getResources().getDrawable(tabIcons[tabLayout.getSelectedTabPosition()]);
        icon.setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
        tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).setIcon(icon);

        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new MemberMapFragment(), "MEMBER_MAP");
        adapter.addFragment(new MemberListFragment(), "MEMBER_LIST");
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
    }


    public void onRegisteProxyReceiver() {
        serverReceiver = new ServerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ProxyService.MEMBER_LOCATION_UPDATE);
        intentFilter.addAction(ProxyService.MEMBER_JOIN_ACTION);
        intentFilter.addAction(ProxyService.MEMBER_LEAVE_ACTION);
        intentFilter.addAction(ProxyService.PROXY_LOCATION_UPDATE);
        registerReceiver(serverReceiver, intentFilter);
    }

    public void onRegisteMemberReceiver() {
        clientReceiver = new ClientReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MemberService.CONNECT_TO_PROXY);
        intentFilter.addAction(MemberService.DISCONNECT_FROM_PROXY);
        intentFilter.addAction(MemberService.GPS_DISABLE_ACTION);
        intentFilter.addAction(MemberService.GPS_ENABLE_ACTION);
        registerReceiver(clientReceiver, intentFilter);
    }

    public void startProxyService() {
        NetWorkUtils.setAPEnabledMethod(getApplicationContext(), true);

        Intent intent = new Intent(InformationActivity.this, ProxyService.class);
        startService(intent);
        Intent intent1 = new Intent(InformationActivity.this, ProxyService.class);
        bindService(intent1, serverConnection, BIND_AUTO_CREATE);

    }

    public void startMemberService() {
        NetWorkUtils.setWiFiEnabled(getApplicationContext(), true);

        Intent intent = new Intent(InformationActivity.this, MemberService.class);
        startService(intent);
        Intent intent1 = new Intent(InformationActivity.this, MemberService.class);
        bindService(intent1, clientConnection, BIND_AUTO_CREATE);

    }

    public void stopProxyService() {
        try {
            Intent intent = new Intent(InformationActivity.this, ProxyService.class);
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
            Intent intent = new Intent(InformationActivity.this, MemberService.class);
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
            Toast.makeText(InformationActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(InformationActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            ProxyService.ProxyBinder mLocalBinder = (ProxyService.ProxyBinder) service;
            mServer = mLocalBinder.getProxyInstance();
        }
    };

    ServiceConnection clientConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(InformationActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mClient = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(InformationActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            MemberService.MemberBinder mLocalBinder = (MemberService.MemberBinder) service;
            mClient = mLocalBinder.getMemberInstance();
        }
    };

    public class ServerReceiver extends BroadcastReceiver {

        public ServerReceiver() {

        }

        public void onReceive(Context context, Intent intent) {
            if (ProxyService.MEMBER_LOCATION_UPDATE.equals(intent.getAction())) {
                Log.d(TAG, "ProxyService.MEMBER_LOCATION_UPDATE");
                if (viewPager.getCurrentItem() == PAGE_MAP) {
                    Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem());
                    ((MemberMapFragment) page).updateMembers(mServer.getProfiles());
                }
            }
            if (ProxyService.MEMBER_JOIN_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "ProxyService.MEMBER_JOIN_ACTION");
                if (viewPager.getCurrentItem() == PAGE_LIST) {
                    Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem());
                    ((MemberListFragment) page).updateMembers(mServer.getProfiles());
                }
            }
            if (ProxyService.MEMBER_LEAVE_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "ProxyService.MEMBER_LEAVE_ACTION");
                if (viewPager.getCurrentItem() == PAGE_LIST) {
                    Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem());
                    ((MemberListFragment) page).updateMembers(mServer.getProfiles());
                }
            }
            if (ProxyService.PROXY_LOCATION_UPDATE.equals(intent.getAction())) {
                Log.d(TAG, "ProxyService.MEMBER_LOCATION_UPDATE");
                if (viewPager.getCurrentItem() == PAGE_MAP) {
                    Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem());
                    ((MemberMapFragment) page).updateProxy(intent.getStringExtra("location"));
                }
            }
        }
    }

    public class ClientReceiver extends BroadcastReceiver {

        public ClientReceiver() {

        }

        public void onReceive(Context context, Intent intent) {
            if (MemberService.CONNECT_TO_PROXY.equals(intent.getAction())) {
                Log.d(TAG, "MemberService.CONNECT_TO_PROXY");
            }
            if (MemberService.DISCONNECT_FROM_PROXY.equals(intent.getAction())) {
                Log.d(TAG, "MemberService.DISCONNECT_FROM_PROXY");
            }
            if (MemberService.GPS_DISABLE_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "MemberService.GPS_DISABLE_ACTION");
            }
            if (MemberService.GPS_ENABLE_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "MemberService.GPS_ENABLE_ACTION");
            }
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(android.support.v4.app.FragmentManager manager) {
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
