package act.util;

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

import act.app.App;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.osgl.Lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Namespace for FastJSON relevant annotations/utilities
 */
public enum FastJson {
    ;

    /**
     * Used to annotate on user defined {@link com.alibaba.fastjson.serializer.ObjectSerializer}
     * or {@link com.alibaba.fastjson.parser.deserializer.ObjectDeserializer} to specify
     * the type and sub type to which it shall apply
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface For {
        /**
         * Specify the classes of the type the annotated Serializer/Deserializer shall
         * apply to.
         *
         * @return the classes as described above
         */
        Class[] value();
    }

    /**
     * Used to annotate on a class that specified {@link com.alibaba.fastjson.serializer.ObjectSerializer}
     * shall be applied to.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Serializer {
        /**
         * Specify the Serializer class the annotated type and it's sub type shall be applied
         *
         * @return the Serializer type
         */
        Class<? extends ObjectSerializer> value();
    }

    /**
     * Used to annotate on a class that specified {@link com.alibaba.fastjson.parser.deserializer.ObjectDeserializer}
     * shall be applied to.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Deserializer {
        /**
         * Specify the Deserializer class the annotated type and it's sub type shall be applied
         *
         * @return the Deserializer type
         */
        Class<? extends ObjectDeserializer> value();
    }

    @Singleton
    public static class Explorer extends LogSupport {

        @Inject
        private App app;

        @Inject
        private ClassInfoRepository repo;

        @AnnotatedClassFinder(For.class)
        public void foundFor(Class<?> type) {
            For forAnno = type.getAnnotation(For.class);
            Class[] targetTypes = forAnno.value();
            if (0 == targetTypes.length) {
                warn("@For annotation on [%s] must have target type specified", type.getName());
                return;
            }
            if (ObjectSerializer.class.isAssignableFrom(type)) {
                ObjectSerializer serializer = (ObjectSerializer) app.getInstance(type);
                handleForSerializer(serializer, targetTypes);
            }
            if (ObjectDeserializer.class.isAssignableFrom(type)) {
                ObjectDeserializer deserializer = (ObjectDeserializer) app.getInstance(type);
                handleForDeserializer(deserializer, targetTypes);
            }
        }

        @AnnotatedClassFinder(Serializer.class)
        public void foundSerializer(Class<?> targetType) {
            Serializer serializerAnno = targetType.getAnnotation(Serializer.class);
            Class<? extends ObjectSerializer> serializerType = serializerAnno.value();
            ObjectSerializer serializer = app.getInstance(serializerType);
            handleForSerializer(serializer, targetType);
        }

        @AnnotatedClassFinder(Deserializer.class)
        public void foundDeserializer(Class<?> targetType) {
            Deserializer deserializerAnno = targetType.getAnnotation(Deserializer.class);
            Class<? extends ObjectDeserializer> deserializerType = deserializerAnno.value();
            ObjectDeserializer deserializer = app.getInstance(deserializerType);
            handleForDeserializer(deserializer, targetType);
        }

        private void handleForSerializer(ObjectSerializer serializer, Class[] targetTypes) {
            for (Class type : targetTypes) {
                handleForSerializer(serializer, type);
            }
        }

        private void handleForSerializer(final ObjectSerializer serializer, Class targetType) {
            ClassNode node = repo.node(targetType.getName());
            if (null == node) {
                warn("Unknown target type: " + targetType.getName());
                return;
            }
            final SerializeConfig config = SerializeConfig.getGlobalInstance();
            node.visitSubTree(new Lang.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws Lang.Break {
                    Class type = app.classForName(classNode.name());
                    config.put(type, serializer);
                }
            });
            config.put(targetType, serializer);
        }

        private void handleForDeserializer(ObjectDeserializer serializer, Class[] targetTypes) {
            for (Class type : targetTypes) {
                handleForDeserializer(serializer, type);
            }
        }

        private void handleForDeserializer(final ObjectDeserializer deserializer, Class targetType) {
            ClassNode node = repo.node(targetType.getName());
            if (null == node) {
                warn("Unknown target type: " + targetType.getName());
                return;
            }
            final ParserConfig config = ParserConfig.getGlobalInstance();
            node.visitSubTree(new Lang.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws Lang.Break {
                    Class type = app.classForName(classNode.name());
                    config.putDeserializer(type, deserializer);
                }
            });
            config.putDeserializer(targetType, deserializer);
        }
    }

}
