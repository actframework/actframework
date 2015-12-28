package act.cli.util;

import jline.Terminal2;
import jline.TerminalSupport;
import jline.UnixTerminal;
import net.wimpi.telnetd.io.BasicTerminalIO;
import org.osgl.$;

import java.io.IOException;

/**
 * Implement jline2 Terminal on Telnet Socket IO
 */
public class TelnetTerminal extends UnixTerminal implements Terminal2 {

    private BasicTerminalIO io;

    protected TelnetTerminal(BasicTerminalIO io) throws Exception {
        super("/dev/tty");
        this.io = $.NPE(io);
    }

    @Override
    public int getWidth() {
        return io.getColumns();
    }

    @Override
    public int getHeight() {
        return io.getRows();
    }

    @Override
    public boolean getBooleanCapability(String capability) {
        return false;
    }

    @Override
    public Integer getNumericCapability(String capability) {
        return null;
    }

    @Override
    public String getStringCapability(String capability) {
        return null;
    }
}
