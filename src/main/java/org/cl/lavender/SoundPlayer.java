package org.cl.lavender;

import javax.sound.sampled.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

public class SoundPlayer {

    private static final int POOL_SIZE = 6;
    private static final Preferences PREFS = Preferences.userRoot().node("org/cl/lavender");

    private static volatile boolean enabled;
    private static final Clip[] pool;
    private static final AtomicInteger poolIndex = new AtomicInteger();

    static {
        enabled = PREFS.getBoolean("typewriterSounds", false);
        Clip[] clips = null;
        try {
            AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
            byte[] buf = generateClick(fmt);
            clips = new Clip[POOL_SIZE];
            for (int i = 0; i < POOL_SIZE; i++) {
                clips[i] = AudioSystem.getClip();
                clips[i].open(fmt, buf, 0, buf.length);
            }
        } catch (Exception ignored) {
            // Audio unavailable — sounds silently disabled
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
        int i = Math.abs(poolIndex.getAndIncrement() % POOL_SIZE);
        Clip clip = pool[i];
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    // ── Sound synthesis ───────────────────────────────────────────────────────

    private static byte[] generateClick(AudioFormat fmt) {
        int rate     = (int) fmt.getSampleRate();
        int duration = (int) (rate * 0.030); // 30 ms
        byte[] buf   = new byte[duration * 2];
        Random rng   = new Random(0xC0FFEE);

        for (int i = 0; i < duration; i++) {
            double t = (double) i / rate;

            // Sharp noise transient — very fast decay (~3 ms half-life)
            double click = (rng.nextDouble() * 2 - 1) * Math.exp(-t * 300);

            // Low-frequency body resonance (~120 Hz, ~12 ms decay)
            double body = Math.sin(2 * Math.PI * 120 * t) * Math.exp(-t * 80) * 0.35;

            // Softer second transient at 8 ms (key bottom-out)
            double t2     = t - 0.008;
            double click2 = t2 > 0 ? (rng.nextDouble() * 2 - 1) * Math.exp(-t2 * 400) * 0.4 : 0;

            double s      = Math.max(-1.0, Math.min(1.0, (click + body + click2) * 0.65));
            short  sample = (short) (s * Short.MAX_VALUE);
            buf[i * 2]     = (byte)  (sample & 0xFF);
            buf[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buf;
    }
}
