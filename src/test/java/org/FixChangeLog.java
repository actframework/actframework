package org;

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
