package gg.moonflower.etched.client.radio.stream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gg.moonflower.etched.client.radio.RadioSession;
import gg.moonflower.etched.client.radio.net.RadioHttpTransportImpl;
import gg.moonflower.etched.client.radio.net.RadioNetworkPolicy;
import gg.moonflower.etched.client.radio.source.DirectRadioSourceResolver;
import gg.moonflower.etched.client.radio.source.RadioResolveContext;
import gg.moonflower.etched.client.radio.source.RadioResolveLimits;
import gg.moonflower.etched.client.radio.source.RadioResolvedSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioStreamPipelineTest {

    private final ExecutorService producers = Executors.newFixedThreadPool(2);
    private final ExecutorService decoders = Executors.newFixedThreadPool(2);
    private HttpServer server;
    private URI uri;
    private byte[] mp3;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        try (var fixture = RadioStreamPipelineTest.class.getResourceAsStream(
                "/gg/moonflower/etched/client/radio/audio/mono.mp3")) {
            if (fixture == null) {
                throw new IllegalStateException("Missing MP3 fixture");
            }
            this.mp3 = fixture.readAllBytes();
        }
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/radio", this::serveIcyMp3);
        this.server.start();
        this.uri = URI.create("http://127.0.0.1:" + this.server.getAddress().getPort() + "/radio");
    }

    @AfterEach
    void tearDown() {
        this.server.stop(0);
        this.producers.shutdownNow();
        this.decoders.shutdownNow();
    }

    @Test
    void resolvesBuffersStripsIcyAndDecodesUsingOneGet() throws Exception {
        RadioSession.Attempt attempt = new RadioSession().start(this.uri.toString());
        RadioResolvedSource source = this.resolve(attempt);
        RadioStreamPipeline.Preparation preparation = RadioStreamPipeline.prepare(source,
                attempt.cancellation(), this.producers, this.decoders, true);
        try (preparation) {
            RadioAudioStream audio = preparation.stream().toCompletableFuture().get(5, TimeUnit.SECONDS);
            try (audio) {
                assertEquals(1, audio.getFormat().getChannels());
                assertEquals(22_050.0F, audio.getFormat().getSampleRate());
                assertTrue(drain(audio) > 4_000);
            }
        }
        assertEquals(1, this.requests.get());
    }

    @Test
    void identicalUrlsProduceIndependentBuffersAndDecoders() throws Exception {
        RadioSession.Attempt firstAttempt = new RadioSession().start(this.uri.toString());
        RadioSession.Attempt secondAttempt = new RadioSession().start(this.uri.toString());
        RadioStreamPipeline.Preparation first = RadioStreamPipeline.prepare(
                this.resolve(firstAttempt), firstAttempt.cancellation(), this.producers, this.decoders, true);
        RadioStreamPipeline.Preparation second = RadioStreamPipeline.prepare(
                this.resolve(secondAttempt), secondAttempt.cancellation(), this.producers, this.decoders, true);
        try (first; second) {
            RadioAudioStream firstAudio = first.stream().toCompletableFuture().get(5, TimeUnit.SECONDS);
            RadioAudioStream secondAudio = second.stream().toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertNotSame(firstAudio, secondAudio);
            try (firstAudio; secondAudio) {
                assertTrue(firstAudio.read(512).hasRemaining());
                assertTrue(secondAudio.read(512).hasRemaining());
            }
        }
        assertEquals(2, this.requests.get());
    }

    @Test
    void transferredDecoderOutlivesPreparationLease() throws Exception {
        RadioSession.Attempt attempt = new RadioSession().start(this.uri.toString());
        RadioStreamPipeline.Preparation preparation = RadioStreamPipeline.prepare(
                this.resolve(attempt), attempt.cancellation(), this.producers, this.decoders, true);
        RadioAudioStream audio = preparation.stream().toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertTrue(preparation.transfer(audio));
        preparation.close();

        try (audio) {
            assertTrue(audio.read(512).hasRemaining());
        }
    }

    @Test
    void rejectsSharedProducerAndDecoderExecutor() throws Exception {
        RadioSession.Attempt attempt = new RadioSession().start(this.uri.toString());
        RadioResolvedSource source = this.resolve(attempt);
        try (source) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> RadioStreamPipeline.prepare(source, attempt.cancellation(),
                            this.producers, this.producers, true));
        }
    }

    @Test
    void decoderExecutorRejectionClosesTheBuffer() throws Exception {
        ExecutorService rejecting = Executors.newSingleThreadExecutor();
        rejecting.shutdownNow();
        RadioSession.Attempt attempt = new RadioSession().start(this.uri.toString());
        RadioStreamPipeline.Preparation preparation = RadioStreamPipeline.prepare(
                this.resolve(attempt), attempt.cancellation(), this.producers, rejecting, true);
        try (preparation) {
            org.junit.jupiter.api.Assertions.assertThrows(ExecutionException.class,
                    () -> preparation.stream().toCompletableFuture().get(5, TimeUnit.SECONDS));
            assertEquals(RadioBufferedInputStream.State.CANCELLED, preparation.bufferState());
        }
    }

    private RadioResolvedSource resolve(RadioSession.Attempt attempt) throws Exception {
        RadioNetworkPolicy allowTestServer = ignored -> {
        };
        RadioHttpTransportImpl transport = new RadioHttpTransportImpl(Proxy.NO_PROXY,
                allowTestServer, Duration.ofSeconds(2), Duration.ofSeconds(2), 2);
        return new DirectRadioSourceResolver().resolve(this.uri,
                new RadioResolveContext(transport, allowTestServer, attempt.cancellation(),
                        RadioResolveLimits.DEFAULT));
    }

    private void serveIcyMp3(HttpExchange exchange) throws IOException {
        this.requests.incrementAndGet();
        exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
        exchange.getResponseHeaders().set("icy-metaint", "128");
        byte[] encoded = withIcyMetadata(this.mp3, 128);
        exchange.sendResponseHeaders(200, 0);
        try (exchange; var output = exchange.getResponseBody()) {
            for (int offset = 0; offset < encoded.length; offset += 73) {
                int length = Math.min(73, encoded.length - offset);
                output.write(encoded, offset, length);
                output.flush();
            }
        }
    }

    private static byte[] withIcyMetadata(byte[] audio, int interval) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int offset = 0;
        while (audio.length - offset >= interval) {
            output.write(audio, offset, interval);
            output.write(1);
            output.write("StreamTitle='T';".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
            offset += interval;
        }
        output.write(audio, offset, audio.length - offset);
        return output.toByteArray();
    }

    private static int drain(RadioAudioStream stream) throws IOException {
        int bytes = 0;
        while (true) {
            ByteBuffer output = stream.read(1024);
            if (!output.hasRemaining()) {
                return bytes;
            }
            bytes += output.remaining();
        }
    }
}
