package org.cl.lavender;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

public class FileSidebar extends JPanel {

    static final int PREFERRED_WIDTH = 220;

    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final JLabel folderLabel;
    private final JPanel header;
    private final JScrollPane scrollPane;
    private File rootFolder;
    private final Consumer<File> onFileOpen;

    public FileSidebar(Consumer<File> onFileOpen) {
        super(new BorderLayout());
        this.onFileOpen = onFileOpen;
        setPreferredSize(new Dimension(PREFERRED_WIDTH, 0));

        // ── Header ──────────────────────────────────────────────────────────
        header = new JPanel(new BorderLayout(4, 0));
        header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 6));

        folderLabel = new JLabel("No Folder Open");
        folderLabel.setFont(folderLabel.getFont().deriveFont(Font.BOLD, 11f));

        JButton openBtn = new JButton("···");
        openBtn.setFont(openBtn.getFont().deriveFont(11f));
        openBtn.setPreferredSize(new Dimension(32, 20));
        openBtn.setToolTipText("Open Folder");
        openBtn.setContentAreaFilled(false);
        openBtn.setBorderPainted(false);
        openBtn.setFocusable(false);
        openBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openBtn.addActionListener(e -> chooseFolderAction());

        header.add(folderLabel, BorderLayout.CENTER);
        header.add(openBtn, BorderLayout.EAST);

        // ── Tree ────────────────────────────────────────────────────────────
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        tree.setCellRenderer(new FileTreeCellRenderer());
        tree.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tree.setRowHeight(22);

        tree.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            @Override
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent event) {
                DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                ensureChildrenLoaded(node);
            }
            @Override
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent event) {}
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof File f && f.isFile()) {
                    onFileOpen.accept(f);
                }
            }
        });

        scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ── Public API ──────────────────────────────────────────────────────────

    public void setFolder(File folder) {
        rootFolder = folder;
        folderLabel.setText(folder.getName());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(folder);
        loadDirectChildren(root, folder);
        treeModel.setRoot(root);
    }

    public void applyTheme(Theme theme) {
        Color bg = theme.gutterBg();
        Color fg = theme.gutterFg();

        setBackground(bg);
        header.setBackground(bg);
        folderLabel.setForeground(fg);

        tree.setBackground(bg);
        tree.setForeground(fg);
        scrollPane.setBackground(bg);
        scrollPane.getViewport().setBackground(bg);

        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, theme.findBorderColor()));
        repaint();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void chooseFolderAction() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        if (rootFolder != null) fc.setCurrentDirectory(rootFolder.getParentFile());
        Window parent = SwingUtilities.getWindowAncestor(this);
        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            setFolder(fc.getSelectedFile());
        }
    }

    /** Loads direct children of {@code dir} into {@code node}. Subdirectories get a
     *  placeholder child so the expand triangle is shown; real content loads on expand. */
    private void loadDirectChildren(DefaultMutableTreeNode node, File dir) {
        File[] children = dir.listFiles();
        if (children == null || children.length == 0) return;

        Arrays.sort(children, Comparator
                .comparing((File f) -> f.isFile() ? 1 : 0)   // dirs first
                .thenComparing(f -> f.getName().toLowerCase()));

        for (File child : children) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            if (child.isDirectory() && hasChildren(child)) {
                childNode.add(new DefaultMutableTreeNode(null)); // lazy placeholder
            }
            node.add(childNode);
        }
    }

    private static boolean hasChildren(File dir) {
        String[] entries = dir.list();
        return entries != null && entries.length > 0;
    }

    /** Called before a node expands; replaces the lazy placeholder with real children. */
    private void ensureChildrenLoaded(DefaultMutableTreeNode node) {
        if (!(node.getUserObject() instanceof File dir) || !dir.isDirectory()) return;
        if (node.getChildCount() == 0) return;

        // Already loaded if first child is a File
        DefaultMutableTreeNode first = (DefaultMutableTreeNode) node.getChildAt(0);
        if (first.getUserObject() instanceof File) return;

        node.removeAllChildren();
        loadDirectChildren(node, dir);
        treeModel.nodeStructureChanged(node);
    }

    // ── Cell renderer ───────────────────────────────────────────────────────

    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final FileSystemView FSV = FileSystemView.getFileSystemView();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof File f) {
                setText(f.getName());
                try { setIcon(FSV.getSystemIcon(f)); } catch (Exception ignored) {}
            } else {
                setText("");
                setIcon(null);
            }

            if (!selected) {
                setBackground(tree.getBackground());
                setForeground(tree.getForeground());
            }

            return this;
        }
    }
}
