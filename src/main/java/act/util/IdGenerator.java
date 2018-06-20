package act.util;

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

import org.joda.time.DateTime;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generate unique ID in a cluster
 */
public class IdGenerator {
    /**
     * Implementation of {@code StartIdProvider} shall return a
     * unique id per each system start
     */
    public interface StartIdProvider {
        /**
         * Returns the system start ID. The start ID shall be different
         * between System starts, but it shall remaining the same value
         * within one system life time
         */
        long startId();

        /**
         * Generate system start ID based on timestamp
         */
        class Timestamp implements StartIdProvider {
            private final long id;
            public Timestamp() {
                long origin = DateTime.parse("2016-05-10").getMillis();
                // let's assume the system cannot be restart within 10 seconds
                long l = ($.ms() - origin) / 1000 / 10;
                id = l;
            }

            @Override
            public long startId() {
                return id;
            }
        }

        /**
         * Generate system start ID based on incremental sequence. The newly generated ID
         * will be write to a File
         */
        class FileBasedStartCounter implements StartIdProvider {
            private final long id;

            public FileBasedStartCounter() {
                this(".act.id-global");
            }

            public FileBasedStartCounter(String path) {
                File file = new File(path);
                if (file.exists()) {
                    String s = IO.readContentAsString(file);
                    long seq = Long.parseLong(s);
                    seq = seq + 1;
                    IO.write(S.str(seq), file);
                    id = (seq);
                } else {
                    id = 0;
                    IO.write(Long.toString(id), file);
                }
            }

            @Override
            public long startId() {
                return id;
            }
        }

        /**
         * Default start ID provider will try to use the {@link act.util.IdGenerator.StartIdProvider.FileBasedStartCounter}. In case
         * File IO is not allowed (e.g. in GAE), then it will use {@link act.util.IdGenerator.StartIdProvider.Timestamp}
         */
        class DefaultStartIdProvider implements StartIdProvider {

            private StartIdProvider delegate;

            public DefaultStartIdProvider() {
                this(".act.id-global");
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
            public long startId() {
                return delegate.startId();
            }
        }
    }

    /**
     * {@code SequenceProvider} shall generate unique ID within
     * one JVM per each call
     */
    public interface SequenceProvider {
        long seqId();

        class AtomicLongSeq implements SequenceProvider {
            private final AtomicLong seq = new AtomicLong(0);

            @Override
            public long seqId() {
                return (seq.incrementAndGet());
            }
        }
    }

    public interface NodeIdProvider {
        long nodeId();

        class IpProvider implements NodeIdProvider {

            private enum EffectiveBytes {
                ONE(1), TWO(2), THREE(3), FOUR(4);
                private int value;
                EffectiveBytes(int value) {
                    this.value = value;
                }
                public static EffectiveBytes valueOf(int n) {
                    switch (n) {
                        case 1: return ONE;
                        case 2: return TWO;
                        case 3: return THREE;
                        case 4: return FOUR;
                        default :
                            throw E.unexpected("Invalid EffectiveByte value: %s. Expected: 1 - 4", n);
                    }
                }
            }

            private final EffectiveBytes effectiveBytes;
            private final long id;

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
                id = (l);
            }

            public long nodeId() {
                return id;
            }

        }
    }

    public interface LongEncoder {

        String longToStr(long l);

        abstract class LongEncoderBase implements LongEncoder {

            private final char[] digits;
            private final int MAX_RADIX;
            public LongEncoderBase(char[] digits) {
                this.digits = digits;
                this.MAX_RADIX = digits.length;
            }
            /**
             * Code copied from JDK Long.toString(long, String)
             */
            public String longToStr(long l) {
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
    }

    public static class UnsafeLongEncoder extends LongEncoder.LongEncoderBase {

        /**
         * Extended char table for representing a number as a String
         */
        private final static char[] digits = {
                '0', '1', '2', '3', '4', '5',
                '6', '7', '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f', 'g', 'h',
                'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z',
                'A', 'B', 'C', 'D', 'E', 'F',
                'G', 'H', 'I', 'J', 'K', 'L',
                'M', 'N', 'O', 'P', 'Q', 'R',
                'S', 'T', 'U', 'V', 'W', 'X',
                'Y', 'Z', '!', '$', '%', '&',
                '.', ',', ';', ':', '=', '?',
                '+', '-', '*', '/', '<', '>',
                '_', '~', '#', '^', '@', '|',
                '(', ')', '[', ']', '{', '}'
        };

        public UnsafeLongEncoder() {
            super(digits);
        }

    }

    public static class SafeLongEncoder extends LongEncoder.LongEncoderBase {
        /**
         * Extended char table for representing a number as a String
         */
        private final static char[] digits = {
                '0', '1', '2', '3', '4', '5',
                '6', '7', '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f', 'g', 'h',
                'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z',
                'A', 'B', 'C', 'D', 'E', 'F',
                'G', 'H', 'I', 'J', 'K', 'L',
                'M', 'N', 'O', 'P', 'Q', 'R',
                'S', 'T', 'U', 'V', 'W', 'X',
                'Y', 'Z', '.', '-', '_', '~',
        };

        public SafeLongEncoder() {
            super(digits);
        }

    }

    public static final LongEncoder SAFE_ENCODER = new SafeLongEncoder();
    public static final LongEncoder UNSAFE_ENCODER = new UnsafeLongEncoder();

    private final NodeIdProvider nodeIdProvider;
    private final StartIdProvider startIdProvider;
    private final SequenceProvider sequenceProvider;
    private LongEncoder longEncoder;

    /**
     * Create a default IdGenerator with following configuration:
     * <ul>
     *     <li>Node ID provider: four byte IP address</li>
     *     <li>Start ID provider: stored in <code>.act.id-global</code> file</li>
     *     <li>Sequence ID provider: Atomic Long sequence</li>
     *     <li>Long Encoder: {@link SafeLongEncoder}</li>
     * </ul>
     */
    public IdGenerator() {
        this(new NodeIdProvider.IpProvider(), new StartIdProvider.DefaultStartIdProvider(),
                new SequenceProvider.AtomicLongSeq(), SAFE_ENCODER);
    }

    /**
     * Create a default IdGenerator with following configuration:
     * <ul>
     *     <li>Node ID provider: four byte IP address</li>
     *     <li>Start ID provider: stored in <code>.act.id-global</code> file</li>
     *     <li>Sequence ID provider: Atomic Long sequence</li>
     *     <li>
     *         Long Encoder: {@link UnsafeLongEncoder} when `useUnsafeLongEncoder` is set to
     *         `true` or {@link SafeLongEncoder} otherwise
     *     </li>
     * </ul>
     * @param useUnsafeLongEncoder indicate use safe or unsafe long encoder
     */
    public IdGenerator(boolean useUnsafeLongEncoder) {
        this(new NodeIdProvider.IpProvider(), new StartIdProvider.DefaultStartIdProvider(),
                new SequenceProvider.AtomicLongSeq(), useUnsafeLongEncoder ? UNSAFE_ENCODER : SAFE_ENCODER);
    }

    /**
     * Create a default IdGenerator with specified node id provider, start id provider and sequence provider:
     */
    public IdGenerator(NodeIdProvider nodeIdProvider, StartIdProvider startIdProvider, SequenceProvider sequenceProvider, LongEncoder longEncoder) {
        this.nodeIdProvider = $.requireNotNull(nodeIdProvider);
        this.startIdProvider = $.requireNotNull(startIdProvider);
        this.sequenceProvider = $.requireNotNull(sequenceProvider);
        this.longEncoder = $.requireNotNull(longEncoder);
    }

    /**
     * Create a default IdGenerator with following configuration:
     * <ul>
     *     <li>Node ID provider: N byte IP address, where N is specified by effectiveIpBytes argument</li>
     *     <li>Start ID provider: stored in <code>.act.id-global</code> file</li>
     *     <li>Sequnce ID provider: Atomic Long sequence</li>
     * </ul>
     */
    public IdGenerator(int effectiveIpBytes) {
        this.nodeIdProvider = new NodeIdProvider.IpProvider(effectiveIpBytes);
        this.startIdProvider = new StartIdProvider.DefaultStartIdProvider();
        this.sequenceProvider = new SequenceProvider.AtomicLongSeq();
        this.longEncoder = SAFE_ENCODER;
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
        this.longEncoder = SAFE_ENCODER;
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
        this.longEncoder = SAFE_ENCODER;
    }

    /**
     * Generate a unique ID across the cluster
     * @return generated ID
     */
    public String genId() {
        S.Buffer sb = S.newBuffer();
        sb.a(longEncoder.longToStr(nodeIdProvider.nodeId()))
          .a(longEncoder.longToStr(startIdProvider.startId()))
          .a(longEncoder.longToStr(sequenceProvider.seqId()));
        return sb.toString();
    }

}
