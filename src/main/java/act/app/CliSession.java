package act.app;

import act.Act;
import act.cli.CliDispatcher;
import act.cli.builtin.Exit;
import act.handler.CliHandler;
import act.util.Banner;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

class CliSession extends DestroyableBase implements Runnable {

    private static final AtomicInteger ID_GEN = new AtomicInteger();

    private int id;
    private CliServer server;
    private CliDispatcher dispatcher;
    private App app;
    private Socket socket;
    private long ts;
    private boolean exit;
    private Thread runningThread;

    CliSession(Socket socket, CliServer server) {
        this.socket = $.NPE(socket);
        this.server = $.NPE(server);
        this.app = server.app();
        this.dispatcher = app.cliDispatcher();
        id = ID_GEN.getAndIncrement();
        ts = $.ms();
    }

    int id() {
        return id;
    }

    /**
     * Check if this session is expired.
     * @param expiration the expiration in seconds
     * @return {@code true} if this session is expired
     */
    boolean expired(int expiration) {
        long l = expiration * 1000;
        return l < ($.ms() - ts);
    }

    @Override
    protected void releaseResources() {
        stop();
        server = null;
    }

    @Override
    public void run() {
        runningThread = Thread.currentThread();
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(Banner.banner(Act.VERSION));
            String user = username();

            while (!exit) {
                out.print("act[" + user + "]>");
                out.flush();
                final String line = reader.readLine();
                if (exit) {
                    out.println("session terminated");
                    return;
                }
                ts = $.ms();
                try {
                    app.detectChanges();
                } catch (RequestRefreshClassLoader refreshRequest) {
                    app.refresh();
                }
                if (S.blank(line)) {
                    continue;
                }

                CliContext context = new CliContext(line, app, out);

                //handle the command
                final CliHandler handler = dispatcher.handler(context.command());
                if (null == handler) {
                    context.println("Command not recognized: %s", context.command());
                    continue;
                }
                if (handler == Exit.INSTANCE) {
                    out.println("bye");
                    exit = true;
                    return;
                }
                try {
                    handler.handle(context);
                } catch (Exception e) {
                    if (e instanceof IOException) {
                        throw e;
                    }
                    out.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error(e, "Error processing cli session");
        } finally {
            server.remove(this);
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn(e, "Failed to close the socket");
            }
        }
    }

    private String username() {
        String user = System.getProperty("user.name");
        if (S.blank(user)) {
            user = System.getenv("USER");
            if (S.blank(user)) {
                user = System.getenv("USERNAME");
                if (S.blank(user)) {
                    user = "anonymous";
                }
            }
        }
        return user;
    }

    void stop() {
        exit = true;
        IO.close(socket);
        runningThread.interrupt();
    }

}
