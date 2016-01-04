package com.mmlab.n1;


import com.mmlab.n1.info.POI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mmlab on 2015/9/23.
 */
public class Utils {

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final String IMAGE_JPG = "jpg";
    private static final String VIDEO_MP4 = "mp4";
    private static final String AUDIO_MP3 = "mp3";

    public static String byteArrayToString(byte[] bytes) {
        return new String(bytes, UTF8_CHARSET);
    }

    public static byte[] stringToByteArray(String string) {
        return string.getBytes(UTF8_CHARSET);
    }

    public static boolean isVideo(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return extension.equals(VIDEO_MP4) || extension.toLowerCase().equals(VIDEO_MP4);
    }

    public static boolean isImage(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return extension.equals(IMAGE_JPG) || extension.toLowerCase().equals(IMAGE_JPG);
    }

    public static boolean isAudio(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        return extension.equals(AUDIO_MP3) || extension.toLowerCase().equals(AUDIO_MP3);
    }

    /**
     * 去除wifi SSID頭尾的雙引號
     * @return
     */
    public static  String unornamatedSsid(String ssid) {
        ssid = ssid.replaceFirst("^\"", "");
        return ssid.replaceFirst("\"$", "");
    }

    public static int fileType(String filename) {
        if (isImage(filename)) return ExternalStorage.TYPE_IMAGE;
        if (isVideo(filename)) return ExternalStorage.TYPE_VIDEO;
        if (isAudio(filename)) return ExternalStorage.TYPE_AUDIO;
        return ExternalStorage.TYPE_NONE;
    }

    /**
     * 處理單個Poi的資訊
     *
     * @param result
     * @return
     */
    public static POI parsePoiJSONObject(String result) {

        POI POI = new POI();
        JSONObject poiObject = null;
        try {
            poiObject = new JSONObject(result);
            POI.setJsonObject(poiObject);
            POI.setId(poiObject.getString("POI_id"));
            POI.setLatitude(Double.parseDouble(poiObject.getString("latitude")));
            POI.setLongtitude(Double.parseDouble(poiObject.getString("longitude")));
            POI.setDescript(poiObject.getString("POI_description"));
            POI.setName(poiObject.getString("POI_title"));
            POI.setSubject(poiObject.getString("subject"));
            POI.setAddress(poiObject.getString("POI_address"));
            POI.setPeriod(poiObject.getString("period"));

            String[] tmpKeyword = new String[5];
            tmpKeyword[0] = poiObject.getString("keyword1");
            tmpKeyword[1] = poiObject.getString("keyword2");
            tmpKeyword[2] = poiObject.getString("keyword3");
            tmpKeyword[3] = poiObject.getString("keyword4");
            tmpKeyword[4] = poiObject.getString("keyword5");
            POI.setKeyword(tmpKeyword);

            String[] tmpType = new String[2];
            tmpType[0] = poiObject.getString("type1");
            tmpType[1] = poiObject.getString("type2");
            POI.setType(tmpType);

            // 取得圖片url, 作為之後比對SDcard中是否有相同圖片的依據和ImageView的Tag
            JSONObject picsObject = poiObject.getJSONObject("PICs");
            JSONArray picArray = picsObject.getJSONArray("pic");

            List<String> tmpUrl = new ArrayList<String>();
            List<String> imgUrl = new ArrayList<String>();
            List<String> videoUrl = new ArrayList<String>();
            List<String> audioUrl = new ArrayList<String>();
            for (int j = 0; j < picArray.length(); ++j) {
                JSONObject urlObject = picArray.getJSONObject(j);

                String url = urlObject.getString("url");
                tmpUrl.add(url);

                if (Utils.fileType(url) == ExternalStorage.TYPE_IMAGE) {
                    imgUrl.add(url);
                } else if (Utils.fileType(url) == ExternalStorage.TYPE_VIDEO) {
                    videoUrl.add(url);
                } else if (Utils.fileType(url) == ExternalStorage.TYPE_AUDIO) {
                    audioUrl.add(url);
                }
            }
            POI.setUrl(tmpUrl);
            POI.setImgUrl(imgUrl);
            POI.setAudioUrl(audioUrl);
            POI.setVideoUrl(videoUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return POI;
    }

    /**
     * 處理多個Poi的資訊並存成列表回傳
     *
     * @param result
     * @return
     */
    public static ArrayList<POI> parsePoisJSONObject(String result) {
        ArrayList<POI> list = new ArrayList<POI>();

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(result);

            JSONArray array = jsonObject.getJSONArray("results");
            for (int i = 0; i < array.length(); i++) {
                JSONObject poiObject = array.getJSONObject(i);
                list.add(parsePoiJSONObject(poiObject.toString()));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 將url轉換成檔案名稱
     *
     * @param url
     * @return
     */
    public static String urlToFilename(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
