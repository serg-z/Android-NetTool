package com.Chris.NetTool;

public class Utils {
    public static native String wlan0DriverDesc();

    static {
        System.loadLibrary("nettool");
    }
}
