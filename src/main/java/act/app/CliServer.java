package act.app;

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

import act.Act;
import act.Destroyable;
import act.cli.CliSession;
import act.conf.AppConfig;
import act.exception.PortOccupiedException;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Servicing CLI session
 */
@ApplicationScoped
public class CliServer extends AppServiceBase<CliServer> implements Runnable {

    private static final Logger logger = LogManager.get(CliServer.class);

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
                InetAddress addr = socket.getInetAddress();
                if (!addr.isLoopbackAddress() && !addr.isSiteLocalAddress() && !addr.isLinkLocalAddress()) {
                    logger.warn("remote connection request rejected: " + addr.getHostAddress());
                    socket.close();
                    continue;
                }
                CliSession session = new CliSession(socket, this);
                sessions.put(session.id(), session);
                executor.submit(session);
            } catch (Exception e) {
                if (isDestroyed()) {
                    return;
                }
                logger.error(e, "Error processing CLI session");
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
            logger.warn(e, "error closing server socket");
        } finally {
            serverSocket = null;
        }
    }

    void start() {
        if (running()) {
            return;
        }
        try {
            if (Act.isTest()) {
                AppConfig.clearRandomServerSocket(port);
            }
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
                        List<CliSession> toBeRemoved = new ArrayList<>();
                        for (CliSession session : sessions.values()) {
                            if (session.expired(expiration)) {
                                toBeRemoved.add(session);
                            }
                        }
                        for (CliSession session: toBeRemoved) {
                            session.stop("Your session has expired");
                            sessions.remove(session.id());
                        }
                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException e) {
                            throw new UnexpectedException(e);
                        }
                        if (app().checkUpdatesNonBlock(false)) {
                            return;
                        }
                    }
                }
            });
        } catch (IOException e) {
            if (e instanceof BindException) {
                throw new PortOccupiedException(port);
            }
            Throwable t = e.getCause();
            if (null != t && t.getMessage().contains("Address already in use")) {
                throw new PortOccupiedException(port);
            }
            throw new ConfigurationException(e, "Cannot start CLI server on port: %s", port);
        }
    }

    public void logStart() {
        Act.LOGGER.info("CLI server started on port: %s", port);
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
