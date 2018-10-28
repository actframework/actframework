package act.db.util;

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
