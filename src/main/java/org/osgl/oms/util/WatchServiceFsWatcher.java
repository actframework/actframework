package org.osgl.oms.util;

import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Implementing {@code FsWatcher} using the new java7 nio2
 * {@code WatchService}
 */
public class WatchServiceFsWatcher extends FsWatcher {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;

    public WatchServiceFsWatcher(File dir) {
        super(dir);
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw E.ioException(e);
        }
        keys = C.newMap();
        register(dir.toPath());
    }

    @Override
    public void run() {
        while (true) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }
            Path dir = keys.get(key);
            if (dir == null) {
                logger.error("WatchKey not recognized: " + key);
                continue;
            }

            for (WatchEvent<?> e : key.pollEvents()) {
                on(e, dir);
            }
            if (!key.reset()) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    logger.warn("watcher root has been removed, watch service exit");
                    return;
                }
            }
        }
    }

    private void on(WatchEvent<?> event, Path dir) {
        WatchEvent.Kind k = event.kind();
        Path path = (Path) event.context();
        Path child = dir.resolve(path);
        String s = child.toAbsolutePath().toString();
        FsEvent e;
        boolean isDir = isDirectory(child, NOFOLLOW_LINKS);
        if (!isDir && !s.endsWith(".java")) {
            // ignore non-java files
            return;
        }
        if (ENTRY_CREATE.equals(k)) {
            if (isDir) {
                register(child);
                return;
            } else {
                e = new FsEvent(FsEvent.Kind.CREATE, s);
            }
        } else if (ENTRY_DELETE.equals(k)) {
            e = new FsEvent(FsEvent.Kind.DELETE, s);
        } else if (ENTRY_MODIFY.equals(k)) {
            e = new FsEvent(FsEvent.Kind.MODIFY, s);
        } else {
            // ignore unknown event kind
            return;
        }
        trigger(e);
    }

    private void register(Path dir) {
        try {
            // register directory and sub-directories
            walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    if (dir.startsWith(".")) {
                        // skip .svn dir
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                    keys.put(key, dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw E.ioException(e);
        }
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
        FsWatcher watcher = new WatchServiceFsWatcher(file);
        watcher.registerListener(testListener);
        watcher.run();
    }
}
