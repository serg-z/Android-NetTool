package com.Chris.NetTool;

public class Utils {
    public static native String wlan0DriverDesc();
    public static native String dumpStats();

    static {
        System.loadLibrary("nettool");
    }
}
