package com.mmlab.n1.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mmlab.n1.InformationActivity;
import com.mmlab.n1.R;
import com.mmlab.n1.adapter.MemberAdapter;
import com.mmlab.n1.decoration.DividerItemDecoration;
import com.mmlab.n1.info.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MemberListFragment extends Fragment {

    private static String TAG = "MemberListFragment";

    private RecyclerView mRecyclerView = null;
    private static List<Profile> mMembers = new ArrayList<Profile>();
    private static MemberAdapter mAdapter = null;

    private SwipeRefreshLayout swipeRefreshLayout = null;
    private View layout_member;

    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d(TAG, "setUserVisibleHint()..." + isVisibleToUser);
        if (isVisibleToUser) {
            InformationActivity activity = (InformationActivity) getActivity();
            if (activity.mServer != null)
                updateMembers(activity.mServer.getProfiles());
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        layout_member = inflater.inflate(R.layout.fragment_member_list, container, false);
       //  initData();
        mAdapter = new MemberAdapter(getActivity().getApplicationContext(), mMembers);
        mRecyclerView = (RecyclerView) layout_member.findViewById(R.id.recyclerView);
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
        mAdapter.setOnItemClickLitener(new MemberAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {

            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        // 設置Adapter
        mRecyclerView.setAdapter(mAdapter);

        swipeRefreshLayout = (SwipeRefreshLayout) layout_member.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        return layout_member;
    }

    public void initData() {
        for (int i = 0; i < 10; ++i) {
            Profile profile = new Profile(String.valueOf(i), String.valueOf(i), String.valueOf(i), String.valueOf(i), true);
            mMembers.add(profile);
        }
    }


    public void updateMembers(ConcurrentHashMap<String, Profile> hashMap) {
        List<Profile> list = new ArrayList<Profile>(hashMap.values());
        mMembers.clear();
        mMembers.addAll(list);
        mAdapter.notifyDataSetChanged();
    }
}
