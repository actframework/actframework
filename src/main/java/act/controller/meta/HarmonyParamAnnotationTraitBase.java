package act.controller.meta;

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

import org.osgl.$;
import org.osgl.util.S;

/**
 * A {@code HarmonyParamAnnotationTraitBase} is compatible with other {@link ParamAnnoInfoTrait}
 * except a annotation with the same type of itself
 */
public abstract class HarmonyParamAnnotationTraitBase extends ParamAnnoInfoTraitBase {
    protected HarmonyParamAnnotationTraitBase(int index) {
        super(index);
    }

    @Override
    public boolean compatibleWith(ParamAnnoInfoTrait otherParamAnnotation) {
        return $.eq(otherParamAnnotation.getClass(), this.getClass());
    }

    @Override
    public String compatibilityErrorMessage(ParamAnnoInfoTrait otherParamAnnotation) {
        if ($.eq(otherParamAnnotation.getClass(), getClass())) {
            return S.fmt("Duplicated annotation found: %s", getClass());
        }
        return null;
    }
}
