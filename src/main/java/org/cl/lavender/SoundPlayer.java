package org.cl.lavender;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.prefs.Preferences;

public class SoundPlayer {

    private static final int POOL_SIZE = 6;
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
            clips = new Clip[POOL_SIZE];
            for (int i = 0; i < POOL_SIZE; i++) {
                String resource = SOUND_FILE;
                try (InputStream is  = SoundPlayer.class.getResourceAsStream(resource);
                     AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is))) {
                    clips[i] = AudioSystem.getClip();
                    clips[i].open(ais);
                }
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
