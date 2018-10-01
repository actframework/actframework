package testapp.benchmark;

import act.util.IdGenerator;
import org.junit.Ignore;
import org.junit.Test;
import org.osgl.$;
import testapp.TestBase;

import java.util.UUID;

@Ignore
public class CuidBenchmark extends TestBase {

    @Test
    public void cuidVsUuid() {
        final int TIMES = 10000;
        IdGenerator idGenerator = new IdGenerator(4);
        long ts1 = $.ns();
        for (int i = 0; i < TIMES; ++i) {
            idGenerator.genId();
        }
        long ts2 = $.ns();
        ts1 = ts2 - ts1;
        for (int i = 0; i < TIMES; ++i) {
            UUID.randomUUID();
        }
        ts2 = $.ns() - ts2;
        System.out.printf("cuid: %sns\nuuid: %sns\n", ts1 / TIMES, ts2 / TIMES);
        yes(ts2 > ts1);
        System.out.println(idGenerator.genId());
        System.out.println(UUID.randomUUID());
    }
}
