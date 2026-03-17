package org.cl.lavender;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

public class EditorTab extends JPanel {

    final JTextArea textArea;
    final JScrollPane scrollPane;
    final LineNumberGutter gutter;
    final UndoManager undoManager = new UndoManager();
    File file;
    boolean dirty;

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

    void setLineNumbersVisible(boolean visible) {
        scrollPane.setRowHeaderView(visible ? gutter : null);
        scrollPane.revalidate();
    }

    void setEditorFont(Font font) {
        textArea.setFont(font);
    }

    boolean load(Component parent, File f) {
        try {
            textArea.setText(Files.readString(f.toPath()));
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

    boolean save(Component parent) {
        if (file == null) return saveAs(parent);
        return writeFile(parent, file);
    }

    boolean saveAs(Component parent) {
        JFileChooser fc = chooser();
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return false;
        File f = fc.getSelectedFile();
        if (!f.getName().contains(".")) f = new File(f.getPath() + ".txt");
        return writeFile(parent, f);
    }

    boolean confirmDiscard(Component parent) {
        if (!dirty) return true;
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
