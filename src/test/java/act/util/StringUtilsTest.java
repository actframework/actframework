package act.util;

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

import org.junit.BeforeClass;
import org.junit.Test;
import org.osgl.util.S;
import osgl.ut.TestBase;

public class StringUtilsTest extends TestBase {

    @Test
    public void testGH1223() {
        String s = "a${f}b${f}c";
        eq("abc", process(s));
    }

    @Test
    public void test() {
        String s = "${foo}";
        eq("", process(s));

        s = "abc${foo}";
        eq("abc", process(s));

        s = "abc${foo}xyz${foo}ijk";
        eq("abcxyzijk", process(s));

        s = "abc${foo}xyz";
        eq("abcxyz", process(s));

        s = "<script src=\"abc/xyz/?v=${foo}\"><script>";
        eq("<script src=\"abc/xyz/?v=\"><script>", process(s));

        s = "<script src=\"abc/xyz/?v=${foo}\"><script>\n<script src=\"abc/xyz/?v=${foo}\"><script>";
        eq("<script src=\"abc/xyz/?v=\"><script>\n<script src=\"abc/xyz/?v=\"><script>", process(s));
    }

    private String process(String s) {
        return StringUtils.processStringSubstitution(s);
    }

}
