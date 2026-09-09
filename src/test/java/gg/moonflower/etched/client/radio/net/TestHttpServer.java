package gg.moonflower.etched.client.radio.net;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class TestHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final ExecutorService executor;

    TestHttpServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.executor = Executors.newCachedThreadPool();
        this.server.setExecutor(this.executor);
        this.server.start();
    }

    void handle(String path, HttpHandler handler) {
        this.server.createContext(path, handler);
    }

    URI uri(String path) {
        return URI.create("http://" + this.server.getAddress().getHostString() + ":"
                + this.server.getAddress().getPort() + path);
    }

    InetSocketAddress address() {
        return this.server.getAddress();
    }

    @Override
    public void close() {
        this.server.stop(0);
        this.executor.shutdownNow();
    }
}
