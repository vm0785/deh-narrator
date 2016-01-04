package com.mmlab.n1.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.mmlab.n1.R;
import com.mmlab.n1.constant.ENCRYPT;
import com.mmlab.n1.constant.MSN;
import com.mmlab.n1.info.Group;


/**
 * 建立Sender與Receiver之間的連線
 * Created by mmlab on 2015/9/18.
 */
public class ConnectionManager {

    private static String TAG = "ConnectionManager";

    public static AlertDialog createConfiguredDialog(final WirelessManager wirelessManager, final Activity activity, final Group group) {

        // get prompts.xml view
    LayoutInflater layoutInflater = LayoutInflater.from(activity);
    View promptView = layoutInflater.inflate(R.layout.configured_dialog, null);
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
    alertDialogBuilder.setView(promptView);

    final TextView securityshowTextView = (TextView) promptView.findViewById(R.id.securityshow_textView);
    final TextView passwordTextView = (TextView) promptView.findViewById(R.id.password_textView);
    final EditText passwordshowEditView = (EditText) promptView.findViewById(R.id.passwordshow_editText);

    if (group.getWifiEncrypt() == ENCRYPT.NOPASS) {
        passwordTextView.setVisibility(View.GONE);
        passwordshowEditView.setVisibility(View.GONE);
    }
    // 設置安全性
    securityshowTextView.setText(group.getEncryptString());

    // 建立對話窗視窗
    alertDialogBuilder.setTitle(group.SSID);

    alertDialogBuilder.setPositiveButton("連線", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {

            if (passwordTextView.getVisibility() == passwordTextView.GONE) {
                WifiConfiguration existingConfig = wirelessManager.isExsits(group.SSID);

                if (existingConfig != null && existingConfig.status == 0) {
                    // current connected device
                } else if (existingConfig == null && group.getWifiEncrypt() == ENCRYPT.NOPASS) {
                    // not in the configured list
                    wirelessManager.disableNetwork();
                    wirelessManager.connectConfigured(group.SSID);
                } else if (!group.SSIDpwd.equals("")) {
                    MSN.proxy_password = group.SSIDpwd;
                    MSN.proxy_ssid = group.SSID;
                    wirelessManager.disableNetwork();
                    wirelessManager.connectUnconfigured(group.SSID, group.SSIDpwd, group.getWifiEncrypt());
                } else {
                    passwordTextView.setVisibility(View.VISIBLE);
                    passwordshowEditView.setVisibility(View.VISIBLE);
                }
            } else {
                // 檢查輸入是否為空或空白
                if (!(group.getWifiEncrypt() == ENCRYPT.NOPASS) && passwordshowEditView.getText() != null && passwordshowEditView.getText().toString().trim() != "") {
                    // 結束當前連線後，進行認證並連線
                    wirelessManager.disableNetwork();
                    wirelessManager.connectUnconfigured(group.SSID, passwordshowEditView.getText().toString().trim(), group.getWifiEncrypt());
                    Log.d(TAG, "password " + passwordshowEditView.getText());
                    MSN.proxy_password = group.SSIDpwd;
                    MSN.proxy_ssid = group.SSID;
                } else {
                    // 不做任何事
                }
            }
        }
    });

    alertDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
        }
    });

    // 創建對話窗視窗
    return alertDialogBuilder.create();
}

    public static AlertDialog createConnectedDialog(final WirelessManager wirelessManager, final Activity activity, final Group group) {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View promptView = layoutInflater.inflate(R.layout.configured_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setView(promptView);

        final TextView securityshowTextView = (TextView) promptView.findViewById(R.id.securityshow_textView);
        final TextView passwordTextView = (TextView) promptView.findViewById(R.id.password_textView);
        final EditText passwordshowEditView = (EditText) promptView.findViewById(R.id.passwordshow_editText);
        passwordTextView.setVisibility(View.GONE);
        passwordshowEditView.setVisibility(View.GONE);

        // 設置安全性
        securityshowTextView.setText(group.getEncryptString());

        // 建立對話窗視窗
        alertDialogBuilder.setTitle(group.SSID);
        alertDialogBuilder.setPositiveButton("離開", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                wirelessManager.clear(group.SSID);
            }
        });

        alertDialogBuilder.setNeutralButton("查看", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
//                Intent intent = new Intent(activity, GroupActivity.class);
//                activity.startActivity(intent);
            }
        });

        alertDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // 創建對話窗視窗
        return alertDialogBuilder.create();
    }
}
