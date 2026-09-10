package gg.moonflower.etched.client.radio.stream;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;

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
