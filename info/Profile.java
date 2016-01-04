package com.mmlab.n1.info;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by mmlab on 2015/12/11.
 */
public class Profile implements Serializable {
    public String FB_NAME = "";
    public String PIC_URL = "";
    public String MAC_ADDRESS = "";
    public String IP_ADDRESS = "";
    public boolean isConnected = true;
    // 記錄成員位置
    public double longitude = 0;
    public double latitude = 0;

    public Profile() {

    }

    public Profile(String FB_NAME, String PIC_URL, String MAC_ADDRESS, String IP_ADDRESS, boolean isConnected) {
        this.FB_NAME = FB_NAME;
        this.PIC_URL = PIC_URL;
        this.MAC_ADDRESS = MAC_ADDRESS;
        this.IP_ADDRESS = IP_ADDRESS;
        this.isConnected = isConnected;
    }

    public String getJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("FB_NAME", FB_NAME);
            jsonObject.put("PIC_URL", PIC_URL);
            jsonObject.put("MAC_ADDRESS", MAC_ADDRESS);
            jsonObject.put("IP_ADDRESS", IP_ADDRESS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public void setProfile(String str) {
        try {
            JSONObject jsonObject = new JSONObject(str);
            this.FB_NAME = jsonObject.getString("FB_NAME");
            this.PIC_URL = jsonObject.getString("PIC_URL");
            this.IP_ADDRESS = jsonObject.getString("IP_ADDRESS");
            this.MAC_ADDRESS = jsonObject.getString("MAC_ADDRESS");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
