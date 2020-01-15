package ascii;

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

import net.sf.image4j.codec.ico.ICODecoder;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.Keyword;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

// Adapted from https://github.com/netwinder-dev/ASCIIConversion/blob/master/src/ASCIIConvert/ASCIIConversions.java
public class Image2ascii {

    private static final int MAX_WIDTH = 80;

    private int[] statsArray = new int[12];
    char[] convRefArray = {' ', '~', '-', ':', '+', '%', '=', 'W', '@', '$', '#', '▆'};
    char[] imgArray;
    private long dStart;
    private long dEnd;

    /**
     * The main conversion method.
     * @param image A BufferedImage containing the image that needs to be converted
     * @param favicon When ico is true it will convert ico file into 16 x 16 size
     * @return A string containing the ASCII version of the original image.
     */
    public String convert(BufferedImage image, boolean favicon) {
        // Reset statistics before anything
        statsArray = new int[12];
        // Begin the timer
        dStart = System.nanoTime();
        // Scale the image
        image = scale(image, favicon);
        // The +1 is for the newline characters
        StringBuilder sb = new StringBuilder((image.getWidth() + 1) * image.getHeight());

        for (int y = 0; y < image.getHeight(); y++) {
            // At the end of each line, add a newline character
            if (sb.length() != 0) sb.append("\n");
            for (int x = 0; x < image.getWidth(); x++) {
                //
                Color pixelColor = new Color(image.getRGB(x, y), true);
                int alpha = pixelColor.getAlpha();
                boolean isTransient = alpha < 0.1;
                double gValue = isTransient ? 250 : ((double) pixelColor.getRed() * 0.2989 + (double) pixelColor.getBlue() * 0.5870 + (double) pixelColor.getGreen() * 0.1140) / ((double)alpha / (double)250);
                final char s = gValue < 130 ? darkGrayScaleMap(gValue) : lightGrayScaleMap(gValue);
                sb.append(s);
            }
        }
        imgArray = sb.toString().toCharArray();
        dEnd = System.nanoTime();
        return sb.toString();
    }


    /**
     * Image scale method
     * @param imageToScale The image to be scaled
     * @param dWidth Desired width, the new image object is created to this size
     * @param dHeight Desired height, the new image object is created to this size
     * @param fWidth What to multiply the width by. value < 1 scales down, and value > one scales up
     * @param fHeight What to multiply the height by. value < 1 scales down, and value > one scales up
     * @return A scaled image
     */
    private static BufferedImage scale(BufferedImage imageToScale, int dWidth, int dHeight, double fWidth, double fHeight) {
        BufferedImage dbi = null;
        // Needed to create a new BufferedImage object
        int imageType = imageToScale.getType();
        if (imageToScale != null) {
            dbi = new BufferedImage(dWidth, dHeight, imageType);
            Graphics2D g = dbi.createGraphics();
            AffineTransform at = AffineTransform.getScaleInstance(fWidth, fHeight);
            g.drawRenderedImage(imageToScale, at);
        }
        return dbi;
    }

    private char darkGrayScaleMap(double g) {
        char str;
        if (g >= 120.0) {
            str = '=';
        } else if (g >= 100.0) {
            str = 'W';
        } else if (g >= 80.0) {
            str = '@';
        } else if (g >= 70.0) {
            str = '$';
        } else if (g >= 30.0) {
            str = '#';
        } else if (g >= 12) {
            str = '▓';
        } else {
            str = '¶';
        }
        return str;
    }

    private char lightGrayScaleMap(double g) {
        char str;
        // Light
        if (g >= 240.0) {
            str = ' ';
        } else if (g >= 220) {
            str = '~';
        } else if (g >= 200.0) {
            str = '-';
        } else if (g >= 180.0) {
            str = ':';
        } else if (g >= 160.0) {
            str = '+';
        } else {
            str = '%';
        }
        return str;
    }

    private static BufferedImage scale(BufferedImage original, boolean favicon) {
        $.T3<Integer, Integer, Double> params = calcScaleParams(original, favicon);
        return scale(original, params._1, params._2 / 2, params._3, params._3 / 2);
    }

    private static $.T3<Integer, Integer, Double> calcScaleParams(BufferedImage original, boolean favicon) {
        int width = original.getWidth(), height = original.getHeight();
        int max = favicon ? 16 : MAX_WIDTH;
        if (width <= max) {
            return $.T3(width, height, 1.0d);
        }
        double factor = ((double) max / (double) width);
        return $.T3(max, (int)(height * factor), factor);
    }

    public static String render(File imageSource, boolean favicon) {
        try {
            URL url = imageSource.toURI().toURL();
            boolean isIcon = imageSource.getName().endsWith(".ico");
            return render(url, favicon, isIcon);
        } catch (MalformedURLException e) {
            // this is never gonna happen
            throw E.unexpected(e);
        }
    }

    public static String render(URL imageSource, boolean iconFile) {
        return render(imageSource, false, iconFile);
    }

    public static String render(URL imageSource, boolean favicon, boolean iconFile) {
        try {
            BufferedImage image = read(imageSource, iconFile);
            return new Image2ascii().convert(image, favicon);
        } catch (Exception e) {
            return "";
        }
    }

    private static BufferedImage read(URL imageSource, boolean isIcon) throws Exception {
        if (isIcon) {
            java.util.List<BufferedImage> images = ICODecoder.read(imageSource.openStream());
            return images.get(0);
        } else {
            return ImageIO.read(imageSource);
        }
    }


}
