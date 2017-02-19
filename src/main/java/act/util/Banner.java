package act.util;

import act.Act;
import act.Zen;
import act.conf.ConfLoader;
import act.sys.Env;
import com.github.lalyos.jfiglet.FigletFont;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
        S.Buffer sb = S.buffer(s);
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

    private static final String[] _BANNER_FONTS = {
            "banner3", "big", "marquee",
            "mini", "slant", "small", "speed",
            "standard", "starwars",
    };

    private static String asciiArt(String s) {
        String font = System.getProperty("banner.font");
        if (null == font) {
            int len = s.length();
            if (len < 5) {
                font = "big";
            } else if (len < 9) {
                font = "standard";
            } else if (len < 13) {
                font = "small";
            } else {
                font = "mini";
            }
        } else if ("BianLian".equals(font)) {
            font = $.random(_BANNER_FONTS);
        }
        String path = font.endsWith(".flf") ? font : S.builder("/").append(font).append(".flf").toString();
        File file = new File(path);
        if (file.exists() && file.canRead()) {
            try {
                return FigletFont.convertOneLine(file, s.toUpperCase());
            } catch (IOException e) {
                throw E.ioException(e);
            }
        }
        InputStream is = Banner.class.getResourceAsStream(path);
        if (null == is) {
            is = Banner.class.getResourceAsStream("/standard.flf");
        }
        try {
            return FigletFont.convertOneLine(is, s.toUpperCase());
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
        List<String> fail = C.newList();
        for (String font : _BANNER_FONTS) {
            if (!test(font)) {
                fail.add(font);
            }
        }
        if (!fail.isEmpty()) {
            System.out.println(">>>>>Failed:");
            System.out.println(S.join(",", fail));
        }
    }

    private static boolean test(String font) {
        System.setProperty("banner.font", font);
        try {
            System.out.println("================= " + font + "==================");
            printArt("ABCDEFGHIJKLM");
            printArt("NOPQRSTUVWXYZ");
            printArt("1234567890");
            System.out.println("\n\n\n");
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
        return true;
    }

    private static void printArt(String s) {
        System.out.println(asciiArt(s));
    }
}
