package org.cl.lavender;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

class EditorTabTest {

    @TempDir Path tempDir;

    EditorTab tab;
    AtomicInteger changeCount;

    @BeforeEach
    void setUp() {
        changeCount = new AtomicInteger();
        tab = new EditorTab(changeCount::incrementAndGet);
    }

    // ── Title ────────────────────────────────────────────────────────────────

    @Test
    void title_initiallyNewFile() {
        assertEquals("New File", tab.getTitle());
    }

    @Test
    void title_dirtyNoFile_prefixedWithAsterisk() {
        tab.textArea.setText("hello");
        assertEquals("* New File", tab.getTitle());
    }

    @Test
    void title_afterLoad_reflectsFileName() throws IOException {
        File f = tempDir.resolve("notes.txt").toFile();
        Files.writeString(f.toPath(), "content");
        tab.load(null, f);
        assertEquals("notes.txt", tab.getTitle());
    }

    @Test
    void title_dirtyAfterLoad() throws IOException {
        File f = tempDir.resolve("doc.txt").toFile();
        Files.writeString(f.toPath(), "original");
        tab.load(null, f);
        tab.textArea.setText("modified");
        assertEquals("* doc.txt", tab.getTitle());
    }

    // ── Dirty flag ───────────────────────────────────────────────────────────

    @Test
    void notDirty_initially() {
        assertFalse(tab.dirty);
    }

    @Test
    void dirty_afterTyping() {
        tab.textArea.setText("hello");
        assertTrue(tab.dirty);
    }

    @Test
    void dirty_clearedAfterLoad() throws IOException {
        File f = tempDir.resolve("a.txt").toFile();
        Files.writeString(f.toPath(), "hi");
        tab.textArea.setText("changed");
        assertTrue(tab.dirty);
        tab.load(null, f);
        assertFalse(tab.dirty);
    }

    @Test
    void dirty_clearedAfterSave() throws IOException {
        File f = tempDir.resolve("b.txt").toFile();
        tab.file = f;
        tab.textArea.setText("data");
        assertTrue(tab.dirty);
        tab.save(null);
        assertFalse(tab.dirty);
    }

    // ── File I/O ─────────────────────────────────────────────────────────────

    @Test
    void load_setsTextAndFileReference() throws IOException {
        File f = tempDir.resolve("test.txt").toFile();
        Files.writeString(f.toPath(), "hello world");
        boolean result = tab.load(null, f);
        assertTrue(result);
        assertEquals("hello world", tab.textArea.getText());
        assertEquals(f, tab.file);
    }

    @Test
    void save_writesContentToFile() throws IOException {
        File f = tempDir.resolve("out.txt").toFile();
        tab.file = f;
        tab.textArea.setText("saved content");
        tab.save(null);
        assertEquals("saved content", Files.readString(f.toPath()));
    }

    @Test
    void save_overwritesExistingContent() throws IOException {
        File f = tempDir.resolve("overwrite.txt").toFile();
        Files.writeString(f.toPath(), "old");
        tab.file = f;
        tab.textArea.setText("new");
        tab.save(null);
        assertEquals("new", Files.readString(f.toPath()));
    }

    // ── onChange callback ────────────────────────────────────────────────────

    @Test
    void onChange_firedOnTextInsert() {
        int before = changeCount.get();
        tab.textArea.setText("hello");
        assertTrue(changeCount.get() > before);
    }

    @Test
    void onChange_firedOnLoad() throws IOException {
        File f = tempDir.resolve("c.txt").toFile();
        Files.writeString(f.toPath(), "hi");
        int before = changeCount.get();
        tab.load(null, f);
        assertTrue(changeCount.get() > before);
    }

    @Test
    void onChange_firedOnSave() throws IOException {
        File f = tempDir.resolve("d.txt").toFile();
        tab.file = f;
        tab.textArea.setText("x");
        int before = changeCount.get();
        tab.save(null);
        assertTrue(changeCount.get() > before);
    }

    // ── Undo manager ─────────────────────────────────────────────────────────

    @Test
    void undoManager_resetOnLoad() throws IOException {
        File f = tempDir.resolve("undo.txt").toFile();
        Files.writeString(f.toPath(), "content");
        tab.textArea.setText("pre-load text");
        assertTrue(tab.undoManager.canUndo());
        tab.load(null, f);
        assertFalse(tab.undoManager.canUndo(), "undo history should be cleared after load");
    }

    @Test
    void undoManager_canUndoAfterTyping() {
        tab.textArea.setText("typed");
        assertTrue(tab.undoManager.canUndo());
    }

    // ── confirmDiscard ───────────────────────────────────────────────────────

    @Test
    void confirmDiscard_returnsTrueWhenClean() {
        assertFalse(tab.dirty);
        assertTrue(tab.confirmDiscard(null));
    }
}
