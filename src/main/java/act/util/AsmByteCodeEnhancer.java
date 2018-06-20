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
import act.asm.ClassVisitor;
import act.asm.Opcodes;
import act.plugin.Plugin;
import org.osgl.$;
import org.osgl.util.E;

/**
 * Base class for all bytecode enhancer that using ASM lib
 */
public abstract class AsmByteCodeEnhancer<T extends AsmByteCodeEnhancer> extends PredictableByteCodeVisitor
        implements Opcodes, Plugin, Cloneable {
    private $.Predicate<String> targetClassPredicate;

    protected abstract Class<T> subClass();

    protected AsmByteCodeEnhancer() {
        this.targetClassPredicate = new $.Predicate<String>() {
            @Override
            public boolean test(String s) {
                return Act.appConfig().needEnhancement(s);
            }
        };
    }

    protected AsmByteCodeEnhancer($.Predicate<String> targetClassPredicate) {
        this.targetClassPredicate = targetClassPredicate;
    }

    protected AsmByteCodeEnhancer($.Predicate<String> targetClassPredicate, ClassVisitor cv) {
        super(cv);
        this.targetClassPredicate = targetClassPredicate;
    }

    public void predicate($.Predicate<String> predicate) {
        targetClassPredicate = predicate;
    }

    @Override
    public boolean isTargetClass(String className) {
        return (null == targetClassPredicate) || targetClassPredicate.test(className);
    }

    @Override
    public void register() {
        Act.enhancerManager().register(this);
    }

    protected void reset() {}

    public T clone() {
        try {
            T clone = (T) super.clone();
            clone.reset();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw E.unexpected(e);
        }
    }
}
