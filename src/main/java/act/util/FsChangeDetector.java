package act.util;

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

import act.exception.ActException;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.io.File;
import java.util.*;

/**
 * {@code FsChangeDetector} detects changes files in a folder and all sub folders.
 * The changes includes delete/add/update events
 */
public class FsChangeDetector {

    protected static Logger logger = L.get(FsChangeDetector.class);

    private List<FsEventListener> listeners = new ArrayList<>();
    private final File dir;
    private final $.Predicate<String> fileNameFilter;
    private final Map<String, Long> timestamps = new HashMap<>();
    private final int contextLen;
    private final String context;
    private final $.Var<Long> lastChecksum = $.var(0L);

    public FsChangeDetector(File file, $.Predicate<String> fileNameFilter) {
        this.dir = file;
        this.fileNameFilter = fileNameFilter;
        this.context = file.getAbsolutePath();
        this.contextLen = context.length();
        initialWalkThrough();
    }


    public FsChangeDetector(File file, $.Predicate<String> fileNameFilter, FsEventListener... listeners) {
        this(file, fileNameFilter);
        this.listeners.addAll(C.listOf(listeners));
    }

    public void registerListener(FsEventListener listener) {
        listeners.add(listener);
    }

    public void detectChanges() {
        $.Var<Long> checksum = $.var(0L);
        Map<String, Long> newTimestamps = walkThrough(dir, checksum);
        if (!checksum.get().equals(lastChecksum.get())) {
            if (dir.isDirectory()) {
                check(newTimestamps);
                timestamps.clear();
                timestamps.putAll(newTimestamps);
            } else {
                trigger(new FsEvent(FsEvent.Kind.MODIFY, dir.getAbsolutePath()));
            }
            lastChecksum.set(checksum);
        }
    }

    private void initialWalkThrough() {
        walkThrough(dir, timestamps, lastChecksum);
    }

    private Map<String, Long> walkThrough(File file, $.Var<Long> checksum) {
        Map<String, Long> map = new HashMap<>();
        walkThrough(file, map, checksum);
        return map;
    }

    private void check(Map<String, Long> newTimestamps) {
        C.List<FsEvent> events = C.newSizedList(3);

        C.Set<String> set0 = C.set(timestamps.keySet());
        C.Set<String> set1 = C.set(newTimestamps.keySet());

        C.Set<String> added = set1.without(set0);
        if (!added.isEmpty()) {
            events.add(createEvent(FsEvent.Kind.CREATE, added));
        }

        C.Set<String> removed = set0.without(set1);
        if (!removed.isEmpty()) {
            events.add(createEvent(FsEvent.Kind.DELETE, removed));
        }

        C.Set<String> retained = set1.withIn(set0);
        C.Set<String> modified = modified(retained, newTimestamps);
        if (!modified.isEmpty()) {
            events.add(createEvent(FsEvent.Kind.MODIFY, modified));
        }

        if (!events.isEmpty()) {
            trigger(events.toArray(new FsEvent[events.size()]));
        }
    }

    private FsEvent createEvent(FsEvent.Kind kind, C.Set<String> paths) {
        return new FsEvent(kind, prependContext(paths));
    }

    private C.Set<String> modified(C.Set<String> retained, Map<String, Long> newTimestamps) {
        C.Set<String> modified = C.newSet();
        for (String path : retained) {
            long ts0 = timestamps.get(path);
            long ts1 = newTimestamps.get(path);
            if (ts0 != ts1) {
                modified.add(path);
            }
        }
        return modified;
    }

    private Set<String> prependContext(C.Set<String> paths) {
        return C.set(paths.map(new $.F1<String, String>() {
            @Override
            public String apply(String s) throws NotAppliedException, $.Break {
                return context + s;
            }
        }));
    }

    private void walkThrough(File file, Map<String, Long> timestamps, $.Var<Long> checksum) {
        if (file.isDirectory()) {
            Files.filter(file, fileNameFilter, visitor(timestamps, checksum));
        } else {
            if (!file.exists()) {
                throw new ActException("File deleted: %s", dir);
            }
            checksum.set(file.lastModified());
        }
    }

    private $.Visitor<File> visitor(final Map<String, Long> timestamps, final $.Var<Long> checksum) {
        return new $.Visitor<File>() {
            @Override
            public void visit(File file) throws $.Break {
                long ts = file.lastModified();
                String path = file.getAbsolutePath().substring(contextLen);
                checksum.set(checksum.get() + path.hashCode() + ts);
                timestamps.put(file.getAbsolutePath().substring(contextLen), file.lastModified());
            }
        };
    }

    private void trigger(final FsEvent... events) {
        int n = listeners.size();
        for (int i = 0; i < n; ++i) {
            FsEventListener l = listeners.get(i);
            l.on(events);
        }
    }
}
