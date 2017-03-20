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
import act.Destroyable;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.osgl.$;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The repository to keep class information
 */
@ApplicationScoped
public class ClassInfoRepository extends DestroyableBase {

    protected ConcurrentMap<String, ClassNode> classes = new ConcurrentHashMap<String, ClassNode>();

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

    public boolean isEmpty() {
        return classes.isEmpty();
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.destroyAll(classes.values(), ApplicationScoped.class);
        classes.clear();
    }

    public Map<String, ClassNode> classes() {
        return C.map(classes);
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
}
