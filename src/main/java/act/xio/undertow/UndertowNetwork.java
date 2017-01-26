package act.xio.undertow;

import act.app.App;
import act.conf.AppConfig;
import act.xio.Network;
import act.xio.NetworkBase;
import act.xio.NetworkHandler;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.protocol.http.HttpOpenListener;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.xnio.*;
import org.xnio.channels.AcceptingChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Implement {@link Network} using undertow
 */
public class UndertowNetwork extends NetworkBase {

    private static final Logger logger = L.get(UndertowNetwork.class);

    private Xnio xnio;
    private int ioThreads;
    private XnioWorker worker;
    private int buffersPerRegion;
    private Pool<ByteBuffer> buffers;
    private boolean directBuffers;
    private OptionMap socketOptions;
    private OptionMap undertowOptions;
    private List<AcceptingChannel<? extends StreamConnection>> channels;

    @Override
    protected void bootUp() {
        try {
            xnio = Xnio.getInstance();
            worker = createWorker();
            buffers = createBuffer();
            socketOptions = createSocketOptions();
            undertowOptions = OptionMap.builder().set(UndertowOptions.BUFFER_PIPELINED_DATA, true).getMap();
            channels = C.newList();
        } catch (Exception e) {
            throw E.unexpected(e, "Error booting up Undertow service: %s", e.getMessage());
        }
    }

    @Override
    protected void setUpClient(NetworkHandler client, int port) throws IOException {

        HttpHandler handler = new ActHttpHandler(client);
        HttpOpenListener openListener = new HttpOpenListener(buffers, undertowOptions);
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
        ioThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2);
        int workerThreads = ioThreads * 8;
        return xnio.createWorker(OptionMap.builder().set(Options.WORKER_IO_THREADS, ioThreads)
                .set(Options.WORKER_TASK_CORE_THREADS, workerThreads)
                .set(Options.WORKER_TASK_MAX_THREADS, workerThreads)
                .set(Options.TCP_NODELAY, true).getMap());
    }

    private Pool<ByteBuffer> createBuffer() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        int bufferSize;
        //smaller than 64mb of ram we use 512b buffers
        if (maxMemory < 64 * 1024 * 1024) {
            //use 512b buffers
            directBuffers = false;
            bufferSize = 512;
            buffersPerRegion = 10;
        } else if (maxMemory < 128 * 1024 * 1024) {
            //use 1k buffers
            directBuffers = true;
            bufferSize = 1024;
            buffersPerRegion = 10;
        } else {
            //use 16k buffers for best performance
            //as 16k is generally the max amount of data that can be sent in a single write() call
            directBuffers = true;
            bufferSize = 1024 * 16;
            buffersPerRegion = 20;
        }
        return new ByteBufferSlicePool(directBuffers ? BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR : BufferAllocator.BYTE_BUFFER_ALLOCATOR, bufferSize, bufferSize * buffersPerRegion);
    }

    private OptionMap createSocketOptions() {
        OptionMap socketOptions = OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, ioThreads)
                .set(Options.TCP_NODELAY, true)
                .set(Options.REUSE_ADDRESSES, true)
                .set(Options.BALANCING_TOKENS, 1)
                .set(Options.BALANCING_CONNECTIONS, 2)
                .getMap();
        return socketOptions;
    }
}
