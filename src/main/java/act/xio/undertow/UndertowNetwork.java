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
import act.conf.AppConfig;
import act.controller.meta.ActionMethodMetaInfo;
import act.ws.WebSocketConnectionManager;
import act.xio.Network;
import act.xio.NetworkBase;
import act.xio.NetworkHandler;
import act.xio.WebSocketConnectionHandler;
import io.undertow.UndertowOptions;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.protocol.http.HttpOpenListener;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.xnio.*;
import org.xnio.channels.AcceptingChannel;
import org.xnio.ssl.SslConnection;
import org.xnio.ssl.XnioSsl;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.*;

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
    public void bootUp() {
        try {
            xnio = Xnio.getInstance(UndertowNetwork.class.getClassLoader());
            // abcdefgdgd1234566789(dddd)
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
    protected void setUpClient(NetworkHandler client, int port, boolean secure) throws IOException {
        HttpHandler handler = new ActHttpHandler(client);
        ByteBufferPool buffers = new DefaultByteBufferPool(true, 16 * 1024, -1, 4);
        HttpOpenListener openListener = new HttpOpenListener(buffers, serverOptions);
        openListener.setRootHandler(handler);
        ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ChannelListeners.openListenerAdapter(openListener);

        if (Act.isTest()) {
            debug("Try clearing random server socket: " + port);
            AppConfig.clearRandomServerSocket(port);
        }
        if (!secure) {
            AcceptingChannel<? extends StreamConnection> server = worker.createStreamConnectionServer(new InetSocketAddress(port), acceptListener, socketOptions);
            server.resumeAccepts();
            channels.add(server);
        } else {
            XnioSsl xnioSsl;
            try {
                SSLContext sslContext = createSSLContext(loadKeyStore("server.keystore"), loadKeyStore("server.truststore"));
                xnioSsl = new UndertowXnioSsl(xnio, OptionMap.create(Options.USE_DIRECT_BUFFERS, true), sslContext);
                AcceptingChannel<SslConnection> sslServer = xnioSsl.createSslConnectionServer(worker, new InetSocketAddress(port), (ChannelListener)acceptListener, socketOptions);
                sslServer.resumeAccepts();
                channels.add(sslServer);
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
    }

    @Override
    protected WebSocketConnectionHandler internalCreateWsConnHandler(ActionMethodMetaInfo methodInfo, WebSocketConnectionManager manager) {
        return new UndertowWebSocketConnectionHandler(methodInfo, manager);
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
        ioThreads = Act.isDev() ? 2 : Runtime.getRuntime().availableProcessors() * 2;
        int workerThreads = Act.isDev() ? 4 : ioThreads * 8;
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
                .getMap()
        );
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

    private static final char[] STORE_PASSWORD = "password".toCharArray();

    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password("key"));
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

    private static KeyStore loadKeyStore(String name) {
        String storeLoc = System.getProperty(name);
        final InputStream stream;
        if (storeLoc == null) {
            stream = UndertowNetwork.class.getResourceAsStream(name);
        } else {
            try {
                stream = Files.newInputStream(Paths.get(storeLoc));
            } catch (IOException e) {
                throw E.ioException(e);
            }
        }

        if (stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try (InputStream is = stream) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, password(name));
            return loadedKeystore;
        } catch (IOException e) {
            throw E.ioException(e);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }


    static char[] password(String name) {
        String pw = System.getProperty(name + ".password");
        return pw != null ? pw.toCharArray() : STORE_PASSWORD;
    }

}
