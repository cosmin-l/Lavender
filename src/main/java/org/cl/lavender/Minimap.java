package org.cl.lavender;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Minimap extends JComponent {

    private static final int WIDTH = 110;

    private final JTextArea   textArea;
    private final JScrollPane scrollPane;
    private BufferedImage     cache;
    private int               cacheW, cacheH, cacheLines;

    public Minimap(JTextArea textArea, JScrollPane scrollPane) {
        this.textArea   = textArea;
        this.scrollPane = scrollPane;
        setPreferredSize(new Dimension(WIDTH, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Rebuild text image on document changes
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { invalidateCache(); }
            public void removeUpdate(DocumentEvent e)  { invalidateCache(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        // Repaint viewport indicator on scroll
        scrollPane.getViewport().addChangeListener(e -> repaint());

        // Rebuild when font size changes (affects line heights / preferred size)
        textArea.addPropertyChangeListener("font", e -> invalidateCache());

        // Click / drag to scroll the main editor
        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { scrollTo(e.getY()); }
            public void mouseDragged(MouseEvent e)  { scrollTo(e.getY()); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, 0);
    }

    // ── Painting ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        g.setColor(ThemeManager.current().minimapBg());
        g.fillRect(0, 0, w, h);

        Element root  = textArea.getDocument().getDefaultRootElement();
        int     lines = root.getElementCount();
        if (lines == 0) return;

        if (cache == null || cacheW != w || cacheH != h || cacheLines != lines) {
            rebuildCache(w, h, root, lines);
        }
        g.drawImage(cache, 0, 0, null);

        // Viewport indicator
        int totalH = textArea.getPreferredSize().height;
        if (totalH > 0) {
            Rectangle view = scrollPane.getViewport().getViewRect();
            int vpY = (int) ((long) view.y * h / totalH);
            int vpH = Math.max(4, (int) ((long) view.height * h / totalH));

            g.setColor(ThemeManager.current().minimapViewportBg());
            g.fillRect(0, vpY, w, vpH);
            g.setColor(ThemeManager.current().minimapViewportFg());
            g.drawLine(0, vpY,       w, vpY);
            g.drawLine(0, vpY + vpH, w, vpY + vpH);
        }
    }

    private void rebuildCache(int w, int h, Element root, int lines) {
        cache      = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        cacheW     = w;
        cacheH     = h;
        cacheLines = lines;

        Graphics2D g = cache.createGraphics();
        g.setColor(ThemeManager.current().minimapBg());
        g.fillRect(0, 0, w, h);

        Document doc = textArea.getDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
            g.dispose();
            return;
        }

        float rowH = (float) h / lines;
        int   dotH = Math.max(1, (int) rowH);

        g.setColor(ThemeManager.current().minimapText());
        for (int i = 0; i < lines; i++) {
            Element el  = root.getElement(i);
            int     s   = el.getStartOffset();
            int     end = Math.min(el.getEndOffset() - 1, text.length());
            int     y   = (int) (i * rowH);
            int     x   = 2;
            for (int j = s; j < end && x < w - 2; j++) {
                char c = text.charAt(j);
                if      (c == '\t') x += 4;
                else if (c == ' ')  x += 2;
                else { g.fillRect(x, y, 1, dotH); x += 2; }
            }
        }
        g.dispose();
    }

    // ── Scrolling ─────────────────────────────────────────────────────────────

    private void scrollTo(int mouseY) {
        int h = getHeight();
        if (h == 0) return;
        int       totalH = textArea.getPreferredSize().height;
        Rectangle view   = scrollPane.getViewport().getViewRect();
        int       target = (int) ((long) mouseY * totalH / h) - view.height / 2;
        scrollPane.getVerticalScrollBar().setValue(Math.max(0, target));
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    public void themeChanged() { invalidateCache(); }

    // ── Visibility ────────────────────────────────────────────────────────────

    private void invalidateCache() {
        cache = null;
        repaint();
    }
}
