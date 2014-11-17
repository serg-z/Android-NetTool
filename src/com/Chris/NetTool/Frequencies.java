package com.Chris.NetTool;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class Frequencies {
    public static final Map<Integer, Integer> sChannels;

    static {
        Map<Integer, Integer> tmp = new HashMap<Integer, Integer>();

        // 2.4 GHz
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

        // 5 GHz
        tmp.put(4915, 183);
        tmp.put(4920, 184);
        tmp.put(4925, 185);
        tmp.put(4935, 187);
        tmp.put(4940, 188);
        tmp.put(4945, 189);
        tmp.put(4960, 192);
        tmp.put(4980, 196);
        tmp.put(5035, 7);
        tmp.put(5040, 8);
        tmp.put(5045, 9);
        tmp.put(5055, 11);
        tmp.put(5060, 12);
        tmp.put(5080, 16);
        tmp.put(5170, 34);
        tmp.put(5180, 36);
        tmp.put(5190, 38);
        tmp.put(5200, 40);
        tmp.put(5210, 42);
        tmp.put(5220, 44);
        tmp.put(5230, 46);
        tmp.put(5240, 48);
        tmp.put(5260, 52);
        tmp.put(5280, 56);
        tmp.put(5300, 60);
        tmp.put(5320, 64);
        tmp.put(5500, 100);
        tmp.put(5520, 104);
        tmp.put(5540, 108);
        tmp.put(5560, 112);
        tmp.put(5580, 116);
        tmp.put(5600, 120);
        tmp.put(5620, 124);
        tmp.put(5640, 128);
        tmp.put(5660, 132);
        tmp.put(5680, 136);
        tmp.put(5700, 140);
        tmp.put(5745, 149);
        tmp.put(5765, 153);
        tmp.put(5785, 157);
        tmp.put(5805, 161);
        tmp.put(5825, 165);

        sChannels = Collections.unmodifiableMap(tmp);
    }
}
