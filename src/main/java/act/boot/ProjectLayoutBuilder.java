package act.boot;

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

import act.app.ProjectLayout;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Helper to build {@link ProjectLayout}
 */
public class ProjectLayoutBuilder implements ProjectLayout {

    private String src;
    private String tstSrc;
    private String rsrc;
    private String tstRsrc;
    private String lib;
    private String tstLib;
    private String tgt;
    private String routeTable;
    private String conf;

    private ProjectLayout _layout;

    private void _refresh() {
        _layout = toLayout();
    }

    public ProjectLayoutBuilder source(String src) {
        this.src = src;
        _refresh();
        return this;
    }

    public ProjectLayoutBuilder testSource(String s) {
        this.tstSrc = s;
        _refresh();
        return this;
    }

    public ProjectLayoutBuilder resource(String rsrc) {
        this.rsrc = rsrc;
        _refresh();
        return this;
    }

    public ProjectLayoutBuilder testResource(String s) {
        this.tstRsrc = s;
        _refresh();
        return this;
    }

    public ProjectLayoutBuilder lib(String lib) {
        this.lib = lib;
        _refresh();
        return this;
    }

    public ProjectLayoutBuilder testLib(String s) {
        this.tstLib = s;
        _refresh();
        return this;
    }

    public ProjectLayoutBuilder target(String tgt) {
        this.tgt = tgt;
        _refresh();
        return this;
    }

    public ProjectLayoutBuilder routeTable(String routeTable) {
        this.routeTable = routeTable;
        _refresh();
        return this;
    }

    public ProjectLayoutBuilder conf(String conf) {
        this.conf = conf;
        _refresh();
        return this;
    }

    @Override
    public File source(File appBase) {
        return _layout.source(appBase);
    }

    @Override
    public File testSource(File appBase) {
        return _layout.testSource(appBase);
    }

    @Override
    public File resource(File appBase) {
        return _layout.resource(appBase);
    }

    @Override
    public File testResource(File appBase) {
        return _layout.testResource(appBase);
    }

    @Override
    public String classes() {
        return _layout.classes();
    }

    @Override
    public File lib(File appBase) {
        return _layout.lib(appBase);
    }

    @Override
    public File testLib(File appBase) {
        return _layout.testLib(appBase);
    }

    @Override
    public File target(File appBase) {
        return _layout.target(appBase);
    }

    @Override
    public Map<String, List<File>> routeTables(File appBase) {
        return _layout.routeTables(appBase);
    }

    @Override
    public File conf(File appBase) {
        return _layout.conf(appBase);
    }

    public ProjectLayout toLayout() {
        return new ProjectLayout.CustomizedProjectLayout(src, tstSrc, rsrc, tstRsrc, lib, tstLib, tgt, routeTable, conf);
    }
}
