package act.util;

import act.Act;
import com.github.lalyos.jfiglet.FigletFont;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.IOException;

/**
 * ASCII arts for Act
 */
public class Banner {

    private static String cachedBanner;

    public static void print(String appName, String appVersion) {
        String banner;
        if (S.anyBlank(appName, appVersion)) {
            banner = banner("ACT", Act.VERSION, null);
        } else {
            banner = banner(appName, Act.VERSION, appVersion);
        }
        System.out.println(banner);
        cachedBanner = banner;
    }

    public static String cachedBanner() {
        return cachedBanner;
    }

    public static String banner(String text, String actVersion, String appVersion) {
        String s = asciiArt(text);
        StringBuilder sb = S.builder(s);
        if (null == appVersion) {
            int n = actVersion.length();
            int spaceLeft = (24 - n - 1) / 2;
            for (int i = 0; i < spaceLeft; ++i) {
                sb.append(" ");
            }
            sb.append(actVersion);
        } else {
            sb.append(" App: ").append(appVersion);
            sb.append("\n Act: ").append(actVersion);
        }
        sb.append("\n");
        File aFile = new File("");
        sb.append("current dir: ").append(aFile.getAbsolutePath());
        sb.append("\n");
        return sb.toString();
    }

    private static String asciiArt(String s) {
        try {
            return FigletFont.convertOneLine(s);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static void main(String[] args) {
        print("ACT", null);
    }
}
