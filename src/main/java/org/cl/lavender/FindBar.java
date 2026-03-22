package org.cl.lavender;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class FindBar extends JPanel {

    private final JTextArea textArea;
    private final JTextField searchField  = new JTextField(20);
    private final JTextField replaceField = new JTextField(20);
    private final JLabel statusLabel      = new JLabel("  ");
    private final JPanel replaceRow;

    private Highlighter.HighlightPainter matchPainter   =
            new DefaultHighlighter.DefaultHighlightPainter(ThemeManager.current().findMatchColor());
    private Highlighter.HighlightPainter currentPainter =
            new DefaultHighlighter.DefaultHighlightPainter(ThemeManager.current().findCurrentColor());

    private final List<int[]> matches = new ArrayList<>();
    private int currentIndex = -1;

    public FindBar(JTextArea textArea) {
        this.textArea = textArea;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.current().findBorderColor()));

        // ── Find row ─────────────────────────────────────────────────────────
        JPanel findRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));

        JButton closeBtn = smallButton("×");
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> hideBar());

        JButton prevBtn = smallButton("▲");
        JButton nextBtn = smallButton("▼");
        prevBtn.addActionListener(e -> moveToPrev());
        nextBtn.addActionListener(e -> moveToNext());

        findRow.add(closeBtn);
        findRow.add(new JLabel("Find:"));
        findRow.add(searchField);
        findRow.add(prevBtn);
        findRow.add(nextBtn);
        findRow.add(statusLabel);
        add(findRow);

        // ── Replace row ───────────────────────────────────────────────────────
        replaceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));

        JButton replaceBtn    = new JButton("Replace");
        JButton replaceAllBtn = new JButton("Replace All");
        replaceBtn.setFocusable(false);
        replaceAllBtn.setFocusable(false);
        replaceBtn.addActionListener(e -> replaceCurrent());
        replaceAllBtn.addActionListener(e -> replaceAll());

        replaceRow.add(new JLabel("Replace:"));
        replaceRow.add(replaceField);
        replaceRow.add(replaceBtn);
        replaceRow.add(replaceAllBtn);
        add(replaceRow);

        setVisible(false);

        // ── Listeners ─────────────────────────────────────────────────────────
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateMatches(); }
            public void removeUpdate(DocumentEvent e)  { updateMatches(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { if (isVisible()) updateMatches(); }
            public void removeUpdate(DocumentEvent e)  { if (isVisible()) updateMatches(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        searchField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if      (e.getKeyCode() == KeyEvent.VK_ESCAPE) hideBar();
                else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) moveToPrev(); else moveToNext();
                }
            }
        });

        replaceField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) hideBar();
            }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void showBar(boolean withReplace) {
        replaceRow.setVisible(withReplace);
        setVisible(true);
        revalidate();
        SwingUtilities.invokeLater(() -> {
            searchField.requestFocusInWindow();
            searchField.selectAll();
        });
        updateMatches();
    }

    public void hideBar() {
        setVisible(false);
        clearHighlights();
        textArea.requestFocusInWindow();
    }

    public void findNext() { if (isVisible()) moveToNext(); }
    public void findPrev() { if (isVisible()) moveToPrev(); }

    public void applyTheme() {
        Theme t = ThemeManager.current();
        clearHighlights();
        matchPainter   = new DefaultHighlighter.DefaultHighlightPainter(t.findMatchColor());
        currentPainter = new DefaultHighlighter.DefaultHighlightPainter(t.findCurrentColor());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, t.findBorderColor()));
        if (isVisible()) updateMatches();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void updateMatches() {
        matches.clear();
        clearHighlights();
        currentIndex = -1;

        String needle = searchField.getText();
        if (needle.isEmpty()) { statusLabel.setText("  "); return; }

        String haystack = textArea.getText();
        String haystackLc = haystack.toLowerCase();
        String needleLc   = needle.toLowerCase();

        int idx = 0;
        while ((idx = haystackLc.indexOf(needleLc, idx)) >= 0) {
            matches.add(new int[]{idx, idx + needle.length()});
            idx += needle.length();
        }

        if (matches.isEmpty()) {
            statusLabel.setText("No results");
            searchField.setForeground(ThemeManager.current().findErrorColor());
            return;
        }

        searchField.setForeground(UIManager.getColor("TextField.foreground"));

        // Highlight all matches
        Highlighter hl = textArea.getHighlighter();
        for (int[] m : matches) {
            try { hl.addHighlight(m[0], m[1], matchPainter); }
            catch (BadLocationException ignored) {}
        }

        // Move to the match closest to the current caret
        int caret = textArea.getCaretPosition();
        currentIndex = 0;
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i)[0] >= caret) { currentIndex = i; break; }
            currentIndex = i;
        }

        highlightCurrent();
    }

    private void moveToNext() {
        if (matches.isEmpty()) return;
        currentIndex = (currentIndex + 1) % matches.size();
        highlightCurrent();
    }

    private void moveToPrev() {
        if (matches.isEmpty()) return;
        currentIndex = (currentIndex - 1 + matches.size()) % matches.size();
        highlightCurrent();
    }

    private void highlightCurrent() {
        if (currentIndex < 0 || currentIndex >= matches.size()) return;
        int[] m = matches.get(currentIndex);

        // Re-paint all with dim highlight, then current with bright
        clearHighlights();
        Highlighter hl = textArea.getHighlighter();
        for (int[] match : matches) {
            try { hl.addHighlight(match[0], match[1], matchPainter); }
            catch (BadLocationException ignored) {}
        }
        try { hl.addHighlight(m[0], m[1], currentPainter); }
        catch (BadLocationException ignored) {}

        // Scroll to and select current match
        textArea.setCaretPosition(m[1]);
        textArea.select(m[0], m[1]);
        statusLabel.setText(String.format("%d of %d", currentIndex + 1, matches.size()));

        try {
            var view = textArea.modelToView2D(m[0]);
            if (view != null) textArea.scrollRectToVisible(view.getBounds());
        } catch (BadLocationException ignored) {}
    }

    // ── Replace ───────────────────────────────────────────────────────────────

    private void replaceCurrent() {
        if (matches.isEmpty() || currentIndex < 0) return;
        int[] m = matches.get(currentIndex);
        try {
            textArea.getDocument().remove(m[0], m[1] - m[0]);
            textArea.getDocument().insertString(m[0], replaceField.getText(), null);
        } catch (BadLocationException ignored) {}
        // updateMatches() fires automatically via document listener
        moveToNext();
    }

    private void replaceAll() {
        if (matches.isEmpty()) return;
        String replacement = replaceField.getText();
        String needle = searchField.getText();
        if (needle.isEmpty()) return;

        // Replace from end to start to preserve offsets
        List<int[]> snapshot = new ArrayList<>(matches);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            int[] m = snapshot.get(i);
            try {
                textArea.getDocument().remove(m[0], m[1] - m[0]);
                textArea.getDocument().insertString(m[0], replacement, null);
            } catch (BadLocationException ignored) {}
        }
        int count = snapshot.size();
        statusLabel.setText(count + " replaced");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearHighlights() {
        Highlighter hl = textArea.getHighlighter();
        for (Highlighter.Highlight h : hl.getHighlights()) {
            if (h.getPainter() == matchPainter || h.getPainter() == currentPainter)
                hl.removeHighlight(h);
        }
    }

    private static JButton smallButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(26, 22));
        btn.setFocusable(false);
        return btn;
    }
}
