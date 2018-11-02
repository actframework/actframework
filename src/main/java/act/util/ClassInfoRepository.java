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

import act.Act;
import act.app.event.SysEventId;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.enterprise.context.ApplicationScoped;

/**
 * The repository to keep class information
 */
@ApplicationScoped
public class ClassInfoRepository extends LogSupportedDestroyableBase {

    public ClassInfoRepository() {
    }

    protected ConcurrentMap<String, ClassNode> classes = new ConcurrentHashMap<String, ClassNode>();

    protected ConcurrentMap<Class<? extends Annotation>, Set<Method>> methodAnnotationLookup = new ConcurrentHashMap<>();

    public boolean has(String className) {
        return classes.containsKey(className);
    }

    public ClassNode node(String name) {
        String cname = canonicalName(name);
        ClassNode node = classes.get(cname);
        if (null == node) {
            ClassNode newNode = new ClassNode(name.replace('/', '.'), cname, this);
            node = classes.putIfAbsent(cname, newNode);
            if (null == node) {
                node = newNode;
            }
        }
        return node;
    }

    public ClassNode node(String name, String canonicalName) {
        String cname = canonicalName(name);
        ClassNode node = classes.get(name);
        if (null == node) {
            ClassNode newNode = new ClassNode(name.replace('/', '.'), canonicalName, this);
            node = classes.putIfAbsent(cname, newNode);
            if (null == node) {
                node = newNode;
            }
        }
        return node;
    }

    public Set<Method> methodsWithAnnotation(Class<? extends Annotation> annoType) {
        Set<Method> set = methodAnnotationLookup.get(annoType);
        return null == set ? C.<Method>Set() : set;
    }

    public void registerMethodAnnotationLookup(final String annotationDesc, final String classInternalName, final String methodName, final String methodDesc) {
        Act.app().jobManager().on(SysEventId.CLASS_LOADED, "ClassInfoRepository:registerMethodAnnotationLookup", new Runnable() {
            @Override
            public void run() {
                Class<? extends Annotation> annoType = AsmType.classForDesc(annotationDesc);
                Class<?> methodHost = AsmType.classForInternalName(classInternalName);
                Class<?>[] methodArgumentTypes = AsmType.methodArgumentTypesForDesc(methodDesc);
                try {
                    Method method = methodHost.getDeclaredMethod(methodName, methodArgumentTypes);
                    Set<Method> set = methodAnnotationLookup.get(annoType);
                    if (null == set) {
                        set = new HashSet<>();
                    }
                    set.add(method);
                    methodAnnotationLookup.put(annoType, set);
                } catch (NoSuchMethodException e) {
                    throw E.unexpected(e);
                }
            }
        });
    }

    public ClassNode findNode(Class<?> type) {
        return classes.get(canonicalName(type.getName()));
    }

    public ClassNode findNode(String typeName) {
        return classes.get(canonicalName(typeName));
    }

    public boolean isEmpty() {
        return classes.isEmpty();
    }

    public void reset() {
        classes.clear();
    }

    @Override
    protected void releaseResources() {
        classes.clear();
    }

    public Map<String, ClassNode> classes() {
        return C.Map(classes);
    }

    public String toJSON() {
        List<ClassNodeDTO> list = new ArrayList<ClassNodeDTO>();
        for (ClassNode node : classes.values()) {
            list.add(node.toDTO());
        }
        ClassLoader cl0 = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ClassNodeDTO.class.getClassLoader());
            return JSON.toJSONString(list);
        } finally {
            Thread.currentThread().setContextClassLoader(cl0);
        }
    }

    /**
     * Java {@code Class.getCanonicalName()} sometimes will throw out
     * {@code InternalError} with message: "{code Malformed class name}"
     * We just ignore it
     * @param c the class on which canonical name is returned
     * @return the canonical name of the class specified or {@code null} if no
     * canonical name found or error returned canonical name on the class
     */
    public static String canonicalName(Class c) {
        try {
            return c.getCanonicalName();
        } catch (InternalError e) {
            return null;
        } catch (IllegalAccessError e) {
            return null;
        }
    }

    @Override
    public int hashCode() {
        return $.hc(classes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ClassInfoRepository) {
            ClassInfoRepository that = $.cast(obj);
            return $.eq(that.classes, this.classes);
        }
        return false;
    }

    public static String canonicalName(String name) {
        return name.replace('/', '.').replace('$', '.');
    }

    public static ClassInfoRepository parseJSON(String json) {
        ClassInfoRepository repo = new ClassInfoRepository();
        List<ClassNodeDTO> list;
        ClassLoader cl0 = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ClassNodeDTO.class.getClassLoader());
            list = JSON.parseObject(json, new TypeReference<List<ClassNodeDTO>>() {});
        } finally {
            Thread.currentThread().setContextClassLoader(cl0);
        }
        for (ClassNodeDTO dto: list) {
            ClassNode classNode = dto.toClassNode(repo);
            repo.classes.putIfAbsent(dto.getCanonicalName(), classNode);
        }
        for (ClassNodeDTO dto: list) {
            ClassNode classNode = repo.classes.get(dto.getCanonicalName());
            if (dto.parent != null) {
                ClassNode parentNode = repo.classes.get(dto.parent);
                if (null == parentNode) {
                    Act.LOGGER.warn("Error de-serializing ClassInfoRepository: parent[%s] not found for classNode[%s]", dto.parent, dto.canonicalName);
                } else {
                    parentNode.addChild(classNode);
                }
            }
            for (String name : dto.annotated) {
                ClassNode node = repo.classes.get(name);
                if (null == node) {
                    Act.LOGGER.warn("Error de-serializing ClassInfoRepository: annotated[%s] not found for classNode[%s]", name, dto.canonicalName);
                } else {
                    classNode.addAnnontated(node);
                }
            }
            for (String name : dto.annotations) {
                ClassNode node = repo.classes.get(name);
                if (null == node) {
                    Act.LOGGER.warn("Error de-serializing ClassInfoRepository: annotation[%s] not found for classNode[%s]", name, dto.canonicalName);
                } else {
                    classNode.addAnnotation(node);
                }
            }
            for (String name : dto.interfaces) {
                ClassNode node = repo.classes.get(name);
                if (null == node) {
                    Act.LOGGER.warn("Error de-serializing ClassInfoRepository: interface[%s] not found for classNode[%s]", name, dto.canonicalName);
                } else {
                    classNode.addInterface(node);
                }
            }
        }
        return repo;
    }

    public Comparator<String> parentClassFirst = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            ClassNode n1 = node(o1);
            ClassNode n2 = node(o2);
            if (n1 == null && n2 == null) {
                return o1.compareTo(o2);
            }
            if (n1 == null) {
                return -1;
            }
            if (n2 == null) {
                return 1;
            }
            if (n1.isMyDescendant(n2)) {
                return -1;
            }
            if (n2.isMyDescendant(n1)) {
                return 1;
            }
            return o1.compareTo(o2);
        }
    };
}
