package com.Chris.NetTool;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class Frequencies {
    public static final Map<Integer, Integer> sChannels;

    static {
        Map<Integer, Integer> tmp = new HashMap<Integer, Integer>();

        tmp.put(2412, 1);
        tmp.put(2417, 2);
        tmp.put(2422, 3);
        tmp.put(2427, 4);
        tmp.put(2432, 5);
        tmp.put(2437, 6);
        tmp.put(2442, 7);
        tmp.put(2447, 8);
        tmp.put(2452, 9);
        tmp.put(2457, 10);
        tmp.put(2462, 11);
        tmp.put(2467, 12);
        tmp.put(2472, 13);
        tmp.put(2484, 14);

        sChannels = Collections.unmodifiableMap(tmp);
    }
}
