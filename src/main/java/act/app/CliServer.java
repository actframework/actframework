package act.app;

import act.Destroyable;
import act.cli.CliSession;
import act.exception.ActException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicing CLI session
 */
@ApplicationScoped
public class CliServer extends AppServiceBase<CliServer> implements Runnable {

    private static final Logger log = LogManager.get(CliServer.class);

    private ScheduledThreadPoolExecutor executor;
    private AtomicBoolean running = new AtomicBoolean();
    private ConcurrentMap<String, CliSession> sessions = new ConcurrentHashMap<String, CliSession>();
    private int port;
    private ServerSocket serverSocket;
    private Thread monitorThread;

    @Inject
    CliServer(App app) {
        super(app);
        port = app.config().cliPort();
        initExecutor(app);
        start();
    }

    @Override
    protected void releaseResources() {
        stop();
        executor.shutdown();
        Destroyable.Util.destroyAll(sessions.values(), ApplicationScoped.class);
        sessions.clear();
    }

    public void remove(CliSession session) {
        sessions.remove(session.id());
    }

    @Override
    public void run() {
        while (running()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                CliSession session = new CliSession(socket, this);
                sessions.put(session.id(), session);
                executor.submit(session);
            } catch (Exception e) {
                if (isDestroyed()) {
                    return;
                }
                log.error(e, "Error processing CLI session");
                stop();
                return;
            } finally {
                // Note we cannot close socket here. The ownership
                // of socket has been transferred to the CliSession
                // and it is up to the session to manage the socket
                // IO.close(socket);
            }
        }
    }

    void stop() {
        if (!running()) {
            return;
        }
        running.set(false);
        if (null != monitorThread) {
            monitorThread.interrupt();
            monitorThread = null;
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            log.warn(e, "error closing server socket");
        } finally {
            serverSocket = null;
        }
    }

    void start() {
        if (running()) {
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            // start server thread
            executor.submit(this);
            // start expiration monitor thread
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    monitorThread = Thread.currentThread();
                    int expiration = app().config().cliSessionExpiration();
                    while (running()) {
                        List<CliSession> toBeRemoved = C.newList();
                        for (CliSession session : sessions.values()) {
                            if (session.expired(expiration)) {
                                toBeRemoved.add(session);
                            }
                        }
                        for (CliSession session: toBeRemoved) {
                            session.stop("your session is timeout");
                            sessions.remove(session.id());
                        }
                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException e) {
                            return;
                        }
                        app().checkUpdates(false);
                    }
                }
            });
            log.info("CLI server started on port: %s", port);
        } catch (IOException e) {
            throw new ActException(e, "Cannot start CLI server on port: %s", port);
        }
    }

    boolean running() {
        return running.get();
    }

    private void initExecutor(App app) {
        // cli session thread + server thread + expiration monitor thread
        int poolSize = app.config().maxCliSession() + 2;
        executor = new ScheduledThreadPoolExecutor(poolSize, new AppThreadFactory("cli", true), new ThreadPoolExecutor.AbortPolicy());
    }

}
