package org.cl.lavender;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "Lavender");
        }
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        ThemeManager.set(ThemeManager.loadTheme());
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new TextEditor().setVisible(true));
    }
}
