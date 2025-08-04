package com.gardensmc.gardensplayers.util;

public class MathUtil {

    public static double round(double num) {
        return Math.round(num * 100.0) / 100.0;
    }

    public static float round(float num) {
        return Math.round(num * 100.0f) / 100.0f;
    }
}
