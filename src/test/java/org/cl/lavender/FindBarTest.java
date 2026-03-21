package org.cl.lavender;

import org.junit.jupiter.api.*;

import javax.swing.*;
import java.lang.reflect.*;

import static org.junit.jupiter.api.Assertions.*;

class FindBarTest {

    JTextArea textArea;
    FindBar findBar;
    JTextField searchField;
    JTextField replaceField;
    JLabel statusLabel;

    @BeforeEach
    void setUp() throws Exception {
        textArea = new JTextArea();
        findBar = new FindBar(textArea);
        searchField  = privateField("searchField");
        replaceField = privateField("replaceField");
        statusLabel  = privateField("statusLabel");
    }

    @SuppressWarnings("unchecked")
    <T> T privateField(String name) throws Exception {
        Field f = FindBar.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(findBar);
    }

    void invokePrivate(String name) {
        try {
            Method m = FindBar.class.getDeclaredMethod(name);
            m.setAccessible(true);
            m.invoke(findBar);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Triggers a search by setting the search field text (fires document listener). */
    void search(String query) {
        searchField.setText(query);
    }

    // ── Basic search ─────────────────────────────────────────────────────────

    @Test
    void emptyQuery_emptyStatus() {
        findBar.showBar(false);
        assertEquals("  ", statusLabel.getText());
    }

    @Test
    void noMatch_noResultsStatus() {
        textArea.setText("hello world");
        findBar.showBar(false);
        search("xyz");
        assertEquals("No results", statusLabel.getText());
    }

    @Test
    void singleMatch_oneOfOne() {
        textArea.setText("hello world");
        findBar.showBar(false);
        search("hello");
        assertEquals("1 of 1", statusLabel.getText());
    }

    @Test
    void multipleMatches_correctCount() {
        textArea.setText("aaa");
        findBar.showBar(false);
        search("a");
        assertEquals("1 of 3", statusLabel.getText());
    }

    @Test
    void search_isCaseInsensitive() {
        textArea.setText("Hello HELLO hello");
        findBar.showBar(false);
        search("hello");
        assertTrue(statusLabel.getText().endsWith("of 3"),
                "expected 3 matches but got: " + statusLabel.getText());
    }

    @Test
    void match_selectsCorrectText() {
        textArea.setText("foo bar");
        findBar.showBar(false);
        search("foo");
        assertEquals("foo", textArea.getSelectedText());
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    @Test
    void findNext_advancesToSecondMatch() {
        textArea.setText("ab ab ab");
        findBar.showBar(false);
        search("ab");
        assertEquals("1 of 3", statusLabel.getText());
        findBar.findNext();
        assertEquals("2 of 3", statusLabel.getText());
    }

    @Test
    void findNext_wrapsAroundToFirst() {
        textArea.setText("x x");
        findBar.showBar(false);
        search("x");
        findBar.findNext(); // 2 of 2
        assertEquals("2 of 2", statusLabel.getText());
        findBar.findNext(); // wraps → 1 of 2
        assertEquals("1 of 2", statusLabel.getText());
    }

    @Test
    void findPrev_goesBackOnMatch() {
        textArea.setText("y y y");
        findBar.showBar(false);
        search("y");
        findBar.findNext(); // 2 of 3
        findBar.findPrev(); // back to 1 of 3
        assertEquals("1 of 3", statusLabel.getText());
    }

    @Test
    void findPrev_wrapsAroundToLast() {
        textArea.setText("z z");
        findBar.showBar(false);
        search("z");
        // Initially at 1 of 2
        findBar.findPrev(); // wraps → 2 of 2
        assertEquals("2 of 2", statusLabel.getText());
    }

    @Test
    void findNext_noOpWhenBarHidden() {
        textArea.setText("test");
        findBar.showBar(false);
        search("test");
        findBar.hideBar();
        findBar.findNext(); // should do nothing (bar hidden)
        // No exception, no state change
    }

    // ── Replace ───────────────────────────────────────────────────────────────

    @Test
    void replaceCurrent_replacesSingleOccurrence() {
        textArea.setText("foo bar foo");
        findBar.showBar(true);
        search("foo");
        replaceField.setText("baz");
        invokePrivate("replaceCurrent");
        String text = textArea.getText();
        assertTrue(text.contains("baz"), "replacement not applied: " + text);
        assertTrue(text.contains("foo"), "only one occurrence should be replaced: " + text);
    }

    @Test
    void replaceAll_replacesAllOccurrences() {
        textArea.setText("cat cat cat");
        findBar.showBar(true);
        search("cat");
        replaceField.setText("dog");
        invokePrivate("replaceAll");
        assertEquals("dog dog dog", textArea.getText());
    }

    @Test
    void replaceAll_preservesOffsetsWithLongerReplacement() {
        textArea.setText("aa bb aa");
        findBar.showBar(true);
        search("aa");
        replaceField.setText("xxxx");
        invokePrivate("replaceAll");
        assertEquals("xxxx bb xxxx", textArea.getText());
    }

    @Test
    void replaceAll_preservesOffsetsWithShorterReplacement() {
        textArea.setText("hello world hello");
        findBar.showBar(true);
        search("hello");
        replaceField.setText("hi");
        invokePrivate("replaceAll");
        assertEquals("hi world hi", textArea.getText());
    }

    @Test
    void replaceAll_noMatchIsNoOp() {
        textArea.setText("original");
        findBar.showBar(true);
        search("notfound");
        replaceField.setText("replacement");
        invokePrivate("replaceAll");
        assertEquals("original", textArea.getText());
    }
}
