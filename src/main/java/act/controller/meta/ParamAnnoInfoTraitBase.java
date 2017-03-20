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

import org.osgl.util.E;
import org.osgl.util.S;

public abstract class ParamAnnoInfoTraitBase implements ParamAnnoInfoTrait {
    private int index;
    protected ParamAnnoInfoTraitBase(int index) {
        E.illegalArgumentIf(index < 0);
        this.index = index;
    }

    @Override
    public boolean compatibleWith(ParamAnnoInfoTrait otherParamAnnotation) {
        return otherParamAnnotation instanceof HarmonyParamAnnotationTraitBase;
    }

    @Override
    public String compatibilityErrorMessage(ParamAnnoInfoTrait otherParamAnnotation) {
        return S.fmt("Param annotations cannot co-exists: %s vs %s", getClass(), otherParamAnnotation.getClass());
    }

    @Override
    public int index() {
        return index;
    }
}
