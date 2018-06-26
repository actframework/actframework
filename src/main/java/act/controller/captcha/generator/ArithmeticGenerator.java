package act.controller.captcha.generator;

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

import act.act_messages;
import act.controller.captcha.CaptchaSession;
import act.controller.captcha.CaptchaSessionGenerator;
import act.i18n.I18n;
import org.osgl.$;
import org.osgl.util.N;
import org.osgl.util.S;

/**
 * Generate {@link act.controller.captcha.CaptchaSession} with
 * a random arithmetic expression.
 */
public class ArithmeticGenerator implements CaptchaSessionGenerator {

    private enum Operator {
        ADD() {
            @Override
            int apply(int left, int right) {
                return left + right;
            }

            @Override
            public String toString() {
                return "+";
            }
        },
        SUBSTRACT() {
            @Override
            int apply(int left, int right) {
                return left - right;
            }

            @Override
            public String toString() {
                return "-";
            }
        },
        TIMES() {
            @Override
            int apply(int left, int right) {
                return left * right;
            }

            @Override
            public String toString() {
                return "x";
            }
        };

        abstract int apply(int left, int right);

        public abstract String toString();
    }

    // a simple ternary arithmetic expression
    private static class ArithmeticExpression {
        int first = N.randInt(10);
        Operator o1 = $.random(Operator.class);
        int second = N.randInt(10);
        Operator o2 = $.random(Operator.class);
        int third = N.randInt(10);

        int evaluate() {
            if (o2 == Operator.TIMES) {
                return o1.apply(first, o2.apply(second, third));
            } else {
                return o2.apply(o1.apply(first, second), third);
            }
        }

        @Override
        public String toString() {
            return S.concat(first, o1, second, o2, third, " = ?");
        }
    }

    @Override
    public CaptchaSession generate() {
        ArithmeticExpression expression = new ArithmeticExpression();
        return new CaptchaSession(
                expression.toString(), S.string(expression.evaluate()), null,
                I18n.i18n(act_messages.class, "act.captcha.generator.arithmetic.instruction"),
                null
        );
    }

}
