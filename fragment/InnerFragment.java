package com.mmlab.n1.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.mmlab.n1.R;


public class InnerFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_inner);
    }
}
