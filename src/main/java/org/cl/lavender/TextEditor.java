package org.cl.lavender;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class TextEditor extends JFrame {

    private static final int CMD = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    private final JTabbedPane tabbedPane;
    private final JLabel statusBar;
    private boolean lineNumbersVisible = true;
    private int fontSize = 14;

    public TextEditor() {
        super("Lavender");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> onTabSwitch());

        statusBar = new JLabel(" Ln 1, Col 1");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        statusBar.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        setJMenuBar(buildMenuBar());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { exitAction(); }
        });

        newTab();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) SwingUtilities.invokeLater(() -> currentTab().textArea.requestFocusInWindow());
    }

    // ── Tab management ───────────────────────────────────────────────────────

    private EditorTab newTab() {
        EditorTab tab = new EditorTab(this::onTabChanged);
        tab.setLineNumbersVisible(lineNumbersVisible);
        tab.setEditorFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        tab.textArea.setComponentPopupMenu(buildContextMenu(tab));
        tab.textArea.addCaretListener(e -> {
            if (tab == currentTab()) updateStatus();
        });

        tabbedPane.addTab(tab.getTitle(), tab);
        int index = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(index, new TabHeader(tab));
        tabbedPane.setSelectedIndex(index);
        tab.textArea.requestFocusInWindow();
        return tab;
    }

    private void closeTab(int index) {
        EditorTab tab = tabAt(index);
        if (!tab.confirmDiscard(this)) return;
        tabbedPane.removeTabAt(index);
        if (tabbedPane.getTabCount() == 0) newTab();
    }

    private EditorTab currentTab() {
        return (EditorTab) tabbedPane.getSelectedComponent();
    }

    private EditorTab tabAt(int index) {
        return (EditorTab) tabbedPane.getComponentAt(index);
    }

    private void refreshTabHeader(EditorTab tab) {
        int i = tabbedPane.indexOfComponent(tab);
        if (i >= 0) ((TabHeader) tabbedPane.getTabComponentAt(i)).refresh();
    }

    // ── Events ───────────────────────────────────────────────────────────────

    private void onTabChanged() {
        EditorTab tab = currentTab();
        if (tab == null) return;
        refreshTabHeader(tab);
        updateTitle();
        updateStatus();
    }

    private void onTabSwitch() {
        updateTitle();
        updateStatus();
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

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

        menu.add(item("New Tab",  KeyEvent.VK_T, CMD, e -> newTab()));
        menu.add(item("Open…",    KeyEvent.VK_O, CMD, e -> openAction()));
        menu.addSeparator();
        menu.add(item("Save",     KeyEvent.VK_S, CMD, e -> saveAction()));
        menu.add(item("Save As…", KeyEvent.VK_S, CMD | InputEvent.SHIFT_DOWN_MASK, e -> saveAsAction()));
        menu.addSeparator();
        menu.add(item("Close Tab",KeyEvent.VK_W, CMD, e -> closeTab(tabbedPane.getSelectedIndex())));
        menu.addSeparator();
        menu.add(item("Exit",     KeyEvent.VK_Q, CMD, e -> exitAction()));
        return menu;
    }

    private JMenu editMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic('E');

        menu.add(item("Undo", KeyEvent.VK_Z, CMD, e -> {
            if (currentTab().undoManager.canUndo()) currentTab().undoManager.undo();
        }));
        menu.add(item("Redo", KeyEvent.VK_Z, CMD | InputEvent.SHIFT_DOWN_MASK, e -> {
            if (currentTab().undoManager.canRedo()) currentTab().undoManager.redo();
        }));
        menu.addSeparator();
        menu.add(item("Cut",        KeyEvent.VK_X, CMD, e -> currentTab().textArea.cut()));
        menu.add(item("Copy",       KeyEvent.VK_C, CMD, e -> currentTab().textArea.copy()));
        menu.add(item("Paste",      KeyEvent.VK_V, CMD, e -> currentTab().textArea.paste()));
        menu.addSeparator();
        menu.add(item("Select All", KeyEvent.VK_A, CMD, e -> currentTab().textArea.selectAll()));
        return menu;
    }

    private JMenu viewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic('V');

        JCheckBoxMenuItem lineNumbers = new JCheckBoxMenuItem("Line Numbers", true);
        lineNumbers.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, CMD | InputEvent.SHIFT_DOWN_MASK));
        lineNumbers.addActionListener(e -> {
            lineNumbersVisible = lineNumbers.isSelected();
            for (int i = 0; i < tabbedPane.getTabCount(); i++)
                tabAt(i).setLineNumbersVisible(lineNumbersVisible);
        });
        menu.add(lineNumbers);

        JCheckBoxMenuItem wrap = new JCheckBoxMenuItem("Line Wrap");
        wrap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, CMD | InputEvent.SHIFT_DOWN_MASK));
        wrap.addActionListener(e -> currentTab().textArea.setLineWrap(wrap.isSelected()));
        menu.add(wrap);

        JMenu fontSizeMenu = new JMenu("Font Size");
        for (int size : new int[]{10, 12, 14, 16, 18, 20, 24}) {
            JMenuItem sizeItem = new JMenuItem(size + "pt");
            sizeItem.addActionListener(e -> {
                fontSize = size;
                for (int i = 0; i < tabbedPane.getTabCount(); i++)
                    tabAt(i).setEditorFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
            });
            fontSizeMenu.add(sizeItem);
        }
        menu.add(fontSizeMenu);

        return menu;
    }

    private JPopupMenu buildContextMenu(EditorTab tab) {
        JPopupMenu popup = new JPopupMenu();
        popup.add(popupItem("Cut",        e -> tab.textArea.cut()));
        popup.add(popupItem("Copy",       e -> tab.textArea.copy()));
        popup.add(popupItem("Paste",      e -> tab.textArea.paste()));
        popup.addSeparator();
        popup.add(popupItem("Select All", e -> tab.textArea.selectAll()));
        popup.addSeparator();
        popup.add(popupItem("Save",       e -> tab.save(this)));
        return popup;
    }

    private static JMenuItem item(String label, int key, int mods, ActionListener al) {
        JMenuItem it = new JMenuItem(label);
        it.setAccelerator(KeyStroke.getKeyStroke(key, mods));
        it.addActionListener(al);
        return it;
    }

    private static JMenuItem popupItem(String label, ActionListener al) {
        JMenuItem it = new JMenuItem(label);
        it.addActionListener(al);
        return it;
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void openAction() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Text files (*.txt, *.md, *.java)", "txt", "md", "java"));
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();

        // Reuse current tab if it's blank and untouched
        EditorTab target = currentTab();
        if (target.dirty || target.file != null || !target.textArea.getText().isEmpty()) {
            target = newTab();
        }
        target.load(this, f);
    }

    private void saveAction()   { currentTab().save(this); }
    private void saveAsAction() { currentTab().saveAs(this); }

    private void exitAction() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (!tabAt(i).confirmDiscard(this)) return;
        }
        dispose();
        System.exit(0);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateTitle() {
        EditorTab tab = currentTab();
        if (tab == null) return;
        setTitle(tab.getTitle() + " — Lavender");
    }

    private void updateStatus() {
        EditorTab tab = currentTab();
        if (tab == null) return;
        try {
            int caret = tab.textArea.getCaretPosition();
            int line  = tab.textArea.getLineOfOffset(caret);
            int col   = caret - tab.textArea.getLineStartOffset(line);
            statusBar.setText(String.format(" Ln %d, Col %d", line + 1, col + 1));
        } catch (Exception ignored) {}
    }

    // ── Tab header with close button ─────────────────────────────────────────

    private class TabHeader extends JPanel {
        private final EditorTab tab;
        private final JLabel label;

        TabHeader(EditorTab tab) {
            super(new FlowLayout(FlowLayout.LEFT, 4, 0));
            setOpaque(false);
            this.tab = tab;

            label = new JLabel(tab.getTitle());
            label.setFont(UIManager.getFont("TabbedPane.font"));
            label.setForeground(UIManager.getColor("TabbedPane.foreground"));

            JButton close = new JButton("×");
            close.setFont(close.getFont().deriveFont(11f));
            close.setPreferredSize(new Dimension(18, 18));
            close.setContentAreaFilled(false);
            close.setBorderPainted(false);
            close.setFocusable(false);
            close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            close.addActionListener(e -> closeTab(tabbedPane.indexOfComponent(tab)));

            add(label);
            add(close);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        void refresh() { label.setText(tab.getTitle()); }
    }
}
