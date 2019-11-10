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

import com.github.javaparser.ast.comments.JavadocComment;
import org.osgl.util.OS;

import java.util.LinkedList;
import java.util.List;

public class Javadoc {

    private static String EOL = OS.get().lineSeparator();

    private JavadocDescription description;
    private List<JavadocBlockTag> blockTags;

    public Javadoc(JavadocDescription description, List<JavadocBlockTag> blockTags) {
        this.description = description;
        this.blockTags = blockTags;
    }

    public Javadoc(JavadocDescription description) {
        this.description = description;
        this.blockTags = new LinkedList<>();
    }

    public List<JavadocBlockTag> blockTags() {
        return blockTags;
    }

    public Javadoc addBlockTag(JavadocBlockTag blockTag) {
        this.blockTags.add(blockTag);
        return this;
    }

    /**
     * For tags like "@return good things" where
     * tagName is "return",
     * and the rest is content.
     */
    public Javadoc addBlockTag(String tagName, String content) {
        return addBlockTag(new JavadocBlockTag(tagName, content));
    }

    /**
     * For tags like "@param abc this is a parameter" where
     * tagName is "param",
     * parameter is "abc"
     * and the rest is content.
     */
    public Javadoc addBlockTag(String tagName, String parameter, String content) {
        return addBlockTag(tagName, parameter + " " + content);
    }

    public Javadoc addBlockTag(String tagName) {
        return addBlockTag(tagName, "");
    }

    /**
     * Return the text content of the document. It does not containing trailing spaces and asterisks
     * at the start of the line.
     */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        if (!description.isEmpty()) {
            sb.append(description.toText());
            sb.append(EOL);
        }
        if (!blockTags.isEmpty()) {
            sb.append(EOL);
        }
        for (JavadocBlockTag tag : blockTags) {
            sb.append(tag.toText()).append(EOL);
        }
        return sb.toString();
    }

    /**
     * Create a JavadocComment, by formatting the text of the Javadoc using no indentation (expecting the pretty printer to do the formatting.)
     */
    public JavadocComment toComment() {
        return toComment("");
    }

    /**
     * Create a JavadocComment, by formatting the text of the Javadoc using the given indentation.
     */
    public JavadocComment toComment(String indentation) {
        for (char c : indentation.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                throw new IllegalArgumentException("The indentation string should be composed only by whitespace characters");
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(EOL);
        final String text = toText();
        if (!text.isEmpty()) {
            for (String line : text.split(EOL)) {
                sb.append(indentation);
                sb.append(" * ");
                sb.append(line);
                sb.append(EOL);
            }
        }
        sb.append(indentation);
        sb.append(" ");
        return new JavadocComment(sb.toString());
    }

    public JavadocDescription getDescription() {
        return description;
    }

    /**
     * @return the current List of associated JavadocBlockTags
     */
    public List<JavadocBlockTag> getBlockTags() {
        return this.blockTags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Javadoc document = (Javadoc) o;

        return description.equals(document.description) && blockTags.equals(document.blockTags);

    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + blockTags.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Javadoc{" +
                "description=" + description +
                ", blockTags=" + blockTags +
                '}';
    }

}
