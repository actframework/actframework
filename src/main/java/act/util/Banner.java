package act.util;

import act.Act;
import act.Zen;
import act.conf.ConfLoader;
import act.sys.Env;
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
        int width = width(s);
        StringBuilder sb = S.builder(s);
        if (null == appVersion) {
            int n = actVersion.length();
            int spaceLeft = (width - n + 1) / 2;
            for (int i = 0; i < spaceLeft; ++i) {
                sb.append(" ");
            }
            sb.append(actVersion).append("\n");
        } else {
            sb.append(poweredBy(width, actVersion));
            sb.append("\n\n version: ").append(appVersion);
        }
        File aFile = new File("");
        sb.append("\nbase dir: ").append(aFile.getAbsolutePath());
        sb.append("\n     pid: ").append(Env.PID.get());
        sb.append("\n profile: ").append(ConfLoader.confSetName());
        sb.append("\n    mode: ").append(Act.mode());
        sb.append("\n   group: ").append(Act.nodeGroup());
        sb.append("\n");
        sb.append("\n     zen: ").append(Zen.wordsOfTheDay());
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

    private static int width(String banner) {
        String[] lines = banner.split("\n");
        int max = 0;
        for (String s : lines) {
            max = Math.max(max, s.length());
        }
        return max;
    }

    private static String poweredBy(int width, String actVersion) {
        String poweredBy = "powered by ActFramework " + actVersion;
        int pw = poweredBy.length();
        int gap = width - pw;
        gap = Math.max(gap, 0);
        if (gap == 0) {
            return poweredBy;
        }
        return S.builder(S.times(" ", gap)).append(poweredBy).toString();
    }

    public static void main(String[] args) {
        print("ACT", null);
    }
}
