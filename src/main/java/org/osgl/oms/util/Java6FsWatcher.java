package org.osgl.oms.util;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.S;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java 6 or below doesn't provide WatchService, thus we
 * have to implement our own
 */
public class Java6FsWatcher extends FsWatcher {

    private static _.Predicate<String> FILTER = S.F.endsWith(".java");
    private final Map<String, Long> timestamps = C.newMap();
    private final int contextLen;
    private final String context;
    private final _.Var<Long> lastChecksum = _.var(0L);

    public Java6FsWatcher(File file) {
        super(file);
        context = file.getAbsolutePath();
        contextLen = context.length();
        timestamps.putAll(walkThrough(file, lastChecksum));
    }

    @Override
    public void run() {
        while (true) {
            long sleep;
            _.Var<Long> checksum = _.var(0L);
            Map<String, Long> newTimestamps = walkThrough(base(), checksum);
            if (!checksum.get().equals(lastChecksum.get())) {
                check(newTimestamps);
                timestamps.clear();
                timestamps.putAll(newTimestamps);
                lastChecksum.set(checksum);
                sleep = 1000L;
            } else {
                sleep = 2000L;
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private Map walkThrough(File file, _.Var<Long> checksum) {
        Map<String, Long> map = C.newMap();
        walkThrough(file, map, checksum);
        return map;
    }

    private void check(Map<String, Long> newTimestamps) {
        C.Set<String> set0 = C.set(timestamps.keySet());
        C.Set<String> set1 = C.set(newTimestamps.keySet());
        C.Set<String> added = set1.without(set0);
        handleAdded(added);
        C.Set<String> removed = set0.without(set1);
        handleRemoved(removed);
        C.Set<String> retained = set1.withIn(set0);
        checkTimestamp(retained, newTimestamps);
    }

    private void handleAdded(C.Set<String> added) {
        if (added.isEmpty()) return;
        FsEvent e = new FsEvent(FsEvent.Kind.CREATE, prependContext(added));
        trigger(e);
    }

    private void handleRemoved(C.Set<String> removed) {
        if (removed.isEmpty()) return;
        FsEvent e = new FsEvent(FsEvent.Kind.DELETE, prependContext(removed));
        trigger(e);
    }

    private void checkTimestamp(C.Set<String> retained, Map<String, Long> newChecksum) {
        C.Set<String> modified = C.newSet();
        for (String path : retained) {
            long ts0 = timestamps.get(path);
            long ts1 = newChecksum.get(path);
            if (ts0 != ts1) {
                modified.add(path);
            }
        }
        if (modified.isEmpty()) return;
        FsEvent e = new FsEvent(FsEvent.Kind.MODIFY, prependContext(modified));
        trigger(e);
    }

    private Set<String> prependContext(C.Set<String> paths) {
        return C.set(paths.map(new _.F1<String, String>() {
            @Override
            public String apply(String s) throws NotAppliedException, _.Break {
                return context + s;
            }
        }));
    }

    private void walkThrough(File file, Map<String, Long> timestamps, _.Var<Long> checksum) {
        Files.filter(file, FILTER, visitor(timestamps, checksum));
    }

    private _.Visitor<File> visitor(final Map<String, Long> timestamps, final _.Var<Long> checksum) {
        return new _.Visitor<File>() {
            @Override
            public void visit(File file) throws _.Break {
                long ts = file.lastModified();
                String path = file.getAbsolutePath().substring(contextLen);
                checksum.set(checksum.get() + path.hashCode() + ts);
                timestamps.put(file.getAbsolutePath().substring(contextLen), file.lastModified());
            }
        };
    }

    private static FsEventListener testListener = new FsEventListener() {
        @Override
        public void on(FsEvent event) {
            List<String> paths = event.paths();
            FsEvent.Kind kind = event.kind();
            System.out.printf("Event: %s, affected: %s\n", kind, S.join("\n\t", paths));
        }
    };

    public static void main(String[] args) {
        File file = new File("P:\\ob\\src");
        FsWatcher watcher = new Java6FsWatcher(file);
        watcher.registerListener(testListener);
        watcher.run();
    }
}
