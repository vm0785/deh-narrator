package com.mmlab.n1.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mmlab.n1.MainActivity;
import com.mmlab.n1.R;
import com.mmlab.n1.constant.IDENTITY;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.network.ProxyService;
import com.mmlab.n1.preference.Preset;


public class IdentityDialog extends DialogFragment {

    public static  final String TAG = "IdentityDialog";

    public IdentityDialog() {
        // Required empty public constructor
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void onDetach() {
        super.onDetach();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "identity : " + Preset.loadPreferences(getActivity()));
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.title_dialog_identity)
                .items(R.array.item_identity)
                .itemsCallbackSingleChoice(Preset.loadPreferences(getActivity()), new MaterialDialog.ListCallbackSingleChoice() {
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        Toast.makeText(getActivity(), which + ": " + text, Toast.LENGTH_SHORT).show();
                        if (which == 0) {
                            Preset.savePreferences(getActivity(), IDENTITY.PROXY);
                            MSN.identity = IDENTITY.PROXY;
                            ((MainActivity) getActivity()).stopMemberService();
                            ((MainActivity) getActivity()).startProxyService();
                        } else {
                            Preset.savePreferences(getActivity(), IDENTITY.MEMBER);
                            MSN.identity = IDENTITY.MEMBER;
                            ((MainActivity) getActivity()).stopProxyService();
                            ((MainActivity) getActivity()).startMemberService();
                        }
                        return true;
                    }
                })
                .positiveText(R.string.confirm)
                .contentLineSpacing(1.6f)
                .build();
    }
}
