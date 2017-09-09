package act.cli.util;

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

import act.ActTestBase;
import org.junit.Test;
import org.osgl.util.C;

public class CommandLineParserTest extends ActTestBase {

    private CommandLineParser p;

    private static CommandLineParser p(String line) {
        return new CommandLineParser(line);
    }

    @Test
    public void commandShallBeTheFirstTokenAndInterned() {
        p = p("myCommand -o x -t y a b c");
        same("myCommand", p.command());
    }

    @Test
    public void testGetBooleanOption() {
        p = p("myCommand -o x -b -t y a b c");
        yes(p.getBoolean("-b", null));
        no(p.getBoolean("-o", "-t"));
        no(p.getBoolean("-z", "--zoo"));
    }

    @Test
    public void testGetStringWithQuotes() {
        p = p("myCommand -s \"some string\"");
        eq("some string", p.getString("-s", "--string"));
        p = p("myCommand --string \"some string\"");
        eq("some string", p.getString("-s", "--string"));
    }

    @Test
    public void testGetStringWithSingleQuotes() {
        p = p("myCommand -s 'some string'");
        eq("some string", p.getString("-s", "--string"));
        p = p("myCommand --string 'some string'");
        eq("some string", p.getString("-s", "--string"));
    }

    @Test
    public void testGetStringWithSingleQuotesInsideQuotes() {
        p = p("myCommand -s \"he said: 'some string'\" -i 10");
        eq("he said: 'some string'", p.getString("-s", "--string"));
        p = p("myCommand --string \"he said: 'some string'\"");
        eq("he said: 'some string'", p.getString("-s", "--string"));
    }

    @Test
    public void testGetStringWithQuotesInsideSingleQuotes() {
        p = p("myCommand -s 'he said: \"some string\"' -i 10");
        eq("he said: \"some string\"", p.getString("-s", "--string"));
        p = p("myCommand --string 'he said: \"some string\"'");
        eq("he said: \"some string\"", p.getString("-s", "--string"));
    }

    @Test
    public void testGetArguments() {
        p = p("myCommand -o x -b -s \"some string\" arg1 2 3");
        ceq(C.listOf("arg1 2 3".split(" ")), p.arguments());
    }

    @Test
    public void argumentsDeclaredBeforeOptions() {
        p = p("myCommand -o x arg1 -b -n 1 arg2");
        eq('x', p.getChar("-o", null, 'y'));
        yes(p.getBoolean("-b", null));
        same(1, p.getInt("-n", null, 2));
        ceq(C.listOf("arg1 arg2".split(" ")), p.arguments());
    }

    @Test
    public void useEqSignToSeparateLeadValue() {
        p = p("myCommand --option=x arg1 -b -n 1 arg2");
        eq('x', p.getChar("-o", "--option", 'y'));
        p = p("myCommand --option:x arg1 -b -n 1 arg2");
        eq('x', p.getChar("-o", "--option", 'y'));
        p = p("myCommand --option x arg1 -b -n 1 arg2");
        eq('x', p.getChar("-o", "--option", 'y'));
    }

    @Test
    public void useEqSignInQuotes() {
        p = p("myCommand -o x -b -s \"some=string\" arg1 2 3");
        eq("some=string", p.getString("-s", "--string"));
    }

    @Test
    public void booleanTypeOptionNotAtTail() {
        p = p("myCommand -t -a abc");
        yes(p.getBoolean("-t", null));
    }

    @Test
    public void booleanTypeOptionAtTail() {
        p = p("myCommand -a abc -t");
        yes(p.getBoolean("-t", null));
    }
}
