package com.mmlab.n1.info;

import java.io.Serializable;

/**
 * Created by mmlab on 2015/9/22.
 */
public class Package implements Serializable {

    public static final int TAG_NONE = 0;
    public static final int TAG_COMMAND = 1;
    public static final int TAG_DATA = 2;

    public static final int SHOW_NONE = 0;
    public static final int SHOW_AUTO = 1;

    public static final int TYPE_NONE = 0;
    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_AUDIO = 2;
    public static final int TYPE_IMAGE = 3;
    public static final int TYPE_POI = 4;
    public static final int TYPE_POI_SIN = 5;
    public static final int TYPE_LOI = 6;
    public static final int TYPE_LOI_SIN = 7;
    public static final int TYPE_AOI = 8;
    public static final int TYPE_AOI_SIN = 9;
    public static final int TYPE_LOCATION = 10;
    public static final int TYPE_PROFILE = 11;
    private static final long serialVersionUID = -6863761269375960760L;

    public int tag = TAG_NONE;
    public int type = TYPE_NONE;
    public int show = SHOW_NONE;
    public String name = "";
    public byte[] payload;

    public Package(int tag, int type, String name, byte[] payload) {
        this.tag = tag;
        this.type = type;
        this.name = name;
        this.payload = payload;
    }

    public Package() {

    }
}
