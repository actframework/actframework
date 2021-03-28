package act;

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

import act.apidoc.Description;
import act.app.ActionContext;
import act.controller.ExpressController;
import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/**
 * Wisdom quotes about programming.
 *
 * The list come from:
 * * https://phauer.com/2020/wall-coding-wisdoms-quotes/
 * * https://zen-of-python.info/
 */
@Singleton
@ExpressController
public final class Zen {

    private static final List<String> WORDS = loadQuotes(false);
    private static final List<String> WORDS_FOR_BANNER = loadQuotes(true);

    private Info.Unit zenInfo;

    public Zen() {
        String zenTxt = S.join("\n\n", WORDS);
        zenInfo = new Info.Unit("zen", zenTxt);
    }

    public static String wordsOfTheDay(boolean forBanner) {
        return $.random(forBanner ? WORDS_FOR_BANNER : WORDS);
    }

    @GetAction("zen")
    @Description("Get my zen words (禅语)")
    public void zen(ActionContext context) {
        zenInfo.applyTo(context);
    }

    private static List<String> loadQuotes(boolean forBanner) {
        URL url = Act.getResource("act_zen.txt");
        List<String> words = C.newList(defaultQuotes());
        if (null != url) {
            try {
                List<String> myWords = IO.readLines(url.openStream());
                if (!myWords.isEmpty()) {
                    words = myWords;
                }
            } catch (Exception e) {
                // ignore it
            }
        }
        List<String> retVal = new ArrayList<>(words.size());
        for (String s : words) {
            if (forBanner) {
                if (s.contains("\n")) {
                    s = s.replaceAll("\n", "\n          ");
                } else if (s.contains("\\n")) {
                    s = s.replaceAll("\\\\n", "\n          ");
                }
            }
            retVal.add(S.join("\n", processQuote(s, forBanner)));
        }
        return retVal;
    }

    private static List<String> processQuote(String quote, boolean forBanner) {
        if (quote.indexOf('―') < 0) {
            return S.fastSplit(quote, "\n");
        }
        S.Pair pair = S.binarySplit(quote, '―');
        String source = pair._2;
        S.List lines = S.fastSplit(pair._1, "\n");
        int maxLen = 0;
        for (String line: lines) {
            maxLen = Math.max(maxLen, line.length());
        }
        final int bannerGap = forBanner ? "          ".length() : 0;
        return lines.append("\n" + S.padLeft("- " + source, Math.max(5, maxLen - source.length() - 3 + bannerGap)));
    }

    private static List<String> defaultQuotes() {
        return C.listOf(
                "Premature optimization is the root of all evil. ― Donald Knuth",
                "Rules of optimization: " +
                        "\n\t1. Don't! " +
                        "\n\t2. Don't… yet. " +
                        "\n\t3. Profile before optimizing. ― Michael Jackson",
                "As a programmer, never underestimate your ability to come up with ridiculously complex solutions " +
                        "for simple problems. ― Thomas Fuchs",
                "Get your data structures correct first, and the rest of the program will write itself. ― David Jones",
                "The #1 rule of distribute computing: " +
                        "\n\tDon't distribute your computing! " +
                        "- At least if you can in any way avoid it. ― DHH (David Heinemeier Hanson)",
                "Scalability. The #1 problem people don’t actually have but still solve. ― Eberhard Wolff",
                "Shared + Mutable = Danger! ― Andrey Breslav",
                "Prefer duplication over the wrong abstraction. ― Sandi Metz",
                "1. Avoid premature distribution. " +
                        "\n2. Avoid premature abstraction. " +
                        "\n\nBoth offer a lure of purity, cleanliness and scalability but add complexity and " +
                        "operational/cognitive overhead. ― Karl Isenberg",
                "Data structures, not algorithms, are central to programming. " +
                        "― Rob Pike’s 5. Rules of Programming",
                "Compassionate Tech Values: \n" +
                        "\n\tEgo < Humility. " +
                        "\n\tElitism < Inclusion. " +
                        "\n\tCompetition < Cooperation. " +
                        "\n\tBeing Smart < Learning. " +
                        "\n\tBeing a Rockstar < Being a Mentor. ― April Wensel",
                "KISS > DRY ― Philipp Hauer",
                "Beautiful is better than ugly. ― Zen of Python",
                "Explicit is better than implicit. ― Zen of Python",
                "Simple is better than complex. ― Zen of Python",
                "Complex is better than complicated. ― Zen of Python",
                "Flat is better than nested. ― Zen of Python",
                "Sparse is better than dense. ― Zen of Python",
                "Readability counts. ― Zen of Python",
                "Special cases aren't special enough to break the rules. \n" +
                        "Although practicality beats purity. ― Zen of Python",
                "Errors should never pass silently" +
                        "\nUnless explicitly silenced. ― Zen of Python",
                "In the face of ambiguity, refuse the temptation to guess. ― Zen of Python",
                "There should be one -- and preferably only one -- obvious way to do it.\n" +
                        "Although that way may not be obvious at first unless you're Dutch." +
                        " ― Zen of Python",
                "Now is better than never. \n" +
                        "Although never is often better than *right* now. ― Zen of Python",
                "If the implementation is hard to explain, it's a bad idea. ― Zen of Python",
                "If the implementation is easy to explain, it may be a good idea. ― Zen of Python",
                "Namespaces are one honking great idea -- let's do more of those! ― Zen of Python",
                "Simple things should be simple, complex things should be possible. ― Zen of Python"
        );
    }

    public static void main(String[] args) {
        System.out.println(S.join("\n", WORDS));
        System.out.printf(S.repeat('*').times(100));
        System.out.println(S.join("\n", WORDS_FOR_BANNER));
    }
}
