package act.cli;

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

import act.Destroyable;
import act.app.*;
import act.cli.event.CliSessionStart;
import act.cli.event.CliSessionTerminate;
import act.cli.util.CliCursor;
import act.handler.CliHandler;
import act.job.JobContext;
import act.util.Banner;
import act.util.DestroyableBase;
import jline.Terminal;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import jline.console.history.PersistentHistory;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.IO;
import org.osgl.util.S;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

public class CliSession extends DestroyableBase implements Runnable {

    private static final Logger LOGGER = LogManager.get(CliSession.class);

    private String id;
    private CliServer server;
    protected App app;
    private Socket socket;
    private long ts;
    private boolean exit;
    private Thread runningThread;
    private ConsoleReader console;
    private FileHistory history;
    private CliCursor cursor;
    private CommandNameCompleter commandNameCompleter;
    // the current handler
    private CliHandler handler;
    private boolean daemon;
    private CliContext cliContext;
    private boolean started;
    /**
     * Allow user command to attach data to the context and fetched for later use.
     * <p>
     *     A typical usage scenario is user command wants to set up a "context" for the
     *     following commands. However it shall provide a command to exit the "context"
     * </p>
     */
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Construct a CliOverHttp session
     * @param context the ActionContext
     */
    protected CliSession(ActionContext context) {
        this.app = context.app();
        this.id = context.session().id();
        this.ts = $.ms();
    }

    public CliSession(Socket socket, CliServer server) {
        this.socket = $.NPE(socket);
        this.server = $.NPE(server);
        this.app = server.app();
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

    public CliSession dameon(boolean daemon) {
        this.daemon = daemon;
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

    public Terminal term() {
        return console.getTerminal();
    }

    public <T> T attribute(String key) {
        return $.cast(attributes.get(key));
    }

    /**
     * Check if this session is expired.
     * @param expiration the expiration in seconds
     * @return {@code true} if this session is expired
     */
    public boolean expired(int expiration) {
        if (null == cliContext) {
            return started; // see GH #1140
        }
        if ((daemon || cliContext.inProgress()) && !cliContext.disconnected()) {
            return false;
        }
        long l = expiration * 1000;
        return l < ($.ms() - ts);
    }

    @Override
    protected void releaseResources() {
        stop();
        server = null;
        Destroyable.Util.tryDestroyAll(attributes.values(), ApplicationScoped.class);
    }

    private void tryTuneTelnetOptions(OutputStream os) throws IOException {
        // placeholder - if we can tell the incoming connection is from
        // a telnet program we can negotiate the stty options here
//        Writer w = new OutputStreamWriter(os, "ISO-8859-1");
//        w.write((char)255);
//        w.write((char)251);
//        w.write((char)1);
//        w.write((char)255);
//        w.write((char)251);
//        w.write((char)3);
//        w.write((char)255);
//        w.write((char)252);
//        w.write((char)34);
//        w.flush();
    }

    private File historyFile() {
        String fileName = System.getProperty("cli.history");
        if (S.blank(fileName)) {
            fileName = ".act.cli-history";
        }
        return new File(fileName);
    }

    @Override
    public void run() {
        runningThread = Thread.currentThread();
        try {
            app.eventBus().emitSync(new CliSessionStart(this));
            OutputStream os = socket.getOutputStream();
            tryTuneTelnetOptions(os);
            console = new ConsoleReader(socket.getInputStream(), os);
            history = new FileHistory(historyFile(), true);
            console.setHistory(history);
            String banner = Banner.cachedBanner();
            printBanner(banner, console);
            String appName = App.instance().name();
            if (S.blank(appName)) {
                appName = "act";
            }
            console.setPrompt(S.fmt("%s[%s]>", appName, id));
            console.addCompleter(commandNameCompleter);
            console.getTerminal().setEchoEnabled(false);

            while (!exit) {
                final String line = console.readLine();
                if (exit) {
                    console.println("session terminated");
                    console.flush();
                    started = true;
                    return;
                }
                ts = $.ms();
                if (app.checkUpdatesNonBlock(true)) {
                    console.print("app reloading ...");
                    started = true;
                    return;
                }
                if (S.blank(line)) {
                    continue;
                }

                try {
                    CliContext context = new CliContext(line, app, console, this);
                    cliContext = context;
                    JobContext.init(id());
                    started = true;
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
                } finally {
                    JobContext.clear();
                }
            }
        } catch (InterruptedIOException e) {
            LOGGER.info("session thread interrupted");
        } catch (SocketException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e, "Error processing cli session");
        } finally {
            IO.flush(history);
            if (null != server) {
                server.remove(this);
            }
            IO.close(socket);
            app.eventBus().emitSync(new CliSessionTerminate(this));
        }
    }

    public void stop() {
        exit = true;
        if (null != runningThread) {
            runningThread.interrupt();
        }
        console = null;
        IO.close(socket);
    }

    public void stop(String message) {
        if (null != console) {
            PrintWriter pw = new PrintWriter(console.getOutput());
            pw.println(message);
            pw.flush();
        }
        stop();
    }

    void handler(CliHandler handler) {
        handler.resetCursor(this);
        this.handler = handler;
    }

    private static void printBanner(String banner, ConsoleReader console) throws IOException {
        String[] lines = banner.split("[\n\r]");
        for (String line : lines) {
            console.println(line);
        }
    }

}
