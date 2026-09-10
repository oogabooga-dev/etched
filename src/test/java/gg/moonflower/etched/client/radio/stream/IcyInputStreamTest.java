package gg.moonflower.etched.client.radio.stream;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IcyInputStreamTest {

    @Test
    void stripsFragmentedMetadataBlocksFromAudio() throws Exception {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encoded.write(new byte[]{1, 2, 3});
        encoded.write(1);
        encoded.write("StreamTitle='A';".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        encoded.write(new byte[]{4, 5, 6});
        encoded.write(0);
        encoded.write(new byte[]{7, 8});

        try (IcyInputStream stream = new IcyInputStream(
                new OneByteInputStream(encoded.toByteArray()), 3)) {
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, stream.readAllBytes());
        }
    }

    @Test
    void reportsChangedTitlesAndPreservesAudioBytes() throws Exception {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encoded.write(new byte[]{1, 2});
        writeMetadata(encoded, "StreamUrl='https://example.invalid';StreamTitle='Artist; Track';");
        encoded.write(new byte[]{3, 4});
        writeMetadata(encoded, "StreamTitle='Artist; Track';");
        encoded.write(new byte[]{5, 6});
        writeMetadata(encoded, "StreamTitle='';");
        encoded.write(7);
        List<String> titles = new ArrayList<>();

        try (IcyInputStream stream = new IcyInputStream(
                new OneByteInputStream(encoded.toByteArray()), 2, titles::add)) {
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7}, stream.readAllBytes());
        }

        assertEquals(List.of("Artist; Track", ""), titles);
    }

    @Test
    void sanitizesAndBoundsDisplayedTitle() throws Exception {
        String rawTitle = "\n" + "a".repeat(300) + "\r";
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encoded.write(new byte[]{1, 2});
        writeMetadata(encoded, "StreamTitle='" + rawTitle + "';");
        encoded.write(3);
        List<String> titles = new ArrayList<>();

        try (IcyInputStream stream = new IcyInputStream(
                new ByteArrayInputStream(encoded.toByteArray()), 2, titles::add)) {
            assertArrayEquals(new byte[]{1, 2, 3}, stream.readAllBytes());
        }

        assertEquals(1, titles.size());
        assertEquals("a".repeat(IcyInputStream.MAX_TITLE_CODE_POINTS - 1), titles.get(0));
    }

    @Test
    void malformedMetadataAndListenerFailuresDoNotCorruptAudio() throws Exception {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encoded.write(new byte[]{1, 2});
        writeMetadata(encoded, "StreamTitle='missing delimiter'");
        encoded.write(new byte[]{3, 4});
        writeMetadata(encoded, "StreamTitle='valid';");
        encoded.write(5);
        AtomicInteger callbacks = new AtomicInteger();

        try (IcyInputStream stream = new IcyInputStream(
                new ByteArrayInputStream(encoded.toByteArray()), 2, title -> {
            callbacks.incrementAndGet();
            throw new IllegalStateException("listener failed");
        })) {
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, stream.readAllBytes());
        }

        assertEquals(1, callbacks.get());
    }

    @Test
    void acceptsTheLargestProtocolMetadataBlock() throws Exception {
        String metadata = "StreamTitle='" + "x".repeat(
                IcyInputStream.MAX_METADATA_BYTES - "StreamTitle='';".length()) + "';";
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encoded.write(new byte[]{1, 2});
        writeMetadata(encoded, metadata);
        encoded.write(3);
        List<String> titles = new ArrayList<>();

        try (IcyInputStream stream = new IcyInputStream(
                new OneByteInputStream(encoded.toByteArray()), 2, titles::add)) {
            assertArrayEquals(new byte[]{1, 2, 3}, stream.readAllBytes());
        }

        assertEquals(List.of("x".repeat(IcyInputStream.MAX_TITLE_CODE_POINTS)), titles);
    }

    @Test
    void reportsTruncatedMetadataInsteadOfPassingItToDecoder() throws Exception {
        byte[] encoded = {1, 2, 1, 'x'};
        try (IcyInputStream stream = new IcyInputStream(new ByteArrayInputStream(encoded), 2)) {
            assertEquals(1, stream.read());
            assertEquals(2, stream.read());
            assertThrows(EOFException.class, stream::read);
        }
    }

    @Test
    void reportsMissingLengthByteAtMetadataBoundary() throws Exception {
        try (IcyInputStream stream = new IcyInputStream(
                new ByteArrayInputStream(new byte[]{1, 2}), 2)) {
            assertEquals(1, stream.read());
            assertEquals(2, stream.read());
            assertThrows(EOFException.class, stream::read);
        }
    }

    @Test
    void validatesIntervalAndZeroLengthRead() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> new IcyInputStream(new ByteArrayInputStream(new byte[0]), 0));
        try (IcyInputStream stream = new IcyInputStream(
                new ByteArrayInputStream(new byte[]{1}), 2)) {
            assertEquals(0, stream.read(new byte[1], 0, 0));
        }
    }

    private static void writeMetadata(ByteArrayOutputStream output, String metadata) throws Exception {
        byte[] bytes = metadata.getBytes(StandardCharsets.ISO_8859_1);
        int blocks = (bytes.length + 15) / 16;
        if (blocks > 255) {
            throw new IllegalArgumentException("Metadata fixture is too large");
        }
        output.write(blocks);
        output.write(bytes);
        output.write(new byte[blocks * 16 - bytes.length]);
    }

    private static final class OneByteInputStream extends ByteArrayInputStream {

        private OneByteInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] output, int offset, int length) {
            return super.read(output, offset, Math.min(length, 1));
        }

        @Override
        public synchronized long skip(long count) {
            return super.skip(Math.min(count, 1));
        }
    }
}
