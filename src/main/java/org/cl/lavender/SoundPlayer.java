package org.cl.lavender;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.prefs.Preferences;

public class SoundPlayer {

    // Large pool so fast typing never needs to stop a still-playing clip.
    // At ~1 s per sound, exhausting 20 clips requires >1200 WPM.
    private static final int POOL_SIZE = 20;
    private static final String SOUND_FILE = "/sounds/key1.wav";
    private static final Preferences PREFS = Preferences.userRoot().node("org/cl/lavender");

    private static volatile boolean enabled;
    private static final Clip[] pool;
    private static int poolIndex;
    private static final Random random = new Random();

    static {
        enabled = PREFS.getBoolean("typewriterSounds", false);
        Clip[] clips = null;
        try {
            // Load audio data once, then share the buffer across all clips.
            AudioFormat format;
            byte[] audioData;
            try (InputStream is = SoundPlayer.class.getResourceAsStream(SOUND_FILE);
                 AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is))) {
                format    = ais.getFormat();
                audioData = ais.readAllBytes();
            }
            clips = new Clip[POOL_SIZE];
            for (int i = 0; i < POOL_SIZE; i++) {
                clips[i] = AudioSystem.getClip();
                clips[i].open(format, audioData, 0, audioData.length);
            }
        } catch (Exception ignored) {
            // Audio unavailable — sounds silently disabled
            clips = null;
        }
        pool = clips;
    }

    static boolean isEnabled() { return enabled; }

    static void setEnabled(boolean on) {
        enabled = on;
        PREFS.putBoolean("typewriterSounds", on);
    }

    static void playKeystroke() {
        if (!enabled || pool == null) return;
        Clip clip = pool[poolIndex++ % POOL_SIZE];
        // Subtle volume variation for a natural feel
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(-random.nextFloat() * 2.0f); // 0 to -2 dB
        }
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }
}
