package org.osgl.util;

import act.TestBase;
import act.util.UnknownProtocolException;
import org.junit.Test;

import static act.util.Protocol.*;

/**
 * Test cases for {@link act.util.Protocol}
 */
public class ProtocolTest extends TestBase {
    @Test
    public void parseValidProtocolNameInCapitalOrLowerCaseLetters() {
        eq(FTP, lookup("ftp"));
        eq(HTTP, lookup("HttP"));
        eq(HTTPS, lookup("HTTPS"));
    }

    @Test
    public void parseValidProtocolNameWithEndingSpaces() {
        eq(WS, lookup(" ws "));
        eq(WSS, lookup(" wss"));
    }

    @Test(expected = UnknownProtocolException.class)
    public void parseUnknownProtocolName() {
        lookup("gopher");
    }
}
