package com.mmlab.n1.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mmlab.n1.MainActivity;
import com.mmlab.n1.R;

public class ExitDialog extends DialogFragment {

    public interface GroupListener {
        void onGroupCreate(String group_name, String group_description);
    }

    public ExitDialog() {
        // Required empty public constructor
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    public void onDetach() {
        super.onDetach();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new MaterialDialog.Builder(getActivity()).title("離開DEH")
                .content("確定要離開")
                .positiveText("確定")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                        ((MainActivity)getActivity()).exitDEH();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {

                    }
                })
                .negativeText("取消").build();
    }
}
