package act.controller.captcha.render;

import act.controller.captcha.generator.RandomTextGenerator;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Singleton;

@Singleton
public class FontProvider extends $.F1<String, Font> {

    private static final int MIN_SIZE = 28;
    private static final int MAX_SIZE = 36;

    private C.List<String> families = C.newList();

    public FontProvider() {
        String[] fontNames = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String name : fontNames) {
            if (goodToUse(name)) {
                families.add(name);
            }
        }
    }

    @Override
    public Font apply(String text) throws NotAppliedException, Lang.Break {
        Random r = ThreadLocalRandom.current();
        int style = randomStyle(r);
        String fontName = $.random(families);
        int size = MIN_SIZE + r.nextInt(MAX_SIZE - MIN_SIZE);
        return new Font(fontName, style, size);
    }

    private int randomStyle(Random r) {
        int style = Font.PLAIN;
        if (r.nextBoolean()) {
            style = Font.BOLD;
        }
        if (r.nextBoolean()) {
            style |= Font.ITALIC;
        }
        return style;
    }

    private boolean goodToUse(String fontFamily) {
        Font font = new Font(fontFamily, Font.PLAIN, 10);
        return -1 == font.canDisplayUpTo(RandomTextGenerator.SPACE);
    }
}
