package com.mmlab.n1.info;

import com.mmlab.n1.Utils;
import com.mmlab.n1.constant.ENCRYPT;

/**
 * Created by mmlab on 2015/9/17.
 */
public class Group {
    public String FBID = "";
    public String username = "";
    public String SSID = "";
    public String SSIDpwd = "";
    public String encrypt = "";
    public int level = 0;
    public boolean isConnected = false;

    public Group(String FBID, String username, String SSID, String SSIDpwd, String encrypt, int level, boolean isConnected) {
        this.FBID = FBID;
        this.username = username;
        this.SSID = Utils.unornamatedSsid(SSID);
        this.SSIDpwd = SSIDpwd;
        this.encrypt = encrypt;
        this.level = level;
        this.isConnected = isConnected;
    }

    public Group() {
    }

    public int getWifiEncrypt() {
        if (encrypt.contains("WPA") || encrypt.contains("WPA2")) {
            return ENCRYPT.WPA;
        }

        if (encrypt.contains("WEP")) {
            return ENCRYPT.WEP;
        }

        return ENCRYPT.NOPASS;
    }

    public String getEncryptString() {
        String result = "";

        if (encrypt.equals("Connected") || encrypt.equals("Connecting") || encrypt.equals("Authenticating")) {
            result += encrypt;
            return result;
        }

        if (encrypt.contains("WPA")) {
            result += "[WPA]";
        }
        if (encrypt.contains("WPA2")) {
            result += "[WPA2]";
        }
        if (encrypt.contains("WEP")) {
            result += "[WEP]";
        }
        if (this.getWifiEncrypt() == ENCRYPT.NOPASS) {
            result = "[NONE]";
        }

        return result;
    }
}
