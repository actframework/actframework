package act.util;

import org.osgl.util.S;

/**
 * ASCII arts for Act
 */
public class Banner {

    public static void print(String version) {
        System.out.println(banner(version));
    }

    private static String banner(String version) {
        String s = "    _    ____ _____ \n" +
                "   / \\  / ___|_   _|\n" +
                "  / _ \\| |     | |  \n" +
                " / ___ \\| |___  | |  \n" +
                "/_/   \\_\\____| |_|  \n";
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
