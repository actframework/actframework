package org.osgl.mvc.server;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import org.junit.Rule;
import org.junit.rules.TestRule;

public class BenchmarkBase extends TestBase {
    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();
}
