package org.cl.lavender;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new TextEditor().setVisible(true));
    }
}
