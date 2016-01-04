package com.mmlab.n1.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mmlab.n1.R;

public class VideoRetainFragment extends Fragment {

    private boolean isPlaying = false;
    private boolean isMaximum = false;
    private boolean isMinimum = false;
    private boolean canBe = false;

    public boolean isCanBe() {
        return canBe;
    }

    public void setCanBe(boolean canBe) {
        this.canBe = canBe;
    }

    public boolean isMaximum() {
        return isMaximum;
    }

    public void setIsMaximum(boolean isMaximum) {
        this.isMaximum = isMaximum;
    }

    public boolean isMinimum() {
        return isMinimum;
    }

    public void setIsMinimum(boolean isMinimum) {
        this.isMinimum = isMinimum;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setIsClosed(boolean isClosed) {
        this.isClosed = isClosed;
    }

    private boolean isClosed = false;
    private String remoteUri = "";
    private long currentPosition = 0;

    public String getRemoteUri() {
        return remoteUri;
    }

    public void setRemoteUri(String remoteUri) {
        this.remoteUri = remoteUri;
    }

    public long getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(long currentPosition) {
        this.currentPosition = currentPosition;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
