package org;

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

import org.osgl.util.IO;

import java.io.File;
import java.util.List;

public class FixChangeLog {

    private static void processLine(String line) {
        if (line.startsWith("- ")) {
            line = "* " + line.substring(2);
        }
        if (line.startsWith("* #")) {
            line = line.substring(3);
            String ghNum = line.substring(0, 4);
            String rest = line.substring(4);
            line = "* " + rest + " #" + ghNum;
        }
        if (line.startsWith("* Fix #")) {
            line = line.substring(7);
            String ghNum = line.substring(0, 3);
            String rest = line.substring(3);
            line = "* " + rest + " #" + ghNum;
        }
        System.out.println(line);
    }


    public static void main(String[] args) {
        List<String> lines = IO.readLines(new File("CHANGELOG.md"));
        for (String line : lines) {
            processLine(line);
        }
    }
}
