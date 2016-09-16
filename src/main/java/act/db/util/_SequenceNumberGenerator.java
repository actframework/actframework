package act.db.util;

import act.app.DbServiceManager;
import act.conf.AppConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Define the interface that can generate next or retrieve current sequence number
 */
public interface _SequenceNumberGenerator {
    /**
     * Generate next sequence number
     * @param name the name of the sequence
     * @return the next sequence number
     */
    long next(String name);

    /**
     * Returns the current sequence number
     * @param name the name of the sequence
     * @return the current sequence number
     */
    long get(String name);

    /**
     * Configure the sequence generator
     */
    void configure(AppConfig config, DbServiceManager dbManager);

    class InMemorySequenceNumberGenerator implements _SequenceNumberGenerator {

        private ConcurrentMap<String, AtomicLong> seqs = new ConcurrentHashMap<String, AtomicLong>();
        @Override
        public long next(String name) {
            return getSeq(name).getAndIncrement();
        }

        @Override
        public long get(String name) {
            return getSeq(name).get();
        }

        private AtomicLong getSeq(String name) {
            AtomicLong al = seqs.get(name);
            if (null == al) {
                AtomicLong al0 = new AtomicLong(0);
                al = seqs.putIfAbsent(name, al0);
                if (null == al) {
                    al = al0;
                }
            }
            return al;
        }

        @Override
        public void configure(AppConfig config, DbServiceManager dbManager) {
            // do nothing configuration
        }
    }

}
