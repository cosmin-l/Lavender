package org.cl.lavender;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

public class TextEditor extends JFrame {

    private final JTextArea textArea;
    private final JLabel statusBar;
    private File currentFile = null;
    private boolean dirty = false;

    public TextEditor() {
        super("Lavender - New File");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        // Text area
        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setTabSize(4);
        textArea.setLineWrap(false);
        textArea.setMargin(new Insets(4, 6, 4, 6));

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { markDirty(); updateStatus(); }
            public void removeUpdate(DocumentEvent e)  { markDirty(); updateStatus(); }
            public void changedUpdate(DocumentEvent e) { updateStatus(); }
        });

        textArea.addCaretListener(e -> updateStatus());

        // Status bar
        statusBar = new JLabel(" Ln 1, Col 1");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        statusBar.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        textArea.setComponentPopupMenu(buildContextMenu());

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        setJMenuBar(buildMenuBar());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { exitAction(); }
        });
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.add(fileMenu());
        bar.add(editMenu());
        bar.add(viewMenu());
        return bar;
    }

    private JMenu fileMenu() {
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');

        menu.add(item("New",      KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, e -> newAction()));
        menu.add(item("Open…",    KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK, e -> openAction()));
        menu.addSeparator();
        menu.add(item("Save",     KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, e -> saveAction()));
        menu.add(item("Save As…", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, e -> saveAsAction()));
        menu.addSeparator();
        menu.add(item("Exit",     KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK, e -> exitAction()));
        return menu;
    }

    private JMenu editMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic('E');

        menu.add(item("Cut",        KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK, e -> textArea.cut()));
        menu.add(item("Copy",       KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, e -> textArea.copy()));
        menu.add(item("Paste",      KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK, e -> textArea.paste()));
        menu.addSeparator();
        menu.add(item("Select All", KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK, e -> textArea.selectAll()));
        return menu;
    }

    private JMenu viewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic('V');

        JCheckBoxMenuItem wrap = new JCheckBoxMenuItem("Line Wrap");
        wrap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        wrap.addActionListener(e -> textArea.setLineWrap(wrap.isSelected()));
        menu.add(wrap);

        JMenu fontSizeMenu = new JMenu("Font Size");
        for (int size : new int[]{10, 12, 14, 16, 18, 20, 24}) {
            JMenuItem sizeItem = new JMenuItem(size + "pt");
            sizeItem.addActionListener(e -> textArea.setFont(
                    textArea.getFont().deriveFont((float) size)));
            fontSizeMenu.add(sizeItem);
        }
        menu.add(fontSizeMenu);

        return menu;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu popup = new JPopupMenu();
        popup.add(popupItem("Cut",        e -> textArea.cut()));
        popup.add(popupItem("Copy",       e -> textArea.copy()));
        popup.add(popupItem("Paste",      e -> textArea.paste()));
        popup.addSeparator();
        popup.add(popupItem("Select All", e -> textArea.selectAll()));
        popup.addSeparator();
        popup.add(popupItem("Save",       e -> saveAction()));
        return popup;
    }

    private static JMenuItem popupItem(String label, ActionListener al) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(al);
        return item;
    }

    private static JMenuItem item(String label, int key, int mods, ActionListener al) {
        JMenuItem item = new JMenuItem(label);
        item.setAccelerator(KeyStroke.getKeyStroke(key, mods));
        item.addActionListener(al);
        return item;
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    private void newAction() {
        if (!confirmDiscard()) return;
        textArea.setText("");
        currentFile = null;
        dirty = false;
        updateTitle();
    }

    private void openAction() {
        if (!confirmDiscard()) return;
        JFileChooser fc = chooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            textArea.setText(Files.readString(f.toPath()));
            textArea.setCaretPosition(0);
            currentFile = f;
            dirty = false;
            updateTitle();
        } catch (IOException ex) {
            error("Could not open file:\n" + ex.getMessage());
        }
    }

    private void saveAction() {
        if (currentFile == null) { saveAsAction(); return; }
        writeFile(currentFile);
    }

    private void saveAsAction() {
        JFileChooser fc = chooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().contains(".")) f = new File(f.getPath() + ".txt");
        writeFile(f);
    }

    private void exitAction() {
        if (!confirmDiscard()) return;
        dispose();
        System.exit(0);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void writeFile(File f) {
        try {
            Files.writeString(f.toPath(), textArea.getText());
            currentFile = f;
            dirty = false;
            updateTitle();
        } catch (IOException ex) {
            error("Could not save file:\n" + ex.getMessage());
        }
    }

    private boolean confirmDiscard() {
        if (!dirty) return true;
        int choice = JOptionPane.showConfirmDialog(this,
                "You have unsaved changes. Discard them?",
                "Unsaved Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void markDirty() {
        if (!dirty) { dirty = true; updateTitle(); }
    }

    private void updateTitle() {
        String name = currentFile != null ? currentFile.getName() : "New File";
        setTitle((dirty ? "* " : "") + name + " — Lavender");
    }

    private void updateStatus() {
        try {
            int caret = textArea.getCaretPosition();
            int line  = textArea.getLineOfOffset(caret);
            int col   = caret - textArea.getLineStartOffset(line);
            statusBar.setText(String.format(" Ln %d, Col %d", line + 1, col + 1));
        } catch (Exception ignored) {}
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JFileChooser chooser() {
        JFileChooser fc = new JFileChooser(currentFile != null ? currentFile.getParentFile() : null);
        fc.setFileFilter(new FileNameExtensionFilter("Text files (*.txt, *.md, *.java)", "txt", "md", "java"));
        fc.setAcceptAllFileFilterUsed(true);
        return fc;
    }
}
