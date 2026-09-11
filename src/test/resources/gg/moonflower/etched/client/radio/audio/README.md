# Test Audio Fixtures

The audio files in this directory contain synthetic sine waves generated from
FFmpeg's `lavfi` source. They do not contain third-party recordings.

Regenerate all fixtures from the repository root with:

```bash
./scripts/generate-test-audio.sh
```

The fixtures intentionally cover mono MP3, stereo VBR MP3 with ID3 metadata,
long stereo MP3, and stereo Ogg/Vorbis. Encoder output can differ between
FFmpeg versions, so run the related test suite after regeneration.

The generation script and generated fixtures are distributed under the same
GPL-3.0-only terms as the Re-Etched test code.
