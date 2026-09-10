package gg.moonflower.etched.client.radio.stream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/** Removes ICY metadata blocks from audio and reports bounded stream titles. */
public final class IcyInputStream extends InputStream {

    static final int MAX_METADATA_BYTES = 255 * 16;
    static final int MAX_TITLE_CODE_POINTS = 256;

    private final InputStream source;
    private final int metadataInterval;
    private final Consumer<String> streamTitleListener;
    private final byte[] metadata = new byte[MAX_METADATA_BYTES];
    private int audioRemaining;
    private boolean hasStreamTitle;
    private String streamTitle;

    public IcyInputStream(InputStream source, int metadataInterval) {
        this(source, metadataInterval, ignored -> {
        });
    }

    public IcyInputStream(InputStream source, int metadataInterval, Consumer<String> streamTitleListener) {
        this.source = Objects.requireNonNull(source, "source");
        if (metadataInterval <= 0) {
            throw new IllegalArgumentException("metadataInterval must be positive");
        }
        this.metadataInterval = metadataInterval;
        this.streamTitleListener = Objects.requireNonNull(streamTitleListener, "streamTitleListener");
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
        int offset = 0;
        while (offset < metadataBytes) {
            int read = this.source.read(this.metadata, offset, metadataBytes - offset);
            if (read < 0) {
                throw new EOFException("ICY metadata block ended unexpectedly");
            }
            if (read == 0) {
                int value = this.source.read();
                if (value < 0) {
                    throw new EOFException("ICY metadata block ended unexpectedly");
                }
                this.metadata[offset++] = (byte) value;
            } else {
                offset += read;
            }
        }
        this.audioRemaining = this.metadataInterval;
        if (metadataBytes > 0) {
            this.reportStreamTitle(metadataBytes);
        }
        return true;
    }

    private void reportStreamTitle(int length) {
        int contentLength = 0;
        while (contentLength < length && this.metadata[contentLength] != 0) {
            contentLength++;
        }
        String title = parseStreamTitle(new String(
                this.metadata, 0, contentLength, StandardCharsets.ISO_8859_1));
        if (title == null || this.hasStreamTitle && title.equals(this.streamTitle)) {
            return;
        }

        this.hasStreamTitle = true;
        this.streamTitle = title;
        try {
            this.streamTitleListener.accept(title);
        } catch (RuntimeException ignored) {
        }
    }

    private static String parseStreamTitle(String metadata) {
        int offset = 0;
        while (offset < metadata.length()) {
            while (offset < metadata.length()
                    && (metadata.charAt(offset) == ';' || Character.isWhitespace(metadata.charAt(offset)))) {
                offset++;
            }
            int keyStart = offset;
            while (offset < metadata.length()) {
                char character = metadata.charAt(offset);
                if (!Character.isLetterOrDigit(character) && character != '-' && character != '_') {
                    break;
                }
                offset++;
            }
            int keyEnd = offset;
            while (offset < metadata.length() && Character.isWhitespace(metadata.charAt(offset))) {
                offset++;
            }
            if (keyStart == keyEnd || offset >= metadata.length() || metadata.charAt(offset) != '=') {
                offset = nextField(metadata, offset);
                continue;
            }
            offset++;
            while (offset < metadata.length() && Character.isWhitespace(metadata.charAt(offset))) {
                offset++;
            }
            if (offset >= metadata.length() || metadata.charAt(offset) != '\'') {
                offset = nextField(metadata, offset);
                continue;
            }

            int valueStart = ++offset;
            int delimiter = -1;
            while (offset < metadata.length()) {
                if (metadata.charAt(offset) == '\'') {
                    int candidate = offset + 1;
                    while (candidate < metadata.length() && Character.isWhitespace(metadata.charAt(candidate))) {
                        candidate++;
                    }
                    if (candidate < metadata.length() && metadata.charAt(candidate) == ';') {
                        delimiter = candidate;
                        break;
                    }
                }
                offset++;
            }
            if (delimiter < 0) {
                return null;
            }
            if (metadata.regionMatches(true, keyStart, "StreamTitle", 0, "StreamTitle".length())
                    && keyEnd - keyStart == "StreamTitle".length()) {
                return sanitizeTitle(metadata.substring(valueStart, offset));
            }
            offset = delimiter + 1;
        }
        return null;
    }

    private static int nextField(String metadata, int offset) {
        int delimiter = metadata.indexOf(';', offset);
        return delimiter < 0 ? metadata.length() : delimiter + 1;
    }

    private static String sanitizeTitle(String title) {
        StringBuilder sanitized = new StringBuilder(Math.min(title.length(), MAX_TITLE_CODE_POINTS));
        int offset = 0;
        int codePoints = 0;
        boolean previousSpace = false;
        while (offset < title.length() && codePoints < MAX_TITLE_CODE_POINTS) {
            int codePoint = title.codePointAt(offset);
            offset += Character.charCount(codePoint);
            codePoints++;
            if (Character.isISOControl(codePoint)) {
                if (!previousSpace) {
                    sanitized.append(' ');
                    previousSpace = true;
                }
            } else {
                sanitized.appendCodePoint(codePoint);
                previousSpace = Character.isWhitespace(codePoint);
            }
        }
        return sanitized.toString().strip();
    }
}
