package act.xio.undertow;

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

import act.Act;
import act.xio.Network;
import act.xio.NetworkBase;
import act.xio.NetworkHandler;
import io.undertow.UndertowOptions;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.protocol.http.HttpOpenListener;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.xnio.*;
import org.xnio.channels.AcceptingChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Implement {@link Network} using undertow
 */
public class UndertowNetwork extends NetworkBase {

    private static final Logger logger = LogManager.get(UndertowNetwork.class);

    private Xnio xnio;
    private int ioThreads;
    private XnioWorker worker;
    private OptionMap socketOptions;
    private OptionMap serverOptions;
    private List<AcceptingChannel<? extends StreamConnection>> channels;

    @Override
    protected void bootUp() {
        try {
            xnio = Xnio.getInstance(UndertowNetwork.class.getClassLoader());
            worker = createWorker();
            socketOptions = createSocketOptions();
            serverOptions = OptionMap.builder()
                    .set(UndertowOptions.BUFFER_PIPELINED_DATA, true)
                    .set(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
                    .set(UndertowOptions.ALWAYS_SET_DATE, true)
                    .set(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .set(UndertowOptions.NO_REQUEST_TIMEOUT, 60 * 1000)
                    .set(UndertowOptions.ENABLE_STATISTICS, Act.conf().xioStatistics())
                    .getMap();
            channels = new ArrayList<>();
        } catch (Exception e) {
            throw E.unexpected(e, "Error booting up Undertow service: %s", e.getMessage());
        }
    }

    @Override
    protected void setUpClient(NetworkHandler client, int port) throws IOException {
        HttpHandler handler = new ActHttpHandler(client);
        ByteBufferPool buffers = new DefaultByteBufferPool(true, 16 * 1024, -1, 4);
        HttpOpenListener openListener = new HttpOpenListener(buffers, serverOptions);
        openListener.setRootHandler(handler);
        ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ChannelListeners.openListenerAdapter(openListener);

        AcceptingChannel<? extends StreamConnection> server = worker.createStreamConnectionServer(new InetSocketAddress(port), acceptListener, socketOptions);
        server.resumeAccepts();
        channels.add(server);
    }

    @Override
    protected void close() {
        if (null == channels) {
            // not booted yet
            return;
        }
        for (AcceptingChannel<? extends StreamConnection> channel : channels) {
            IO.close(channel);
        }
        channels.clear();
        worker.shutdownNow();
    }

    private XnioWorker createWorker() throws IOException {
        ioThreads = Runtime.getRuntime().availableProcessors() * 2;
        int workerThreads = ioThreads * 8;
        int maxWorkerThreads = Act.conf().xioMaxWorkerThreads();
        if (maxWorkerThreads > 0) {
            workerThreads = Math.min(maxWorkerThreads, workerThreads);
        }
        return xnio.createWorker(OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, ioThreads)
                .set(Options.WORKER_TASK_CORE_THREADS, workerThreads)
                .set(Options.WORKER_TASK_MAX_THREADS, workerThreads)
                .set(Options.CONNECTION_HIGH_WATER, 1000000)
                .set(Options.CONNECTION_LOW_WATER, 1000000)
                .set(Options.TCP_NODELAY, true)
                .set(Options.CORK, true)
                .getMap());
    }

    private OptionMap createSocketOptions() {
        OptionMap socketOptions = OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, ioThreads)
                .set(Options.TCP_NODELAY, true)
                .set(Options.REUSE_ADDRESSES, true)
                .set(Options.BALANCING_TOKENS, 1)
                .set(Options.BALANCING_CONNECTIONS, 2)
                .set(Options.BACKLOG, 10000)
                .getMap();

        return socketOptions;
    }
}
