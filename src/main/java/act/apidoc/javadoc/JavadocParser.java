package act.apidoc.javadoc;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import static act.apidoc.javadoc.Utils.nextWord;

import com.github.javaparser.ast.comments.JavadocComment;
import org.osgl.util.S;
import org.osgl.util.VM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class JavadocParser {

    private static final String EOL = System.getProperty("line.separator");
    private static String BLOCK_TAG_PREFIX = "@";
    private static String INHERIT_DOC = "@inheritDoc";
    private static Pattern BLOCK_PATTERN = Pattern.compile("^" + BLOCK_TAG_PREFIX, Pattern.MULTILINE);

    public static Javadoc parse(JavadocComment comment) {
        return parse(comment.getContent());
    }

    public static Javadoc parse(String commentContent) {
        List<String> cleanLines = cleanLines(normalizeEolInTextBlock(commentContent, EOL));
        int indexOfFirstBlockTag = -1;
        for (int i = 0, j = cleanLines.size(); i < j; ++i) {
            String line = cleanLines.get(i);
            if (isABlockLine(line)) {
                indexOfFirstBlockTag = i;
                break;
            }
        }
        List<String> blockLines;
        String descriptionText;
        if (indexOfFirstBlockTag == -1) {
            descriptionText = trimRight(S.join(EOL, cleanLines));
            blockLines = Collections.emptyList();
        } else {
            descriptionText = trimRight(S.join(EOL, cleanLines.subList(0, indexOfFirstBlockTag)));

            //Combine cleaned lines, but only starting with the first block tag till the end
            //In this combined string it is easier to handle multiple lines which actually belong together
            String tagBlock = S.join(EOL, cleanLines.subList(indexOfFirstBlockTag, cleanLines.size()));

            //Split up the entire tag back again, considering now that some lines belong to the same block tag.
            //The pattern splits the block at each new line starting with the '@' symbol, thus the symbol
            //then needs to be added again so that the block parsers handles everything correctly.
            String[] sa = BLOCK_PATTERN.split(tagBlock);
            blockLines = new ArrayList<>();
            for (String s : sa) {
                if (S.notEmpty(s)) {
                    blockLines.add(BLOCK_TAG_PREFIX + s);
                }
            }
        }
        Javadoc document = new Javadoc(JavadocDescription.parseText(descriptionText));
        for (String line : blockLines) {
            document.addBlockTag(parseBlockTag(line));
        }
        return document;
    }

    private static JavadocBlockTag parseBlockTag(String line) {
        line = line.trim().substring(1);
        String tagName = nextWord(line);
        String rest = line.substring(tagName.length()).trim();
        return new JavadocBlockTag(tagName, rest);
    }

    private static boolean isABlockLine(String line) {
        return !isInheritDoc(line) && line.trim().startsWith(BLOCK_TAG_PREFIX);
    }

    private static boolean isInheritDoc(String line) {
        return line.trim().startsWith(INHERIT_DOC);
    }

    private static String trimRight(String string) {
        while (!string.isEmpty() && Character.isWhitespace(string.charAt(string.length() - 1))) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    private static List<String> cleanLines(String content) {
        String[] lines = content.split(EOL);
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            int asteriskIndex = startsWithAsterisk(line);
            if (asteriskIndex == -1) {
                cleanedLines.add(S.trim(line));
            } else {
                if (line.length() > (asteriskIndex + 1)) {
                    char c = line.charAt(asteriskIndex + 1);
                    if (c == ' ' || c == '\t') {
                        cleanedLines.add(line.substring(asteriskIndex + 2));
                        continue;
                    }
                }
                cleanedLines.add(line.substring(asteriskIndex + 1));
            }
        }
        // if the first starts with a space, remove it
        if (!cleanedLines.get(0).isEmpty() && (cleanedLines.get(0).charAt(0) == ' ' || cleanedLines.get(0).charAt(0) == '\t')) {
            cleanedLines.set(0, cleanedLines.get(0).substring(1));
        }
        // drop empty lines at the beginning and at the end
        while (cleanedLines.size() > 0 && cleanedLines.get(0).trim().isEmpty()) {
            cleanedLines = cleanedLines.subList(1, cleanedLines.size());
        }
        while (cleanedLines.size() > 0 && cleanedLines.get(cleanedLines.size() - 1).trim().isEmpty()) {
            cleanedLines = cleanedLines.subList(0, cleanedLines.size() - 1);
        }
        return cleanedLines;
    }

    // Visible for testing
    static int startsWithAsterisk(String line) {
        if (line.startsWith("*")) {
            return 0;
        } else if ((line.startsWith(" ") || line.startsWith("\t")) && line.length() > 1) {
            int res = startsWithAsterisk(line.substring(1));
            if (res == -1) {
                return -1;
            } else {
                return 1 + res;
            }
        } else {
            return -1;
        }
    }

    /**
     * @return content with all kinds of EOL characters replaced by endOfLineCharacter
     */
    public static String normalizeEolInTextBlock(String content, String endOfLineCharacter) {
        String regex = "\\R";
        if (VM.VERSION < 8) {
            regex = "[(\\r\\n)\\n\\r]";
        }
        return content.replaceAll(regex, endOfLineCharacter);
    }
}
