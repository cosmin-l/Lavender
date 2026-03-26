package org.cl.lavender;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

public class TextEditor extends JFrame {

    private static final int CMD   = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    private static final Preferences PREFS = Preferences.userRoot().node("org/cl/lavender");

    private final JTabbedPane tabbedPane;
    private final JLabel statusBar;
    private final FileSidebar sidebar;
    private boolean sidebarVisible;
    private boolean lineNumbersVisible = true;
    private boolean minimapVisible     = true;
    private int fontSize = PREFS.getInt("fontSize", 14);

    public TextEditor() {
        super("Lavender");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setAppIcons();
        registerMacAboutHandler();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);
        tabbedPane.addChangeListener(e -> onTabSwitch());
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabbedPane.indexAtLocation(e.getX(), e.getY()) < 0)
                    newTab();
            }
        });

        statusBar = new JLabel(" Ln 1, Col 1");
        statusBar.setOpaque(true);
        statusBar.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        sidebar = new FileSidebar(this::openFileInEditor);
        sidebarVisible = PREFS.getBoolean("sidebarVisible", false);
        if (sidebarVisible) add(sidebar, BorderLayout.WEST);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        setJMenuBar(buildMenuBar());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { exitAction(); }
        });

        newTab();
        applyTheme(ThemeManager.current());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) SwingUtilities.invokeLater(() -> currentTab().textArea.requestFocusInWindow());
    }

    // ── Tab management ───────────────────────────────────────────────────────

    private void setAppIcons() {
        int[] sizes = {16, 32, 64, 128, 256, 512};
        List<Image> icons = new ArrayList<>();
        for (int sz : sizes) {
            var url = getClass().getResource("/icon_" + sz + ".png");
            if (url != null) icons.add(new ImageIcon(url).getImage());
        }
        if (!icons.isEmpty()) setIconImages(icons);
    }

    private void registerMacAboutHandler() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) return;
        Desktop.getDesktop().setAboutHandler(e -> {
            ImageIcon icon = null;
            var url = getClass().getResource("/icon_128.png");
            if (url != null) icon = new ImageIcon(url);

            JLabel nameLabel = new JLabel("Lavender", SwingConstants.CENTER);
            nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

            JLabel versionLabel = new JLabel("Version 1.0", SwingConstants.CENTER);
            versionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(nameLabel);
            panel.add(Box.createVerticalStrut(4));
            panel.add(versionLabel);

            JOptionPane.showMessageDialog(this, panel, "About Lavender",
                    JOptionPane.PLAIN_MESSAGE, icon);
        });
    }

    private EditorTab newTab() {
        EditorTab tab = new EditorTab(this::onTabChanged);
        tab.setLineNumbersVisible(lineNumbersVisible);
        tab.setMinimapVisible(minimapVisible);
        tab.setEditorFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        tab.textArea.setComponentPopupMenu(buildContextMenu(tab));
        tab.textArea.addCaretListener(e -> {
            if (tab == currentTab()) updateStatus();
        });

        tabbedPane.addTab(tab.getTitle(), tab);
        int index = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(index, new TabHeader(tab));
        tabbedPane.setSelectedIndex(index);
        tab.applyTheme();
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
        EditorTab tab = currentTab();
        if (tab != null) tab.textArea.requestFocusInWindow();
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

        menu.add(item("New Tab",      KeyEvent.VK_T, CMD, e -> newTab()));
        menu.add(item("Open…",        KeyEvent.VK_O, CMD, e -> openAction()));
        menu.add(item("Open Folder…", KeyEvent.VK_O, CMD | InputEvent.SHIFT_DOWN_MASK, e -> openFolderAction()));
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
        menu.addSeparator();
        menu.add(item("Find…",          KeyEvent.VK_F, CMD, e -> currentTab().showFind()));
        menu.add(item("Find & Replace…", KeyEvent.VK_H, CMD, e -> currentTab().showReplace()));
        return menu;
    }

    private JMenu viewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic('V');

        JCheckBoxMenuItem sidebarToggle = new JCheckBoxMenuItem("Sidebar", sidebarVisible);
        sidebarToggle.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, CMD));
        sidebarToggle.addActionListener(e -> setSidebarVisible(sidebarToggle.isSelected()));
        menu.add(sidebarToggle);
        menu.addSeparator();

        JCheckBoxMenuItem lineNumbers = new JCheckBoxMenuItem("Line Numbers", true);
        lineNumbers.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, CMD | InputEvent.SHIFT_DOWN_MASK));
        lineNumbers.addActionListener(e -> {
            lineNumbersVisible = lineNumbers.isSelected();
            for (int i = 0; i < tabbedPane.getTabCount(); i++)
                tabAt(i).setLineNumbersVisible(lineNumbersVisible);
        });
        menu.add(lineNumbers);

        JCheckBoxMenuItem minimap = new JCheckBoxMenuItem("Minimap", true);
        minimap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, CMD | InputEvent.SHIFT_DOWN_MASK));
        minimap.addActionListener(e -> {
            minimapVisible = minimap.isSelected();
            for (int i = 0; i < tabbedPane.getTabCount(); i++)
                tabAt(i).setMinimapVisible(minimapVisible);
        });
        menu.add(minimap);

        JCheckBoxMenuItem wrap = new JCheckBoxMenuItem("Line Wrap");
        wrap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, CMD | InputEvent.SHIFT_DOWN_MASK));
        wrap.addActionListener(e -> currentTab().textArea.setLineWrap(wrap.isSelected()));
        menu.add(wrap);

        ButtonGroup fontSizeGroup = new ButtonGroup();
        JMenu fontSizeMenu = new JMenu("Font Size");
        for (int size : new int[]{10, 12, 14, 16, 18, 20, 24}) {
            JRadioButtonMenuItem sizeItem = new JRadioButtonMenuItem(size + "pt", size == fontSize);
            sizeItem.addActionListener(e -> {
                fontSize = size;
                PREFS.putInt("fontSize", fontSize);
                for (int i = 0; i < tabbedPane.getTabCount(); i++)
                    tabAt(i).setEditorFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
            });
            fontSizeGroup.add(sizeItem);
            fontSizeMenu.add(sizeItem);
        }
        menu.add(fontSizeMenu);

        ButtonGroup themeGroup = new ButtonGroup();
        JMenu themeMenu = new JMenu("Theme");
        for (Theme theme : ThemeManager.THEMES) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.name(),
                    theme == ThemeManager.current());
            item.addActionListener(e -> applyTheme(theme));
            themeGroup.add(item);
            themeMenu.add(item);
        }
        menu.add(themeMenu);

        menu.addSeparator();
        JCheckBoxMenuItem sounds = new JCheckBoxMenuItem("Typewriter Sounds", SoundPlayer.isEnabled());
        sounds.addActionListener(e -> SoundPlayer.setEnabled(sounds.isSelected()));
        menu.add(sounds);

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
        ThemeManager.saveTheme();
        dispose();
        System.exit(0);
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private void applyTheme(Theme theme) {
        ThemeManager.set(theme);

        // Tab bar
        UIManager.put("TabbedPane.background",        theme.gutterBg());
        UIManager.put("TabbedPane.selectedBackground",theme.editorBg());
        UIManager.put("TabbedPane.foreground",        theme.editorFg());
        UIManager.put("TabbedPane.hoverBackground",   theme.editorBg());
        tabbedPane.updateUI();
        // updateUI's installColors() skips setBackground if already set — override explicitly
        tabbedPane.setBackground(theme.gutterBg());
        // FlatLaf only fills the union of tab rects; empty strip area falls through to the
        // content pane — theme it so the full-width tab bar looks correct
        getContentPane().setBackground(theme.gutterBg());
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTabComponentAt(i) instanceof TabHeader th) th.refreshTheme();
        }

        // Status bar
        statusBar.setBackground(theme.gutterBg());
        statusBar.setForeground(theme.gutterFg());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, theme.findBorderColor()),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        // Editor tabs
        for (int i = 0; i < tabbedPane.getTabCount(); i++)
            tabAt(i).applyTheme();

        // Sidebar
        sidebar.applyTheme(theme);
    }

    private void openFolderAction() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        sidebar.setFolder(fc.getSelectedFile());
        if (!sidebarVisible) setSidebarVisible(true);
    }

    private void openFileInEditor(File file) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (file.equals(tabAt(i).file)) { tabbedPane.setSelectedIndex(i); return; }
        }
        EditorTab target = currentTab();
        if (target.dirty || target.file != null || !target.textArea.getText().isEmpty()) {
            target = newTab();
        }
        target.load(this, file);
    }

    private void setSidebarVisible(boolean visible) {
        sidebarVisible = visible;
        PREFS.putBoolean("sidebarVisible", visible);
        if (visible) {
            add(sidebar, BorderLayout.WEST);
        } else {
            remove(sidebar);
        }
        revalidate();
        repaint();
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

        void refreshTheme() {
            label.setForeground(UIManager.getColor("TabbedPane.foreground"));
        }
    }
}
