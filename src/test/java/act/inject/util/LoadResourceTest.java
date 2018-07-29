package act.inject.util;

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

import org.junit.Before;
import org.junit.Test;
import org.osgl.inject.Genie;
import org.osgl.util.C;
import org.osgl.util.Keyword;
import osgl.ut.TestBase;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class LoadResourceTest extends TestBase {

    public static class TestBed {

        @LoadResource("public/foo/foo.list")
        private String string;

        @LoadResource("/public/foo/foo.list")
        private String string2;

        @LoadResource("public/foo/foo.list")
        private ByteBuffer buffer;

        @LoadResource("public/foo/foo.list")
        private List<String> lines;

        @LoadResource("public/foo/foo.list")
        private byte[] ba;

        @LoadResource("public/foo/matrix.txt")
        private int[][] intMatrix;

        @LoadResource("public/foo/str_matrix.txt")
        private String[][] strMatrix;

        @LoadResource("tags")
        private Map<String, String> tagsByString;

        @LoadResource("tags")
        private Map<Keyword, String> tagsByKeyword;

    }

    Genie genie = Genie.createWithoutPlugins();

    TestBed testBed;

    @Before
    public void prepare() {
        testBed = genie.get(TestBed.class);
    }

    @Test
    public void testLoadString() {
        eq("line1\nline2", testBed.string);
        eq("line1\nline2", testBed.string2);
    }

    @Test
    public void testLoadLines() {
        eq(C.listOf("line1", "line2"), testBed.lines);
    }

    @Test
    public void testByteBuffer() {
        ByteBuffer copy = testBed.buffer.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        eq("line1\nline2", new String(bytes));
    }

    @Test
    public void testByteArray() {
        eq("line1\nline2", new String(testBed.ba));
    }

    @Test
    public void testIntMatrix() {
        eq(4, testBed.intMatrix[1][1]);
    }

    @Test
    public void testStrMatrix() {
        eq("yesterday", testBed.strMatrix[1][1]);
    }

    @Test
    public void testTagsByString() {
        eq("foo_tag", testBed.tagsByString.get("foo"));
        eq("bar_tag", testBed.tagsByString.get("bar"));
    }

    @Test
    public void testTagsByKeyword() {
        eq("foo_tag", testBed.tagsByKeyword.get(Keyword.of("foo")));
        eq("bar_tag", testBed.tagsByKeyword.get(Keyword.of("bar")));
    }
}
