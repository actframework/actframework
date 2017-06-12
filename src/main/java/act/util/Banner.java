package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import act.Zen;
import act.conf.AppConfigKey;
import act.conf.ConfLoader;
import act.sys.Env;
import ascii.Image2ascii;
import com.github.lalyos.jfiglet.FigletFont;
import org.fusesource.jansi.Ansi;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * ASCII arts for Act
 */
public class Banner {

    private static String cachedBanner;

    public static void print() {
        String banner = banner();
        System.out.println(banner);
        cachedBanner = banner;
    }

    public static String cachedBanner() {
        return cachedBanner;
    }

    public static String banner() {
        String bannerText = null;

        String udfBanner = udfBanner();
        if (null != udfBanner) {
            bannerText = S.concat(udfBanner, "\n");
        }
        if (null == bannerText) {
            bannerText = asciiArt(Act.appName());
        }
        int bannerTextWidth = width(bannerText);

        String favicon = favicon();
        int faviconWidth = width(favicon);
        int maxWidth = Math.max(faviconWidth, bannerTextWidth);

        S.Buffer sb = S.buffer();
        String actVersion = Act.actVersion();
        if ("ACTFRAMEWORK".equals(Act.appName())) {
            sb.append(bannerText);
            if (S.notBlank(favicon)) {
                sb.append("\n");
                addFavicon(sb, favicon, maxWidth, faviconWidth);
            }
            int n = actVersion.length();
            int padLeft = (maxWidth - n + 1) / 2;
            sb.append(S.times(" ", padLeft)).append(actVersion).append("\n");
        } else {
            sb.append(bannerText);
            if (S.notBlank(favicon)) {
                sb.append("\n");
                addFavicon(sb, favicon, maxWidth, faviconWidth);
                sb.append("\n");
                sb.append(poweredBy(maxWidth, actVersion, true));
            } else {
                sb.append(poweredBy(maxWidth, actVersion, false));
            }
            sb.append("\n\n version: ").append(Act.appVersion());
        }
        File aFile = new File("");
        String group = Act.nodeGroup();
        sb.append("\nscan pkg: ").append(System.getProperty(AppConfigKey.SCAN_PACKAGE.key()));
        sb.append("\nbase dir: ").append(aFile.getAbsolutePath());
        sb.append("\n     pid: ").append(Env.PID.get());
        sb.append("\n profile: ").append(ConfLoader.confSetName());
        sb.append("\n    mode: ").append(Act.mode());
        if (S.notBlank(group)) {
            sb.append("\n   group: ").append(group);
        }
        sb.append("\n");
        sb.append("\n     zen: ").append(Ansi.ansi().a(Ansi.Attribute.ITALIC).a(Zen.wordsOfTheDay()).a(Ansi.Attribute.ITALIC_OFF));
        sb.append("\n");

        return sb.toString();
    }

    private static final String[] _BANNER_FONTS = {
            "banner3", "big", "doom", "marquee",
            "lcd", "mini", "slant", "small",
            "speed", "standard", "starwars",
    };

    private static void addFavicon(S.Buffer buffer, String favicon, int maxWidth, int faviconWidth) {
        if (S.blank(favicon)) {
            return;
        }
        int delta = maxWidth - faviconWidth;
        if (0 == delta) {
            buffer.append(favicon).append("\n");
        } else {
            int padLeft = (delta + 1) / 2;
            String[] lines = favicon.split("\n");
            for (String line : lines) {
                buffer.append(S.times(" ", padLeft)).append(line).append("\n");
            }
        }
    }

    private static String favicon() {
        boolean isIcon = true;
        URL url = Banner.class.getResource("/asset/favicon.png");
        if (null == url) {
            url = Banner.class.getResource("/asset/img/favicon.png");
            if (null == url) {
                url = Banner.class.getResource("/asset/image/favicon.png");
            }
        }
        if (null != url) {
            isIcon = false;
        } else {
            url = Banner.class.getResource("/asset/favicon.ico");
            if (null == url) {
                url = Banner.class.getResource("/asset/img/favicon.ico");
                if (null == url) {
                    url = Banner.class.getResource("/asset/image/favicon.ico");
                }
            }
        }
        if (null == url) {
            return "";
        }
        return removeEndingBlankLines(Image2ascii.render(url, true, isIcon));
    }

    private static String asciiArt(String s) {
        String font = System.getProperty("banner.font");
        if (null == font) {
            int len = s.length();
            if (len < 5) {
                font = "big";
            } else if (len < 7) {
                font = "standard";
            } else if (len < 10) {
                font = "small";
            } else {
                font = "mini";
            }
        } else if ("BianLian".equals(font)) {
            font = $.random(_BANNER_FONTS);
        }
        String path = font.endsWith(".flf") ? font : S.concat("/", font, ".flf");
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
            String bannerText = FigletFont.convertOneLine(is, s.toUpperCase());
            return S.concat(removeEndingBlankLines(bannerText), "\n");
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    private static String removeEndingBlankLines(String text) {
        int lastLineBreak = text.lastIndexOf("\n");
        boolean lastLineIsBlank = (S.isBlank(text.substring(lastLineBreak, text.length())));
        return lastLineIsBlank ? removeEndingBlankLines(text.substring(0, lastLineBreak)) : text;
    }

    private static int width(String banner) {
        String[] lines = banner.split("\n");
        int max = 0;
        for (String s : lines) {
            max = Math.max(max, s.length());
        }
        return max;
    }

    private static String poweredBy(int width, String actVersion, boolean center) {
        String raw = S.concat("powered by @|bold ActFramework|@ ", actVersion);
        String poweredBy = Ansi.ansi().render(raw).toString();
        int pw = raw.length() - 9;
        int gap = width - pw;
        gap = Math.max(gap, 0);
        if (gap == 0) {
            return poweredBy;
        }
        if (center) {
            gap = (gap + 1) / 2;
        }
        return S.concat(S.times(" ", gap), poweredBy);
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
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    private static String udfBanner() {
        URL url = Banner.class.getResource("/act_banner.txt");
        return null == url ? null : IO.readContentAsString(url);
    }

    private static void printArt(String s) {
        System.out.println(asciiArt(s));
    }
}
