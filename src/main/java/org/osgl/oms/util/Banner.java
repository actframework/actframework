package org.osgl.oms.util;

import org.osgl.util.S;

/**
 * ASCII arts for OMS
 */
public class Banner {

    public static void print(String version) {
        System.out.println(banner(version));
    }

    private static String banner(String version) {
        String s = "   ____    __  __    _____ \n" +
                "  / __ \\  |  \\/  |  / ____|\n" +
                " | |  | | | \\  / | | (___  \n" +
                " | |  | | | |\\/| |  \\___ \\ \n" +
                " | |__| | | |  | |  ____) |\n" +
                "  \\____/  |_|  |_| |_____/ \n";
        int n = version.length();
        int spaceLeft = (29 - n - 1) / 2;
        StringBuilder sb = S.builder(s).append("\n");
        for (int i = 0; i < spaceLeft; ++i) {
            sb.append(" ");
        }
        sb.append("v").append(version);
        sb.append("\n");
        sb.append("-----------------------------\n");
        return sb.toString();
    }
}
