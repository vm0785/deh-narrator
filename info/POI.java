package com.mmlab.n1.info;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class POI implements Serializable {

    private static final long serialVersionUID = 7543513388757919078L;
    private String id;
    private String name;
    private String descript;
    private String address;
    private String period;
    private double latitude;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(double longtitude) {
        this.longtitude = longtitude;
    }

    private double longtitude;
    private String[] keyword = new String[5];
    private String[] type = new String[2];
    private String subject;
    /**
     * 連結包括圖片影像聲音
     */
    private List<String> url = new ArrayList<String>();
    private List<String> imgUrl = new ArrayList<String>();
    private List<String> videoUrl = new ArrayList<String>();
    private List<String> audioUrl = new ArrayList<String>();
    private String jsonObject = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getKeyword() {
        return keyword;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescript() {
        return descript;
    }

    public List getUrl() {
        return url;
    }

    public JSONObject getJsonObject() {
        try {
            return new JSONObject(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public void setDescript(String descript) {
        this.descript = descript;
    }

    public List<String> getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(List<String> imgUrl) {
        this.imgUrl = imgUrl;
    }

    public List<String> getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(List<String> videoUrl) {
        this.videoUrl = videoUrl;
    }

    public List<String> getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(List<String> audioUrl) {
        this.audioUrl = audioUrl;
    }

    public void setKeyword(String[] keyword) {
        this.keyword = keyword;
    }

    public void setType(String[] type) {
        this.type = type;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setUrl(List url) {
        this.url.addAll(url);
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject.toString();
    }
}
