package org.osgl.oms.controller.meta;

import org.osgl.oms.asm.Label;
import org.osgl.util.E;
import org.osgl.util.S;

public class LocalVariableMetaInfo {
    private Label start;
    private Label end;
    private String name;
    private String type;
    private int index;

    public LocalVariableMetaInfo(int index, String name, String type, Label start, Label end) {
        E.NPE(name, start, end);
        this.index = index;
        this.name = name;
        this.type = type;
        this.start = start;
        this.end = end;
    }

    public Label start() {
        return start;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public int index() {
        return index;
    }

    @Override
    public String toString() {
        // print out the local variable className as
        // $index   $className   $start_label  $end_label
        return S.fmt("%s\t%s\t%s\t%s", index, name, start, end);
    }
}
