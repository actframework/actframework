package act.app;

import act.Act;
import act.cli.CliDispatcher;
import act.cli.CommandNameCompleter;
import act.cli.builtin.Exit;
import act.cli.builtin.Help;
import act.cli.event.CliSessionStart;
import act.cli.event.CliSessionTerminate;
import act.cli.util.CommandLineParser;
import act.handler.CliHandler;
import act.util.Banner;
import act.util.DestroyableBase;
import jline.console.ConsoleReader;
import org.osgl.$;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

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
    private CommandNameCompleter commandNameCompleter;

    CliSession(Socket socket, CliServer server) {
        this.socket = $.NPE(socket);
        this.server = $.NPE(server);
        this.app = server.app();
        this.dispatcher = app.cliDispatcher();
        id = app.cuid();
        ts = $.ms();
        commandNameCompleter = app.newInstance(CommandNameCompleter.class);
    }

    public String id() {
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
        app.eventBus().emitSync(new CliSessionStart(this));
        try {
            OutputStream os = socket.getOutputStream();
            console = new ConsoleReader(socket.getInputStream(), os);
            new PrintWriter(os).println(Banner.banner(Act.VERSION));
            console.setPrompt("act[" + id + "]>");
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
                    app.refresh();
                }
                if (S.blank(line)) {
                    continue;
                }

                CliContext context = new CliContext(id, line, app, console);

                //handle the command
                final CliHandler handler = dispatcher.handler(context.command());
                if (null == handler) {
                    context.println("Command not recognized: %s", context.command());
                    continue;
                }
                if (handler == Exit.INSTANCE) {
                    console.println("bye");
                    console.flush();
                    exit = true;
                    return;
                }
                CommandLineParser parser = context.commandLine();
                boolean help = parser.getBoolean("-h", "--help");
                if (help) {
                    Help.INSTANCE.showHelp(parser.command(), context);
                    continue;
                }
                try {
                    handler.handle(context);
                } catch (Exception e) {
                    console.println("Error: " + e.getMessage());
                }
            }
        } catch (InterruptedIOException e) {
            logger.info("session thread interrupted");
        } catch (SocketException e) {
            logger.error(e.getMessage());
        } catch (Throwable e) {
            logger.error(e, "Error processing cli session");
        } finally {
            server.remove(this);
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

}
