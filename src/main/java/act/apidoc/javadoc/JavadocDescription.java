/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

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

import org.osgl.$;

import java.util.LinkedList;
import java.util.List;

/**
 * A javadoc text, potentially containing inline tags.
 */
public class JavadocDescription {

    private List<JavadocDescriptionElement> elements;

    public static JavadocDescription parseText(String text) {
        JavadocDescription instance = new JavadocDescription();
        int index = 0;
        $.T2<Integer, Integer> nextInlineTagPos;
        while ((nextInlineTagPos = indexOfNextInlineTag(text, index)) != null) {
            if (nextInlineTagPos._1 != index) {
                instance.addElement(new JavadocSnippet(text.substring(index, nextInlineTagPos._1 + 1)));
            }
            instance.addElement(JavadocInlineTag.fromText(text.substring(nextInlineTagPos._1, nextInlineTagPos._2 + 1)));
            index = nextInlineTagPos._2;
        }
        if (index < text.length()) {
            instance.addElement(new JavadocSnippet(text.substring(index)));
        }
        return instance;
    }

    private static $.T2<Integer, Integer> indexOfNextInlineTag(String text, int start) {
        int index = text.indexOf("{@", start);
        if (index == -1) {
            return null;
        }
        // we are interested only in complete inline tags
        int closeIndex = text.indexOf("}", index);
        if (closeIndex == -1) {
            return null;
        }
        return $.T2(index, closeIndex);
    }

    public JavadocDescription() {
        elements = new LinkedList<>();
    }

    public JavadocDescription(List<JavadocDescriptionElement> elements) {
        this();

        this.elements.addAll(elements);
    }

    public boolean addElement(JavadocDescriptionElement element) {
        return this.elements.add(element);
    }

    public List<JavadocDescriptionElement> getElements() {
        return this.elements;
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        for (JavadocDescriptionElement e : elements) {
            sb.append(e.toText());
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return toText().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavadocDescription that = (JavadocDescription) o;

        return elements.equals(that.elements);

    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return "JavadocDescription{" +
                "elements=" + elements +
                '}';
    }

}
