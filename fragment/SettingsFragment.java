package com.mmlab.n1.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mmlab.n1.R;


public class SettingsFragment extends Fragment {

    private View layout_setting = null;
    private Button button_login = null;


    public SettingsFragment() {
        // Required empty public constructor
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        layout_setting = inflater.inflate(R.layout.fragment_settings, container, false);
        button_login = (Button) layout_setting.findViewById(R.id.button_login);
        button_login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LoginDialog dialog = new LoginDialog();
                dialog.show(getActivity().getFragmentManager(), "loginDialog");
            }
        });
        return layout_setting;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void onDetach() {
        super.onDetach();
    }
}
