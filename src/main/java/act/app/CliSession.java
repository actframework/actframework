package act.app;

import act.Destroyable;
import act.cli.CliDispatcher;
import act.cli.CommandNameCompleter;
import act.cli.builtin.Exit;
import act.cli.builtin.Help;
import act.cli.event.CliSessionStart;
import act.cli.event.CliSessionTerminate;
import act.cli.util.CliCursor;
import act.cli.util.CommandLineParser;
import act.handler.CliHandler;
import act.util.Banner;
import act.util.DestroyableBase;
import jline.console.ConsoleReader;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

import static act.app.App.logger;

public class CliSession extends DestroyableBase implements Runnable {

    private String id;
    private CliServer server;
    private CliDispatcher dispatcher;
    private App app;
    private Socket socket;
    private long ts;
    private boolean exit;
    private Thread runningThread;
    private ConsoleReader console;
    private CliCursor cursor;
    private CommandNameCompleter commandNameCompleter;
    /**
     * Allow user command to attach data to the context and fetched for later use.
     * <p>
     *     A typical usage scenario is user command wants to set up a "context" for the
     *     following commands. However it shall provide a command to exit the "context"
     * </p>
     */
    private Map<String, Object> attributes = C.newMap();

    CliSession(Socket socket, CliServer server) {
        this.socket = $.NPE(socket);
        this.server = $.NPE(server);
        this.app = server.app();
        this.dispatcher = app.cliDispatcher();
        id = app.cuid();
        ts = $.ms();
        commandNameCompleter = new CommandNameCompleter(app);
    }

    public String id() {
        return id;
    }

    public CliSession attribute(String key, Object val) {
        attributes.put(key, val);
        return this;
    }

    public CliSession removeAttribute(String key) {
        attributes.remove(key);
        return this;
    }

    public CliCursor cursor() {
        return cursor;
    }

    public CliSession cursor(CliCursor cursor) {
        this.cursor = cursor;
        return this;
    }

    public void removeCursor() {
        cursor = null;
    }

    public <T> T attribute(String key) {
        return $.cast(attributes.get(key));
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
        Destroyable.Util.tryDestroyAll(attributes.values());
    }

    @Override
    public void run() {
        runningThread = Thread.currentThread();
        try {
            app.eventBus().emitSync(new CliSessionStart(this));
            OutputStream os = socket.getOutputStream();
            console = new ConsoleReader(socket.getInputStream(), os);
            String banner = Banner.cachedBanner();
            printBanner(banner, console);
            String appName = App.instance().name();
            if (S.blank(appName)) {
                appName = "act";
            }
            console.setPrompt(S.fmt("%s[%s]>", appName, id));
            console.addCompleter(commandNameCompleter);

            while (!exit) {
                final String line = console.readLine();
                if (exit) {
                    console.println("session terminated");
                    console.flush();
                    return;
                }
                ts = $.ms();
                try {
                    app.detectChanges();
                } catch (RequestRefreshClassLoader refreshRequest) {
                    refreshRequest.doRefresh(app);
                    return;
                }
                if (S.blank(line)) {
                    continue;
                }

                try {
                    CliContext context = new CliContext(line, app, console, this);
                    context.handle();
                } catch ($.Break b) {
                    Object payload = b.get();
                    if (null == payload) {
                        continue;
                    }
                    if (payload instanceof Boolean) {
                        exit = b.get();
                    } else if (payload instanceof String) {
                        console.println((String) payload);
                    } else {
                        console.println(S.fmt("INTERNAL ERROR: unknown payload type: %s", payload.getClass()));
                    }
                }
            }
        } catch (InterruptedIOException e) {
            logger.info("session thread interrupted");
        } catch (SocketException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e, "Error processing cli session");
        } finally {
            if (null != server) {
                server.remove(this);
            }
            IO.close(socket);
            app.eventBus().emitSync(new CliSessionTerminate(this));
        }
    }

    void stop() {
        exit = true;
        if (null != runningThread) {
            runningThread.interrupt();
        }
        console = null;
        IO.close(socket);
    }

    void stop(String message) {
        if (null != console) {
            PrintWriter pw = new PrintWriter(console.getOutput());
            pw.println(message);
            pw.flush();
        }
        stop();
    }

    private static void printBanner(String banner, ConsoleReader console) throws IOException {
        String[] lines = banner.split("[\n\r]");
        for (String line : lines) {
            console.println(line);
        }
    }

}
