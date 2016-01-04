package com.mmlab.n1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.mmlab.n1.adapter.SiteAdapter;
import com.mmlab.n1.constant.IDENTITY;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.decoration.DividerItemDecoration;
import com.mmlab.n1.info.POI;

import java.util.ArrayList;
import java.util.List;

public class PageFragment extends Fragment {
    public static final String ARG_PAGE = "ARG_PAGE";
    private String titles[] = new String[]{"景點", "景線", "景區"};

    private int mPage;
    private RecyclerView mRecyclerView = null;
    private List<POI> mSites = new ArrayList<>();
    private SiteAdapter sAdapter = null;


    public static PageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        PageFragment fragment = new PageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page, container, false);
        // 創建MainAdapter實例
        sAdapter = new SiteAdapter(getActivity().getApplicationContext(), mSites);
        // 取得RecyclerView實例
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        // 設置布局管理器
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        // 設置Item增加移除動畫
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        // 設置分割線
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        // 設置ClickListener
        // 根據每個Group的連現狀態會有不同的Dialog
        sAdapter.setOnItemClickLitener(new SiteAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                if (MSN.identity == IDENTITY.PROXY) {
                  //  ((GroupActivity) getActivity()).sendSinglePOI(position);

                    Intent intent = new Intent(getActivity(), POIActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("poi", mSites.get(position));
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else if (MSN.identity == IDENTITY.MEMBER) {
                    Intent intent = new Intent(getActivity(), POIActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("poi", mSites.get(position));
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        // 設置Adapter
        mRecyclerView.setAdapter(sAdapter);

        return view;
    }

    public void updateSites(List<POI> list) {
        mSites.clear();
        mSites.addAll(list);
        sAdapter.notifyDataSetChanged();
    }

    public void showSite(POI poi){
        Intent intent = new Intent(getActivity(), POIActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("poi", poi);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}