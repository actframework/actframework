package org.osgl.oms;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestRule;

@Ignore
public class BenchmarkBase extends TestBase {
    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();
}
