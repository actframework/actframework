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
import org.rythmengine.utils.S;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

@Singleton
@ExpressController
public final class Zen {

    private static final List<String> WORDS = loadWords();

    private Info.Unit zenInfo;

    public Zen() {
        String zenTxt = S.join("\n\n", WORDS);
        zenInfo = new Info.Unit("zen", zenTxt);
    }

    public static String wordsOfTheDay() {
        return $.random(WORDS);
    }

    @GetAction("zen")
    @Description("Get my zen words (禅语)")
    public void zen(ActionContext context) {
        zenInfo.applyTo(context);
    }

    private static List<String> loadWords() {
        URL url = Act.getResource("act_zen.txt");
        List<String> words = C.newList(defaultWords());
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
