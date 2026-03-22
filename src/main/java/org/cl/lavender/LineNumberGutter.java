package org.cl.lavender;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import java.awt.*;

public class LineNumberGutter extends JComponent {

    private static final int PADDING = 10;

    private final JTextArea textArea;

    public LineNumberGutter(JTextArea textArea) {
        this.textArea = textArea;
        setFont(textArea.getFont());
        setOpaque(true);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { revalidate(); repaint(); }
            public void removeUpdate(DocumentEvent e)  { revalidate(); repaint(); }
            public void changedUpdate(DocumentEvent e) { revalidate(); repaint(); }
        });

        textArea.addPropertyChangeListener("font", e -> {
            setFont(textArea.getFont());
            revalidate(); repaint();
        });
    }

    @Override
    public Dimension getPreferredSize() {
        int lines = textArea.getDocument().getDefaultRootElement().getElementCount();
        String widest = String.valueOf(lines);
        int width = getFontMetrics(getFont()).stringWidth(widest) + PADDING * 2;
        return new Dimension(width, textArea.getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(ThemeManager.current().gutterBg());
        g.fillRect(0, 0, getWidth(), getHeight());
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Rectangle clip = g.getClipBounds();
        FontMetrics fm = g2.getFontMetrics(getFont());
        int fontAscent = fm.getAscent();
        int lineHeight = textArea.getFontMetrics(textArea.getFont()).getHeight();

        Element root = textArea.getDocument().getDefaultRootElement();
        int startLine = Math.max(0, clip.y / lineHeight);
        int endLine   = Math.min(root.getElementCount() - 1, (clip.y + clip.height) / lineHeight);

        int gutterWidth = getWidth();
        g2.setColor(ThemeManager.current().gutterFg());

        for (int i = startLine; i <= endLine; i++) {
            String label = String.valueOf(i + 1);
            int x = gutterWidth - fm.stringWidth(label) - PADDING;
            int y = i * lineHeight + fontAscent + textArea.getInsets().top;
            g2.drawString(label, x, y);
        }
    }
}
