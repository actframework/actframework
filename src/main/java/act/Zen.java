package act;

import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.rythmengine.utils.S;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static act.controller.Controller.Util.text;

public final class Zen {

    private Zen() {
    }

    private static final List<String> WORDS = loadWords();

    public static String wordsOfTheDay() {
        return $.random(WORDS);
    }

    @GetAction("zen")
    public static Result zen() {
        return text(S.join("\n\n", WORDS));
    }

    private static List<String> loadWords() {
        URL url = Zen.class.getResource("/zen.txt");
        List<String> words = C.newList(defaultWords());
        if (null != url) {
            try {
                words.addAll(IO.readLines(url.openStream()));
            } catch (IOException e) {
                // ignore it
            }
        }
        List<String> retVal = new ArrayList<>(words.size());
        for (String s : words) {
            if (s.contains("\n")) {
                s = s.replaceAll("\n", "\n          ");
            } else if (s.contains("\\n")) {
                s = s.replaceAll("\\\\n", "\n          ");
            }
            retVal.add(s);
        }
        return retVal;
    }

    private static List<String> defaultWords() {
        return C.listOf(
                "Beautiful is better than ugly.",
                "Explicit is better than implicit.",
                "Simple is better than complex.",
                "Complex is better than complicated.",
                "Flat is better than nested.",
                "Sparse is better than dense.",
                "Readability counts.",
                "Special cases aren't special enough to break the rules. \n" +
                        "Although practicality beats purity.",
                "Errors should never pass silently \n" +
                        "Unless explicitly silenced.",
                "In the face of ambiguity, refuse the temptation to guess.",
                "There should be one-- and preferably only one --obvious way to do it.\n" +
                        "Although that way may not be obvious at first unless you're Dutch.",
                "Now is better than never. \n" +
                        "Although never is often better than *right* now.",
                "If the implementation is hard to explain, it's a bad idea.",
                "If the implementation is easy to explain, it may be a good idea.",
                "Namespaces are one honking great idea -- let's do more of those!",
                "Simple things should be simple, complex things should be possible."
        );
    }
}
