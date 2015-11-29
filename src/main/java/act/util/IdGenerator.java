package act.util;

import org.joda.time.DateTime;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generate unique ID in a cluster
 */
public class IdGenerator {
    /**
     * Implementation of {@code StartIdProvider} shall return a
     * unique id per each system start
     */
    public static interface StartIdProvider {
        /**
         * Returns the system start ID. The start ID shall be different
         * between System starts, but it shall remaining the same value
         * within one system life time
         */
        String startId();

        /**
         * Generate system start ID based on timestamp
         */
        public static class Timestamp implements StartIdProvider {
            private final String id;
            public Timestamp() {
                long origin = DateTime.parse("2015-08-23").getMillis();
                // let's assume the system cannot be restart within 10 seconds
                long l = ($.ms() - origin) / 1000 / 10;
                id = longToStr(l);
            }

            @Override
            public String startId() {
                return id;
            }
        }

        /**
         * Generate system start ID based on incremental sequence. The newly generated ID
         * will be write to a File
         */
        public static class FileBasedStartCounter implements StartIdProvider {
            private final String id;

            public FileBasedStartCounter() {
                this("_sys_start.do.not.delete");
            }

            public FileBasedStartCounter(String path) {
                File file = new File(path);
                if (file.exists()) {
                    String s = IO.readContentAsString(file);
                    long seq = Long.parseLong(s);
                    seq = seq + 1;
                    IO.writeContent(S.str(seq), file);
                    id = longToStr(seq);
                } else {
                    id = "0";
                    IO.writeContent(id, file);
                }
            }

            @Override
            public String startId() {
                return id;
            }
        }

        /**
         * Default start ID provider will try to use the {@link act.util.IdGenerator.StartIdProvider.FileBasedStartCounter}. In case
         * File IO is not allowed (e.g. in GAE), then it will use {@link act.util.IdGenerator.StartIdProvider.Timestamp}
         */
        public static class DefaultStartIdProvider implements StartIdProvider {

            private StartIdProvider delegate;

            public DefaultStartIdProvider() {
                this("_sys_start.do.not.delete");
            }

            public DefaultStartIdProvider(String path) {
                try {
                    delegate = new FileBasedStartCounter(path);
                    delegate.startId();
                } catch (Exception e) {
                    delegate = new Timestamp();
                }
            }

            @Override
            public String startId() {
                return delegate.startId();
            }
        }
    }

    /**
     * {@code SequenceProvider} shall generate unique ID within
     * one JVM per each call
     */
    public static interface SequenceProvider {
        String seqId();

        public static class AtomicLongSeq implements SequenceProvider {
            private final AtomicLong seq = new AtomicLong(0);

            @Override
            public String seqId() {
                return longToStr(seq.incrementAndGet());
            }
        }
    }

    public static interface NodeIdProvider {
        String nodeId();

        public static class IpProvider implements NodeIdProvider {

            private static enum EffectiveBytes {
                ONE(1), TWO(2), THREE(3), FOUR(4);
                private int value;
                private EffectiveBytes(int value) {
                    this.value = value;
                }
                public static EffectiveBytes valueOf(int n) {
                    switch (n) {
                        case 1: return ONE;
                        case 2: return TWO;
                        case 3: return THREE;
                        case 4: return FOUR;
                        default :
                            throw E.unexpected("Invalid EffectiveByte value: %s", n);
                    }
                }
            }

            private final EffectiveBytes effectiveBytes;
            private final String id;

            public IpProvider() {
                this(4);
            }

            public IpProvider(int effectBytes) {
                this.effectiveBytes = EffectiveBytes.valueOf(effectBytes);
                String ip = LocalIpAddressUtil.ip();
                String[] sa = ip.split("\\.");
                int n = effectiveBytes.value;
                long l = 0;
                for (int i = 0; i < n; ++i) {
                    String b = sa[3 - i];
                    long factor = 1;
                    for (int j = 0; j < i; ++j) {
                        factor = factor * 256;
                    }
                    l += Long.valueOf(b) * factor;
                }
                id = longToStr(l);
            }

            public String nodeId() {
                return id;
            }

        }
    }

    private final NodeIdProvider nodeIdProvider;
    private final StartIdProvider startIdProvider;
    private final SequenceProvider sequenceProvider;

    /**
     * Create a default IdGenerator with following configuration:
     * <ul>
     *     <li>Node ID provider: four byte IP address</li>
     *     <li>Start ID provider: stored in <code>_sys_start.do.not.delete</code> file</li>
     *     <li>Sequence ID provider: Atomic Long sequence</li>
     * </ul>
     */
    public IdGenerator() {
        nodeIdProvider = new NodeIdProvider.IpProvider();
        startIdProvider = new StartIdProvider.DefaultStartIdProvider();
        sequenceProvider = new SequenceProvider.AtomicLongSeq();
    }

    /**
     * Create a default IdGenerator with specified node id provider, start id provider and sequence provider:
     */
    public IdGenerator(NodeIdProvider nodeIdProvider, StartIdProvider startIdProvider, SequenceProvider sequenceProvider) {
        this.nodeIdProvider = Objects.requireNonNull(nodeIdProvider);
        this.startIdProvider = Objects.requireNonNull(startIdProvider);
        this.sequenceProvider = Objects.requireNonNull(sequenceProvider);
    }

    /**
     * Create a default IdGenerator with following configuration:
     * <ul>
     *     <li>Node ID provider: N byte IP address, where N is specified by effectiveIpBytes argument</li>
     *     <li>Start ID provider: stored in <code>_sys_start.do.not.delete</code> file</li>
     *     <li>Sequnce ID provider: Atomic Long sequence</li>
     * </ul>
     */
    public IdGenerator(int effectiveIpBytes) {
        this.nodeIdProvider = new NodeIdProvider.IpProvider(effectiveIpBytes);
        this.startIdProvider = new StartIdProvider.DefaultStartIdProvider();
        this.sequenceProvider = new SequenceProvider.AtomicLongSeq();
    }

    /**
     * Create a default IdGenerator with following configuration:
     * <ul>
     *     <li>Node ID provider: N byte IP address, where N is specified by effectiveIpBytes argument</li>
     *     <li>Start ID provider: use start ID file specified by startIdFile argument</li>
     *     <li>Sequnce ID provider: Atomic Long sequence</li>
     * </ul>
     */
    public IdGenerator(int effectiveIpBytes, String startIdFile) {
        this.nodeIdProvider = new NodeIdProvider.IpProvider(effectiveIpBytes);
        this.startIdProvider = new StartIdProvider.DefaultStartIdProvider(startIdFile);
        this.sequenceProvider = new SequenceProvider.AtomicLongSeq();
    }

    /**
     * Create a default IdGenerator with following configuration:
     * <ul>
     *     <li>Node ID provider: 4 byte IP address</li>
     *     <li>Start ID provider: use start ID file specified by startIdFile argument</li>
     *     <li>Sequnce ID provider: Atomic Long sequence</li>
     * </ul>
     */
    public IdGenerator(String startIdFile) {
        this.nodeIdProvider = new NodeIdProvider.IpProvider();
        this.startIdProvider = new StartIdProvider.DefaultStartIdProvider(startIdFile);
        this.sequenceProvider = new SequenceProvider.AtomicLongSeq();
    }

    /**
     * Generate a unique ID across the cluster
     * @return
     */
    public String genId() {
        StringBuilder sb = S.builder();
        sb.append(nodeIdProvider.nodeId()).append(startIdProvider.startId()).append(sequenceProvider.seqId());
        return sb.toString();
    }

    public static void main(String[] args) {
        IdGenerator idGen = new IdGenerator(1);
        for (int i = 0; i < 10; ++i) {
            System.out.println(idGen.genId());
        }
        idGen = new IdGenerator(4);
        for (int i = 0; i < 10; ++i) {
            System.out.println(idGen.genId());
        }
    }

    /**
     * Extended char table for representing a number as a String
     */
    private final static char[] digits = {
            '0' , '1' , '2' , '3' , '4' , '5' ,
            '6' , '7' , '8' , '9' , 'a' , 'b' ,
            'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
            'i' , 'j' , 'k' , 'm' , 'n' ,
            'o' , 'p' , 'q' , 'r' , 's' , 't' ,
            'u' , 'v' , 'w' , 'x' , 'y' , 'z' ,
            'A' , 'B' , 'C' , 'D' , 'E' , 'F' ,
            'G' , 'H' , 'J' , 'K' , 'L' ,
            'M' , 'N' , 'P' , 'Q' , 'R' ,
            'S' , 'T' , 'U' , 'V' , 'W' , 'X' ,
            'Y' , 'Z' , '!' , '$' , '%' , '&' ,
            '.' , ',' , ';' , ':' , '=' , '?' ,
            '+' , '-' , '*' , '/' , '<' , '>' ,
    };

    private final static int MAX_RADIX = digits.length;

    /**
     * Code copied from JDK Long.toString(long, String)
     */
    private static String longToStr(long l) {
        int radix = MAX_RADIX;
        char[] buf = new char[65];
        int charPos = 64;
        boolean negative = (l < 0);

        if (!negative) {
            l = -l;
        }

        while (l <= -radix) {
            buf[charPos--] = digits[(int)(-(l % radix))];
            l = l / radix;
        }
        buf[charPos] = digits[(int)(-l)];

        if (negative) {
            buf[--charPos] = '-';
        }

        return new String(buf, charPos, (65 - charPos));
    }

}
