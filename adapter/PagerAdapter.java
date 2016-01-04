package com.mmlab.n1.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;


import com.mmlab.n1.PageFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mmlab on 2015/9/22.
 */
public class PagerAdapter extends FragmentPagerAdapter {
    final int PAGE_COUNT = 3;
    private String tabTitles[] = new String[]{"景點", "景線", "景區"};
    private Context context;
    private Map<Integer, Fragment> mPageReferenceMap = new HashMap<>();

    public PagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment pageFragment = PageFragment.newInstance(position + 1);
        mPageReferenceMap.put(position, pageFragment);
        return pageFragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        mPageReferenceMap.remove(position);
    }

    public PageFragment getFragment(int position) {
        return (PageFragment) mPageReferenceMap.get(position);
    }

    /**
     * add this in adapter, for after an orientation change
     *
     * @param container
     * @param position
     * @return
     */
    public Object instantiateItem(ViewGroup container, int position) {
        PageFragment fragment = (PageFragment) super.instantiateItem(container,
                position);
        mPageReferenceMap.put(position, fragment);
        return fragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
}