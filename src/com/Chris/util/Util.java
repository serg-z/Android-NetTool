package com.Chris.util;

import android.app.Activity;

import android.graphics.PointF;

import android.content.Context;

import android.media.ToneGenerator;
import android.media.AudioManager;

import com.androidplot.util.PixelUtils;

public final class Util {
    private Util() {}

    // cross product
    public static float cross(PointF a, PointF b) {
        return a.x * b.y - a.y * b.x;
    }

    // intersection point of two segments (or null if segments are collinear)
    public static PointF intersection(PointF a0, PointF a1, PointF b0, PointF b1) {
        PointF v0 = PixelUtils.sub(a1, a0);
        PointF v1 = PixelUtils.sub(b1, b0);

        float n = cross(PixelUtils.sub(a0, b0), v1);
        float m = cross(v1, v0);

        if (Math.abs(m) < 1e-8) {
            return null;
        }

        float u = n / m;

        if (Math.abs(u) < 1e-8) {
            return null;
        }

        v1.x *= u;
        v1.y *= u;

        return PixelUtils.add(b0, v1);
    }

    public static void playTone(Activity activity, int toneType, int durationMs) {
        AudioManager audioManager = (AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);

        final int notificationMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        final int notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        final int beepVolume = (int)(((float)notificationVolume * 100) / notificationMaxVolume);

        new ToneGenerator(AudioManager.STREAM_ALARM, beepVolume)
            .startTone(toneType, durationMs);
    }
}
