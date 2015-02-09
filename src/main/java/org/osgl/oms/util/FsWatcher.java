package org.osgl.oms.util;

import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import java.io.File;

/**
 * {@code FsWatcher} watches changes to a folder and all files/sub folders of the folder,
 * this includes delete/add/update
 */
public abstract class FsWatcher implements Runnable {

    protected static Logger logger = L.get(FsWatcher.class);

    private C.List<FsEventListener> listeners = C.newList();
    private File dir;

    protected FsWatcher(File file) {
        E.illegalArgumentIf(!file.isDirectory());
        this.dir = file;
    }

    public void registerListener(FsEventListener listener) {
        listeners.append(listener);
    }

    protected File base() {
        return dir;
    }

    protected final void trigger(final FsEvent e) {
        int n = listeners.size();
        for (int i = 0; i < n; ++i) {
            FsEventListener l = listeners.get(i);
            l.on(e);
        }
    }
}
