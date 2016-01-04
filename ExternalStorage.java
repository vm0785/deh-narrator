package com.mmlab.n1;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by mmlab on 2015/9/25.
 */
public class ExternalStorage {

    private static final String TAG = "ExternalStorage";
    public static final String BASE_ROOT = Environment.getExternalStorageDirectory().getPath(); // 根目錄
    public static final String TARGET_DIRECTORY = "VideoCache";

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_AUDIO = 3;
    public static final int TYPE_NONE = 4;

    /**
     * 確認檔案是否有下載過
     *
     * @param url  檔案的網路連結
     * @param type 檔案類型
     * @return
     */
    public static boolean isFileExists(String url, int type) {
        File file = null;

        switch (type) {
            case TYPE_TEXT:
                file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + url);
                return file.exists();
            case TYPE_IMAGE:
                file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + url);
                return file.exists();
            case TYPE_VIDEO:
                file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + url);
                return file.exists();
            case TYPE_AUDIO:
                file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + url);
                return file.exists();
            default:
                return false;
        }
    }

    /**
     * 將文字、影片和聲音寫進SD卡中
     *
     * @param fileName 檔案名稱
     * @param data     檔案內容
     */
    public static void writeToSDcard(String fileName, byte[] data) {
        try {
            File file = null;

            Log.d(TAG, "file name : " + fileName);

            switch (Utils.fileType(fileName)) {
                case ExternalStorage.TYPE_TEXT:
                    file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + fileName);
                    break;
                case ExternalStorage.TYPE_IMAGE:
                    file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + fileName);
                    break;
                case ExternalStorage.TYPE_VIDEO:
                    file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + fileName);
                    break;
                case ExternalStorage.TYPE_AUDIO:
                    file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + fileName);
                    break;
                default:
            }
            // Get the abstract pathname of this abstract pathname's parent
            file.getParentFile().mkdirs();

            // Check whether it exists
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fout = new FileOutputStream(file);
            fout.write(data);
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 從SDcard中取得圖片或景點文字訊息
     * isText為True時讀取POI內容，False則為圖片
     *
     * @param filename
     * @return
     */
    public static byte[] readFromSDcard(String filename, boolean isText) {
        File file;
        if (isText) {
            file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + filename);
        } else {
            file = new File(BASE_ROOT + File.separator + TARGET_DIRECTORY + File.separator + filename);
        }

        byte[] data = new byte[(int) file.length()];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(data, 0, data.length);
            buf.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "FileNotFoundException: " + filename);
            e.printStackTrace();
        } catch (IOException e) {
            Log.i(TAG, "IOException: " + filename);
            e.printStackTrace();
        }
        return data;
    }
}
