package act.annotations;

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
import act.inject.util.Sorter;
import org.osgl.inject.annotation.PostConstructProcess;

import java.lang.annotation.*;

/**
 * A hint to tell framework that a list
 * shall be sorted in a certain order
 *
 * @see Order
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@PostConstructProcess(Sorter.class)
public @interface Sorted {
}
