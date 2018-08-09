package act.e2e.verifier;

/*-
 * #%L
 * ACT E2E Plugin
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.e2e.util.NamedLogic;
import org.osgl.util.converter.TypeConverterRegistry;

public abstract class Verifier<T extends Verifier> extends NamedLogic<T> {

    public abstract boolean verify(Object value);

    @Override
    protected final Class<? extends NamedLogic> type() {
        return Verifier.class;
    }

    public static void registerTypeConverters() {
        TypeConverterRegistry.INSTANCE.register(new FromLinkedHashMap(Verifier.class));
        TypeConverterRegistry.INSTANCE.register(new FromString(Verifier.class));
    }
}
