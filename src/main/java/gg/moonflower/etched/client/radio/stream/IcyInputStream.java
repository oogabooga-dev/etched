package gg.moonflower.etched.client.radio.stream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Removes ICY metadata blocks while leaving the audio bytes unchanged. */
public final class IcyInputStream extends InputStream {

    private final InputStream source;
    private final int metadataInterval;
    private int audioRemaining;

    public IcyInputStream(InputStream source, int metadataInterval) {
        this.source = Objects.requireNonNull(source, "source");
        if (metadataInterval <= 0) {
            throw new IllegalArgumentException("metadataInterval must be positive");
        }
        this.metadataInterval = metadataInterval;
        this.audioRemaining = metadataInterval;
    }

    @Override
    public int read() throws IOException {
        if (!this.prepareAudio()) {
            return -1;
        }
        int value = this.source.read();
        if (value >= 0) {
            this.audioRemaining--;
        }
        return value;
    }

    @Override
    public int read(byte[] output, int offset, int length) throws IOException {
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(offset, length, output.length);
        if (length == 0) {
            return 0;
        }
        if (!this.prepareAudio()) {
            return -1;
        }
        int read = this.source.read(output, offset, Math.min(length, this.audioRemaining));
        if (read > 0) {
            this.audioRemaining -= read;
        }
        return read;
    }

    @Override
    public long skip(long count) throws IOException {
        if (count <= 0 || !this.prepareAudio()) {
            return 0;
        }
        long skipped = this.source.skip(Math.min(count, this.audioRemaining));
        if (skipped > 0) {
            this.audioRemaining -= (int) skipped;
        }
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return Math.min(this.audioRemaining, this.source.available());
    }

    @Override
    public void close() throws IOException {
        this.source.close();
    }

    private boolean prepareAudio() throws IOException {
        if (this.audioRemaining > 0) {
            return true;
        }
        int lengthByte = this.source.read();
        if (lengthByte < 0) {
            throw new EOFException("ICY metadata length byte is missing");
        }
        int metadataBytes = lengthByte * 16;
        while (metadataBytes > 0) {
            long skipped = this.source.skip(metadataBytes);
            if (skipped > 0) {
                metadataBytes -= (int) skipped;
                continue;
            }
            if (this.source.read() < 0) {
                throw new EOFException("ICY metadata block ended unexpectedly");
            }
            metadataBytes--;
        }
        this.audioRemaining = this.metadataInterval;
        return true;
    }
}
