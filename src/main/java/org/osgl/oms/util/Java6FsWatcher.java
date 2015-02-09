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
    private final Map<String, Long> hashTable = C.newMap();
    private final int contextLen;
    private final String context;
    private final _.Var<Long> lastMagicNumber = _.var(0L);

    public Java6FsWatcher(File file) {
        super(file);
        context = file.getAbsolutePath();
        contextLen = context.length();
        hashTable.putAll(walkThrough(file, lastMagicNumber));
    }

    @Override
    public void run() {
        while (true) {
            long sleep = 1000L;
            _.Var<Long> magicNumber = _.var(0L);
            Map<String, Long> newHashMap = walkThrough(base(), magicNumber);
            if (!magicNumber.get().equals(lastMagicNumber.get())) {
                check(newHashMap);
                lastMagicNumber.set(magicNumber);
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

    private Map walkThrough(File file, _.Var<Long> magicNumber) {
        Map<String, Long> map = C.newMap();
        walkThrough(file, map, magicNumber);
        return map;
    }

    private void check(Map<String, Long> newHashTable) {
        C.Set<String> set0 = C.set(hashTable.keySet());
        C.Set<String> set1 = C.set(newHashTable.keySet());
        C.Set<String> added = set1.without(set0);
        handleAdded(added);
        C.Set<String> removed = set0.without(set1);
        handleRemoved(removed);
        C.Set<String> retained = set1.withIn(set0);
        checkTimestamp(retained, newHashTable);
        hashTable.clear();
        hashTable.putAll(newHashTable);
    }

    private void handleAdded(C.Set<String> added) {
        if (added.isEmpty()) return;
        FsEvent e = new FsEvent(FsEvent.Kind.CREATE, added);
        trigger(e);
    }

    private void handleRemoved(C.Set<String> removed) {
        if (removed.isEmpty()) return;
        FsEvent e = new FsEvent(FsEvent.Kind.DELETE, removed);
        trigger(e);
    }

    private void checkTimestamp(C.Set<String> retained, Map<String, Long> newHashTable) {
        C.Set<String> modified = C.newSet();
        for (String path : retained) {
            long ts0 = hashTable.get(path);
            long ts1 = newHashTable.get(path);
            if (ts0 != ts1) {
                modified.add(path);
            }
        }
        if (modified.isEmpty()) return;
        FsEvent e = new FsEvent(FsEvent.Kind.MODIFY, modified);
        trigger(e);
    }

    private Set<String> prependContext(C.Set<String> paths) {
        return paths.map(new _.F1<String, String>() {
            @Override
            public String apply(String s) throws NotAppliedException, _.Break {
                return null;
            }
        });
    }

    private void walkThrough(File file, Map<String, Long> hashTable, _.Var<Long> magicNumber) {
        Files.filter(file, FILTER, visitor(hashTable, magicNumber));
    }

    private _.Visitor<File> visitor(final Map<String, Long> hashTable, final _.Var<Long> magicNumber) {
        return new _.Visitor<File>() {
            @Override
            public void visit(File file) throws _.Break {
                long ts = file.lastModified();
                String path = file.getAbsolutePath().substring(contextLen);
                magicNumber.set(magicNumber.get() + path.hashCode() + ts);
                hashTable.put(file.getAbsolutePath().substring(contextLen), file.lastModified());
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
