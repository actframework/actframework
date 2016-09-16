package act.db.util;

import act.cli.Command;
import act.cli.Required;
import org.osgl.$;
import org.osgl.inject.annotation.TypeOf;

import javax.inject.Inject;

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


    public static class Provider implements javax.inject.Provider<_SequenceNumberGenerator> {
        @Inject
        @TypeOf
        private java.util.List<_SequenceNumberGenerator> generators;

        @Override
        public _SequenceNumberGenerator get() {
            if (generators.size() > 1) {
                for (_SequenceNumberGenerator gen: generators) {
                    if (!_SequenceNumberGenerator.InMemorySequenceNumberGenerator.class.isInstance(gen)) {
                        return gen;
                    }
                }
            }
            return generators.get(0);
        }
    }

    @SuppressWarnings("unused")
    public static class SequenceAdmin {

        @Command(name = "act.seq.next", help = "display the next number in the sequence specified")
        public long generateNext(
                @Required("specify sequence name") String sequence
        ) {
            return next(sequence);
        }

        @Command(name = "act.seq.get", help = "display the current number in the sequence specified")
        public long getCurrent(
                @Required("Specify sequence name") String sequence
        ) {
            return get(sequence);
        }

    }

}
