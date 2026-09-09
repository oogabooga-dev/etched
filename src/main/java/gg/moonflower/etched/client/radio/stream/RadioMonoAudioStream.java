package gg.moonflower.etched.client.radio.stream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Downmixes stereo 16-bit radio PCM for positional playback. */
public final class RadioMonoAudioStream implements RadioAudioStream {

    private final RadioAudioStream source;
    private final AudioFormat sourceFormat;
    private final AudioFormat format;

    public RadioMonoAudioStream(RadioAudioStream source) {
        this.source = Objects.requireNonNull(source, "source");
        this.sourceFormat = source.getFormat();
        int channels = this.sourceFormat.getChannels();
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("Radio PCM must be mono or stereo");
        }
        if (this.sourceFormat.getSampleSizeInBits() != Short.SIZE
                || !AudioFormat.Encoding.PCM_SIGNED.equals(this.sourceFormat.getEncoding())) {
            throw new IllegalArgumentException("Radio PCM must use signed 16-bit samples");
        }
        if (this.sourceFormat.getFrameSize() != channels * Short.BYTES) {
            throw new IllegalArgumentException("Radio PCM must not contain frame padding");
        }
        this.format = channels == 1 ? this.sourceFormat : new AudioFormat(
                this.sourceFormat.getEncoding(), this.sourceFormat.getSampleRate(), Short.SIZE,
                1, Short.BYTES, this.sourceFormat.getFrameRate(), this.sourceFormat.isBigEndian());
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    @Override
    public ByteBuffer read(int requestedBytes) throws IOException {
        if (this.sourceFormat.getChannels() == 1 || requestedBytes <= 0) {
            return this.source.read(requestedBytes);
        }

        int outputBytes = Math.max(Short.BYTES, requestedBytes - requestedBytes % Short.BYTES);
        int sourceBytes = outputBytes > Integer.MAX_VALUE / 2 ? Integer.MAX_VALUE : outputBytes * 2;
        sourceBytes -= sourceBytes % this.sourceFormat.getFrameSize();
        ByteBuffer stereo = this.source.read(sourceBytes);
        if (stereo.remaining() % this.sourceFormat.getFrameSize() != 0) {
            throw new IOException("Stereo radio decoder returned a partial PCM frame");
        }

        ByteOrder order = this.sourceFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        stereo = stereo.slice().order(order);
        ByteBuffer mono = ByteBuffer.allocateDirect(stereo.remaining() / 2).order(order);
        while (stereo.remaining() >= 2 * Short.BYTES) {
            int left = stereo.getShort();
            int right = stereo.getShort();
            mono.putShort((short) ((left + right) / 2));
        }
        mono.flip();
        return mono;
    }

    @Override
    public CompletionStage<Termination> termination() {
        return this.source.termination();
    }

    @Override
    public void close() throws IOException {
        this.source.close();
    }
}
