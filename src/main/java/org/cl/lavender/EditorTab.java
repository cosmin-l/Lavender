package org.cl.lavender;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class EditorTab extends JPanel {

    final JTextArea textArea;
    final JScrollPane scrollPane;
    final LineNumberGutter gutter;
    final FindBar findBar;
    final Minimap minimap;
    final UndoManager undoManager = new UndoManager();
    File file;
    boolean dirty;
    boolean isBinary;

    private static final int MAX_HEX_BYTES = 512 * 1024;

    private final Runnable onChange;

    public EditorTab(Runnable onChange) {
        super(new BorderLayout());
        this.onChange = onChange;

        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setTabSize(4);
        textArea.setLineWrap(false);
        textArea.setMargin(new Insets(4, 6, 4, 6));

        gutter = new LineNumberGutter(textArea);
        scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setRowHeaderView(gutter);
        add(scrollPane, BorderLayout.CENTER);

        minimap = new Minimap(textArea, scrollPane);
        add(minimap, BorderLayout.EAST);

        findBar = new FindBar(textArea);
        add(findBar, BorderLayout.SOUTH);

        textArea.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) {
                // keyTyped fires for printable chars, backspace, enter, tab — skip shortcuts
                if (!e.isControlDown() && !e.isMetaDown() && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
                    SoundPlayer.playKeystroke();
            }
            @Override public void keyPressed(KeyEvent e) {
                // Delete doesn't produce a keyTyped char on all platforms
                if (e.getKeyCode() == KeyEvent.VK_DELETE)
                    SoundPlayer.playKeystroke();
            }
        });

        textArea.getDocument().addUndoableEditListener(undoManager);
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { markDirty(); }
            public void removeUpdate(DocumentEvent e)  { markDirty(); }
            public void changedUpdate(DocumentEvent e) { onChange.run(); }
        });
    }

    String getTitle() {
        String name = file != null ? file.getName() : "New File";
        return dirty ? "* " + name : name;
    }

    void showFind()    { findBar.showBar(false); }
    void showReplace() { findBar.showBar(true); }

    void setLineNumbersVisible(boolean visible) {
        scrollPane.setRowHeaderView(visible ? gutter : null);
        scrollPane.revalidate();
    }

    void setMinimapVisible(boolean visible) {
        minimap.setVisible(visible);
        revalidate();
    }

    void setEditorFont(Font font) {
        textArea.setFont(font);
    }

    void applyTheme() {
        Theme t = ThemeManager.current();
        textArea.setBackground(t.editorBg());
        textArea.setForeground(t.editorFg());
        textArea.setCaretColor(t.editorFg());
        scrollPane.getViewport().setBackground(t.editorBg());
        gutter.repaint();
        minimap.themeChanged();
        findBar.applyTheme();
    }

    boolean load(Component parent, File f) {
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            String text = tryDecodeUtf8(bytes);
            isBinary = (text == null);
            if (isBinary) {
                textArea.setText(formatHexDump(bytes));
                textArea.setEditable(false);
            } else {
                textArea.setText(text);
                textArea.setEditable(true);
            }
            textArea.setCaretPosition(0);
            undoManager.discardAllEdits();
            file = f;
            dirty = false;
            onChange.run();
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    "Could not open file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Returns decoded UTF-8 text, or null if the file should be treated as binary. */
    private static String tryDecodeUtf8(byte[] bytes) {
        int checkLen = Math.min(bytes.length, 8192);
        for (int i = 0; i < checkLen; i++) {
            if (bytes[i] == 0) return null; // null byte → binary
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static String formatHexDump(byte[] bytes) {
        int displayLen = Math.min(bytes.length, MAX_HEX_BYTES);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < displayLen; i += 16) {
            int end    = Math.min(i + 16, displayLen);
            int filled = end - i;
            sb.append(String.format("%08X  ", i));
            for (int j = i; j < end; j++) {
                sb.append(String.format("%02X ", bytes[j] & 0xFF));
                if (j - i == 7) sb.append(' ');
            }
            // Pad so the ASCII column always starts at the same position.
            // A full 16-byte row occupies 8*3 + 1 + 8*3 = 49 hex chars.
            int hexWritten = filled * 3 + (filled >= 8 ? 1 : 0);
            sb.append(" ".repeat(49 - hexWritten));
            sb.append(" |");
            for (int j = i; j < end; j++) {
                byte b = bytes[j];
                sb.append(b >= 0x20 && b < 0x7F ? (char) b : '.');
            }
            sb.append("|\n");
        }
        if (bytes.length > MAX_HEX_BYTES) {
            sb.append(String.format("%n[truncated — showing first %,d of %,d bytes]%n",
                    MAX_HEX_BYTES, bytes.length));
        }
        return sb.toString();
    }

    boolean save(Component parent) {
        if (isBinary) return true;
        if (file == null) return saveAs(parent);
        return writeFile(parent, file);
    }

    boolean saveAs(Component parent) {
        if (isBinary) return true;
        JFileChooser fc = chooser();
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return false;
        File f = fc.getSelectedFile();
        if (!f.getName().contains(".")) f = new File(f.getPath() + ".txt");
        return writeFile(parent, f);
    }

    boolean confirmDiscard(Component parent) {
        if (!dirty || isBinary) return true;
        return JOptionPane.showConfirmDialog(parent,
                "\"" + getTitle() + "\" has unsaved changes. Discard?",
                "Unsaved Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private void markDirty() {
        dirty = true;
        onChange.run();
    }

    private boolean writeFile(Component parent, File f) {
        try {
            Files.writeString(f.toPath(), textArea.getText());
            file = f;
            dirty = false;
            onChange.run();
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    "Could not save file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private JFileChooser chooser() {
        JFileChooser fc = new JFileChooser(file != null ? file.getParentFile() : null);
        fc.setFileFilter(new FileNameExtensionFilter("Text files (*.txt, *.md, *.java)", "txt", "md", "java"));
        fc.setAcceptAllFileFilterUsed(true);
        return fc;
    }
}
