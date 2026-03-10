package com.gardensmc.gardensplayers.util;

import java.util.HashMap;

public class FontShift {

    public static FontShift INSTANCE = new FontShift();

    // amount -> symbol
    private final HashMap<Integer, String> shiftsMapPos = new HashMap<>();
    private final HashMap<Integer, String> shiftsMapNeg = new HashMap<>();


    private final int[] pos = new int[]{1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1};

    public FontShift() {
        compileShifts();
    }

    private void compileShifts() {
        shiftsMapPos.put(1024, "\u0115");
        shiftsMapPos.put(512, "\u0114");
        shiftsMapPos.put(256, "\u0113");
        shiftsMapPos.put(128, "\u0112");
        shiftsMapPos.put(64, "\u0111");
        shiftsMapPos.put(32, "\u0110");
        shiftsMapPos.put(16, "\u010F");
        shiftsMapPos.put(8, "\u010E");
        shiftsMapPos.put(4, "\u010D");
        shiftsMapPos.put(2, "\u010C");
        shiftsMapPos.put(1, "\u010B");

        shiftsMapNeg.put(1024, "\u010A");
        shiftsMapNeg.put(512, "\u0109");
        shiftsMapNeg.put(256, "\u0108");
        shiftsMapNeg.put(128, "\u0107");
        shiftsMapNeg.put(64, "\u0106");
        shiftsMapNeg.put(32, "\u0105");
        shiftsMapNeg.put(16, "\u0104");
        shiftsMapNeg.put(8, "\u0103");
        shiftsMapNeg.put(4, "\u0102");
        shiftsMapNeg.put(2, "\u0101");
        shiftsMapNeg.put(1, "\u0100");
    }

    public String getShift(int shift) {
        boolean neg = shift < 0;
        shift = Math.abs(shift);
        StringBuilder stringBuilder = new StringBuilder();
        int r = shift;

        for (int i : pos) {
            if (i <= r) {
                if (neg) {
                    stringBuilder.append(shiftsMapNeg.get(i));
                }
                else {
                    stringBuilder.append(shiftsMapPos.get(i));
                }
                r -= i;
                if (r == 0) { // we're done
                    break;
                }
            }
        }
        return stringBuilder.toString();
    }

    public int getStringWidth(String s) {
        int width = 0;
        for (char c : s.toCharArray()) {
            width += getPixelWidth(c);
        }
        return width;
    }

    // for large character size
    private int getPixelWidth(char c) {
        int pixels;
        switch (c) {
            case '!', '.' -> pixels = 1;
            case ' ', 'I' -> pixels = 3;
            default -> pixels = 5;
        }
        pixels++; //account for space between letters
        return pixels;
    }

}

