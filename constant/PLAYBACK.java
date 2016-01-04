package com.mmlab.n1.constant;

/**
 * 當前多媒體檔案播放
 * Created by mmlab on 2015/10/14.
 */
public class PLAYBACK {
    public volatile static String remoteUri = "";
    public volatile static long mediaLength = -1;
    public volatile static boolean isReady = false;
    public volatile static long readSize = 0;
    public volatile static int errorCount = 0;
    public volatile static boolean isDownloaded = false;
    public volatile static int currentPosition = 0;
    public volatile static boolean isError = false;
    public volatile static long session = 0;
}
