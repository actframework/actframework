package act.db.util;

import act.cli.Command;
import org.osgl.$;
import org.osgl.util.S;

/**
 * Sequence number manipulation utility class
 */
@SuppressWarnings("unused")
public class SequenceNumberGenerator {

    private static volatile  _SequenceNumberGenerator impl;

    public static void registerImpl(_SequenceNumberGenerator impl) {
        SequenceNumberGenerator.impl = $.notNull(impl);
    }

    public static long next(String name) {
        return impl.next(name);
    }

    public static long get(String name) {
        return impl.get(name);
    }

    @SuppressWarnings("unused")
    public static class SequenceAdmin {

        @Command(name = "act.seq.next", help = "display the next number in the sequence specified")
        public long generateNext(String sequence) {
            return next(sequence);
        }

        @Command(name = "act.seq.get", help = "display the current number in the sequence specified")
        public long getCurrent(String sequence) {
            return get(sequence);
        }

    }

    public static void main(String[] args) {
        long l = 3;
        System.out.println(S.fmt("%06d", l));
    }

}
