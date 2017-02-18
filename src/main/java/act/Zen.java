package act;

import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.rythmengine.utils.S;

import static act.controller.Controller.Util.text;

public final class Zen {

    private Zen() {
    }

    private static final C.List<String> WORDS = C.listOf(
            "Beautiful is better than ugly.",
            "Explicit is better than implicit.",
            "Simple is better than complex.",
            "Complex is better than complicated.",
            "Flat is better than nested.",
            "Sparse is better than dense.",
            "Readability counts.",
            "Special cases aren't special enough to break the rules. \n" +
            "          Although practicality beats purity.",
            "Errors should never pass silently \n" +
            "          Unless explicitly silenced.",
            "In the face of ambiguity, refuse the temptation to guess.",
            "There should be one-- and preferably only one --obvious way to do it.\n" +
            "Although that way may not be obvious at first unless you're Dutch.",
            "Now is better than never. \n" +
            "          Although never is often better than *right* now.",
            "If the implementation is hard to explain, it's a bad idea.",
            "If the implementation is easy to explain, it may be a good idea.",
            "Namespaces are one honking great idea -- let's do more of those!"
    );

    public static String wordsOfTheDay() {
        return $.random(WORDS);
    }

    @GetAction("zen")
    public static Result zen() {
        return text(S.join("\n\n", WORDS));
    }

}
