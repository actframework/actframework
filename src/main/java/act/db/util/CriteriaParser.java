package act.db.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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

import act.db.CriteriaComponent;
import org.osgl.util.E;
import org.scijava.parse.ExpressionParser;
import org.scijava.parse.SyntaxTree;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CriteriaParser {

    @Inject
    private CriteriaOperatorRegistry registry;

    // ParsingtonExpressionParser = pep
    private volatile ExpressionParser pep;

    public CriteriaComponent parse(String expression, Map<String, Object> params) {
        SyntaxTree tree = pep().parseTree(expression);
        throw E.tbd();
    }

    private ExpressionParser pep() {
        if (null == pep) {
            synchronized (this) {
                if (null == pep) {
                    pep = new ExpressionParser(registry.parsingtonOperators);
                }
            }
        }
        return pep;
    }

}
