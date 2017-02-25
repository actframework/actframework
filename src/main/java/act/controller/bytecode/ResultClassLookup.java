package act.controller.bytecode;

import act.Act;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import act.view.*;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.mvc.result.*;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Filter;

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
                    Unauthorized.class, ServerError.class, NotModified.class, RenderBinary.class,
                    Accepted.class, Created.class, NoResult.class, Redirect.class, RenderTemplate.class,
                    RenderAny.class, ZXingResult.class, RenderJsonMap.class, RenderJSON.class,
                    RenderContent.class, RenderXML.class, RenderCSV.class, RenderHtml.class,
                    FilteredRenderJSON.class, FilteredRenderXML.class, RenderText.class
            );
        } else {
            resultNode.visitPublicSubTreeNodes(new $.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws Osgl.Break {
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
