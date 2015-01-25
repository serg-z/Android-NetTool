package com.Chris.NetTool;

import android.util.SparseIntArray;

import java.util.Map;
import java.util.Collections;

/*
 * Static constant map used to convert Wi-Fi frequencies to channels.
 */

public class Frequencies {
    public static final SparseIntArray sChannels = new SparseIntArray() {
        {
            // 2.4 GHz
            append(2412, 1);
            append(2417, 2);
            append(2422, 3);
            append(2427, 4);
            append(2432, 5);
            append(2437, 6);
            append(2442, 7);
            append(2447, 8);
            append(2452, 9);
            append(2457, 10);
            append(2462, 11);
            append(2467, 12);
            append(2472, 13);
            append(2484, 14);

            // 5 GHz
            append(4915, 183);
            append(4920, 184);
            append(4925, 185);
            append(4935, 187);
            append(4940, 188);
            append(4945, 189);
            append(4960, 192);
            append(4980, 196);
            append(5035, 7);
            append(5040, 8);
            append(5045, 9);
            append(5055, 11);
            append(5060, 12);
            append(5080, 16);
            append(5170, 34);
            append(5180, 36);
            append(5190, 38);
            append(5200, 40);
            append(5210, 42);
            append(5220, 44);
            append(5230, 46);
            append(5240, 48);
            append(5260, 52);
            append(5280, 56);
            append(5300, 60);
            append(5320, 64);
            append(5500, 100);
            append(5520, 104);
            append(5540, 108);
            append(5560, 112);
            append(5580, 116);
            append(5600, 120);
            append(5620, 124);
            append(5640, 128);
            append(5660, 132);
            append(5680, 136);
            append(5700, 140);
            append(5745, 149);
            append(5765, 153);
            append(5785, 157);
            append(5805, 161);
            append(5825, 165);
        }
    };
}
