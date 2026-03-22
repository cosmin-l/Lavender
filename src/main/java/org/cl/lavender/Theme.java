package org.cl.lavender;

import java.awt.Color;

public record Theme(
        String name,
        Color editorBg,
        Color editorFg,
        Color gutterBg,
        Color gutterFg,
        Color minimapBg,
        Color minimapText,
        Color minimapViewportBg,
        Color minimapViewportFg,
        Color findMatchColor,
        Color findCurrentColor,
        Color findBorderColor,
        Color findErrorColor
) {}
