package com.mmlab.n1.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mmlab.n1.R;

public class GroupDialog extends DialogFragment {

    private EditText editText_group_name;
    private EditText editText_group_description;

    public interface GroupListener {
        void onGroupCreate(String group_name, String group_description);
    }

    public GroupDialog() {
        // Required empty public constructor
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    public void onDetach() {
        super.onDetach();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_group, null);
        editText_group_name = (EditText) view.findViewById(R.id.editText_name);
        editText_group_description = (EditText) view.findViewById(R.id.editText_description);

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.title_dialog_group)
                .customView(view, true)
                .positiveText(R.string.dismiss)
                .negativeText(R.string.confirm)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                        GroupListener listener = (GroupListener) getActivity();
                        listener.onGroupCreate(editText_group_name
                                .getText().toString(), editText_group_description
                                .getText().toString());
                    }
                })
                .contentLineSpacing(1.6f)
                .build();
    }
}
