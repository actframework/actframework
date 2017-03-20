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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class ClassNodeDTO {
    String name;
    String canonicalName;
    int modifiers;
    String parent;
    Set<String> interfaces = new HashSet<String>();
    Set<String> annotations = new HashSet<String>();
    Set<String> annotated = new HashSet<String>();

    // For JSON deserialization
    ClassNodeDTO() {}


    ClassNodeDTO(ClassNode node) {
        this.name = node.name();
        this.canonicalName = node.canonicalName();
        this.modifiers = node.modifiers();
        ClassNode parent = node.parent();
        this.parent = parent == null ? null : parent.canonicalName();
        convert(node.interfaces.values(), interfaces);
        convert(node.annotations, annotations);
        convert(node.annotated, annotated);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public int getModifiers() {
        return modifiers;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Set<String> interfaces) {
        this.interfaces = interfaces;
    }

    public Set<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Set<String> annotations) {
        this.annotations = annotations;
    }

    public Set<String> getAnnotated() {
        return annotated;
    }

    public void setAnnotated(Set<String> annotated) {
        this.annotated = annotated;
    }

    public ClassNode toClassNode(ClassInfoRepository infoBase) {
        return new ClassNode(name, canonicalName, modifiers, infoBase);
    }

    private static void convert(Collection<ClassNode> from, Set<String> to) {
        for (ClassNode node : from) {
            to.add(node.canonicalName());
        }
    }
}
