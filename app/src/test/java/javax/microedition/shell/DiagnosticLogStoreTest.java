package javax.microedition.shell;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiagnosticLogStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void rotatesCurrentLogAndKeepsPreviousGeneration() throws Exception {
        File current = new File(temporary.getRoot(), "che779-diag.txt");
        DiagnosticLogStore store = new DiagnosticLogStore(current, 128);
        store.append("first-generation-" + repeat('A', 60));
        store.append("second-generation-" + repeat('B', 60));

        File previous = new File(temporary.getRoot(), "che779-diag.txt.old");
        assertTrue(previous.isFile());
        assertTrue(new String(Files.readAllBytes(previous.toPath()), StandardCharsets.UTF_8)
                .startsWith("first-generation-"));
        assertTrue(new String(Files.readAllBytes(current.toPath()), StandardCharsets.UTF_8)
                .startsWith("second-generation-"));
        assertTrue(current.length() <= 128);
        assertTrue(previous.length() <= 128);
    }

    @Test public void oversizedUtf8EntryIsBoundedAndValid() throws Exception {
        File current = new File(temporary.getRoot(), "diag.txt");
        DiagnosticLogStore store = new DiagnosticLogStore(current, 128);
        store.append(repeat('界', 100));
        byte[] bytes = Files.readAllBytes(current.toPath());
        assertTrue(bytes.length <= 128);
        assertEquals(0, new String(bytes, StandardCharsets.UTF_8).indexOf('界'));
        assertFalse(new String(bytes, StandardCharsets.UTF_8).contains("\uFFFD"));
    }

    private static String repeat(char c, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(c);
        return result.toString();
    }
}
