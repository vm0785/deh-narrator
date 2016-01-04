package com.mmlab.n1.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mmlab.n1.R;
import com.mmlab.n1.network.NetWorkUtils;
import com.mmlab.n1.network.NetworkManager;


public class NetworkDialog extends DialogFragment {

    private Switch switch_wifi = null;
    private Switch switch_mobile = null;
    private Switch switch_hotspot = null;

    public NetworkDialog() {
        // Required empty public constructor
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void onDetach() {
        super.onDetach();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_network, null);
        switch_wifi = (Switch) view.findViewById(R.id.switch_wifi);
        switch_wifi.setChecked(NetWorkUtils.isWiFiEnabled(getActivity()));
        switch_wifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    NetWorkUtils.setWiFiEnabled(getActivity(), true);
                    switch_hotspot.setChecked(false);
                } else {
                    NetWorkUtils.setWiFiEnabled(getActivity(), false);
                }
            }
        });
        switch_mobile = (Switch) view.findViewById(R.id.switch_mobile);
        switch_mobile.setChecked(NetWorkUtils.isMobileEnabled(getActivity()));
        switch_mobile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    NetWorkUtils.setMobileDataEnabledMethod1(getActivity(), true);
                } else {
                    NetWorkUtils.setMobileDataEnabledMethod1(getActivity(), false);
                }
            }
        });
        switch_hotspot = (Switch) view.findViewById(R.id.switch_hotspot);
        switch_hotspot.setChecked(NetWorkUtils.isAPEnabled(getActivity()));
        switch_hotspot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    NetWorkUtils.setAPEnabledMethod(getActivity(), true);
                    switch_wifi.setChecked(false);
                } else {
                    NetWorkUtils.setAPEnabledMethod(getActivity(), false);
                }
            }
        });

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.title_dialog_network)
                .customView(view, true)
                .positiveText(R.string.dismiss)
                .contentLineSpacing(1.6f)
                .build();
    }
}
