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

import org.osgl.util.E;

import java.lang.annotation.Annotation;

/**
 * Defines class filter specification and handle method when
 * the class been found. <b>Note</b> only public and non-abstract
 * class will be filtered out, these two requirements are
 * implicit specification
 */
public abstract class ClassFilter<SUPER_TYPE, ANNOTATION_TYPE extends Annotation> {

    private Class<SUPER_TYPE> superType;
    private Class<ANNOTATION_TYPE> annotationType;
    private boolean noAbstract;
    private boolean publicOnly;


    public ClassFilter(Class<SUPER_TYPE> superType, Class<ANNOTATION_TYPE> annotationType) {
        this(false, false, superType, annotationType);
    }

    public ClassFilter(boolean publicOnly, boolean noAbstract, Class<SUPER_TYPE> superType, Class<ANNOTATION_TYPE> annotationType) {
        E.npeIf(superType == null && annotationType == null);
        this.superType = superType;
        this.annotationType = annotationType;
        this.noAbstract = noAbstract;
        this.publicOnly = publicOnly;
    }

    /**
     * Once a class has been found as per the requirements
     * of this class filter, Act will load the class and call
     * this method on this filter instance
     *
     * @param clazz the class instance been found
     */
    public abstract void found(Class<? extends SUPER_TYPE> clazz);

    /**
     * Specify the super type that must be extended or implemented
     * by the targeting class
     */
    public Class<SUPER_TYPE> superType() {
        return superType;
    }

    /**
     * Specify the annotation type that target class will be annotated
     */
    public Class<ANNOTATION_TYPE> annotationType() {
        return annotationType;
    }

    public boolean noAbstract() {
        return noAbstract;
    }

    public boolean publicOnly() {
        return publicOnly;
    }

}
