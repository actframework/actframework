package act.controller.bytecode;

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
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import act.view.*;
import org.osgl.$;
import org.osgl.mvc.result.*;

import java.util.HashSet;
import java.util.Set;

// For quickly check if a class is children/descendant of org.osgl.mvc.Result
class ResultClassLookup {

    private static volatile ResultClassLookup _inst;

    private Set<String> _set = new HashSet<>();

    private ResultClassLookup() {
        loadResultClasses();
    }

    private void loadResultClasses() {
        final String RESULT = Result.class.getName();
        _set.add(RESULT);
        ClassInfoRepository repo = Act.app().classLoader().classInfoRepository();
        ClassNode resultNode = repo.node(RESULT);
        if (null == resultNode) {
            // inside unit test
            add(
                    Ok.class, NotFound.class, ErrorResult.class, NotAcceptable.class, Forbidden.class,
                    NotImplemented.class, BadRequest.class, Conflict.class, MethodNotAllowed.class,
                    Unauthorized.class, ServerError.class, InternalServerError.class, NotModified.class, RenderBinary.class,
                    Accepted.class, Created.class, NoResult.class, NoContent.class, Redirect.class, RenderTemplate.class,
                    RenderAny.class, ZXingResult.class, RenderJsonMap.class, RenderJSON.class,
                    RenderContent.class, RenderXML.class, RenderCSV.class, RenderHtml.class,
                    FilteredRenderJSON.class, FilteredRenderXML.class, RenderText.class
            );
        } else {
            resultNode.visitPublicSubTreeNodes(new $.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws $.Break {
                    _set.add(classNode.name());
                }
            });
        }
    }

    private ResultClassLookup add(Class<? extends Result> ... resultClasses) {
        for (Class<? extends Result> resultClass : resultClasses) {
            _set.add(resultClass.getName());
        }
        return this;
    }

    public static boolean isResult(String className) {
        return get()._set.contains(className);
    }

    private static ResultClassLookup get() {
        if (null == _inst) {
            synchronized (ResultClassLookup.class) {
                if (null == _inst) {
                    _inst = new ResultClassLookup();
                }
            }
        }
        return _inst;
    }

}
